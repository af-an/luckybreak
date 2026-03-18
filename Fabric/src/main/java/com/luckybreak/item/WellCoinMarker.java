package com.luckybreak.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class WellCoinMarker {

    private static final String MARKER_KEY = "luckybreak_well_coin";

    private WellCoinMarker() {
    }

    public static void mark(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        stack.set(DataComponents.CUSTOM_DATA, data.update(tag -> tag.putBoolean(MARKER_KEY, true)));
    }

    public static boolean isMarked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }

        CompoundTag tag = data.copyTag();
        return tag.getBoolean(MARKER_KEY).orElse(false);
    }
}