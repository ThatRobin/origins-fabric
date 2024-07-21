package io.github.apace100.origins.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.origins.networking.packet.s2c.SyncOriginRegistryS2CPacket;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OriginRegistry {

    public static final Codec<Identifier> VALIDATING_CODEC = Identifier.CODEC.comapFlatMap(
        id -> contains(id)
            ? DataResult.success(id)
            : DataResult.error(() -> "Could not get origin from id '" + id + "', as it was not registered!"),
        id -> id
    );

    public static final PacketCodec<ByteBuf, Origin> DISPATCH_PACKET_CODEC = Identifier.PACKET_CODEC.xmap(OriginRegistry::get, Origin::getIdentifier);
    public static final Codec<Origin> DISPATCH_CODEC = Identifier.CODEC.comapFlatMap(
        OriginRegistry::getResult,
        Origin::getIdentifier
    );

    private static final Map<Identifier, Origin> idToOrigin = new HashMap<>();

    public static Origin register(Origin origin) {
        return register(origin.getIdentifier(), origin);
    }

    public static Origin register(Identifier id, Origin origin) {

        if (idToOrigin.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate origin id tried to register: '" + id.toString() + "'");
        }

        idToOrigin.put(id, origin);
        return origin;

    }

    protected static Origin update(Identifier id, Origin origin) {
        idToOrigin.remove(id);
        return register(id, origin);
    }

    public static int size() {
        return idToOrigin.size();
    }

    public static Set<Identifier> keys() {
        return idToOrigin.keySet();
    }

    public static Set<Map.Entry<Identifier, Origin>> entries() {
        return idToOrigin.entrySet();
    }

    public static Collection<Origin> values() {
        return idToOrigin.values();
    }

    public static DataResult<Origin> getResult(Identifier id) {
        return idToOrigin.containsKey(id)
            ? DataResult.success(idToOrigin.get(id))
            : DataResult.error(() -> "Could not get origin from id '" + id.toString() + "', as it was not registered!");
    }

    public static Origin get(Identifier id) {

        if (!idToOrigin.containsKey(id)) {
            throw new IllegalArgumentException("Could not get origin from id '" + id.toString() + "', as it was not registered!");
        }

        return idToOrigin.get(id);

    }

    @Nullable
    public static Origin getNullable(Identifier originId) {
        return idToOrigin.get(originId);
    }

    public static boolean contains(Identifier id) {
        return idToOrigin.containsKey(id);
    }

    public static boolean contains(Origin origin) {
        return contains(origin.getIdentifier());
    }

    public static void clear() {
        idToOrigin.clear();
    }

    public static void reset() {
        clear();
        register(Origin.EMPTY);
    }

    public static void remove(Identifier id) {
        idToOrigin.remove(id);
    }

    public static void send(ServerPlayerEntity player) {

        Map<Identifier, SerializableData.Instance> origins = new HashMap<>();
        idToOrigin.forEach((id, origin) -> origins.put(id, origin.toData()));

        ServerPlayNetworking.send(player, new SyncOriginRegistryS2CPacket(origins));

    }

}
