package com.luckybreak.item;

import com.luckybreak.events.LuckyEventRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class LuckyTierBlockItem extends BlockItem {

    private final ChatFormatting color;

    public LuckyTierBlockItem(Block block, ResourceKey<Item> key, ChatFormatting color) {
        super(block, new Item.Properties().setId(key));
        this.color = color;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (LuckyEventRegistry.INSTANCE.matchLuckyBlockNameColorForTierBlocks()) {
            return super.getName(stack);
        }
        return super.getName(stack).copy().withStyle(color);
    }
}
