package com.luckybreak.tooltip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luckybreak.LuckyBreak;
import com.luckybreak.item.WellCoinMarker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TooltipConfig {

    private static final String CONFIG_PATH = "assets/luckybreak/held_tooltips.json";
    private static final int DEFAULT_HELD_COLOR = 0xCFCFCF;
    private static final int DEFAULT_MENU_COLOR = 0xAAAAAA;
    private static final int DEFAULT_HUD_COLOR = 0xFFFFFF;
    private static final boolean DEFAULT_HUD_ACTION_BAR = true;
    private static final int DEFAULT_HELD_Y_OFFSET = 9;
    private static final Map<Item, List<TooltipEntry>> TOOLTIPS = new HashMap<>();
    private static int heldYOffset = DEFAULT_HELD_Y_OFFSET;
    private static boolean loaded;

    private TooltipConfig() {
    }

    public static Component getHeldTooltipFor(ItemStack stack) {
        TooltipTexts texts = getTooltipForStack(stack);
        return texts == null ? null : texts.held();
    }

    public static Component getHeldTooltipFor(Item item) {
        ensureLoaded();
        TooltipTexts texts = getDefaultTooltipForItem(item);
        return texts == null ? null : texts.held();
    }

    public static int getHeldColorFor(ItemStack stack) {
        TooltipTexts texts = getTooltipForStack(stack);
        return texts == null ? DEFAULT_HELD_COLOR : texts.heldColor();
    }

    public static int getHeldColorFor(Item item) {
        ensureLoaded();
        TooltipTexts texts = getDefaultTooltipForItem(item);
        return texts == null ? DEFAULT_HELD_COLOR : texts.heldColor();
    }

    public static Component getMenuTooltipFor(ItemStack stack) {
        TooltipTexts texts = getTooltipForStack(stack);
        return texts == null ? null : texts.menu();
    }

    public static Component getMenuTooltipFor(Item item) {
        ensureLoaded();
        TooltipTexts texts = getDefaultTooltipForItem(item);
        return texts == null ? null : texts.menu();
    }

    public static int getMenuColorFor(ItemStack stack) {
        TooltipTexts texts = getTooltipForStack(stack);
        return texts == null ? DEFAULT_MENU_COLOR : texts.menuColor();
    }

    public static int getMenuColorFor(Item item) {
        ensureLoaded();
        TooltipTexts texts = getDefaultTooltipForItem(item);
        return texts == null ? DEFAULT_MENU_COLOR : texts.menuColor();
    }

    public static List<Integer> getMenuLetterColorsFor(ItemStack stack) {
        TooltipTexts texts = getTooltipForStack(stack);
        return texts == null ? List.of() : texts.menuLetterColors();
    }

    public static Component getHudTooltipFor(ItemStack stack) {
        TooltipTexts texts = getTooltipForStack(stack);
        return texts == null ? null : texts.hud();
    }

    public static int getHudColorFor(ItemStack stack) {
        TooltipTexts texts = getTooltipForStack(stack);
        return texts == null ? DEFAULT_HUD_COLOR : texts.hudColor();
    }

    public static boolean getHudActionBarFor(ItemStack stack) {
        TooltipTexts texts = getTooltipForStack(stack);
        return texts == null || texts.hudActionBar();
    }

    public static boolean getHidePotionEffectsFor(ItemStack stack) {
        TooltipTexts texts = getTooltipForStack(stack);
        return texts != null && texts.hidePotionEffects();
    }

    public static int getHeldYOffset() {
        ensureLoaded();
        return heldYOffset;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        TOOLTIPS.clear();
        heldYOffset = DEFAULT_HELD_Y_OFFSET;

        InputStream input = TooltipConfig.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing tooltip config: {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            heldYOffset = parseInt(root, "held_y_offset", DEFAULT_HELD_Y_OFFSET, 0, 40);
            JsonArray entries = root.getAsJsonArray("entries");
            if (entries == null) {
                return;
            }

            for (JsonElement element : entries) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject entry = element.getAsJsonObject();
                Item target = parseTargetItem(entry);
                TooltipEntry tooltip = parseTooltip(entry);
                if (target != null && tooltip != null) {
                    TOOLTIPS.computeIfAbsent(target, ignored -> new ArrayList<>()).add(tooltip);
                }
            }
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load tooltip config", exception);
        }
    }

    private static TooltipTexts getTooltipForStack(ItemStack stack) {
        if (stack == null) {
            return null;
        }

        ensureLoaded();
        List<TooltipEntry> entries = TOOLTIPS.get(stack.getItem());
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        var customNameComponent = stack.get(DataComponents.CUSTOM_NAME);
        String customName = customNameComponent == null ? null : customNameComponent.getString();

        TooltipTexts fallback = null;
        for (TooltipEntry entry : entries) {
            if (entry.requireWellCoinMarker() && !WellCoinMarker.isMarked(stack)) {
                continue;
            }

            if (entry.customNameEquals() == null || entry.customNameEquals().isEmpty()) {
                if (fallback == null) {
                    fallback = entry.texts();
                }
                continue;
            }

            if (customName != null && customName.equalsIgnoreCase(entry.customNameEquals())) {
                return entry.texts();
            }
        }

        return fallback;
    }

    private static TooltipTexts getDefaultTooltipForItem(Item item) {
        if (item == null) {
            return null;
        }

        List<TooltipEntry> entries = TOOLTIPS.get(item);
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        for (TooltipEntry entry : entries) {
            if (entry.customNameEquals() == null || entry.customNameEquals().isEmpty()) {
                return entry.texts();
            }
        }

        return entries.getFirst().texts();
    }

    private static Item parseTargetItem(JsonObject entry) {
        try {
            if (entry.has("item")) {
                Identifier itemId = Identifier.parse(entry.get("item").getAsString());
                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                    return null;
                }
                Item item = BuiltInRegistries.ITEM.getValue(itemId);
                return item == Items.AIR ? null : item;
            }

            if (entry.has("block")) {
                Identifier blockId = Identifier.parse(entry.get("block").getAsString());
                if (!BuiltInRegistries.BLOCK.containsKey(blockId)) {
                    return null;
                }
                Item item = BuiltInRegistries.BLOCK.getValue(blockId).asItem();
                return item == Items.AIR ? null : item;
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private static TooltipEntry parseTooltip(JsonObject entry) {
        Component shared = parseComponent(entry, "text_key", "text");
        Component held = parseComponent(entry, "held_text_key", "held_text");
        Component menu = parseComponent(entry, "menu_text_key", "menu_text");
        Component hud = parseComponent(entry, "hud_text_key", "hud_text");
        Integer sharedColor = parseColor(entry, "color");
        Integer heldColor = parseColor(entry, "held_color");
        Integer menuColor = parseColor(entry, "menu_color");
        Integer hudColor = parseColor(entry, "hud_color");
        String customName = parseString(entry, "custom_name");
        boolean requireWellCoinMarker = parseBoolean(entry, "require_well_coin_marker", false);
        boolean hudActionBar = parseBoolean(entry, "hud_action_bar", DEFAULT_HUD_ACTION_BAR);
        boolean hidePotionEffects = parseBoolean(entry, "hide_potion_effects", false);
        List<Integer> menuLetterColors = parseColorList(entry, "menu_letter_colors");

        if (held == null) {
            held = shared;
        }
        if (menu == null) {
            menu = shared;
        }
        if (heldColor == null) {
            heldColor = sharedColor;
        }
        if (menuColor == null) {
            menuColor = sharedColor;
        }
        if (heldColor == null) {
            heldColor = DEFAULT_HELD_COLOR;
        }
        if (menuColor == null) {
            menuColor = DEFAULT_MENU_COLOR;
        }
        if (hudColor == null) {
            hudColor = DEFAULT_HUD_COLOR;
        }
        if (held == null && menu == null && hud == null) {
            return null;
        }

        return new TooltipEntry(customName, requireWellCoinMarker, new TooltipTexts(
                held,
                menu,
                heldColor,
                menuColor,
                List.copyOf(menuLetterColors),
                hud,
                hudColor,
                hudActionBar,
                hidePotionEffects
        ));
    }

    private static boolean parseBoolean(JsonObject entry, String field, boolean fallback) {
        if (!entry.has(field)) {
            return fallback;
        }
        try {
            return entry.get(field).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String parseString(JsonObject entry, String field) {
        if (!entry.has(field)) {
            return null;
        }

        try {
            String value = entry.get(field).getAsString();
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Component parseComponent(JsonObject entry, String keyField, String textField) {
        if (entry.has(keyField)) {
            return Component.translatable(entry.get(keyField).getAsString());
        }
        if (entry.has(textField)) {
            return Component.literal(entry.get(textField).getAsString());
        }
        return null;
    }

    private static List<Integer> parseColorList(JsonObject entry, String field) {
        if (!entry.has(field) || !entry.get(field).isJsonArray()) {
            return List.of();
        }

        List<Integer> colors = new ArrayList<>();
        for (JsonElement element : entry.getAsJsonArray(field)) {
            Integer parsed = parseColorElement(element);
            if (parsed != null) {
                colors.add(parsed);
            }
        }

        return colors;
    }

    private static Integer parseColor(JsonObject entry, String field) {
        if (!entry.has(field)) {
            return null;
        }

        return parseColorElement(entry.get(field));
    }

    private static Integer parseColorElement(JsonElement element) {
        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt() & 0xFFFFFF;
            }

            String value = element.getAsString().trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            } else if (value.startsWith("0x") || value.startsWith("0X")) {
                value = value.substring(2);
            }

            if (value.length() != 6) {
                return null;
            }

            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int parseInt(JsonObject root, String field, int fallback, int min, int max) {
        if (!root.has(field)) {
            return fallback;
        }
        try {
            int value = root.get(field).getAsInt();
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            return value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

        private record TooltipTexts(
            Component held,
            Component menu,
            int heldColor,
            int menuColor,
            List<Integer> menuLetterColors,
            Component hud,
            int hudColor,
                boolean hudActionBar,
                boolean hidePotionEffects
        ) {
    }

    private record TooltipEntry(String customNameEquals, boolean requireWellCoinMarker, TooltipTexts texts) {
    }
}
