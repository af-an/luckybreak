package com.luckybreak.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luckybreak.LuckyBreak;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GoldenHenConfig {

    private static final String CONFIG_PATH = "data/luckybreak/entities/golden_hen.json";

    private static final int DEFAULT_LAY_INTERVAL_MIN_TICKS = 6000;
    private static final int DEFAULT_LAY_INTERVAL_MAX_TICKS = 12000;
    private static final int DEFAULT_DROP_COUNT_MIN = 1;
    private static final int DEFAULT_DROP_COUNT_MAX = 1;
    private static final int DEFAULT_GOLD_NUGGET_DROP_COUNT_MIN = 3;
    private static final int DEFAULT_GOLD_NUGGET_DROP_COUNT_MAX = 9;
    private static final String DEFAULT_DROP_ITEM = "minecraft:gold_ingot";
    private static final String DEFAULT_DEATH_DROP_ITEM = "minecraft:gold_ingot";
    private static final int DEFAULT_DEATH_DROP_COUNT = 1;
    private static final boolean DEFAULT_PLAY_LAY_SOUND = true;
    private static final boolean DEFAULT_ALLOW_SPAWN_EGG_ON_PARENT = true;
    private static final boolean DEFAULT_GLINT_ENABLED = true;
    private static final int DEFAULT_GLINT_COLOR = 0xFFFFFF;
    private static final float DEFAULT_GLINT_MIN_ALPHA = 0.08F;
    private static final float DEFAULT_GLINT_MAX_ALPHA = 0.45F;
    private static final float DEFAULT_GLINT_PULSE_SPEED = 0.12F;
    private static final String DEFAULT_GLINT_TEXTURE = "luckybreak:textures/misc/enchanted_glint_armor.png";
    private static final String DEFAULT_AMBIENT_SOUND_ID = "minecraft:entity.chicken.ambient";
    private static final String DEFAULT_HURT_SOUND_ID = "minecraft:entity.chicken.hurt";
    private static final String DEFAULT_DEATH_SOUND_ID = "minecraft:entity.chicken.death";
    private static final String DEFAULT_LAY_SOUND_ID = "minecraft:entity.chicken.egg";

    private static int layIntervalMinTicks = DEFAULT_LAY_INTERVAL_MIN_TICKS;
    private static int layIntervalMaxTicks = DEFAULT_LAY_INTERVAL_MAX_TICKS;
    private static int dropCountMin = DEFAULT_DROP_COUNT_MIN;
    private static int dropCountMax = DEFAULT_DROP_COUNT_MAX;
    private static int goldNuggetDropCountMin = DEFAULT_GOLD_NUGGET_DROP_COUNT_MIN;
    private static int goldNuggetDropCountMax = DEFAULT_GOLD_NUGGET_DROP_COUNT_MAX;
    private static List<DropEntry> dropPool = new ArrayList<>();
    private static Item deathDropItem = Items.GOLD_INGOT;
    private static int deathDropCount = DEFAULT_DEATH_DROP_COUNT;
    private static boolean playLaySound = DEFAULT_PLAY_LAY_SOUND;
    private static boolean allowSpawnEggOnParent = DEFAULT_ALLOW_SPAWN_EGG_ON_PARENT;
    private static boolean goldenHenGlintEnabled = DEFAULT_GLINT_ENABLED;
    private static int goldenHenGlintColor = DEFAULT_GLINT_COLOR;
    private static float goldenHenGlintMinAlpha = DEFAULT_GLINT_MIN_ALPHA;
    private static float goldenHenGlintMaxAlpha = DEFAULT_GLINT_MAX_ALPHA;
    private static float goldenHenGlintPulseSpeed = DEFAULT_GLINT_PULSE_SPEED;
    private static Identifier goldenHenGlintTexture = Identifier.parse(DEFAULT_GLINT_TEXTURE);
    private static SoundEvent ambientSound = SoundEvents.CHICKEN_AMBIENT;
    private static SoundEvent hurtSound = SoundEvents.CHICKEN_HURT;
    private static SoundEvent deathSound = SoundEvents.CHICKEN_DEATH;
    private static SoundEvent laySound = SoundEvents.CHICKEN_EGG;
    private static boolean loaded;

    private GoldenHenConfig() {
    }

    public static int nextLayIntervalTicks(RandomSource random) {
        ensureLoaded();
        return Mth.nextInt(random, layIntervalMinTicks, layIntervalMaxTicks);
    }

    public static int nextDropCount(RandomSource random, Item item) {
        ensureLoaded();
        if (item == Items.GOLD_NUGGET) {
            return Mth.nextInt(random, goldNuggetDropCountMin, goldNuggetDropCountMax);
        }

        return Mth.nextInt(random, dropCountMin, dropCountMax);
    }

    public static Item nextDropItem(RandomSource random) {
        ensureLoaded();
        int totalWeight = 0;
        for (DropEntry entry : dropPool) {
            totalWeight += entry.weight();
        }

        if (totalWeight <= 0) {
            return Items.GOLD_INGOT;
        }

        int roll = random.nextInt(totalWeight);
        int running = 0;
        for (DropEntry entry : dropPool) {
            running += entry.weight();
            if (roll < running) {
                return entry.item();
            }
        }

        return dropPool.getLast().item();
    }

    public static Item deathDropItem() {
        ensureLoaded();
        return deathDropItem;
    }

    public static int deathDropCount() {
        ensureLoaded();
        return deathDropCount;
    }

    public static boolean playLaySound() {
        ensureLoaded();
        return playLaySound;
    }

    public static boolean allowSpawnEggOnParent() {
        ensureLoaded();
        return allowSpawnEggOnParent;
    }

    public static boolean goldenHenGlintEnabled() {
        ensureLoaded();
        return goldenHenGlintEnabled;
    }

    public static int goldenHenGlintColor() {
        ensureLoaded();
        return goldenHenGlintColor;
    }

    public static float goldenHenGlintMinAlpha() {
        ensureLoaded();
        return goldenHenGlintMinAlpha;
    }

    public static float goldenHenGlintMaxAlpha() {
        ensureLoaded();
        return goldenHenGlintMaxAlpha;
    }

    public static float goldenHenGlintPulseSpeed() {
        ensureLoaded();
        return goldenHenGlintPulseSpeed;
    }

    public static Identifier goldenHenGlintTexture() {
        ensureLoaded();
        return goldenHenGlintTexture;
    }

    public static SoundEvent ambientSound() {
        ensureLoaded();
        return ambientSound;
    }

    public static SoundEvent hurtSound() {
        ensureLoaded();
        return hurtSound;
    }

    public static SoundEvent deathSound() {
        ensureLoaded();
        return deathSound;
    }

    public static SoundEvent laySound() {
        ensureLoaded();
        return laySound;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        layIntervalMinTicks = DEFAULT_LAY_INTERVAL_MIN_TICKS;
        layIntervalMaxTicks = DEFAULT_LAY_INTERVAL_MAX_TICKS;
        dropCountMin = DEFAULT_DROP_COUNT_MIN;
        dropCountMax = DEFAULT_DROP_COUNT_MAX;
        goldNuggetDropCountMin = DEFAULT_GOLD_NUGGET_DROP_COUNT_MIN;
        goldNuggetDropCountMax = DEFAULT_GOLD_NUGGET_DROP_COUNT_MAX;
        dropPool = new ArrayList<>();
        dropPool.add(new DropEntry(Items.GOLD_INGOT, 100));
        deathDropItem = Items.GOLD_INGOT;
        deathDropCount = DEFAULT_DEATH_DROP_COUNT;
        playLaySound = DEFAULT_PLAY_LAY_SOUND;
        allowSpawnEggOnParent = DEFAULT_ALLOW_SPAWN_EGG_ON_PARENT;
        goldenHenGlintEnabled = DEFAULT_GLINT_ENABLED;
        goldenHenGlintColor = DEFAULT_GLINT_COLOR;
        goldenHenGlintMinAlpha = DEFAULT_GLINT_MIN_ALPHA;
        goldenHenGlintMaxAlpha = DEFAULT_GLINT_MAX_ALPHA;
        goldenHenGlintPulseSpeed = DEFAULT_GLINT_PULSE_SPEED;
        goldenHenGlintTexture = Identifier.parse(DEFAULT_GLINT_TEXTURE);
        ambientSound = SoundEvents.CHICKEN_AMBIENT;
        hurtSound = SoundEvents.CHICKEN_HURT;
        deathSound = SoundEvents.CHICKEN_DEATH;
        laySound = SoundEvents.CHICKEN_EGG;

        InputStream input = GoldenHenConfig.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing Golden Hen config: {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            layIntervalMinTicks = parseInt(root, "lay_interval_min_ticks", DEFAULT_LAY_INTERVAL_MIN_TICKS, 20, 72000);
            layIntervalMaxTicks = parseInt(root, "lay_interval_max_ticks", DEFAULT_LAY_INTERVAL_MAX_TICKS, layIntervalMinTicks, 72000);
            dropCountMin = parseInt(root, "drop_count_min", DEFAULT_DROP_COUNT_MIN, 1, 64);
            dropCountMax = parseInt(root, "drop_count_max", DEFAULT_DROP_COUNT_MAX, dropCountMin, 64);
            goldNuggetDropCountMin = parseInt(root, "gold_nugget_drop_count_min", DEFAULT_GOLD_NUGGET_DROP_COUNT_MIN, 1, 64);
            goldNuggetDropCountMax = parseInt(root, "gold_nugget_drop_count_max", DEFAULT_GOLD_NUGGET_DROP_COUNT_MAX, goldNuggetDropCountMin, 64);
            playLaySound = parseBoolean(root, "play_lay_sound", DEFAULT_PLAY_LAY_SOUND);
            allowSpawnEggOnParent = parseBoolean(root, "allow_spawn_egg_on_parent", DEFAULT_ALLOW_SPAWN_EGG_ON_PARENT);
            goldenHenGlintEnabled = parseBoolean(root, "glint_enabled", DEFAULT_GLINT_ENABLED);
            goldenHenGlintColor = parseColor(root, "glint_color", DEFAULT_GLINT_COLOR);
            goldenHenGlintMinAlpha = parseFloat(root, "glint_min_alpha", DEFAULT_GLINT_MIN_ALPHA, 0.0F, 1.0F);
            goldenHenGlintMaxAlpha = parseFloat(root, "glint_max_alpha", DEFAULT_GLINT_MAX_ALPHA, goldenHenGlintMinAlpha, 1.0F);
            goldenHenGlintPulseSpeed = parseFloat(root, "glint_pulse_speed", DEFAULT_GLINT_PULSE_SPEED, 0.0F, 5.0F);
            goldenHenGlintTexture = parseIdentifier(root, "glint_texture", Identifier.parse(DEFAULT_GLINT_TEXTURE));

            ambientSound = parseSoundEvent(root, "ambient_sound", DEFAULT_AMBIENT_SOUND_ID, SoundEvents.CHICKEN_AMBIENT);
            hurtSound = parseSoundEvent(root, "hurt_sound", DEFAULT_HURT_SOUND_ID, SoundEvents.CHICKEN_HURT);
            deathSound = parseSoundEvent(root, "death_sound", DEFAULT_DEATH_SOUND_ID, SoundEvents.CHICKEN_DEATH);
            laySound = parseSoundEvent(root, "lay_sound", DEFAULT_LAY_SOUND_ID, SoundEvents.CHICKEN_EGG);

            Item parsedDeathDrop = parseItem(root, "death_drop_item");
            if (parsedDeathDrop != null) deathDropItem = parsedDeathDrop;
            deathDropCount = parseInt(root, "death_drop_count", DEFAULT_DEATH_DROP_COUNT, 0, 64);

            List<DropEntry> parsedPool = parseDropPool(root);
            if (!parsedPool.isEmpty()) {
                dropPool = parsedPool;
            }

            Item configuredDrop = parseItem(root, "drop_item");
            if (configuredDrop != null) {
                if (!root.has("drop_pool")) {
                    dropPool = new ArrayList<>();
                    dropPool.add(new DropEntry(configuredDrop, 1));
                }
            }
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load Golden Hen config", exception);
        }
    }

    private static List<DropEntry> parseDropPool(JsonObject root) {
        List<DropEntry> parsed = new ArrayList<>();
        if (!root.has("drop_pool") || !root.get("drop_pool").isJsonArray()) {
            return parsed;
        }

        JsonArray pool = root.getAsJsonArray("drop_pool");
        for (JsonElement element : pool) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject entry = element.getAsJsonObject();
            Item item = parseItem(entry, "item");
            if (item == null) {
                continue;
            }

            int weight = parseInt(entry, "weight", 1, 1, 100000);
            parsed.add(new DropEntry(item, weight));
        }

        return parsed;
    }

    private static int parseInt(JsonObject root, String field, int fallback, int min, int max) {
        if (!root.has(field)) {
            return fallback;
        }

        try {
            int value = root.get(field).getAsInt();
            return Mth.clamp(value, min, max);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float parseFloat(JsonObject root, String field, float fallback, float min, float max) {
        if (!root.has(field)) {
            return fallback;
        }

        try {
            float value = root.get(field).getAsFloat();
            return Mth.clamp(value, min, max);
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

    private static Item parseItem(JsonObject root, String field) {
        if (!root.has(field)) {
            return null;
        }

        try {
            Identifier id = Identifier.parse(root.get(field).getAsString());
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                return null;
            }

            Item item = BuiltInRegistries.ITEM.getValue(id);
            return item == Items.AIR ? null : item;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int parseColor(JsonObject root, String field, int fallback) {
        if (!root.has(field)) {
            return fallback;
        }

        try {
            JsonElement element = root.get(field);
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

    private static Identifier parseIdentifier(JsonObject root, String field, Identifier fallback) {
        if (!root.has(field)) {
            return fallback;
        }

        try {
            return Identifier.parse(root.get(field).getAsString());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static SoundEvent parseSoundEvent(JsonObject root, String field, String fallbackId, SoundEvent fallback) {
        try {
            String rawId = root.has(field) ? root.get(field).getAsString() : fallbackId;
            Identifier id = Identifier.parse(rawId);
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
                return fallback;
            }

            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(id);
            return sound == null ? fallback : sound;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record DropEntry(Item item, int weight) {
    }
}
