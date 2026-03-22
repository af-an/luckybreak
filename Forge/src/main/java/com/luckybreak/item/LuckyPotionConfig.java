package com.luckybreak.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luckybreak.LuckyBreak;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LuckyPotionConfig {

    private static final String CONFIG_PATH = "data/luckybreak/items/lucky_potion.json";
    private static PositiveEffectSettings positiveEffectSettings = PositiveEffectSettings.defaults();
    private static boolean loaded;

    private LuckyPotionConfig() {
    }

    public static PositiveEffectSettings positiveEffectSettings() {
        ensureLoaded();
        return positiveEffectSettings;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        positiveEffectSettings = PositiveEffectSettings.defaults();

        InputStream input = LuckyPotionConfig.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing Lucky Potion config: {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            positiveEffectSettings = parsePositiveEffects(root);
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load Lucky Potion config", exception);
        }
    }

    private static PositiveEffectSettings parsePositiveEffects(JsonObject root) {
        if (!root.has("positive_effects") || !root.get("positive_effects").isJsonObject()) {
            return PositiveEffectSettings.defaults();
        }

        PositiveEffectSettings defaults = PositiveEffectSettings.defaults();
        JsonObject obj = root.getAsJsonObject("positive_effects");

        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        int minEffects = parseInt(obj, "min_effects", defaults.minEffects(), 1, 16);
        int maxEffects = parseInt(obj, "max_effects", defaults.maxEffects(), minEffects, 16);

        List<PositiveEffectEntry> entries = parseEntries(obj, defaults.entries());
        if (entries.isEmpty()) {
            entries = defaults.entries();
        }

        return new PositiveEffectSettings(enabled, minEffects, maxEffects, List.copyOf(entries));
    }

    private static List<PositiveEffectEntry> parseEntries(JsonObject root, List<PositiveEffectEntry> fallback) {
        if (!root.has("entries") || !root.get("entries").isJsonArray()) {
            return fallback;
        }

        List<PositiveEffectEntry> entries = new ArrayList<>();
        var array = root.getAsJsonArray("entries");
        for (var element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            try {
                JsonObject obj = element.getAsJsonObject();
                String idValue = parseString(obj, "id", "").trim();
                if (idValue.isBlank()) {
                    continue;
                }

                ResourceLocation id = ResourceLocation.parse(idValue);
                int weight = parseInt(obj, "weight", 1, 0, 100000);
                int durationTicks = parseInt(obj, "duration_ticks", 1200, 1, 20 * 60 * 10);
                int amplifier = parseInt(obj, "amplifier", 0, 0, 10);
                if (weight <= 0) {
                    continue;
                }

                entries.add(new PositiveEffectEntry(id, weight, durationTicks, amplifier));
            } catch (Exception ignored) {
            }
        }

        return entries.isEmpty() ? fallback : entries;
    }

    private static int parseInt(JsonObject root, String field, int fallback, int min, int max) {
        if (!root.has(field)) {
            return fallback;
        }
        try {
            return Mth.clamp(root.get(field).getAsInt(), min, max);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean parseBoolean(JsonObject root, String field, boolean fallback) {
        if (!root.has(field)) {
            return fallback;
        }
        try {
            return root.get(field).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String parseString(JsonObject root, String field, String fallback) {
        if (!root.has(field)) {
            return fallback;
        }
        try {
            String value = root.get(field).getAsString();
            return value == null ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record PositiveEffectEntry(ResourceLocation id, int weight, int durationTicks, int amplifier) {
    }

    public record PositiveEffectSettings(
            boolean enabled,
            int minEffects,
            int maxEffects,
            List<PositiveEffectEntry> entries
    ) {
        public static PositiveEffectSettings defaults() {
            return new PositiveEffectSettings(
                    true,
                    4,
                    8,
                    List.of(
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "speed"), 20, 1800, 1),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "haste"), 18, 1800, 1),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "strength"), 16, 1800, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "instant_health"), 12, 1, 1),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "jump_boost"), 16, 1800, 1),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "regeneration"), 16, 900, 1),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "resistance"), 14, 1800, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "fire_resistance"), 14, 1800, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "water_breathing"), 12, 1800, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "invisibility"), 10, 1200, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "night_vision"), 12, 1800, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "health_boost"), 10, 1800, 1),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "absorption"), 12, 1800, 1),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "saturation"), 6, 1, 1),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "luck"), 10, 1800, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "slow_falling"), 10, 1200, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "conduit_power"), 8, 1200, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "dolphins_grace"), 8, 1200, 0),
                            new PositiveEffectEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "hero_of_the_village"), 6, 2400, 0)
                    )
            );
        }
    }

}
