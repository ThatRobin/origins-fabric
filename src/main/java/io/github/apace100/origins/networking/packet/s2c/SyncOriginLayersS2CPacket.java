package io.github.apace100.origins.networking.packet.s2c;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.origin.OriginLayer;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public record SyncOriginLayersS2CPacket(Map<Identifier, OriginLayer> layersById) implements CustomPayload {

    public static final Id<SyncOriginLayersS2CPacket> PACKET_ID = new Id<>(Origins.identifier("s2c/sync_origin_layer_registry"));
    public static final PacketCodec<RegistryByteBuf, SyncOriginLayersS2CPacket> PACKET_CODEC = PacketCodec.of(SyncOriginLayersS2CPacket::write, SyncOriginLayersS2CPacket::read);

    public static SyncOriginLayersS2CPacket read(RegistryByteBuf buf) {

        Map<Identifier, OriginLayer> layersById = new HashMap<>();
        int count = buf.readVarInt();

        for (int i = 0; i < count; i++) {

            try {
                OriginLayer layer = OriginLayer.DATA_TYPE.receive(buf);
                layersById.put(layer.getId(), layer);
            }

            catch (Exception e) {
                Origins.LOGGER.error(e);
                throw e;
            }

        }

        return new SyncOriginLayersS2CPacket(layersById);

    }

    public void write(RegistryByteBuf buf) {

        Collection<OriginLayer> layers = layersById.values();

        buf.writeVarInt(layers.size());
        layers.forEach(layer -> OriginLayer.DATA_TYPE.send(buf, layer));

    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
