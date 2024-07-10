package io.github.apace100.origins.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.apace100.origins.registry.ModDataComponentTypes;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ComponentHolder {

    @Shadow
    protected abstract <T extends TooltipAppender> void appendTooltip(ComponentType<T> componentType, Item.TooltipContext context, Consumer<Text> textConsumer, TooltipType type);

    @Inject(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;appendTooltip(Lnet/minecraft/component/ComponentType;Lnet/minecraft/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/item/tooltip/TooltipType;)V", ordinal = 0))
    private void origins$appendItemOriginsTooltip(Item.TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir, @Local Consumer<Text> tooltipConsumer) {
        this.appendTooltip(ModDataComponentTypes.ORIGIN, context, tooltipConsumer, type);
    }

}
