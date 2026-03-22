package com.luckybreak;

import com.luckybreak.block.LuckyBlock;
import com.luckybreak.block.ForcedTierLuckyBlock;
import com.luckybreak.block.WeightedTierLuckyBlock;
import com.luckybreak.events.LuckyTier;
import com.luckybreak.item.LuckyTierBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.BiFunction;

public class ModBlocks {

    public static final Block LUCKY_BLOCK = register("lucky_block", LuckyBlock::new);
    public static final Block VERY_LUCKY_BLOCK = register(
        "very_lucky_block",
        key -> new ForcedTierLuckyBlock(key, LuckyTier.LUCKY),
        (block, itemKey) -> new LuckyTierBlockItem(
            block,
            itemKey,
            ChatFormatting.GREEN
        )
    );
    public static final Block VERY_UNLUCKY_BLOCK = register(
        "very_unlucky_block",
        key -> new ForcedTierLuckyBlock(key, LuckyTier.UNLUCKY),
        (block, itemKey) -> new LuckyTierBlockItem(
            block,
            itemKey,
            ChatFormatting.RED
        )
    );
    public static final Block MOSTLY_LUCKY_BLOCK = register(
        "mostly_lucky_block",
        key -> new WeightedTierLuckyBlock(key, WeightedTierLuckyBlock.Profile.MOSTLY_LUCKY),
        (block, itemKey) -> new LuckyTierBlockItem(
            block,
            itemKey,
            ChatFormatting.DARK_AQUA
        )
    );
    public static final Block MOSTLY_UNLUCKY_BLOCK = register(
        "mostly_unlucky_block",
        key -> new WeightedTierLuckyBlock(key, WeightedTierLuckyBlock.Profile.MOSTLY_UNLUCKY),
        (block, itemKey) -> new LuckyTierBlockItem(
            block,
            itemKey,
            ChatFormatting.GOLD
        )
    );

    private static Block register(String name, java.util.function.Function<ResourceKey<Block>, Block> factory) {
    return register(name, factory,
        (block, itemKey) -> new BlockItem(block, new Item.Properties().setId(itemKey)));
    }

    private static Block register(String name,
                  java.util.function.Function<ResourceKey<Block>, Block> factory,
                  BiFunction<Block, ResourceKey<Item>, Item> itemFactory) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Block block = factory.apply(blockKey);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    Registry.register(BuiltInRegistries.ITEM, itemKey, itemFactory.apply(block, itemKey));
        return block;
    }

    public static void register() {
        // Calling this method triggers static field initialization, registering all blocks.
    }
}
