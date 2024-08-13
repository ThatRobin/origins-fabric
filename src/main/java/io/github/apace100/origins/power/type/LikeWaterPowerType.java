package io.github.apace100.origins.power.type;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.type.PowerType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class LikeWaterPowerType extends PowerType {

    public LikeWaterPowerType(Power power, LivingEntity entity) {
        super(power, entity);
    }

    public static Vec3d modifyFluidMovement(Entity entity, Vec3d velocity, double fallVelocity) {
        return PowerHolderComponent.hasPowerType(entity, LikeWaterPowerType.class) && Math.abs(velocity.y - fallVelocity / 16.0D) < 0.025D
            ? new Vec3d(velocity.x, 0, velocity.z)
            : velocity;
    }

}
