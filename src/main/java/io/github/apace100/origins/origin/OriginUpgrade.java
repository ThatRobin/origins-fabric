package io.github.apace100.origins.origin;

import io.github.apace100.calio.data.CompoundSerializableDataType;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Deprecated
public record OriginUpgrade(Identifier advancementCondition, Identifier upgradeToOrigin, @Nullable String announcement) {

    public static final CompoundSerializableDataType<OriginUpgrade> DATA_TYPE = SerializableDataType.compound(
        new SerializableData()
            .add("condition", SerializableDataTypes.IDENTIFIER)
            .add("origin", SerializableDataTypes.IDENTIFIER)
            .add("announcement", SerializableDataTypes.STRING, null),
        data -> new OriginUpgrade(
            data.get("condition"),
            data.get("origin"),
            data.get("announcement")
        ),
        (originUpgrade, serializableData) -> serializableData.instance()
            .set("condition", originUpgrade.advancementCondition())
            .set("origin", originUpgrade.upgradeToOrigin())
            .set("announcement", originUpgrade.announcement())
    );

}
