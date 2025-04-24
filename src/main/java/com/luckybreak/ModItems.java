package com.luckybreak;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles registration of all mod items.
 */
public class ModItems {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModItems.class);
    public static final String MOD_ID = LuckyBreakMod.MOD_ID;
    
    /**
     * Register an item with the given name.
     */
    private static <T extends Item> T register(String name, T item) {
        Identifier id = new Identifier(MOD_ID, name);
        T registeredItem = Registry.register(Registries.ITEM, id, item);
        LOGGER.info("Registered item " + name + " with ID: " + id);
        return registeredItem;
    }
    
    /**
     * Register all items for the mod.
     */
    public static void registerItems() {
        try {
            LOGGER.info("Registering items for " + MOD_ID);
            // Add any standalone items here (not BlockItems)
            LOGGER.info("Successfully registered items for " + MOD_ID);
        } catch (Exception e) {
            LOGGER.error("Error registering items", e);
            throw new RuntimeException("Failed to register items", e);
        }
    }
} 