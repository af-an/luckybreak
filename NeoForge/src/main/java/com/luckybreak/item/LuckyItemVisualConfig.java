package com.luckybreak.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luckybreak.LuckyBreak;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LuckyItemVisualConfig {

    private static final String CONFIG_BASE_PATH = "data/luckybreak/items/";
    private static final boolean DEFAULT_GLINT_ENABLED = true;
    private static final float DEFAULT_GLINT_STRENGTH = 2.0F;
    private static final int DEFAULT_NAME_COLOR = 0xFFD700;

    private static final Map<String, VisualSettings> CACHE = new ConcurrentHashMap<>();

    private LuckyItemVisualConfig() {
    }

    public static VisualSettings forItem(String itemId) {
        return CACHE.computeIfAbsent(itemId, LuckyItemVisualConfig::load);
    }

    private static VisualSettings load(String itemId) {
        String path = CONFIG_BASE_PATH + itemId + ".json";
        boolean glintEnabled = DEFAULT_GLINT_ENABLED;
        float glintStrength = DEFAULT_GLINT_STRENGTH;
        int nameColor = DEFAULT_NAME_COLOR;

        InputStream input = LuckyItemVisualConfig.class.getClassLoader().getResourceAsStream(path);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing item visual config: {}", path);
            return new VisualSettings(glintEnabled, glintStrength, nameColor);
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("glint_enabled")) {
                glintEnabled = root.get("glint_enabled").getAsBoolean();
            }
            glintStrength = parseFloat(root, "glint_strength", DEFAULT_GLINT_STRENGTH, 0.0F, 2.0F);
            nameColor = parseColor(root, "name_color", DEFAULT_NAME_COLOR);
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load item visual config for {}", itemId, exception);
        }

        return new VisualSettings(glintEnabled, glintStrength, nameColor);
    }

    private static float parseFloat(JsonObject root, String field, float fallback, float min, float max) {
        if (!root.has(field)) {
            return fallback;
        }

        try {
            float value = root.get(field).getAsFloat();
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return fallback;
            }
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseColor(JsonObject root, String field, int fallback) {
        if (!root.has(field)) {
            return fallback;
        }

        try {
            var element = root.get(field);
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
                return fallback;
            }

            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record VisualSettings(boolean glintEnabled, float glintStrength, int nameColor) {
    }
}