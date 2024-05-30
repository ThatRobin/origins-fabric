package io.github.apace100.origins.util;

import io.github.apace100.apoli.power.*;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class PowerKeyManager {

    private static final HashMap<Identifier, String> KEY_CACHE = new HashMap<>();

    public static void clearCache() {
        KEY_CACHE.clear();
    }

    public static String getKeyIdentifier(Identifier powerId) {
        return KEY_CACHE.computeIfAbsent(powerId, PowerKeyManager::getKeyFromPower);
    }

    private static String getKeyFromPower(Identifier powerId) {

        PowerType<?> power = PowerTypeRegistry.getNullable(powerId);
        if (power == null || !(power.create(null) instanceof Active activePower)) {
            return "";
        }

        String key = activePower.getKey().key;
        return key.equals("none")
            ? "key.origins.primary_active"
            : key;

    }

}
