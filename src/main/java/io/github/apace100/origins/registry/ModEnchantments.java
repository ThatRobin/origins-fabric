package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public class ModEnchantments {

    public static final RegistryKey<Enchantment> WATER_PROTECTION = RegistryKey.of(RegistryKeys.ENCHANTMENT, Origins.identifier("water_protection"));

}
