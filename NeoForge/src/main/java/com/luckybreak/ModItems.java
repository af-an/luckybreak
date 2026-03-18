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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

public class ModItems {

    public static Item LUCKY_COMPASS;
    public static Item LUCKY_BOW;
    public static Item LUCKY_POTION;
    public static Item LUCKY_SWORD;
    public static Item LUCKY_PICKAXE;
    public static Item LUCKY_AXE;
    public static Item LUCKY_SHOVEL;
    public static Item LUCKY_HOE;
    public static Item GOLDEN_HEN_SPAWN_EGG;

    public static void register() {
        // No-op for NeoForge; registration is event-driven.
    }

    @EventBusSubscriber(modid = LuckyBreak.MOD_ID)
    public static final class NeoForgeEvents {
        @SubscribeEvent
        public static void onRegisterItems(RegisterEvent event) {
            event.register(Registries.ITEM, helper -> {
                Identifier luckyCompassId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_compass");
                Identifier luckyBowId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_bow");
                Identifier luckyPotionId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_potion");
                Identifier luckySwordId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_sword");
                Identifier luckyPickaxeId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_pickaxe");
                Identifier luckyAxeId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_axe");
                Identifier luckyShovelId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_shovel");
                Identifier luckyHoeId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_hoe");
                Identifier goldenHenEggId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "golden_hen_spawn_egg");

                LUCKY_COMPASS = new LuckyCompassItem(ResourceKey.create(Registries.ITEM, luckyCompassId));
                LUCKY_BOW = new LuckyBowItem(ResourceKey.create(Registries.ITEM, luckyBowId));
                LUCKY_POTION = new LuckyPotionItem(ResourceKey.create(Registries.ITEM, luckyPotionId));
                LUCKY_SWORD = new LuckySwordItem(ResourceKey.create(Registries.ITEM, luckySwordId));
                LUCKY_PICKAXE = new LuckyPickaxeItem(ResourceKey.create(Registries.ITEM, luckyPickaxeId));
                LUCKY_AXE = new LuckyAxeItem(ResourceKey.create(Registries.ITEM, luckyAxeId));
                LUCKY_SHOVEL = new LuckyShovelItem(ResourceKey.create(Registries.ITEM, luckyShovelId));
                LUCKY_HOE = new LuckyHoeItem(ResourceKey.create(Registries.ITEM, luckyHoeId));
                GOLDEN_HEN_SPAWN_EGG = new GoldenHenSpawnEggItem(ResourceKey.create(Registries.ITEM, goldenHenEggId));

                helper.register(luckyCompassId, LUCKY_COMPASS);
                helper.register(luckyBowId, LUCKY_BOW);
                helper.register(luckyPotionId, LUCKY_POTION);
                helper.register(luckySwordId, LUCKY_SWORD);
                helper.register(luckyPickaxeId, LUCKY_PICKAXE);
                helper.register(luckyAxeId, LUCKY_AXE);
                helper.register(luckyShovelId, LUCKY_SHOVEL);
                helper.register(luckyHoeId, LUCKY_HOE);
                helper.register(goldenHenEggId, GOLDEN_HEN_SPAWN_EGG);
            });
        }
    }
}
