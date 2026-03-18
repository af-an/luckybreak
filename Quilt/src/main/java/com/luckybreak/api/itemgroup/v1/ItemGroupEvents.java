package com.luckybreak.api.itemgroup.v1;

import com.luckybreak.api.event.Event;
import com.luckybreak.api.event.EventFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class ItemGroupEvents {

    private ItemGroupEvents() {
    }

    public static Event<ModifyEntries> modifyEntriesEvent(ResourceKey<CreativeModeTab> tabKey) {
        return CreativeTabRegistry.modifyEntriesEvent(tabKey);
    }

    @FunctionalInterface
    public interface ModifyEntries {
        void modifyEntries(Entries entries);
    }

    public interface Entries {
        void add(ItemStack stack);

        default void addItem(Item item) {
            add(new ItemStack(item));
        }

        default void addItems(Item... items) {
            if (items == null) {
                return;
            }
            for (Item item : items) {
                if (item != null) {
                    addItem(item);
                }
            }
        }

        default void addAfter(Item anchor, Item... items) {
            addItems(items);
        }

        default void addBefore(Item anchor, Item... items) {
            addItems(items);
        }

        default void addAll(Iterable<ItemStack> stacks) {
            for (ItemStack stack : stacks) {
                add(stack);
            }
        }
        void forEach(Consumer<ItemStack> consumer);
    }
}