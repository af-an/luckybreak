package com.luckybreak;

import com.luckybreak.block.ForcedTierLuckyBlock;
import com.luckybreak.block.LuckyBlock;
import com.luckybreak.block.WeightedTierLuckyBlock;
import com.luckybreak.events.LuckyTier;
import com.luckybreak.item.LuckyTierBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

public class ModBlocks {

    public static Block LUCKY_BLOCK;
    public static Block VERY_LUCKY_BLOCK;
    public static Block VERY_UNLUCKY_BLOCK;
    public static Block MOSTLY_LUCKY_BLOCK;
    public static Block MOSTLY_UNLUCKY_BLOCK;

    public static void register() {
        // No-op for NeoForge; registration is event-driven.
    }

    @EventBusSubscriber(modid = LuckyBreak.MOD_ID)
    public static final class NeoForgeEvents {
        @SubscribeEvent
        public static void onRegisterBlocks(RegisterEvent event) {
            event.register(Registries.BLOCK, helper -> {
                ResourceLocation luckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_block");
                ResourceLocation veryLuckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "very_lucky_block");
                ResourceLocation veryUnluckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "very_unlucky_block");
                ResourceLocation mostlyLuckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "mostly_lucky_block");
                ResourceLocation mostlyUnluckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "mostly_unlucky_block");

                LUCKY_BLOCK = new LuckyBlock(ResourceKey.create(Registries.BLOCK, luckyBlockId));
                VERY_LUCKY_BLOCK = new ForcedTierLuckyBlock(ResourceKey.create(Registries.BLOCK, veryLuckyBlockId), LuckyTier.LUCKY);
                VERY_UNLUCKY_BLOCK = new ForcedTierLuckyBlock(ResourceKey.create(Registries.BLOCK, veryUnluckyBlockId), LuckyTier.UNLUCKY);
                MOSTLY_LUCKY_BLOCK = new WeightedTierLuckyBlock(ResourceKey.create(Registries.BLOCK, mostlyLuckyBlockId), WeightedTierLuckyBlock.Profile.MOSTLY_LUCKY);
                MOSTLY_UNLUCKY_BLOCK = new WeightedTierLuckyBlock(ResourceKey.create(Registries.BLOCK, mostlyUnluckyBlockId), WeightedTierLuckyBlock.Profile.MOSTLY_UNLUCKY);

                helper.register(luckyBlockId, LUCKY_BLOCK);
                helper.register(veryLuckyBlockId, VERY_LUCKY_BLOCK);
                helper.register(veryUnluckyBlockId, VERY_UNLUCKY_BLOCK);
                helper.register(mostlyLuckyBlockId, MOSTLY_LUCKY_BLOCK);
                helper.register(mostlyUnluckyBlockId, MOSTLY_UNLUCKY_BLOCK);
            });
        }

        @SubscribeEvent
        public static void onRegisterItems(RegisterEvent event) {
            event.register(Registries.ITEM, helper -> {
                if (LUCKY_BLOCK == null) {
                    return;
                }

                ResourceLocation luckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_block");
                ResourceLocation veryLuckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "very_lucky_block");
                ResourceLocation veryUnluckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "very_unlucky_block");
                ResourceLocation mostlyLuckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "mostly_lucky_block");
                ResourceLocation mostlyUnluckyBlockId = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "mostly_unlucky_block");

                helper.register(luckyBlockId, new BlockItem(LUCKY_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, luckyBlockId))));
                helper.register(veryLuckyBlockId, new LuckyTierBlockItem(VERY_LUCKY_BLOCK, ResourceKey.create(Registries.ITEM, veryLuckyBlockId), ChatFormatting.GREEN));
                helper.register(veryUnluckyBlockId, new LuckyTierBlockItem(VERY_UNLUCKY_BLOCK, ResourceKey.create(Registries.ITEM, veryUnluckyBlockId), ChatFormatting.RED));
                helper.register(mostlyLuckyBlockId, new LuckyTierBlockItem(MOSTLY_LUCKY_BLOCK, ResourceKey.create(Registries.ITEM, mostlyLuckyBlockId), ChatFormatting.DARK_AQUA));
                helper.register(mostlyUnluckyBlockId, new LuckyTierBlockItem(MOSTLY_UNLUCKY_BLOCK, ResourceKey.create(Registries.ITEM, mostlyUnluckyBlockId), ChatFormatting.GOLD));
            });
        }
    }
}
