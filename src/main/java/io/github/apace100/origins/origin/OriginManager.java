package io.github.apace100.origins.origin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import io.github.apace100.apoli.power.PowerManager;
import io.github.apace100.calio.CalioServer;
import io.github.apace100.calio.data.IdentifiableMultiJsonDataLoader;
import io.github.apace100.calio.data.MultiJsonDataContainer;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.origins.Origins;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class OriginManager extends IdentifiableMultiJsonDataLoader implements IdentifiableResourceReloadListener {

	public static final Identifier PHASE = Origins.identifier("phase/origins");
	private static final Gson GSON = new GsonBuilder()
		.disableHtmlEscaping()
		.setPrettyPrinting()
		.create();

	private static final Map<Identifier, Integer> LOADING_PRIORITIES = new HashMap<>();

	public OriginManager() {
		super(GSON, "origins", ResourceType.SERVER_DATA);
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.addPhaseOrdering(PowerManager.PHASE, PHASE);
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(PHASE, (player, joined) -> OriginRegistry.send(player));
	}

	@Override
	protected void apply(MultiJsonDataContainer prepared, ResourceManager manager, Profiler profiler) {

		Origins.LOGGER.info("Reading origins from data packs...");

		LOADING_PRIORITIES.clear();
		OriginRegistry.reset();

		DynamicRegistryManager dynamicRegistries = CalioServer.getDynamicRegistries().orElse(null);
		if (dynamicRegistries == null) {
			Origins.LOGGER.error("Can't read origins from data packs without access to dynamic registries!");
			return;
		}

		AtomicBoolean hasConfigChanged = new AtomicBoolean(false);
		prepared.forEach((packName, id, jsonElement) -> {

			try {

				SerializableData.CURRENT_NAMESPACE = id.getNamespace();
				SerializableData.CURRENT_PATH = id.getPath();

				if (!(jsonElement instanceof JsonObject jsonObject)) {
					throw new JsonSyntaxException("Not a JSON object: " + jsonElement);
				}

				jsonObject.addProperty("id", id.toString());
				Origin origin = Origin.DATA_TYPE.read(dynamicRegistries.getOps(JsonOps.INSTANCE), jsonObject).getOrThrow();

				int prevLoadingPriority = LOADING_PRIORITIES.computeIfAbsent(id, k -> 0);
				int currLoadingPriority = JsonHelper.getInt(jsonObject, "loading_priority", 0);

				if (!OriginRegistry.contains(id)) {

					origin.validate();

					OriginRegistry.register(id, origin);
					LOADING_PRIORITIES.put(id, currLoadingPriority);

				}

				else if (prevLoadingPriority < currLoadingPriority) {

					Origins.LOGGER.warn("Overriding origin \"{}\" (with prev. loading priority of {}) with a higher loading priority of {} from data pack [{}]!", id, prevLoadingPriority, currLoadingPriority, packName);
					origin.validate();

					OriginRegistry.update(id, origin);
					LOADING_PRIORITIES.put(id, currLoadingPriority);

				}

				origin = OriginRegistry.get(id);
				hasConfigChanged.set(hasConfigChanged.get() | Origins.config.addToConfig(origin));

				if (Origins.config.isOriginDisabled(id)) {
					OriginRegistry.remove(id);
				}

			}

			catch (Exception e) {
				Origins.LOGGER.error("There was a problem reading origin \"{}\": {}", id, e.getMessage());
			}

		});

		Origins.LOGGER.info("Finished reading origins from data packs. Registry contains {} origins.", OriginRegistry.size());
		if (hasConfigChanged.get()) {
			Origins.serializeConfig();
		}

		LOADING_PRIORITIES.clear();

		SerializableData.CURRENT_NAMESPACE = null;
		SerializableData.CURRENT_PATH = null;

	}

	@Override
	public Identifier getFabricId() {
		return Origins.identifier("origins");
	}

	@Override
	public Collection<Identifier> getFabricDependencies() {
		return Set.of(PowerManager.ID);
	}

}
