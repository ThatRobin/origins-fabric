package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.content.TemporaryCobwebBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class ModBlocks {

    public static final TemporaryCobwebBlock TEMPORARY_COBWEB = register("temporary_cobweb", false, () -> new TemporaryCobwebBlock(AbstractBlock.Settings.create()
        .mapColor(MapColor.WHITE_GRAY)
        .strength(4.0F)
        .requiresTool()
        .noCollision()
        .solid()));

    public static void register() {

    }

    private static <B extends Block> B register(String name, boolean withBlockItem, Supplier<B> blockSupplier) {

        Identifier blockId = Origins.identifier(name);
        B block = Registry.register(Registries.BLOCK, blockId, blockSupplier.get());

        if (withBlockItem) {
            Registry.register(Registries.ITEM, blockId, new BlockItem(block, new Item.Settings()));
        }

        return block;

    }

}
