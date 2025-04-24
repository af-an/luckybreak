package com.luckybreak;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.SharedConstants;
import net.minecraft.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.luckybreak.recipe.ModRecipes;
import com.luckybreak.recipe.RecipeEvents;

/**
 * Main mod class for Lucky Break.
 */
public class LuckyBreakMod implements ModInitializer {
    /**
     * ID of the mod.
     */
    public static final String MOD_ID = "lucky_break";

    /**
     * Logger for the mod.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Item group for the mod.
     */
    public static final RegistryKey<ItemGroup> LUCKY_BREAK_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            new Identifier(MOD_ID, "lucky_break_group")
    );

    public static ItemGroup LUCKY_BREAK_GROUP;

    // Flag to avoid initializing Bootstrap multiple times
    private static boolean isBootstrapInitialized = false;

    /**
     * Ensures the Minecraft Bootstrap is initialized.
     * This is required for proper registry access in 1.21.5.
     */
    private static void ensureBootstrapInitialized() {
        if (!isBootstrapInitialized) {
            try {
                LOGGER.info("Initializing Bootstrap for MC 1.21.5");
                SharedConstants.createGameVersion();
                Bootstrap.initialize();
                isBootstrapInitialized = true;
                LOGGER.info("Bootstrap initialization successful");
            } catch (Exception e) {
                LOGGER.warn("Bootstrap initialization failed: " + e.getMessage());
                // Continue anyway - it might already be initialized
            }
        }
    }

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Lucky Break Mod");
        
        // Ensure Bootstrap is initialized before accessing registries
        ensureBootstrapInitialized();
        
        // Register blocks (which also registers BlockItems)
        ModBlocks.registerBlocks();
        
        // Register any standalone items
        ModItems.registerItems();
        
        // Register recipes
        ModRecipes.registerRecipes();
        RecipeEvents.registerEvents();
        
        // Create and register item group
        LUCKY_BREAK_GROUP = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModBlocks.LUCKY_BLOCK.asItem()))
                .displayName(Text.translatable("itemGroup.lucky_break.lucky_break_group"))
                .build();
        
        // Register the item group
        Registry.register(Registries.ITEM_GROUP, LUCKY_BREAK_GROUP_KEY, LUCKY_BREAK_GROUP);
        
        // Add items to the group using the block's item form
        ItemGroupEvents.modifyEntriesEvent(LUCKY_BREAK_GROUP_KEY).register(content -> {
            content.add(ModBlocks.LUCKY_BLOCK.asItem());
            content.add(ModBlocks.UNLUCKY_BLOCK.asItem());
        });

        LOGGER.info("Lucky Break Mod initialized");
    }

    public static Identifier createId(String path) {
        return Identifier.of(MOD_ID, path);
    }
} 