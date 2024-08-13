package io.github.apace100.origins.condition.entity.type;

import io.github.apace100.apoli.condition.factory.ConditionTypeFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class OriginConditionType {

    public static boolean condition(Entity entity, Identifier originId, @Nullable Identifier layerId) {

        OriginComponent originComponent = ModComponents.ORIGIN.getNullable(entity);
        if (originComponent == null) {
            return false;
        }

        if (layerId == null) {
            return originComponent.getOrigins().values()
                .stream()
                .map(Origin::getId)
                .anyMatch(originId::equals);
        }

        OriginLayer layer = OriginLayers.getNullableLayer(layerId);
        if (layer == null) {
            return false;
        }

        Origin origin = originComponent.getOrigin(layer);
        return origin != null
            && origin.getId().equals(originId);

    }

    public static ConditionTypeFactory<Entity> getFactory() {
        return new ConditionTypeFactory<>(
            Origins.identifier("origin"),
            new SerializableData()
                .add("origin", SerializableDataTypes.IDENTIFIER)
                .add("layer", SerializableDataTypes.IDENTIFIER, null),
            (data, entity) -> condition(entity,
                data.get("origin"),
                data.get("layer")
            )
        );
    }

}
