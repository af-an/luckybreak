package com.luckybreak.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;

public final class LuckyItemVisuals {

    private LuckyItemVisuals() {
    }

    public static Item.Properties apply(String itemId, Item.Properties properties) {
        return properties.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, LuckyItemVisualConfig.forItem(itemId).glintEnabled());
    }

    public static Component styleName(String itemId, Component baseName) {
        return baseName.copy().withStyle(style -> style.withColor(TextColor.fromRgb(LuckyItemVisualConfig.forItem(itemId).nameColor())));
    }
}
