package io.github.apace100.origins.networking.packet.s2c;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.badge.Badge;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public record SyncBadgeRegistryS2CPacket(Map<Identifier, List<Badge>> badgesById) implements CustomPayload {

    public static final Id<SyncBadgeRegistryS2CPacket> PACKET_ID = new Id<>(Origins.identifier("s2c/sync_badge_registry"));
    public static final PacketCodec<RegistryByteBuf, SyncBadgeRegistryS2CPacket> PACKET_CODEC = PacketCodec.of(SyncBadgeRegistryS2CPacket::write, SyncBadgeRegistryS2CPacket::read);

    public static SyncBadgeRegistryS2CPacket read(RegistryByteBuf buf) {

        Map<Identifier, List<Badge>> badgesById = new HashMap<>();
        int entriesCount = buf.readVarInt();

        for (int i = 0; i < entriesCount; i++) {

            Identifier id = buf.readIdentifier();
            int badgesCount = buf.readVarInt();

            for (int j = 0; j < badgesCount; j++) {
                badgesById
                    .computeIfAbsent(id, k -> new LinkedList<>())
                    .add(Badge.receive(buf));
            }

        }

        return new SyncBadgeRegistryS2CPacket(badgesById);

    }

    public void write(RegistryByteBuf buf) {

        buf.writeVarInt(badgesById.size());
        for (Map.Entry<Identifier, List<Badge>> entry : badgesById.entrySet()) {

            Identifier id = entry.getKey();
            List<Badge> badges = entry.getValue();

            buf.writeIdentifier(id);
            buf.writeVarInt(badges.size());

            for (Badge badge : badges) {
                badge.send(buf);
            }

        }

    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
