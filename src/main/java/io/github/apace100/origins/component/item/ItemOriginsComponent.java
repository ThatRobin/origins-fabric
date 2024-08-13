package io.github.apace100.origins.component.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.apoli.util.codec.SetCodec;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.networking.packet.s2c.OpenChooseOriginScreenS2CPacket;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayerManager;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class ItemOriginsComponent implements TooltipAppender {

    public static final ItemOriginsComponent DEFAULT = new ItemOriginsComponent(new ObjectLinkedOpenHashSet<>());

    public static final Codec<ItemOriginsComponent> CODEC = SetCodec.of(Entry.CODEC).xmap(
        entries -> new ItemOriginsComponent(new ObjectLinkedOpenHashSet<>(entries)),
        itemOriginsComponent -> itemOriginsComponent.entries
    );
    public static final PacketCodec<PacketByteBuf, ItemOriginsComponent> PACKET_CODEC = PacketCodecs.collection(
        size -> new ObjectLinkedOpenHashSet<>(),
        Entry.PACKET_CODEC
    ).xmap(
        ItemOriginsComponent::new,
        itemOriginsComponent -> itemOriginsComponent.entries
    );

    final ObjectLinkedOpenHashSet<Entry> entries;

    ItemOriginsComponent(ObjectLinkedOpenHashSet<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public void appendTooltip(Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type) {

        String baseKey = "component.item.origins.origin";
        boolean appendedTooltips = false;

        for (Entry entry : entries) {

            if (!entry.canSelect()) {
                continue;
            }

            OriginLayer layer = OriginLayerManager.get(entry.layerId());
            Origin origin = OriginRegistry.get(entry.originId());

            String translationKey;
            Object[] args;

            if (origin == Origin.EMPTY) {
                translationKey = baseKey + ".layer";
                args = new Object[] { layer.getName() };
            }

            else {
                translationKey = baseKey + ".layer_and_origin";
                args = new Object[] { layer.getName(), origin.getName() };
            }

            tooltip.accept(Text.translatable(translationKey, args).formatted(Formatting.GRAY));
            appendedTooltips = true;

        }

        if (!appendedTooltips) {
            tooltip.accept(Text.translatable(baseKey + ".generic").formatted(Formatting.GRAY));
        }

    }

    public void selectOrigins(LivingEntity user) {

        if (!(user instanceof ServerPlayerEntity player)) {
            return;
        }

        OriginComponent originComponent = ModComponents.ORIGIN.get(player);
        boolean assignedOrigin = false;

        for (Entry entry : entries) {

            if (!entry.canSelect()) {
                continue;
            }

            originComponent.setOrigin(OriginLayerManager.get(entry.layerId()), OriginRegistry.get(entry.originId()));
            assignedOrigin = true;

        }

        if (!assignedOrigin) {
            OriginLayerManager.getLayers()
                .stream()
                .filter(OriginLayer::isEnabled)
                .forEach(layer -> originComponent.setOrigin(layer, Origin.EMPTY));
        }

        assignedOrigin |= originComponent.checkAutoChoosingLayers(player, false);
        int originOptions = OriginLayerManager.getOriginOptionCount(player);

        originComponent.selectingOrigin(!assignedOrigin || originOptions > 0);
        originComponent.sync();

        if (originComponent.isSelectingOrigin()) {
            ServerPlayNetworking.send(player, new OpenChooseOriginScreenS2CPacket(false));
        }

    }

    public record Entry(Identifier layerId, Identifier originId) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            OriginLayerManager.VALIDATING_CODEC.fieldOf("layer").forGetter(Entry::layerId),
            OriginRegistry.VALIDATING_CODEC.optionalFieldOf("origin", Origin.EMPTY.getId()).forGetter(Entry::originId)
        ).apply(instance, Entry::new));

        public static final PacketCodec<PacketByteBuf, Entry> PACKET_CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, Entry::layerId,
            Identifier.PACKET_CODEC, Entry::originId,
            Entry::new
        );

        public boolean canSelect() {

            OriginLayer layer = OriginLayerManager.getNullable(layerId);
            Origin origin = OriginRegistry.getNullable(originId);

            return layer != null
                && origin != null
                && layer.isEnabled()
                && (layer.contains(origin) || origin.isSpecial());

        }

    }

}
