package io.github.apace100.origins.networking.packet.s2c;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.origin.Origin;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public record SyncOriginRegistryS2CPacket(Map<Identifier, Origin> originsById) implements CustomPayload {

    public static final Id<SyncOriginRegistryS2CPacket> PACKET_ID = new Id<>(Origins.identifier("s2c/sync_origin_registry"));
    public static final PacketCodec<RegistryByteBuf, SyncOriginRegistryS2CPacket> PACKET_CODEC = PacketCodec.of(SyncOriginRegistryS2CPacket::write, SyncOriginRegistryS2CPacket::read);

    public static SyncOriginRegistryS2CPacket read(RegistryByteBuf buf) {

        Map<Identifier, Origin> originsById = new HashMap<>();
        int count = buf.readVarInt();

        for (int i = 0; i < count; i++) {

            try {
                Origin origin = Origin.receive(buf);
                originsById.put(origin.getId(), origin);
            }

            catch (Exception e) {
                Origins.LOGGER.error(e.getMessage());
                throw e;
            }

        }

        return new SyncOriginRegistryS2CPacket(originsById);

    }

    public void write(RegistryByteBuf buf) {

        Collection<Origin> origins = this.originsById().values();

        buf.writeVarInt(origins.size());
        origins.forEach(origin -> origin.send(buf));

    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
