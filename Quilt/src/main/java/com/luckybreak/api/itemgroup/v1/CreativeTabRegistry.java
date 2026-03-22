package com.luckybreak.api.itemgroup.v1;

import com.luckybreak.api.event.Event;
import com.luckybreak.api.event.EventFactory;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CreativeTabRegistry {

    // Keys of tabs registered by this mod, in insertion order (used for pagination)
    private static final Set<ResourceKey<CreativeModeTab>> MOD_REGISTERED_TABS = new LinkedHashSet<>();

    private static final Map<ResourceKey<CreativeModeTab>, Event<ItemGroupEvents.ModifyEntries>> MODIFY_ENTRIES_EVENTS = new LinkedHashMap<>();

    private CreativeTabRegistry() {
    }

    static Event<ItemGroupEvents.ModifyEntries> modifyEntriesEvent(ResourceKey<CreativeModeTab> tabKey) {
        return MODIFY_ENTRIES_EVENTS.computeIfAbsent(tabKey,
                ignored -> EventFactory.createArrayBacked(ItemGroupEvents.ModifyEntries.class, callbacks -> entries -> {
                    for (ItemGroupEvents.ModifyEntries callback : callbacks) {
                        callback.modifyEntries(entries);
                    }
                }));
    }

    /** Returns true if this registry key belongs to a mod-registered tab. */
    public static boolean isModdedTab(ResourceKey<CreativeModeTab> key) {
        return MOD_REGISTERED_TABS.contains(key);
    }

    /** Returns all mod-registered tab keys in insertion order. */
    public static List<ResourceKey<CreativeModeTab>> getModdedTabKeys() {
        return new ArrayList<>(MOD_REGISTERED_TABS);
    }

    /**
     * Registers a custom creative tab. Initially placed at column -1 (off-screen) so that
     * {@code CreativeModeTabs.validate()} doesn't collide with vanilla slots. The screen
     * mixin ({@code CreativeModeInventoryScreenMixin}) will assign the real row/column
     * from a second "page" of tabs when the creative menu opens.
     */
    public static CreativeModeTab registerTab(ResourceLocation id, Component title, Supplier<ItemStack> iconSupplier) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(iconSupplier, "iconSupplier");

        if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) {
            return BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id);
        }

        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, id);
        Event<ItemGroupEvents.ModifyEntries> event = modifyEntriesEvent(tabKey);

        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, -1)
                .title(title)
                .icon(() -> normalize(iconSupplier.get()))
                .displayItems((params, output) -> {
                    EntryList entries = new EntryList();
                    event.invoker().modifyEntries(entries);
                    entries.forEach(output::accept);
                })
                .build();

        CreativeModeTab registered = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
        MOD_REGISTERED_TABS.add(tabKey);
        return registered;
    }

    private static ItemStack normalize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static final class EntryList implements ItemGroupEvents.Entries {
        private final List<ItemStack> stacks = new ArrayList<>();

        @Override
        public void add(ItemStack stack) {
            ItemStack normalized = normalize(stack);
            if (!normalized.isEmpty()) {
                stacks.add(normalized);
            }
        }

        @Override
        public void addAfter(Item anchor, Item... items) {
            int anchorIndex = findLast(anchor);
            insertAt(anchorIndex < 0 ? stacks.size() : anchorIndex + 1, items);
        }

        @Override
        public void addBefore(Item anchor, Item... items) {
            int anchorIndex = findFirst(anchor);
            insertAt(anchorIndex < 0 ? 0 : anchorIndex, items);
        }

        @Override
        public void forEach(Consumer<ItemStack> consumer) {
            for (ItemStack stack : stacks) {
                consumer.accept(stack);
            }
        }

        private int findFirst(Item item) {
            for (int i = 0; i < stacks.size(); i++) {
                if (stacks.get(i).is(item)) {
                    return i;
                }
            }
            return -1;
        }

        private int findLast(Item item) {
            for (int i = stacks.size() - 1; i >= 0; i--) {
                if (stacks.get(i).is(item)) {
                    return i;
                }
            }
            return -1;
        }

        private void insertAt(int index, Item... items) {
            if (items == null || items.length == 0) {
                return;
            }
            int current = Math.max(0, Math.min(index, stacks.size()));
            for (Item item : items) {
                if (item == null) {
                    continue;
                }
                ItemStack stack = normalize(new ItemStack(item));
                if (stack.isEmpty()) {
                    continue;
                }
                stacks.add(current, stack);
                current++;
            }
        }
    }
}
