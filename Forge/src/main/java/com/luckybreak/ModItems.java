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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

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
        // No-op for Forge; registration is event-driven.
    }

    @Mod.EventBusSubscriber(modid = LuckyBreak.MOD_ID)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onRegisterItems(RegisterEvent event) {
            event.register(Registries.ITEM, helper -> {
                ResourceLocation luckyCompassId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_compass");
                ResourceLocation luckyBowId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_bow");
                ResourceLocation luckyPotionId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_potion");
                ResourceLocation luckySwordId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_sword");
                ResourceLocation luckyPickaxeId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_pickaxe");
                ResourceLocation luckyAxeId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_axe");
                ResourceLocation luckyShovelId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_shovel");
                ResourceLocation luckyHoeId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_hoe");
                ResourceLocation goldenHenEggId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "golden_hen_spawn_egg");

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
