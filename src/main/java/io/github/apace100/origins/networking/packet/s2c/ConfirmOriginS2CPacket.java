package io.github.apace100.origins.networking.packet.s2c;

import io.github.apace100.origins.Origins;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ConfirmOriginS2CPacket(Identifier layerId, Identifier originId) implements CustomPayload {

    public static final Id<ConfirmOriginS2CPacket> PACKET_ID = new Id<>(Origins.identifier("s2c/confirm_origin"));
    public static final PacketCodec<PacketByteBuf, ConfirmOriginS2CPacket> PACKET_CODEC = PacketCodec.tuple(
        Identifier.PACKET_CODEC, ConfirmOriginS2CPacket::layerId,
        Identifier.PACKET_CODEC, ConfirmOriginS2CPacket::originId,
        ConfirmOriginS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
