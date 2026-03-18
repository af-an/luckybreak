package com.luckybreak;

import com.luckybreak.block.LuckyBlock;
import com.luckybreak.block.ForcedTierLuckyBlock;
import com.luckybreak.block.WeightedTierLuckyBlock;
import com.luckybreak.events.LuckyTier;
import com.luckybreak.item.LuckyTierBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

public class ModBlocks {

    public static Block LUCKY_BLOCK;
    public static Block VERY_LUCKY_BLOCK;
    public static Block VERY_UNLUCKY_BLOCK;
    public static Block MOSTLY_LUCKY_BLOCK;
    public static Block MOSTLY_UNLUCKY_BLOCK;

    public static void register() {
        // No-op for Forge; registration is event-driven.
    }

    @Mod.EventBusSubscriber(modid = LuckyBreak.MOD_ID)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onRegisterBlocks(RegisterEvent event) {
            event.register(Registries.BLOCK, helper -> {
                Identifier luckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_block");
                Identifier veryLuckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "very_lucky_block");
                Identifier veryUnluckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "very_unlucky_block");
                Identifier mostlyLuckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "mostly_lucky_block");
                Identifier mostlyUnluckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "mostly_unlucky_block");

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

                Identifier luckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_block");
                Identifier veryLuckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "very_lucky_block");
                Identifier veryUnluckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "very_unlucky_block");
                Identifier mostlyLuckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "mostly_lucky_block");
                Identifier mostlyUnluckyBlockId = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "mostly_unlucky_block");

                helper.register(luckyBlockId, new BlockItem(LUCKY_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, luckyBlockId))));
                helper.register(veryLuckyBlockId, new LuckyTierBlockItem(VERY_LUCKY_BLOCK, ResourceKey.create(Registries.ITEM, veryLuckyBlockId), ChatFormatting.GREEN));
                helper.register(veryUnluckyBlockId, new LuckyTierBlockItem(VERY_UNLUCKY_BLOCK, ResourceKey.create(Registries.ITEM, veryUnluckyBlockId), ChatFormatting.RED));
                helper.register(mostlyLuckyBlockId, new LuckyTierBlockItem(MOSTLY_LUCKY_BLOCK, ResourceKey.create(Registries.ITEM, mostlyLuckyBlockId), ChatFormatting.DARK_AQUA));
                helper.register(mostlyUnluckyBlockId, new LuckyTierBlockItem(MOSTLY_UNLUCKY_BLOCK, ResourceKey.create(Registries.ITEM, mostlyUnluckyBlockId), ChatFormatting.GOLD));
            });
        }
    }
}
