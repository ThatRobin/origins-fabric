package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.content.OrbOfOriginItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

public class ModItems {

    public static final Item ORB_OF_ORIGIN;

    public static void register() {

    }

    static {
        ORB_OF_ORIGIN = Registry.register(Registries.ITEM, Origins.identifier("orb_of_origin"), new OrbOfOriginItem());
    }

}
