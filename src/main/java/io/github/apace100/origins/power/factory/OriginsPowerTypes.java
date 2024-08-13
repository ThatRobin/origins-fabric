package io.github.apace100.origins.power.factory;

import io.github.apace100.apoli.power.factory.PowerTypeFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.power.type.*;
import net.minecraft.registry.Registry;

public class OriginsPowerTypes {

    public static void register() {
        register(OriginsCallbackPowerType.getFactory());
        register(LikeWaterPowerType.createSimpleFactory(Origins.identifier("like_water"), LikeWaterPowerType::new));
        register(WaterBreathingPowerType.createSimpleFactory(Origins.identifier("water_breathing"), WaterBreathingPowerType::new));
        register(ScareCreepersPowerType.createSimpleFactory(Origins.identifier("scare_creepers"), ScareCreepersPowerType::new));
        register(WaterVisionPowerType.createSimpleFactory(Origins.identifier("water_vision"), WaterVisionPowerType::new));
        register(ConduitPowerOnLandPowerType.createSimpleFactory(Origins.identifier("conduit_power_on_land"), ConduitPowerOnLandPowerType::new));
    }

    private static void register(PowerTypeFactory<?> serializer) {
        Registry.register(ApoliRegistries.POWER_FACTORY, serializer.getSerializerId(), serializer);
    }

}
