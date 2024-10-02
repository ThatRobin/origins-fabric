package io.github.apace100.origins.networking.packet;

import io.github.apace100.origins.Origins;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record VersionHandshakePacket(int[] semver) implements CustomPayload {

    private static final PacketCodec<PacketByteBuf, int[]> INT_ARRAY = PacketCodec.ofStatic(PacketByteBuf::writeIntArray, PacketByteBuf::readIntArray);

    public static final Id<VersionHandshakePacket> PACKET_ID = new Id<>(Origins.identifier("handshake/version"));
    public static final PacketCodec<PacketByteBuf, VersionHandshakePacket> PACKET_CODEC = INT_ARRAY.xmap(VersionHandshakePacket::new, VersionHandshakePacket::semver);

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
