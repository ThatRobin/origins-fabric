package io.github.apace100.origins.networking.packet.s2c;

import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.origin.Origin;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Map;

public record SyncOriginRegistryS2CPacket(Map<Identifier, SerializableData.Instance> origins) implements CustomPayload {

    public static final Id<SyncOriginRegistryS2CPacket> PACKET_ID = new Id<>(Origins.identifier("s2c/sync_origin_registry"));
    public static final PacketCodec<RegistryByteBuf, SyncOriginRegistryS2CPacket> PACKET_CODEC = PacketCodec.of(SyncOriginRegistryS2CPacket::write, SyncOriginRegistryS2CPacket::read);

    public static SyncOriginRegistryS2CPacket read(RegistryByteBuf buf) {
        return new SyncOriginRegistryS2CPacket(buf.readMap(PacketByteBuf::readIdentifier, valueBuf -> Origin.DATA.read((RegistryByteBuf) valueBuf)));
    }

    public void write(RegistryByteBuf buf) {
        buf.writeMap(origins, PacketByteBuf::writeIdentifier, (valueBuf, origin) -> Origin.DATA.write((RegistryByteBuf) valueBuf, origin));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
