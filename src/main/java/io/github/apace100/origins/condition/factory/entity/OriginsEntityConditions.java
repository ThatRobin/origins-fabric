package io.github.apace100.origins.condition.factory.entity;

import io.github.apace100.apoli.condition.factory.ConditionTypeFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.origins.condition.entity.type.OriginConditionType;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registry;

public class OriginsEntityConditions {

    public static void register() {
        register(OriginConditionType.getFactory());
    }

    private static void register(ConditionTypeFactory<Entity> conditionFactory) {
        Registry.register(ApoliRegistries.ENTITY_CONDITION, conditionFactory.getSerializerId(), conditionFactory);
    }

}
