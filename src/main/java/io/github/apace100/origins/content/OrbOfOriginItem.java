package io.github.apace100.origins.content;

import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.util.OriginTargetComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;

public class OrbOfOriginItem extends Item {

    public OrbOfOriginItem() {
        this(new Settings().maxCount(1).rarity(Rarity.RARE));
    }

    public OrbOfOriginItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        ItemStack stack = user.getStackInHand(hand);
        OriginTargetComponent.applyTargets(stack, user);

        stack.decrementUnlessCreative(1, user);
        return TypedActionResult.consume(stack);

    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {

        OriginTargetComponent.getTargetsAsMap(stack).forEach((layer, origin) -> {

            String baseKey = "item.origins.orb_of_origin.layer_";
            Object[] args;

            if (origin == Origin.EMPTY) {
                baseKey += "generic";
                args = new Object[] { Text.translatable(layer.getTranslationKey()) };
            } else {
                baseKey += "specific";
                args = new Object[] {
                    Text.translatable(layer.getTranslationKey()),
                    Text.translatable(origin.getOrCreateNameTranslationKey())
                };
            }

            tooltip.add(Text.translatable(baseKey, args));

        });

    }

}
