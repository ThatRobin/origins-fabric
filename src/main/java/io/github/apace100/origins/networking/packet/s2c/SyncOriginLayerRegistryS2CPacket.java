package io.github.apace100.origins.networking.packet.s2c;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.origin.OriginLayer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record SyncOriginLayerRegistryS2CPacket(Map<Identifier, OriginLayer> layers) implements CustomPayload {

    public static final Id<SyncOriginLayerRegistryS2CPacket> PACKET_ID = new Id<>(Origins.identifier("s2c/sync_origin_layer_registry"));
    public static final PacketCodec<RegistryByteBuf, SyncOriginLayerRegistryS2CPacket> PACKET_CODEC = PacketCodec.of(SyncOriginLayerRegistryS2CPacket::write, SyncOriginLayerRegistryS2CPacket::read);

    public static SyncOriginLayerRegistryS2CPacket read(RegistryByteBuf buffer) {
        return new SyncOriginLayerRegistryS2CPacket(buffer.readMap(value -> new HashMap<>(), PacketByteBuf::readIdentifier, valBuf -> OriginLayer.receive((RegistryByteBuf) valBuf)));
    }

    public void write(RegistryByteBuf buffer) {
        buffer.writeMap(layers, PacketByteBuf::writeIdentifier, (valueBuffer, layer) -> layer.send((RegistryByteBuf) valueBuffer));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
