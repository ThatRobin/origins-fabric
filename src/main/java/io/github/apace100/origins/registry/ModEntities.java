package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.entity.EnderianPearlEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {

    public static final EntityType<EnderianPearlEntity> ENDERIAN_PEARL;

    public static void register() {

    }

    static {
        ENDERIAN_PEARL = Registry.register(Registries.ENTITY_TYPE, Origins.identifier("enderian_pearl"), EntityType.Builder.<EnderianPearlEntity>create(EnderianPearlEntity::new, SpawnGroup.MISC)
            .dimensions(0.25F, 0.25F)
            .maxTrackingRange(64)
            .trackingTickInterval(10)
            .build());
    }

}
