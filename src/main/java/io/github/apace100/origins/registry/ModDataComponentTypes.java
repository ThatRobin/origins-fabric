package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.util.OriginTargetComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class ModDataComponentTypes {

    public static final ComponentType<List<OriginTargetComponent>> ORIGIN_TARGET = ComponentType.<List<OriginTargetComponent>>builder()
        .codec(OriginTargetComponent.CODEC.listOf())
        .packetCodec(PacketCodecs.collection(ArrayList::new, OriginTargetComponent.PACKET_CODEC))
        .build();

    public static void register() {
        Registry.register(Registries.DATA_COMPONENT_TYPE, Origins.identifier("targets"), ORIGIN_TARGET);
    }

}
