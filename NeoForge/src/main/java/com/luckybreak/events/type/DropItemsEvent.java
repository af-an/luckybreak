package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import com.luckybreak.util.JsonEnchantmentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Drops one or more specific items above the break position.
 *
 * JSON fields:
 *   items: array of {
 *     "item": "minecraft:diamond_sword",
 *     "count_min": 1,
 *     "count_max": 1,
 *     "enchantments": [
 *       { "id": "minecraft:sharpness", "level": 5 },
 *       { "id": "minecraft:looting", "level": 3 }
 *     ]
 *   }
 */
public class DropItemsEvent implements LuckyEvent {

     private record ItemEntry(Item item, int min, int max, List<JsonEnchantmentUtil.Entry> enchantments) {}

    private final List<ItemEntry> entries;

    private DropItemsEvent(List<ItemEntry> entries) {
        this.entries = entries;
    }

    public static DropItemsEvent fromJson(JsonObject obj) {
        List<ItemEntry> entries = new ArrayList<>();
        JsonArray items = obj.getAsJsonArray("items");
        for (JsonElement el : items) {
            JsonObject it = el.getAsJsonObject();
            String id = it.get("item").getAsString();
            int min = it.has("count_min") ? it.get("count_min").getAsInt() : 1;
            int max = it.has("count_max") ? it.get("count_max").getAsInt() : min;
            List<JsonEnchantmentUtil.Entry> enchantments = JsonEnchantmentUtil.parseList(it, "enchantments");
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            if (item != null && !item.equals(Items.AIR)) {
                entries.add(new ItemEntry(item, min, max, enchantments));
            }
        }
        return new DropItemsEvent(entries);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockPos spawnPos = pos.above();
        for (ItemEntry entry : entries) {
            int count = entry.min() + level.getRandom().nextInt(Math.max(1, entry.max() - entry.min() + 1));
            ItemStack stack = new ItemStack(entry.item(), count);
            JsonEnchantmentUtil.applyAny(stack, level.registryAccess(), entry.enchantments());
            Block.popResource(level, spawnPos, stack);
        }
    }
}
