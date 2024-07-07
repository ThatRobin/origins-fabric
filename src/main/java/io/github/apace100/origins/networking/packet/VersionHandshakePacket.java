package io.github.apace100.origins.networking.packet;

import io.github.apace100.origins.Origins;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record VersionHandshakePacket(int[] semver) implements CustomPayload {

    public static final Id<VersionHandshakePacket> PACKET_ID = new Id<>(Origins.identifier("handshake/version"));
    public static final PacketCodec<PacketByteBuf, VersionHandshakePacket> PACKET_CODEC = PacketCodec.of(VersionHandshakePacket::write, VersionHandshakePacket::read);

    public static VersionHandshakePacket read(PacketByteBuf buffer) {
        return new VersionHandshakePacket(buffer.readIntArray());
    }

    public void write(PacketByteBuf buffer) {
        buffer.writeIntArray(semver);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
