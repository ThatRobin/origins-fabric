package io.github.apace100.origins.power.type;

import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.factory.PowerTypeFactory;
import io.github.apace100.apoli.power.type.ActionOnCallbackPowerType;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.origins.Origins;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.function.Consumer;

public class OriginsCallbackPowerType extends ActionOnCallbackPowerType {

    private final Consumer<Entity> entityActionChosen;
    private final boolean executeChosenWhenOrb;

    public OriginsCallbackPowerType(Power power, LivingEntity entity, Consumer<Entity> entityActionRespawned, Consumer<Entity> entityActionRemoved, Consumer<Entity> entityActionGained, Consumer<Entity> entityActionLost, Consumer<Entity> entityActionAdded, Consumer<Entity> entityActionChosen, boolean executeChosenWhenOrb) {
        super(power, entity, entityActionRespawned, entityActionRemoved, entityActionGained, entityActionLost, entityActionAdded);
        this.entityActionChosen = entityActionChosen;
        this.executeChosenWhenOrb = executeChosenWhenOrb;
    }

    public void onChosen(boolean isOrbOfOrigins) {

        if (this.isActive() && entityActionChosen != null && (!isOrbOfOrigins || executeChosenWhenOrb)) {
            entityActionChosen.accept(entity);
        }

    }

    public static PowerTypeFactory<?> getFactory() {
        return new PowerTypeFactory<>(
            Origins.identifier("action_on_callback"),
            new SerializableData()
                .add("entity_action_respawned", ApoliDataTypes.ENTITY_ACTION, null)
                .add("entity_action_removed", ApoliDataTypes.ENTITY_ACTION, null)
                .add("entity_action_gained", ApoliDataTypes.ENTITY_ACTION, null)
                .add("entity_action_lost", ApoliDataTypes.ENTITY_ACTION, null)
                .add("entity_action_added", ApoliDataTypes.ENTITY_ACTION, null)
                .add("entity_action_chosen", ApoliDataTypes.ENTITY_ACTION, null)
                .add("execute_chosen_when_orb", SerializableDataTypes.BOOLEAN, true),
            data -> (power, livingEntity) -> new OriginsCallbackPowerType(power, livingEntity,
                data.get("entity_action_respawned"),
                data.get("entity_action_removed"),
                data.get("entity_action_gained"),
                data.get("entity_action_lost"),
                data.get("entity_action_added"),
                data.get("entity_action_chosen"),
                data.get("execute_chosen_when_orb")
            )
        ).allowCondition();
    }

}
