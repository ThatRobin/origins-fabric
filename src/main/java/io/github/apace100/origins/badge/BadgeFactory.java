package io.github.apace100.origins.badge;

import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableData.Instance;
import io.github.apace100.calio.registry.DataObjectFactory;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public record BadgeFactory(Identifier id, SerializableData serializableData, Function<SerializableData.Instance, Badge> factory) implements DataObjectFactory<Badge> {

    @Override
    public SerializableData getSerializableData() {
        return serializableData();
    }

    @Override
    public Badge fromData(SerializableData.Instance instance) {
        return factory().apply(instance);
    }

    @Override
    public Instance toData(Badge badge, SerializableData serializableData) {
        return badge.toData(serializableData.instance());
    }

}
