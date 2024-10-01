package io.github.apace100.origins.origin;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.apace100.apoli.util.PrioritizedEntry;
import io.github.apace100.calio.CalioServer;
import io.github.apace100.calio.data.IdentifiableMultiJsonDataLoader;
import io.github.apace100.calio.data.MultiJsonDataContainer;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.integration.CarpetIntegration;
import io.github.apace100.origins.integration.OriginDataLoadedCallback;
import io.github.apace100.origins.networking.packet.s2c.OpenChooseOriginScreenS2CPacket;
import io.github.apace100.origins.networking.packet.s2c.SyncOriginLayerRegistryS2CPacket;
import io.github.apace100.origins.registry.ModComponents;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class OriginLayerManager extends IdentifiableMultiJsonDataLoader implements IdentifiableResourceReloadListener {

    public static final Identifier PHASE = Origins.identifier("phase/origin_layers");
    public static final Codec<Identifier> VALIDATING_CODEC = Identifier.CODEC.comapFlatMap(
        id -> contains(id)
            ? DataResult.success(id)
            : DataResult.error(() -> "Could not get layer from id '" + id + "', as it doesn't exist!"),
        id -> id
    );

    public static final PacketCodec<ByteBuf, OriginLayer> DISPATCH_PACKET_CODEC = Identifier.PACKET_CODEC.xmap(OriginLayerManager::get, OriginLayer::getId);
    public static final Codec<OriginLayer> DISPATCH_CODEC = Identifier.CODEC.comapFlatMap(OriginLayerManager::getResult, OriginLayer::getId);

    private static final Map<Identifier, Integer> LOADING_PRIORITIES = new HashMap<>();
    private static final Map<Identifier, OriginLayer> LAYERS = new HashMap<>();

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();

    public OriginLayerManager() {
        super(GSON, "origin_layers", ResourceType.SERVER_DATA);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.addPhaseOrdering(OriginManager.PHASE, PHASE);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(PHASE, (player, joined) -> {

            OriginComponent component = ModComponents.ORIGIN.get(player);
            getLayers().stream()
                .filter(OriginLayer::isEnabled)
                .filter(Predicate.not(component::hasOrigin))
                .forEach(layer -> component.setOrigin(layer, Origin.EMPTY));

            ServerPlayNetworking.send(player, new SyncOriginLayerRegistryS2CPacket(LAYERS));
            updateData(player, joined);

        });
    }

    private void updateData(ServerPlayerEntity player, boolean init) {

        OriginComponent component = ModComponents.ORIGIN.get(player);
        RegistryOps<JsonElement> jsonOps = player.getRegistryManager().getOps(JsonOps.INSTANCE);

        boolean mismatch = false;

        for (Map.Entry<OriginLayer, Origin> entry : component.getOrigins().entrySet()) {

            OriginLayer oldLayer = entry.getKey();
            OriginLayer newLayer = OriginLayerManager.getNullable(oldLayer.getId());

            Origin oldOrigin = entry.getValue();
            Origin newOrigin = OriginRegistry.getNullable(oldOrigin.getId());

            if (oldOrigin != Origin.EMPTY) {

                if (newLayer == null) {
                    Origins.LOGGER.error("Removed unregistered origin layer \"{}\" from player {}!", oldLayer.getId(), player.getName().getString());
                    component.removeLayer(oldLayer);
                }

                else if (!newLayer.contains(oldOrigin) || newOrigin == null) {
                    Origins.LOGGER.error("Removed unregistered origin \"{}\" from origin layer \"{}\" from player {}!", oldOrigin.getId(), oldLayer.getId(), player.getName().getString());
                    component.setOrigin(newLayer, Origin.EMPTY);
                }

                else {

                    JsonElement oldOriginJson = Origin.DATA_TYPE.write(jsonOps, oldOrigin).getOrThrow(JsonParseException::new);
                    JsonElement newOriginJson = Origin.DATA_TYPE.write(jsonOps, newOrigin).getOrThrow(JsonParseException::new);

                    if (oldOriginJson.equals(newOriginJson)) {
                        continue;
                    }

                    Origins.LOGGER.warn("Mismatched data fields of origin \"{}\" from player {}! Updating...", oldOrigin.getId(), player.getName().getString());
                    mismatch = true;

                    component.setOrigin(newLayer, newOrigin);

                }

            }

        }

        if (mismatch) {
            Origins.LOGGER.info("Finished updating origin data of player {}!", player.getName().getString());
        }

        OriginComponent.sync(player);
        if (component.hasAllOrigins()) {
            return;
        }

        if (component.checkAutoChoosingLayers(player, true)) {
            component.sync();
        }

        if (!init) {
            return;
        }

        if (component.hasAllOrigins()) {
            OriginComponent.onChosen(player, false);
        }

        else if (!CarpetIntegration.isPlayerFake(player)) {

            component.selectingOrigin(true);
            component.sync();

            ServerPlayNetworking.send(player, new OpenChooseOriginScreenS2CPacket(true));

        }

        else {
            component.sync();
        }

    }

    @Override
    protected void apply(MultiJsonDataContainer prepared, ResourceManager manager, Profiler profiler) {

        Origins.LOGGER.info("Reading origin layers from data packs...");

        LOADING_PRIORITIES.clear();
        clear();

        DynamicRegistryManager dynamicRegistries = CalioServer.getDynamicRegistries().orElse(null);
        if (dynamicRegistries == null) {
            Origins.LOGGER.error("Can't read origin layers from data packs without access to dynamic registries!");
            return;
        }

        Map<Identifier, List<PrioritizedEntry<OriginLayer>>> loadedLayers = new HashMap<>();
        Origins.LOGGER.info("Reading origin layers from data packs...");

        prepared.forEach((packName, id, jsonElement) -> {

            try {

                SerializableData.CURRENT_NAMESPACE = id.getNamespace();
                SerializableData.CURRENT_PATH = id.getPath();

                if (!(jsonElement instanceof JsonObject jsonObject)) {
                    throw new JsonSyntaxException("Not a JSON object: " + jsonElement);
                }

                jsonObject.addProperty("id", id.toString());

                OriginLayer layer = OriginLayer.DATA_TYPE.read(dynamicRegistries.getOps(JsonOps.INSTANCE), jsonObject).getOrThrow();
                int currLoadingPriority = JsonHelper.getInt(jsonObject, "loading_priority", 0);

                PrioritizedEntry<OriginLayer> entry = new PrioritizedEntry<>(layer, currLoadingPriority);
                int prevLoadingPriority = LOADING_PRIORITIES.getOrDefault(id, Integer.MIN_VALUE);

                if (layer.shouldReplace() && currLoadingPriority <= prevLoadingPriority) {
                    Origins.LOGGER.warn("Ignoring origin layer \"{}\" with 'replace' set to true from data pack [{}]. Its loading priority ({}) must be higher than {} to replace the origin layer!", id, packName, currLoadingPriority, prevLoadingPriority);
                }

                else {

                    if (layer.shouldReplace()) {
                        Origins.LOGGER.info("Origin layer \"{}\" has been replaced by data pack [{}]!", id, packName);
                    }

                    List<String> invalidOrigins = layer.getConditionedOrigins()
                        .stream()
                        .map(OriginLayer.ConditionedOrigin::origins)
                        .flatMap(Collection::stream)
                        .filter(Predicate.not(OriginRegistry::contains))
                        .map(Identifier::toString)
                        .toList();

                    if (!invalidOrigins.isEmpty()) {
                        Origins.LOGGER.error("Origin layer \"{}\" contained {} invalid origin(s): {}", id, invalidOrigins.size(), String.join(", ", invalidOrigins));
                    }

                    loadedLayers.computeIfAbsent(id, k -> new LinkedList<>()).add(entry);
                    LOADING_PRIORITIES.put(id, currLoadingPriority);

                }

            }

            catch (Exception e) {
                Origins.LOGGER.error("There was a problem reading origin layer \"{}\": {}", id, e.getMessage());
            }

        });

        Origins.LOGGER.info("Finished reading {} origin layer(s). Merging similar origin layers...", loadedLayers.size());
        loadedLayers.forEach((id, entries) -> {

            AtomicReference<OriginLayer> currentLayer = new AtomicReference<>();
            entries.sort(Comparator.comparing(PrioritizedEntry::priority));

            for (PrioritizedEntry<OriginLayer> entry : entries) {

                if (currentLayer.get() == null) {
                    currentLayer.set(entry.value());
                }

                else {
                    currentLayer.accumulateAndGet(entry.value(), OriginLayerManager::merge);
                }

            }

            register(id, currentLayer.get());

        });

        Origins.LOGGER.info("Finished merging similar origin layers. Registry contains {} origin layers.", size());
        OriginDataLoadedCallback.EVENT.invoker().onDataLoaded(false);

        SerializableData.CURRENT_NAMESPACE = null;
        SerializableData.CURRENT_PATH = null;

        LOADING_PRIORITIES.clear();

    }

    private static OriginLayer merge(OriginLayer oldLayer, OriginLayer newLayer) {

        Set<OriginLayer.ConditionedOrigin> origins = new ObjectLinkedOpenHashSet<>(oldLayer.getConditionedOrigins());
        Set<Identifier> originsExcludedFromRandom = new ObjectLinkedOpenHashSet<>(oldLayer.getOriginsExcludedFromRandom());

        Text name = oldLayer.getName();
        OriginLayer.GuiTitle guiTitle = oldLayer.getGuiTitle();

        Text missingName = oldLayer.getMissingName();
        Text missingDescription = oldLayer.getMissingDescription();

        Identifier defaultOrigin = oldLayer.getDefaultOrigin();

        boolean randomAllowed = oldLayer.isRandomAllowed();
        boolean unchoosableRandomAllowed = oldLayer.isUnchoosableRandomAllowed();

        boolean autoChoose = oldLayer.shouldAutoChoose();
        boolean enabled = oldLayer.isEnabled();
        boolean hidden = oldLayer.isHidden();

        int order = oldLayer.getOrder();

        if (newLayer.shouldReplace()) {

            origins.clear();
            originsExcludedFromRandom.clear();

            name = newLayer.getName();
            guiTitle = newLayer.getGuiTitle();

            missingName = newLayer.getMissingName();
            missingDescription = newLayer.getMissingDescription();

            defaultOrigin = newLayer.getDefaultOrigin();

            randomAllowed = newLayer.isRandomAllowed();
            unchoosableRandomAllowed = newLayer.isUnchoosableRandomAllowed();

            autoChoose = newLayer.shouldAutoChoose();
            enabled = newLayer.isEnabled();
            hidden = newLayer.isHidden();

            order = newLayer.getOrder();

        }

        else {

            if (newLayer.shouldReplaceOrigins()) {
                origins.clear();
            }

            if (newLayer.shouldReplaceExcludedOriginsFromRandom()) {
                originsExcludedFromRandom.clear();
            }

        }

        origins.addAll(newLayer.getConditionedOrigins());
        originsExcludedFromRandom.addAll(newLayer.getOriginsExcludedFromRandom());

        return new OriginLayer(
            newLayer.getId(),
            order,
            origins,
            newLayer.shouldReplaceOrigins(),
            newLayer.shouldReplace(),
            enabled,
            name,
            guiTitle,
            missingName,
            missingDescription,
            randomAllowed,
            unchoosableRandomAllowed,
            originsExcludedFromRandom,
            newLayer.shouldReplaceExcludedOriginsFromRandom(),
            defaultOrigin,
            autoChoose,
            hidden
        );

    }

    public static DataResult<OriginLayer> getResult(Identifier id) {
        return LAYERS.containsKey(id)
            ? DataResult.success(LAYERS.get(id))
            : DataResult.error(() -> "Could not get layer from id '" + id.toString() + "', as it doesn't exist!");
    }

    public static OriginLayer get(Identifier id) {

        if (!LAYERS.containsKey(id)) {
            throw new IllegalArgumentException("Could not get layer from id '" + id.toString() + "', as it doesn't exist!");
        }

        else return LAYERS.get(id);

    }

    @Nullable
    public static OriginLayer getNullable(Identifier id) {
        return LAYERS.get(id);
    }

    public static void register(Identifier id, OriginLayer layer) {

        if (LAYERS.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate origin layer id tried to register: '" + id + "'");
        }

        else {
            LAYERS.put(id, layer);
        }

    }

    public static Collection<OriginLayer> getLayers() {
        return LAYERS.values();
    }

    public static int getOriginOptionCount(PlayerEntity playerEntity) {
        return getOriginOptionCount(playerEntity, (layer, component) -> !component.hasOrigin(layer));
    }

    public static int getOriginOptionCount(PlayerEntity playerEntity, BiPredicate<OriginLayer, OriginComponent> condition) {
        return LAYERS.values()
            .stream()
            .filter(ol -> ol.isEnabled() && ModComponents.ORIGIN.maybeGet(playerEntity).map(oc -> condition.test(ol, oc)).orElse(false))
            .flatMapToInt(ol -> IntStream.of(ol.getOriginOptionCount(playerEntity)))
            .sum();
    }

    public static boolean contains(OriginLayer layer) {
        return contains(layer.getId());
    }

    public static boolean contains(Identifier id) {
        return LAYERS.containsKey(id);
    }

    public static int size() {
        return LAYERS.size();
    }

    public static void clear() {
        LAYERS.clear();
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of(Origins.MODID, "origin_layers");
    }

    @Override
    public Collection<Identifier> getFabricDependencies() {
        return Set.of(Origins.identifier("origins"));
    }

}
