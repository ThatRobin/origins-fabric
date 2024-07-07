package io.github.apace100.origins.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.networking.packet.s2c.OpenChooseOriginScreenS2CPacket;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.registry.ModDataComponentTypes;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.stream.Collectors;

public record OriginTargetComponent(OriginLayer layer, Origin origin) {

    public static final Codec<OriginTargetComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        OriginLayers.DISPATCH_CODEC.fieldOf("layer").forGetter(OriginTargetComponent::layer),
        OriginRegistry.DISPATCH_CODEC.optionalFieldOf("origin", Origin.EMPTY).forGetter(OriginTargetComponent::origin)
    ).apply(instance, OriginTargetComponent::new));

    public static final PacketCodec<PacketByteBuf, OriginTargetComponent> PACKET_CODEC = PacketCodec.tuple(
        OriginLayers.DISPATCH_PACKET_CODEC, OriginTargetComponent::layer,
        OriginRegistry.DISPATCH_PACKET_CODEC, OriginTargetComponent::origin,
        OriginTargetComponent::new
    );

    public static void applyTargets(ItemStack stack, PlayerEntity user) {

        if (user.getWorld().isClient || !stack.contains(ModDataComponentTypes.ORIGIN_TARGET)) {
            return;
        }

        OriginComponent component = ModComponents.ORIGIN.get(user);

        Map<OriginLayer, Origin> targets = getTargetsAsMap(stack);
        boolean assignedOrigin = !targets.isEmpty();

        if (!assignedOrigin) {
            OriginLayers.getLayers()
                .stream()
                .filter(OriginLayer::isEnabled)
                .forEach(layer -> component.setOrigin(layer, Origin.EMPTY));
        }

        else {
            targets.forEach(component::setOrigin);
        }

        assignedOrigin |= component.checkAutoChoosingLayers(user, false);
        int originOptions = OriginLayers.getOriginOptionCount(user);

        component.selectingOrigin(!assignedOrigin || originOptions > 0);
        component.sync();

        if (component.isSelectingOrigin()) {
            ServerPlayNetworking.send((ServerPlayerEntity) user, new OpenChooseOriginScreenS2CPacket(false));
        }

    }

    public static Map<OriginLayer, Origin> getTargetsAsMap(ItemStack stack) {

        if (stack.contains(ModDataComponentTypes.ORIGIN_TARGET)) {
            return Objects.requireNonNull(stack.get(ModDataComponentTypes.ORIGIN_TARGET))
                .stream()
                .filter(OriginTargetComponent::canTarget)
                .collect(Collectors.toMap(OriginTargetComponent::layer, OriginTargetComponent::origin));
        }

        else {
            return new HashMap<>();
        }

    }

    public boolean canTarget() {
        return layer.isEnabled()
            && (layer.contains(origin) || origin.isSpecial());
    }

}
