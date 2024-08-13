package io.github.apace100.origins.component;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.type.ModifyPlayerSpawnPowerType;
import io.github.apace100.apoli.power.type.PowerType;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.power.type.OriginsCallbackPowerType;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.CommonTickingComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface OriginComponent extends AutoSyncedComponent, CommonTickingComponent {

	Map<OriginLayer, Origin> getOrigins();
	Origin getOrigin(OriginLayer layer);

	boolean hasSelectionInvulnerability();
	boolean isSelectingOrigin();
	boolean hasOrigin(OriginLayer layer);
	boolean hasAllOrigins();
	boolean hadOriginBefore();

	void selectingOrigin(boolean selectingOrigin);
	void removeLayer(OriginLayer layer);
	void setOrigin(OriginLayer layer, Origin origin);
	void sync();

	static void sync(PlayerEntity player) {
		ModComponents.ORIGIN.sync(player);
	}

	static void onChosen(PlayerEntity player, boolean hadOriginBefore) {

		if(!hadOriginBefore) {
			PowerHolderComponent.getPowerTypes(player, ModifyPlayerSpawnPowerType.class).forEach(ModifyPlayerSpawnPowerType::teleportToModifiedSpawn);
		}

		PowerHolderComponent.getPowerTypes(player, OriginsCallbackPowerType.class).forEach(p -> p.onChosen(hadOriginBefore));

	}

	static void partialOnChosen(PlayerEntity player, boolean hadOriginBefore, Origin origin) {

		PowerHolderComponent powerHolder = PowerHolderComponent.KEY.get(player);

		for (Power power : powerHolder.getPowersFromSource(origin.getId())) {

			PowerType powerType  = powerHolder.getPowerType(power);

			if (powerType instanceof ModifyPlayerSpawnPowerType mps && !hadOriginBefore) {
				mps.teleportToModifiedSpawn();
			}

			else if (powerType instanceof OriginsCallbackPowerType ocp) {
				ocp.onChosen(hadOriginBefore);
			}

		}

	}

	default boolean checkAutoChoosingLayers(PlayerEntity player, boolean includeDefaults) {

		List<OriginLayer> layers = new ArrayList<>();
		boolean choseOneAutomatically = false;

		OriginLayers.getLayers()
			.stream()
			.filter(OriginLayer::isEnabled)
			.forEach(layers::add);

		Collections.sort(layers);
		for (OriginLayer layer : layers) {

			if (!layer.isEnabled() || hasOrigin(layer)) {
				continue;
			}

			if (includeDefaults && layer.hasDefaultOrigin()) {

				setOrigin(layer, OriginRegistry.get(layer.getDefaultOrigin()));
				choseOneAutomatically = true;

			} else if (layer.getOriginOptionCount(player) == 1 && layer.shouldAutoChoose()) {

				List<Origin> origins = layer.getOrigins(player)
					.stream()
					.map(OriginRegistry::get)
					.filter(Origin::isChoosable)
					.toList();

				if (!origins.isEmpty()) {

					setOrigin(layer, origins.getFirst());
					choseOneAutomatically = true;

				} else if (layer.isRandomAllowed() && !layer.getRandomOrigins(player).isEmpty()) {

					List<Identifier> randomOriginIds = layer.getRandomOrigins(player);
					int randomOriginIndex = player.getRandom().nextInt(randomOriginIds.size());

					setOrigin(layer, OriginRegistry.get(randomOriginIds.get(randomOriginIndex)));
					choseOneAutomatically = true;

				}

			}

		}

		return choseOneAutomatically;

	}

}
