package io.github.apace100.origins.networking.packet.s2c;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.badge.Badge;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record SyncBadgeRegistryS2CPacket(Map<Identifier, List<Badge>> badges) implements CustomPayload {

    public static final Id<SyncBadgeRegistryS2CPacket> PACKET_ID = new Id<>(Origins.identifier("s2c/sync_badge_registry"));
    public static final PacketCodec<RegistryByteBuf, SyncBadgeRegistryS2CPacket> PACKET_CODEC = PacketCodec.of(SyncBadgeRegistryS2CPacket::write, SyncBadgeRegistryS2CPacket::read);

    public static SyncBadgeRegistryS2CPacket read(RegistryByteBuf buf) {
        return new SyncBadgeRegistryS2CPacket(buf.readMap(
            PacketByteBuf::readIdentifier,
            valBuf -> valBuf.readCollection(
                ArrayList::new,
                eleBuf -> Badge.receive((RegistryByteBuf) eleBuf)
            )
        ));
    }

    public void write(RegistryByteBuf buf) {
        buf.writeMap(
            badges,
            PacketByteBuf::writeIdentifier,
            (valBuf, badges) -> valBuf.writeCollection(
                badges,
                (eleBuf, badge) -> badge.send((RegistryByteBuf) eleBuf)
            )
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
