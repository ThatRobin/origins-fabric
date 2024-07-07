package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.OriginsClient;
import io.github.apace100.origins.badge.Badge;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.integration.OriginDataLoadedCallback;
import io.github.apace100.origins.networking.packet.VersionHandshakePacket;
import io.github.apace100.origins.networking.packet.s2c.*;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.WaitForNextLayerScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Predicate;

public class ModPacketsS2C {

    @Environment(EnvType.CLIENT)
    public static void register() {

        ClientConfigurationNetworking.registerGlobalReceiver(VersionHandshakePacket.PACKET_ID, ModPacketsS2C::handleHandshake);

        ClientPlayConnectionEvents.INIT.register(((clientPlayNetworkHandler, minecraftClient) -> {
            ClientPlayNetworking.registerReceiver(ConfirmOriginS2CPacket.PACKET_ID, ModPacketsS2C::receiveOriginConfirmation);
            ClientPlayNetworking.registerReceiver(OpenChooseOriginScreenS2CPacket.PACKET_ID, ModPacketsS2C::openOriginScreen);
            ClientPlayNetworking.registerReceiver(SyncOriginLayerRegistryS2CPacket.PACKET_ID, ModPacketsS2C::receiveLayerList);
            ClientPlayNetworking.registerReceiver(SyncOriginRegistryS2CPacket.PACKET_ID, ModPacketsS2C::receiveOriginList);
            ClientPlayNetworking.registerReceiver(SyncBadgeRegistryS2CPacket.PACKET_ID, ModPacketsS2C::receiveBadgeList);
        }));

    }

    @Environment(EnvType.CLIENT)
    private static void receiveOriginConfirmation(ConfirmOriginS2CPacket packet, ClientPlayNetworking.Context context) {

        ClientPlayerEntity player = context.player();

        OriginLayer layer = OriginLayers.getLayer(packet.layerId());
        Origin origin = OriginRegistry.get(packet.originId());

        OriginComponent component = ModComponents.ORIGIN.get(player);
        component.setOrigin(layer, origin);

        if (MinecraftClient.getInstance().currentScreen instanceof WaitForNextLayerScreen nextLayerScreen) {
            nextLayerScreen.openSelection();
        }

    }

    @Environment(EnvType.CLIENT)
    private static void handleHandshake(VersionHandshakePacket packet, ClientConfigurationNetworking.Context context) {
        context.responseSender().sendPacket(new VersionHandshakePacket(Origins.SEMVER));
    }

    @Environment(EnvType.CLIENT)
    private static void openOriginScreen(OpenChooseOriginScreenS2CPacket packet, ClientPlayNetworking.Context context) {

        List<OriginLayer> layers = new ArrayList<>();
        OriginComponent component = ModComponents.ORIGIN.get(context.player());

        OriginLayers.getLayers()
            .stream()
            .filter(ol -> ol.isEnabled() && !component.hasOrigin(ol))
            .forEach(layers::add);

        Collections.sort(layers);
        MinecraftClient.getInstance().setScreen(new ChooseOriginScreen(layers, 0, packet.showBackground()));

    }

    @Environment(EnvType.CLIENT)
    private static void receiveOriginList(SyncOriginRegistryS2CPacket packet, ClientPlayNetworking.Context context) {

        OriginsClient.isServerRunningOrigins = true;

        OriginRegistry.reset();
        packet.origins().entrySet()
            .stream()
            .map(e -> Origin.createFromData(e.getKey(), e.getValue()))
            .filter(Predicate.not(OriginRegistry::contains))
            .forEach(OriginRegistry::register);

    }

    @Environment(EnvType.CLIENT)
    private static void receiveLayerList(SyncOriginLayerRegistryS2CPacket packet, ClientPlayNetworking.Context context) {

        OriginLayers.clear();
        packet.layers().forEach(OriginLayers::register);

        OriginDataLoadedCallback.EVENT.invoker().onDataLoaded(true);

    }

    @Environment(EnvType.CLIENT)
    private static void receiveBadgeList(SyncBadgeRegistryS2CPacket payload, ClientPlayNetworking.Context context) {
        BadgeManager.clear();
        payload.badges().forEach(BadgeManager::putPowerBadges);
    }

}
