package com.luckybreak.api.client.item.v1;

import com.luckybreak.api.event.Event;
import com.luckybreak.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Callback for appending tooltip lines to item stacks.
 * Modeled after Fabric/QFAPI's ItemTooltipCallback; no external dependency.
 */
public final class ItemTooltipCallback {

    private ItemTooltipCallback() {}

    public static final Event<TooltipCallback> EVENT = EventFactory.createArrayBacked(
            TooltipCallback.class,
            callbacks -> (stack, context, flag, lines) -> {
                for (TooltipCallback callback : callbacks) {
                    callback.getTooltip(stack, context, flag, lines);
                }
            }
    );

    @FunctionalInterface
    public interface TooltipCallback {
        void getTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines);
    }
}
