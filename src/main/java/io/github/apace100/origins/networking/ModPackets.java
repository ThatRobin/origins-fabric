package io.github.apace100.origins.networking;

import io.github.apace100.origins.networking.packet.VersionHandshakePacket;
import io.github.apace100.origins.networking.packet.c2s.ChooseOriginC2SPacket;
import io.github.apace100.origins.networking.packet.c2s.ChooseRandomOriginC2SPacket;
import io.github.apace100.origins.networking.packet.s2c.OriginsInstalledS2CPacket;
import io.github.apace100.origins.networking.packet.s2c.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ModPackets {

    public static void register() {

        PayloadTypeRegistry.configurationS2C().register(VersionHandshakePacket.PACKET_ID, VersionHandshakePacket.PACKET_CODEC);
        PayloadTypeRegistry.configurationS2C().register(OriginsInstalledS2CPacket.PACKET_ID, OriginsInstalledS2CPacket.PACKET_CODEC);
        PayloadTypeRegistry.configurationC2S().register(VersionHandshakePacket.PACKET_ID, VersionHandshakePacket.PACKET_CODEC);

        PayloadTypeRegistry.playS2C().register(ConfirmOriginS2CPacket.PACKET_ID, ConfirmOriginS2CPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenChooseOriginScreenS2CPacket.PACKET_ID, OpenChooseOriginScreenS2CPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncBadgesS2CPacket.PACKET_ID, SyncBadgesS2CPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncOriginLayersS2CPacket.PACKET_ID, SyncOriginLayersS2CPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncOriginsS2CPacket.PACKET_ID, SyncOriginsS2CPacket.PACKET_CODEC);

        PayloadTypeRegistry.playC2S().register(ChooseOriginC2SPacket.PACKET_ID, ChooseOriginC2SPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ChooseRandomOriginC2SPacket.PACKET_ID, ChooseRandomOriginC2SPacket.PACKET_CODEC);

    }

}
