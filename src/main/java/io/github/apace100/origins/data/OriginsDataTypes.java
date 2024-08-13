package io.github.apace100.origins.data;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.github.apace100.calio.codec.StrictCodec;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.origins.origin.Impact;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginUpgrade;
import net.minecraft.util.Identifier;

import java.util.List;

public final class OriginsDataTypes {

    public static final SerializableDataType<Impact> IMPACT = SerializableDataType.enumValue(Impact.class);

    @Deprecated(forRemoval = true)
    public static final SerializableDataType<OriginUpgrade> UPGRADE = OriginUpgrade.DATA_TYPE;

    @Deprecated(forRemoval = true)
    public static final SerializableDataType<List<OriginUpgrade>> UPGRADES = UPGRADE.listOf();

    public static final SerializableDataType<OriginLayer.ConditionedOrigin> CONDITIONED_ORIGIN = OriginLayer.ConditionedOrigin.DATA_TYPE;

    public static final SerializableDataType<List<OriginLayer.ConditionedOrigin>> CONDITIONED_ORIGINS = CONDITIONED_ORIGIN.listOf();

    public static final SerializableDataType<OriginLayer.ConditionedOrigin> ORIGIN_OR_CONDITIONED_ORIGIN = SerializableDataType.of(
        new StrictCodec<>() {

            @Override
            public <T> Pair<OriginLayer.ConditionedOrigin, T> strictDecode(DynamicOps<T> ops, T input) {

                DataResult<String> inputString = ops.getStringValue(input);

                if (inputString.isSuccess()) {

                    Identifier originId = SerializableDataTypes.IDENTIFIER.strictParse(ops, ops.createString(inputString.getOrThrow()));
                    OriginLayer.ConditionedOrigin conditionedOrigin = new OriginLayer.ConditionedOrigin(null, Lists.newArrayList(originId));

                    return new Pair<>(conditionedOrigin, input);

                }

                else {
                    return CONDITIONED_ORIGIN.strictDecode(ops, input);
                }

            }

            @Override
            public <T> T strictEncode(OriginLayer.ConditionedOrigin input, DynamicOps<T> ops, T prefix) {
                return CONDITIONED_ORIGIN.strictEncode(input, ops, prefix);
            }

        },
        CONDITIONED_ORIGIN.packetCodec()
    );

    public static final SerializableDataType<List<OriginLayer.ConditionedOrigin>> ORIGINS_OR_CONDITIONED_ORIGINS = ORIGIN_OR_CONDITIONED_ORIGIN.listOf();

    public static final SerializableDataType<OriginLayer.GuiTitle> GUI_TITLE = OriginLayer.GuiTitle.DATA_TYPE;

}
