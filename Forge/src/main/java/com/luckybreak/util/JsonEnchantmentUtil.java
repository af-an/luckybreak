package com.luckybreak.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for parsing and applying explicit enchantments from JSON.
 */
public final class JsonEnchantmentUtil {

    public record Entry(Identifier id, int level) {}

    private JsonEnchantmentUtil() {
    }

    public static List<Entry> parseList(JsonObject obj, String field) {
        List<Entry> parsed = new ArrayList<>();
        if (!obj.has(field)) {
            return parsed;
        }

        JsonElement root = obj.get(field);
        if (root.isJsonArray()) {
            JsonArray array = root.getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject enchantObj = element.getAsJsonObject();
                if (!enchantObj.has("id")) {
                    continue;
                }
                try {
                    Identifier id = Identifier.parse(enchantObj.get("id").getAsString());
                    int level = enchantObj.has("level") ? enchantObj.get("level").getAsInt() : 1;
                    if (level > 0) {
                        parsed.add(new Entry(id, level));
                    }
                } catch (Exception ignored) {
                }
            }
            return parsed;
        }

        if (root.isJsonObject()) {
            JsonObject mapObj = root.getAsJsonObject();
            for (String key : mapObj.keySet()) {
                try {
                    Identifier id = Identifier.parse(key);
                    int level = mapObj.get(key).getAsInt();
                    if (level > 0) {
                        parsed.add(new Entry(id, level));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return parsed;
    }

    public static void applyAny(ItemStack stack, RegistryAccess registryAccess, List<Entry> enchantments) {
        if (enchantments.isEmpty()) {
            return;
        }

        Registry<Enchantment> enchantmentRegistry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        for (Entry entry : enchantments) {
            Enchantment enchantment = enchantmentRegistry.getValue(entry.id());
            if (enchantment == null) {
                continue;
            }
            stack.enchant(enchantmentRegistry.wrapAsHolder(enchantment), entry.level());
        }
    }
}
