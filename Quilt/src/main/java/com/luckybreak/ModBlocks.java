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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.function.BiFunction;

public class ModBlocks {

    public static Block LUCKY_BLOCK = Blocks.DIRT;
    public static Block VERY_LUCKY_BLOCK = Blocks.DIRT;
    public static Block VERY_UNLUCKY_BLOCK = Blocks.DIRT;
    public static Block MOSTLY_LUCKY_BLOCK = Blocks.DIRT;
    public static Block MOSTLY_UNLUCKY_BLOCK = Blocks.DIRT;

    private static boolean registered;

    private static Block register(String name, java.util.function.Function<ResourceKey<Block>, Block> factory) {
    return register(name, factory,
        (block, itemKey) -> new BlockItem(block, new Item.Properties().setId(itemKey)));
    }

    private static Block register(String name,
                  java.util.function.Function<ResourceKey<Block>, Block> factory,
                  BiFunction<Block, ResourceKey<Item>, Item> itemFactory) {
        Identifier id = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Block block = factory.apply(blockKey);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        block.getStateDefinition().getPossibleStates().forEach(state -> {
            if (Block.BLOCK_STATE_REGISTRY.getId(state) < 0) {
                Block.BLOCK_STATE_REGISTRY.add(state);
                state.initCache();
            }
        });
        Registry.register(BuiltInRegistries.ITEM, id, itemFactory.apply(block, itemKey));
        return block;
    }

    public static void register() {
        if (registered) {
            return;
        }

        LUCKY_BLOCK = tryRegister(
            "lucky_block",
            LuckyBlock::new,
            (block, itemKey) -> new BlockItem(block, new Item.Properties().setId(itemKey)),
            Blocks.DIRT
        );
        VERY_LUCKY_BLOCK = tryRegister(
            "very_lucky_block",
            key -> new ForcedTierLuckyBlock(key, LuckyTier.LUCKY),
            (block, itemKey) -> new LuckyTierBlockItem(block, itemKey, ChatFormatting.GREEN),
            Blocks.DIRT
        );
        VERY_UNLUCKY_BLOCK = tryRegister(
            "very_unlucky_block",
            key -> new ForcedTierLuckyBlock(key, LuckyTier.UNLUCKY),
            (block, itemKey) -> new LuckyTierBlockItem(block, itemKey, ChatFormatting.RED),
            Blocks.DIRT
        );
        MOSTLY_LUCKY_BLOCK = tryRegister(
            "mostly_lucky_block",
            key -> new WeightedTierLuckyBlock(key, WeightedTierLuckyBlock.Profile.MOSTLY_LUCKY),
            (block, itemKey) -> new LuckyTierBlockItem(block, itemKey, ChatFormatting.DARK_AQUA),
            Blocks.DIRT
        );
        MOSTLY_UNLUCKY_BLOCK = tryRegister(
            "mostly_unlucky_block",
            key -> new WeightedTierLuckyBlock(key, WeightedTierLuckyBlock.Profile.MOSTLY_UNLUCKY),
            (block, itemKey) -> new LuckyTierBlockItem(block, itemKey, ChatFormatting.GOLD),
            Blocks.DIRT
        );

        registered = true;
    }

    private static Block tryRegister(
            String name,
            java.util.function.Function<ResourceKey<Block>, Block> factory,
            BiFunction<Block, ResourceKey<Item>, Item> itemFactory,
            Block fallback
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, name);
        if (BuiltInRegistries.BLOCK.containsKey(id)) {
            Block existing = BuiltInRegistries.BLOCK.getValue(id);
            return existing == null ? fallback : existing;
        }

        try {
            return register(name, factory, itemFactory);
        } catch (IllegalStateException exception) {
            LuckyBreak.LOGGER.error("Block registration failed for {}. Falling back to {}.", id, BuiltInRegistries.BLOCK.getKey(fallback), exception);
            return fallback;
        }
    }
}
