package io.github.apace100.origins.origin;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.apace100.apoli.util.PrioritizedEntry;
import io.github.apace100.calio.data.IdentifiableMultiJsonDataLoader;
import io.github.apace100.calio.data.MultiJsonDataContainer;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.integration.OriginDataLoadedCallback;
import io.github.apace100.origins.networking.packet.s2c.OpenChooseOriginScreenS2CPacket;
import io.github.apace100.origins.networking.packet.s2c.SyncOriginLayerRegistryS2CPacket;
import io.github.apace100.origins.registry.ModComponents;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentProvider;

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
            Map<Identifier, OriginLayer> layers = new HashMap<>();

            for (OriginLayer layer : OriginLayerManager.getLayers()) {

                layers.put(layer.getId(), layer);

                if (layer.isEnabled() && !component.hasOrigin(layer)) {
                    component.setOrigin(layer, Origin.EMPTY);
                }

            }

            ServerPlayNetworking.send(player, new SyncOriginLayerRegistryS2CPacket(layers));

            if (joined) {

                List<ServerPlayerEntity> otherPlayers = player.getServerWorld().getServer().getPlayerManager().getPlayerList();
                otherPlayers.remove(player);

                otherPlayers.forEach(otherPlayer -> ModComponents.ORIGIN.syncWith(otherPlayer, (ComponentProvider) player));

            }

            postLoading(player, joined);

        });
    }

    private void postLoading(ServerPlayerEntity player, boolean init) {

        OriginComponent component = ModComponents.ORIGIN.get(player);
        boolean mismatch = false;

        for (Map.Entry<OriginLayer, Origin> entry : component.getOrigins().entrySet()) {

            OriginLayer oldLayer = entry.getKey();
            Origin oldOrigin = entry.getValue();

            boolean originOrLayerNotAvailable = !OriginLayerManager.contains(oldLayer)
                                             || !OriginRegistry.contains(oldOrigin);
            boolean originUnregistered = OriginLayerManager.contains(oldLayer)
                                      && !OriginLayerManager.get(oldLayer.getId()).contains(oldOrigin);

            if (originOrLayerNotAvailable || originUnregistered) {

                if (oldOrigin == Origin.EMPTY) {
                    continue;
                }

                if (originUnregistered) {
                    Origins.LOGGER.error("Removed unregistered origin \"{}\" from origin layer \"{}\" from player {}!", oldOrigin.getId(), oldLayer.getId(), player.getName().getString());
                    component.setOrigin(oldLayer, Origin.EMPTY);
                } else {
                    Origins.LOGGER.error("Removed unregistered origin layer \"{}\" from player {}!", oldLayer.getId(), player.getName().getString());
                    component.removeLayer(oldLayer);
                }

                continue;

            }

            Origin newOrigin = OriginRegistry.get(oldOrigin.getId());
            if (oldOrigin.toJson().equals(newOrigin.toJson())) {
                continue;
            }

            Origins.LOGGER.warn("Mismatched data fields of origin \"{}\" from player {}! Updating...", oldOrigin.getId(), player.getName().getString());
            mismatch = true;

            component.setOrigin(oldLayer, newOrigin);

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
        } else if (!isFakePlayer(player)) {

            component.selectingOrigin(true);
            component.sync();

            ServerPlayNetworking.send(player, new OpenChooseOriginScreenS2CPacket(true));

        } else {
            component.sync();
        }

    }

    private static boolean isFakePlayer(ServerPlayerEntity player) {
        return FabricLoader.getInstance().isModLoaded("carpet") && player instanceof EntityPlayerMPFake;
    }

    @Override
    protected void apply(MultiJsonDataContainer prepared, ResourceManager manager, Profiler profiler) {

        LOADING_PRIORITIES.clear();
        clear();

        Map<Identifier, List<PrioritizedEntry<OriginLayer>>> loadedLayers = new HashMap<>();
        Origins.LOGGER.info("Loading origin layers from data packs...");

        prepared.forEach((packName, id, jsonElement) -> {

            try {

                SerializableData.CURRENT_NAMESPACE = id.getNamespace();
                SerializableData.CURRENT_PATH = id.getPath();

                Origins.LOGGER.info("Trying to add origin layer \"{}\" from data pack [{}]", id, packName);

                if (!(jsonElement instanceof JsonObject jsonObject)) {
                    throw new JsonSyntaxException("Expected a JSON object");
                }

                OriginLayer layer = OriginLayer.fromJson(id, jsonObject);
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

        Origins.LOGGER.info("Finished loading {} origin layer(s). Merging similar origin layers...", loadedLayers.size());
        loadedLayers.forEach((id, entries) -> {

            AtomicReference<OriginLayer> currentLayer = new AtomicReference<>();
            entries.sort(Comparator.comparing(PrioritizedEntry::priority));

            for (PrioritizedEntry<OriginLayer> entry : entries) {

                if (currentLayer.get() == null) {
                    currentLayer.set(entry.value());
                }

                else {
                    currentLayer.accumulateAndGet(entry.value(), OriginLayer::merge);
                }

            }

            register(id, currentLayer.get());

        });

        Origins.LOGGER.info("Finished merging similar origin layers. Registry contains {} origin layer(s).", size());
        OriginDataLoadedCallback.EVENT.invoker().onDataLoaded(false);

        SerializableData.CURRENT_NAMESPACE = null;
        SerializableData.CURRENT_PATH = null;

        LOADING_PRIORITIES.clear();

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
