package io.github.apace100.origins.networking.packet.s2c;

import io.github.apace100.origins.Origins;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public class OriginsInstalledS2CPacket implements CustomPayload {

	public static final OriginsInstalledS2CPacket INSTANCE = new OriginsInstalledS2CPacket();

	public static final Id<OriginsInstalledS2CPacket> PACKET_ID = new Id<>(Origins.identifier("s2c/origins_installed"));
	public static final PacketCodec<ByteBuf, OriginsInstalledS2CPacket> PACKET_CODEC = PacketCodec.unit(INSTANCE);

	private OriginsInstalledS2CPacket() {

	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return PACKET_ID;
	}

}
