package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public class ModDamageTypes {

    public static final RegistryKey<DamageType> NO_WATER_FOR_GILLS = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Origins.identifier("no_water_for_gills"));

}
