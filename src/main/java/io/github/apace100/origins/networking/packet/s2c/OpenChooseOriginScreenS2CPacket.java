package io.github.apace100.origins.networking.packet.s2c;

import io.github.apace100.origins.Origins;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record OpenChooseOriginScreenS2CPacket(boolean showBackground) implements CustomPayload {

    public static final Id<OpenChooseOriginScreenS2CPacket> PACKET_ID = new Id<>(Origins.identifier("s2c/open_origin_screen"));
    public static final PacketCodec<ByteBuf, OpenChooseOriginScreenS2CPacket> PACKET_CODEC = PacketCodecs.BOOL.xmap(OpenChooseOriginScreenS2CPacket::new, OpenChooseOriginScreenS2CPacket::showBackground);

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
