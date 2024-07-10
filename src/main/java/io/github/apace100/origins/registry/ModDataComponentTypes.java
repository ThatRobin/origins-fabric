package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.item.ItemOriginsComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModDataComponentTypes {

    public static final ComponentType<ItemOriginsComponent> ORIGIN = ComponentType.<ItemOriginsComponent>builder()
        .codec(ItemOriginsComponent.CODEC)
        .packetCodec(ItemOriginsComponent.PACKET_CODEC)
        .build();

    public static void register() {
        Registry.register(Registries.DATA_COMPONENT_TYPE, Origins.identifier("origin"), ORIGIN);
    }

}
