package io.github.apace100.origins.power;

import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import net.minecraft.entity.LivingEntity;

public class ConduitPowerOnLandPower extends Power {

    public ConduitPowerOnLandPower(PowerType<?> type, LivingEntity entity) {
        super(type, entity);
    }

}
