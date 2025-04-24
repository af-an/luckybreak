package com.luckybreak;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Handles registration of all mod blocks.
 */
public class ModBlocks {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModBlocks.class);
    public static final String MOD_ID = LuckyBreakMod.MOD_ID;
    
    // Block instances
    public static Block LUCKY_BLOCK;
    public static Block UNLUCKY_BLOCK;
    
    /**
     * Register a block and its BlockItem
     */
    private static <T extends Block> T register(String name, T block) {
        // Create identifier
        Identifier id = new Identifier(MOD_ID, name);
        
        // Register the block
        T registeredBlock = Registry.register(Registries.BLOCK, id, block);
        
        // Create and register the BlockItem with specific settings
        Item.Settings itemSettings = new Item.Settings();
        BlockItem blockItem = new BlockItem(registeredBlock, itemSettings);
        Registry.register(Registries.ITEM, id, blockItem);
        
        LOGGER.info("Registered block and BlockItem " + name + " with ID: " + id);
        return registeredBlock;
    }
    
    /**
     * Register all blocks for the mod.
     */
    public static void registerBlocks() {
        try {
            LOGGER.info("Registering blocks for " + MOD_ID);
            
            // Register the lucky block
            LUCKY_BLOCK = register("lucky_block", new LuckyBlock());
            
            // Register the unlucky block
            UNLUCKY_BLOCK = register("unlucky_block", new UnluckyBlock());
            
            LOGGER.info("Successfully registered blocks for " + MOD_ID);
        } catch (Exception e) {
            LOGGER.error("Error registering blocks", e);
            throw new RuntimeException("Failed to register blocks", e);
        }
    }
    
    /**
     * Creates a light level function that always returns the same value
     */
    private static ToIntFunction<BlockState> createLightLevelFunction(int lightLevel) {
        return (state) -> lightLevel;
    }
} 