package io.github.apace100.origins.networking.packet.c2s;

import io.github.apace100.origins.Origins;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ChooseRandomOriginC2SPacket(Identifier layerId) implements CustomPayload {

    public static final Id<ChooseRandomOriginC2SPacket> PACKET_ID = new Id<>(Origins.identifier("c2s/choose_random_origin"));
    public static final PacketCodec<ByteBuf, ChooseRandomOriginC2SPacket> PACKET_CODEC = Identifier.PACKET_CODEC.xmap(ChooseRandomOriginC2SPacket::new, ChooseRandomOriginC2SPacket::layerId);

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
