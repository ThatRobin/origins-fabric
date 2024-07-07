package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.content.TemporaryCobwebBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
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

    public static final TemporaryCobwebBlock TEMPORARY_COBWEB;

    public static void register() {

    }

    private static <B extends Block> B register(String blockId, boolean withBlockItem, Supplier<B> blockSupplier) {

        B block = Registry.register(Registries.BLOCK, Origins.identifier(blockId), blockSupplier.get());
        if (withBlockItem) {
            Registry.register(Registries.ITEM, Origins.identifier(blockId), new BlockItem(block, new Item.Settings()));
        }

        return block;

    }

    static {
        TEMPORARY_COBWEB = register("temporary_cobweb", true, () -> new TemporaryCobwebBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.WHITE_GRAY)
            .strength(4.0F)
            .requiresTool()
            .noCollision()
            .solid()));
    }

}
