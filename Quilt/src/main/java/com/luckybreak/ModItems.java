package com.luckybreak;

import com.luckybreak.item.GoldenHenSpawnEggItem;
import com.luckybreak.item.LuckyAxeItem;
import com.luckybreak.item.LuckyBowItem;
import com.luckybreak.item.LuckyCompassItem;
import com.luckybreak.item.LuckyHoeItem;
import com.luckybreak.item.LuckyPickaxeItem;
import com.luckybreak.item.LuckyPotionItem;
import com.luckybreak.item.LuckyShovelItem;
import com.luckybreak.item.LuckySwordItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {

    public static final Item LUCKY_COMPASS = register("lucky_compass", LuckyCompassItem::new);
    public static final Item LUCKY_BOW = register("lucky_bow", LuckyBowItem::new);
    public static final Item LUCKY_POTION = register("lucky_potion", LuckyPotionItem::new);
    public static final Item LUCKY_SWORD = register("lucky_sword", LuckySwordItem::new);
    public static final Item LUCKY_PICKAXE = register("lucky_pickaxe", LuckyPickaxeItem::new);
    public static final Item LUCKY_AXE = register("lucky_axe", LuckyAxeItem::new);
    public static final Item LUCKY_SHOVEL = register("lucky_shovel", LuckyShovelItem::new);
    public static final Item LUCKY_HOE = register("lucky_hoe", LuckyHoeItem::new);
    public static final Item GOLDEN_HEN_SPAWN_EGG = register("golden_hen_spawn_egg", GoldenHenSpawnEggItem::new);

    private static Item register(String name, Function<ResourceKey<Item>, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, name));
        Item item = factory.apply(key);
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    public static void register() {
        // Triggers static field initialization, registering all items.
    }
}
