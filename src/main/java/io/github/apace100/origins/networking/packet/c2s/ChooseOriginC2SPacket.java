package io.github.apace100.origins.networking.packet.c2s;

import io.github.apace100.origins.Origins;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ChooseOriginC2SPacket(Identifier layerId, Identifier originId) implements CustomPayload {

    public static final Id<ChooseOriginC2SPacket> PACKET_ID = new Id<>(Origins.identifier("c2s/choose_origin"));
    public static final PacketCodec<ByteBuf, ChooseOriginC2SPacket> PACKET_CODEC = PacketCodec.tuple(
        Identifier.PACKET_CODEC, ChooseOriginC2SPacket::layerId,
        Identifier.PACKET_CODEC, ChooseOriginC2SPacket::originId,
        ChooseOriginC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
