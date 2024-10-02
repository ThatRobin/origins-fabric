package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.networking.packet.OriginsInstalledS2CPacket;
import io.github.apace100.origins.networking.packet.VersionHandshakePacket;
import io.github.apace100.origins.networking.packet.c2s.ChooseOriginC2SPacket;
import io.github.apace100.origins.networking.packet.c2s.ChooseRandomOriginC2SPacket;
import io.github.apace100.origins.networking.packet.s2c.ConfirmOriginS2CPacket;
import io.github.apace100.origins.networking.task.VersionHandshakeTask;
import io.github.apace100.origins.origin.*;
import io.github.apace100.origins.registry.ModComponents;
import joptsimple.internal.Strings;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;

public class ModPacketsC2S {

    public static void register() {

        if (Origins.config.performVersionCheck) {
            ServerConfigurationConnectionEvents.CONFIGURE.register(ModPacketsC2S::sendHandshake);
            ServerConfigurationNetworking.registerGlobalReceiver(VersionHandshakePacket.PACKET_ID, ModPacketsC2S::receiveHandshakeReply);
        }

        ServerConfigurationConnectionEvents.CONFIGURE.register(ModPacketsC2S::sendOriginsInstallationStatus);

        ServerPlayNetworking.registerGlobalReceiver(ChooseOriginC2SPacket.PACKET_ID, ModPacketsC2S::onChooseOrigin);
        ServerPlayNetworking.registerGlobalReceiver(ChooseRandomOriginC2SPacket.PACKET_ID, ModPacketsC2S::chooseRandomOrigin);

    }

    private static void onChooseOrigin(ChooseOriginC2SPacket packet, ServerPlayNetworking.Context context) {

        ServerPlayerEntity player = context.player();

        OriginComponent component = ModComponents.ORIGIN.get(player);
        OriginLayer layer = OriginLayerManager.get(packet.layerId());

        if (component.hasAllOrigins() && component.hasOrigin(layer)) {
            Origins.LOGGER.warn("Player {} tried to choose origin for layer \"{}\" while having one already.", player.getName().getString(), packet.layerId());
            return;
        }

        Origin origin = OriginManager.get(packet.originId());
        if (!(origin.isChoosable() || layer.contains(origin, player))) {
            Origins.LOGGER.warn("Player {} tried to choose unchoosable origin \"{}\" from layer \"{}\"!", player.getName().getString(), packet.originId(), packet.layerId());
            component.setOrigin(layer, Origin.EMPTY);
        } else {

            boolean hadOriginBefore = component.hadOriginBefore();
            boolean hadAllOrigins = component.hasAllOrigins();

            component.setOrigin(layer, origin);
            component.checkAutoChoosingLayers(player, false);

            if (component.hasAllOrigins() && !hadAllOrigins) {
                OriginComponent.onChosen(player, hadOriginBefore);
            }

            Origins.LOGGER.info("Player {} chose origin \"{}\" for layer \"{}\"", player.getName().getString(), packet.originId(), packet.layerId());

        }

        confirmOrigin(player, layer, component.getOrigin(layer));

        component.selectingOrigin(false);
        component.sync();

    }

    private static void chooseRandomOrigin(ChooseRandomOriginC2SPacket packet, ServerPlayNetworking.Context context) {

        ServerPlayerEntity player = context.player();

        OriginComponent component = ModComponents.ORIGIN.get(player);
        OriginLayer layer = OriginLayerManager.get(packet.layerId());

        if (component.hasAllOrigins() && component.hasOrigin(layer)) {
            Origins.LOGGER.warn("Player {} tried to choose origin for layer \"{}\" while having one already.", player.getName().getString(), packet.layerId());
            return;
        }

        List<Identifier> randomOriginIds = layer.getRandomOrigins(player);
        if (!layer.isRandomAllowed() || randomOriginIds.isEmpty()) {
            Origins.LOGGER.warn("Player {} tried to choose a random origin for layer \"{}\", which is not allowed!", player.getName().getString(), packet.layerId());
            component.setOrigin(layer, Origin.EMPTY);
        } else {

            Identifier randomOriginId = randomOriginIds.get(player.getRandom().nextInt(randomOriginIds.size()));
            Origin origin = OriginManager.get(randomOriginId);

            boolean hadOriginBefore = component.hadOriginBefore();
            boolean hadAllOrigins = component.hasAllOrigins();

            component.setOrigin(layer, origin);
            component.checkAutoChoosingLayers(player, false);

            if (component.hasAllOrigins() && !hadAllOrigins) {
                OriginComponent.onChosen(player, hadOriginBefore);
            }

            Origins.LOGGER.info("Player {} was randomly assigned the following origin: {}", player.getName().getString(), randomOriginId);

        }

        confirmOrigin(player, layer, component.getOrigin(layer));

        component.selectingOrigin(false);
        component.sync();

    }

    private static void receiveHandshakeReply(VersionHandshakePacket packet, ServerConfigurationNetworking.Context context) {

        ServerConfigurationNetworkHandler handler = context.networkHandler();
        boolean mismatch = packet.semver().length != Origins.SEMVER.length;

        for (int i = 0; !mismatch && i < packet.semver().length - 1; i++) {
            mismatch = packet.semver()[i] != Origins.SEMVER[i];
        }

        if (mismatch) {
            handler.disconnect(Text.translatable("origins.gui.version_mismatch", Origins.VERSION, Strings.join(Arrays.stream(packet.semver()).mapToObj(String::valueOf).toList(), ".")));
        }

        else {
            handler.completeTask(VersionHandshakeTask.KEY);
        }

    }

    private static void sendOriginsInstallationStatus(ServerConfigurationNetworkHandler handler, MinecraftServer server) {
        handler.sendPacket(ServerConfigurationNetworking.createS2CPacket(OriginsInstalledS2CPacket.INSTANCE));
    }

    private static void sendHandshake(ServerConfigurationNetworkHandler handler, MinecraftServer server) {

        if (ServerConfigurationNetworking.canSend(handler, VersionHandshakePacket.PACKET_ID)) {
            handler.addTask(new VersionHandshakeTask(Origins.SEMVER));
        }

        else {
            handler.disconnect(Text.of("This server requires you to install the Origins mod (v " + Origins.VERSION + ") to play."));
        }

    }

    private static void confirmOrigin(ServerPlayerEntity player, OriginLayer layer, Origin origin) {
        ServerPlayNetworking.send(player, new ConfirmOriginS2CPacket(layer.getId(), origin.getId()));
    }

}
