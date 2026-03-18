package com.luckybreak.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luckybreak.LuckyBreak;
import com.luckybreak.ModBlocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LuckyAxeConfig {

    private static final String CONFIG_PATH = "data/luckybreak/items/lucky_axe.json";
    private static final int DEFAULT_DURABILITY = 32;
    private static final float BASE_ATTACK_DAMAGE = 1.0F;
    private static final float BASE_ATTACK_SPEED = 4.0F;
    private static final float DEFAULT_ATTACK_DAMAGE = 6.0F;
    private static final float DEFAULT_ATTACK_SPEED = -3.0F;

    private static int durability = DEFAULT_DURABILITY;
    private static float attackDamage = DEFAULT_ATTACK_DAMAGE;
    private static float attackSpeed = DEFAULT_ATTACK_SPEED;
    private static BonusDropSettings bonusDropSettings = BonusDropSettings.defaults();
    private static LightningSettings lightningSettings = LightningSettings.defaults();
    private static LeafStormSettings leafStormSettings = LeafStormSettings.defaults();
    private static BeeSwarmSettings beeSwarmSettings = BeeSwarmSettings.defaults();
    private static TimberLaunchSettings timberLaunchSettings = TimberLaunchSettings.defaults();
    private static LootPinataSettings lootPinataSettings = LootPinataSettings.defaults();
    private static LuckyBlockTransformSettings luckyBlockTransformSettings = LuckyBlockTransformSettings.defaults();
    private static TemporaryEnchantSettings temporaryEnchantSettings = TemporaryEnchantSettings.defaults();
    private static DropParticleSettings dropParticleSettings = DropParticleSettings.defaults();
    private static boolean loaded;

    private LuckyAxeConfig() {
    }

    public static int durability() {
        ensureLoaded();
        return durability;
    }

    public static float attackDamage() {
        ensureLoaded();
        return attackDamage;
    }

    public static float attackSpeed() {
        ensureLoaded();
        return attackSpeed;
    }

    public static BonusDropSettings bonusDropSettings() {
        ensureLoaded();
        return bonusDropSettings;
    }

    public static LightningSettings lightningSettings() {
        ensureLoaded();
        return lightningSettings;
    }

    public static LeafStormSettings leafStormSettings() {
        ensureLoaded();
        return leafStormSettings;
    }

    public static BeeSwarmSettings beeSwarmSettings() {
        ensureLoaded();
        return beeSwarmSettings;
    }

    public static TimberLaunchSettings timberLaunchSettings() {
        ensureLoaded();
        return timberLaunchSettings;
    }

    public static LootPinataSettings lootPinataSettings() {
        ensureLoaded();
        return lootPinataSettings;
    }

    public static LuckyBlockTransformSettings luckyBlockTransformSettings() {
        ensureLoaded();
        return luckyBlockTransformSettings;
    }

    public static TemporaryEnchantSettings temporaryEnchantSettings() {
        ensureLoaded();
        return temporaryEnchantSettings;
    }

    public static DropParticleSettings dropParticleSettings() {
        ensureLoaded();
        return dropParticleSettings;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        durability = DEFAULT_DURABILITY;
        attackDamage = DEFAULT_ATTACK_DAMAGE;
        attackSpeed = DEFAULT_ATTACK_SPEED;
        bonusDropSettings = BonusDropSettings.defaults();
        lightningSettings = LightningSettings.defaults();
        leafStormSettings = LeafStormSettings.defaults();
        beeSwarmSettings = BeeSwarmSettings.defaults();
        timberLaunchSettings = TimberLaunchSettings.defaults();
        lootPinataSettings = LootPinataSettings.defaults();
        luckyBlockTransformSettings = LuckyBlockTransformSettings.defaults();
        temporaryEnchantSettings = TemporaryEnchantSettings.defaults();
        dropParticleSettings = DropParticleSettings.defaults();

        InputStream input = LuckyAxeConfig.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing Lucky Axe config: {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            durability = parseInt(root, "durability", DEFAULT_DURABILITY, 1, 10000);
            attackDamage = parseAttackDamageModifier(root);
            attackSpeed = parseAttackSpeedModifier(root);
            bonusDropSettings = parseBonusDrops(root);
            lightningSettings = parseLightning(root);
            leafStormSettings = parseLeafStorm(root);
            beeSwarmSettings = parseBeeSwarm(root);
            timberLaunchSettings = parseTimberLaunch(root);
            lootPinataSettings = parseLootPinata(root);
            luckyBlockTransformSettings = parseLuckyBlockTransform(root);
            temporaryEnchantSettings = parseTemporaryEnchantments(root);
            dropParticleSettings = parseDropParticles(root);
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load Lucky Axe config", exception);
        }
    }

    private static float parseAttackDamageModifier(JsonObject root) {
        if (root.has("attack_damage")) {
            float total = parseFloat(root, "attack_damage", BASE_ATTACK_DAMAGE + DEFAULT_ATTACK_DAMAGE, 0.0F, 1001.0F);
            return total - BASE_ATTACK_DAMAGE;
        }
        return parseFloat(root, "attack_damage_modifier", DEFAULT_ATTACK_DAMAGE, -100.0F, 1000.0F);
    }

    private static float parseAttackSpeedModifier(JsonObject root) {
        if (root.has("attack_speed")) {
            float total = parseFloat(root, "attack_speed", BASE_ATTACK_SPEED + DEFAULT_ATTACK_SPEED, 0.0F, 14.0F);
            return total - BASE_ATTACK_SPEED;
        }
        return parseFloat(root, "attack_speed_modifier", DEFAULT_ATTACK_SPEED, -10.0F, 10.0F);
    }

    private static LuckyBlockTransformSettings parseLuckyBlockTransform(JsonObject root) {
        if (!root.has("lucky_block_transform") || !root.get("lucky_block_transform").isJsonObject()) {
            return LuckyBlockTransformSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject("lucky_block_transform");
        boolean enabled = parseBoolean(obj, "enabled", true);
        float chance = parseFloat(obj, "chance", 0.05F, 0.0F, 1.0F);
        float luckyBlockChance = parseFloat(obj, "lucky_block_chance", 1.0F, 0.0F, 1.0F);
        return new LuckyBlockTransformSettings(enabled, chance, luckyBlockChance);
    }

    private static TemporaryEnchantSettings parseTemporaryEnchantments(JsonObject root) {
        if (!root.has("temporary_enchantments") || !root.get("temporary_enchantments").isJsonObject()) {
            return TemporaryEnchantSettings.defaults();
        }

        TemporaryEnchantSettings defaults = TemporaryEnchantSettings.defaults();
        JsonObject obj = root.getAsJsonObject("temporary_enchantments");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int minEnchantments = parseInt(obj, "min_enchantments", defaults.minEnchantments(), 1, 8);
        int maxEnchantments = parseInt(obj, "max_enchantments", defaults.maxEnchantments(), minEnchantments, 8);
        int minDurationSeconds = parseInt(obj, "min_duration_seconds", defaults.minDurationSeconds(), 1, 120);
        int maxDurationSeconds = parseInt(obj, "max_duration_seconds", defaults.maxDurationSeconds(), minDurationSeconds, 120);
        List<EnchantmentEntry> entries = parseEnchantmentEntries(obj, defaults.entries());
        TemporaryEnchantParticleSettings effectParticles = parseTemporaryEnchantParticles(obj, defaults.effectParticles());
        if (entries.isEmpty()) {
            entries = defaults.entries();
        }

        return new TemporaryEnchantSettings(enabled, chance, minEnchantments, maxEnchantments, minDurationSeconds, maxDurationSeconds, List.copyOf(entries), effectParticles);
    }

    private static TemporaryEnchantParticleSettings parseTemporaryEnchantParticles(JsonObject root, TemporaryEnchantParticleSettings fallback) {
        if (!root.has("effect_particles") || !root.get("effect_particles").isJsonObject()) {
            return fallback;
        }

        JsonObject obj = root.getAsJsonObject("effect_particles");
        boolean enabled = parseBoolean(obj, "enabled", fallback.enabled());
        SimpleParticleType applyParticle = parseSimpleParticle(obj, "apply_particle", fallback.applyParticle());
        SimpleParticleType expireParticle = parseSimpleParticle(obj, "expire_particle", fallback.expireParticle());
        int count = parseInt(obj, "count", fallback.count(), 0, 64);
        float spreadX = parseFloat(obj, "spread_x", fallback.spreadX(), 0.0F, 2.0F);
        float spreadY = parseFloat(obj, "spread_y", fallback.spreadY(), 0.0F, 2.0F);
        float spreadZ = parseFloat(obj, "spread_z", fallback.spreadZ(), 0.0F, 2.0F);
        float speed = parseFloat(obj, "speed", fallback.speed(), 0.0F, 1.0F);
        return new TemporaryEnchantParticleSettings(enabled, applyParticle, expireParticle, count, spreadX, spreadY, spreadZ, speed);
    }

    private static List<EnchantmentEntry> parseEnchantmentEntries(JsonObject root, List<EnchantmentEntry> fallback) {
        if (!root.has("entries") || !root.get("entries").isJsonArray()) {
            return fallback;
        }

        List<EnchantmentEntry> entries = new ArrayList<>();
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

                Identifier id = Identifier.parse(idValue);
                int weight = parseInt(obj, "weight", 1, 0, 100000);
                int minLevel = parseInt(obj, "min_level", 1, 1, 10);
                int maxLevel = parseInt(obj, "max_level", minLevel, minLevel, 10);
                if (weight <= 0) {
                    continue;
                }

                entries.add(new EnchantmentEntry(id, weight, minLevel, maxLevel));
            } catch (Exception ignored) {
            }
        }

        return entries.isEmpty() ? fallback : entries;
    }

    private static SimpleParticleType parseSimpleParticle(JsonObject root, String field, SimpleParticleType fallback) {
        if (!root.has(field)) {
            return fallback;
        }

        try {
            String value = root.get(field).getAsString();
            if (value == null || value.isBlank()) {
                return fallback;
            }

            var particle = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(value));
            if (particle instanceof SimpleParticleType simpleParticle) {
                return simpleParticle;
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private static DropParticleSettings parseDropParticles(JsonObject root) {
        if (!root.has("drop_particles") || !root.get("drop_particles").isJsonObject()) {
            return DropParticleSettings.defaults();
        }

        DropParticleSettings defaults = DropParticleSettings.defaults();
        JsonObject obj = root.getAsJsonObject("drop_particles");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        int count = parseInt(obj, "count", defaults.count(), 0, 256);
        float spreadX = parseFloat(obj, "spread_x", defaults.spreadX(), 0.0F, 4.0F);
        float spreadY = parseFloat(obj, "spread_y", defaults.spreadY(), 0.0F, 4.0F);
        float spreadZ = parseFloat(obj, "spread_z", defaults.spreadZ(), 0.0F, 4.0F);
        float speed = parseFloat(obj, "speed", defaults.speed(), 0.0F, 1.0F);
        return new DropParticleSettings(enabled, count, spreadX, spreadY, spreadZ, speed);
    }

    private static LightningSettings parseLightning(JsonObject root) {
        if (!root.has("lightning") || !root.get("lightning").isJsonObject()) {
            return LightningSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject("lightning");
        boolean enabled = parseBoolean(obj, "enabled", true);
        float blockBreakChance = parseFloat(obj, "block_break_chance", 0.08F, 0.0F, 1.0F);
        float mobHitChance = parseFloat(obj, "mob_hit_chance", 0.12F, 0.0F, 1.0F);
        return new LightningSettings(enabled, blockBreakChance, mobHitChance);
    }

    private static LeafStormSettings parseLeafStorm(JsonObject root) {
        if (!root.has("leaf_storm") || !root.get("leaf_storm").isJsonObject()) {
            return LeafStormSettings.defaults();
        }

        LeafStormSettings defaults = LeafStormSettings.defaults();
        JsonObject obj = root.getAsJsonObject("leaf_storm");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int particleCount = parseInt(obj, "particle_count", defaults.particleCount(), 0, 256);
        int stickMin = parseInt(obj, "stick_min", defaults.stickMin(), 0, 64);
        int stickMax = parseInt(obj, "stick_max", defaults.stickMax(), stickMin, 64);
        float appleChance = parseFloat(obj, "apple_chance", defaults.appleChance(), 0.0F, 1.0F);
        return new LeafStormSettings(enabled, chance, particleCount, stickMin, stickMax, appleChance);
    }

    private static BeeSwarmSettings parseBeeSwarm(JsonObject root) {
        if (!root.has("bee_swarm") || !root.get("bee_swarm").isJsonObject()) {
            return BeeSwarmSettings.defaults();
        }

        BeeSwarmSettings defaults = BeeSwarmSettings.defaults();
        JsonObject obj = root.getAsJsonObject("bee_swarm");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int minCount = parseInt(obj, "min_count", defaults.minCount(), 1, 16);
        int maxCount = parseInt(obj, "max_count", defaults.maxCount(), minCount, 16);
        return new BeeSwarmSettings(enabled, chance, minCount, maxCount);
    }

    private static TimberLaunchSettings parseTimberLaunch(JsonObject root) {
        if (!root.has("timber_launch") || !root.get("timber_launch").isJsonObject()) {
            return TimberLaunchSettings.defaults();
        }

        TimberLaunchSettings defaults = TimberLaunchSettings.defaults();
        JsonObject obj = root.getAsJsonObject("timber_launch");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        float upwardBoost = parseFloat(obj, "upward_boost", defaults.upwardBoost(), 0.0F, 3.0F);
        float horizontalBoost = parseFloat(obj, "horizontal_boost", defaults.horizontalBoost(), 0.0F, 2.0F);
        return new TimberLaunchSettings(enabled, chance, upwardBoost, horizontalBoost);
    }

    private static LootPinataSettings parseLootPinata(JsonObject root) {
        if (!root.has("loot_pinata") || !root.get("loot_pinata").isJsonObject()) {
            return LootPinataSettings.defaults();
        }

        LootPinataSettings defaults = LootPinataSettings.defaults();
        JsonObject obj = root.getAsJsonObject("loot_pinata");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int minRolls = parseInt(obj, "min_rolls", defaults.minRolls(), 1, 16);
        int maxRolls = parseInt(obj, "max_rolls", defaults.maxRolls(), minRolls, 16);
        int particleCount = parseInt(obj, "particle_count", defaults.particleCount(), 0, 256);
        List<DropEntry> entries = parseDropEntries(obj, defaults.entries());
        if (entries.isEmpty()) {
            entries = defaults.entries();
        }
        return new LootPinataSettings(enabled, chance, minRolls, maxRolls, particleCount, List.copyOf(entries));
    }

    private static List<DropEntry> parseDropEntries(JsonObject root, List<DropEntry> fallback) {
        if (!root.has("entries") || !root.get("entries").isJsonArray()) {
            return fallback;
        }

        List<DropEntry> entries = new ArrayList<>();
        var array = root.getAsJsonArray("entries");
        for (var element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            try {
                JsonObject obj = element.getAsJsonObject();
                String itemId = parseString(obj, "item", "").trim();
                if (itemId.isBlank()) {
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
                if (item == null || item == Items.AIR) {
                    continue;
                }

                int weight = parseInt(obj, "weight", 1, 0, 100000);
                int minCount = parseInt(obj, "min_count", 1, 1, 64);
                int maxCount = parseInt(obj, "max_count", minCount, minCount, 64);
                if (weight <= 0) {
                    continue;
                }

                entries.add(new DropEntry(item, minCount, maxCount, weight));
            } catch (Exception ignored) {
            }
        }

        return entries.isEmpty() ? fallback : entries;
    }

    private static BonusDropSettings parseBonusDrops(JsonObject root) {
        String section = root.has("axe_bonus_drops") && root.get("axe_bonus_drops").isJsonObject()
                ? "axe_bonus_drops"
                : "wood_bonus_drops";

        if (!root.has(section) || !root.get(section).isJsonObject()) {
            return BonusDropSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject(section);
        boolean enabled = parseBoolean(obj, "enabled", true);
        float dropChance = parseFloat(obj, "drop_chance", 1.0F, 0.0F, 1.0F);

        DropEntry nugget = parseDropEntry(obj, "gold_nugget", Items.GOLD_NUGGET, 80, 1, 2);
        DropEntry charcoal = parseDropEntry(obj, "charcoal", Items.CHARCOAL, 16, 1, 2);
        DropEntry ingot = parseDropEntry(obj, "gold_ingot", Items.GOLD_INGOT, 3, 1, 1);
        DropEntry diamond = parseDropEntry(obj, "diamond", Items.DIAMOND, 1, 1, 1);
        DropEntry emerald = parseDropEntry(obj, "emerald", Items.EMERALD, 1, 1, 1);

        int totalWeight = Math.max(0, nugget.weight())
                + Math.max(0, charcoal.weight())
                + Math.max(0, ingot.weight())
                + Math.max(0, diamond.weight())
                + Math.max(0, emerald.weight());
        if (totalWeight <= 0) {
            nugget = new DropEntry(Items.GOLD_NUGGET, 1, 2, 1);
            charcoal = new DropEntry(Items.CHARCOAL, 1, 2, 0);
            ingot = new DropEntry(Items.GOLD_INGOT, 1, 1, 0);
            diamond = new DropEntry(Items.DIAMOND, 1, 1, 0);
            emerald = new DropEntry(Items.EMERALD, 1, 1, 0);
        }

        return new BonusDropSettings(enabled, dropChance, nugget, charcoal, ingot, diamond, emerald);
    }

    private static DropEntry parseDropEntry(JsonObject parent, String key, Item defaultItem, int defaultWeight, int defaultMin, int defaultMax) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            return new DropEntry(defaultItem, defaultMin, defaultMax, defaultWeight);
        }

        JsonObject obj = parent.getAsJsonObject(key);
        int weight = parseInt(obj, "weight", defaultWeight, 0, 100000);
        int minCount = parseInt(obj, "min_count", defaultMin, 1, 64);
        int maxCount = parseInt(obj, "max_count", defaultMax, 1, 64);
        if (maxCount < minCount) {
            maxCount = minCount;
        }

        return new DropEntry(defaultItem, minCount, maxCount, weight);
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

    private static float parseFloat(JsonObject root, String field, float fallback, float min, float max) {
        if (!root.has(field)) {
            return fallback;
        }
        try {
            float value = root.get(field).getAsFloat();
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return fallback;
            }
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

    public record DropEntry(Item item, int minCount, int maxCount, int weight) {
    }

    public record BonusDropSettings(
            boolean enabled,
            float dropChance,
            DropEntry goldNugget,
            DropEntry charcoal,
            DropEntry goldIngot,
            DropEntry diamond,
            DropEntry emerald
    ) {
        public static BonusDropSettings defaults() {
            return new BonusDropSettings(
                    true,
                    1.0F,
                    new DropEntry(Items.GOLD_NUGGET, 1, 2, 80),
                    new DropEntry(Items.CHARCOAL, 1, 2, 16),
                    new DropEntry(Items.GOLD_INGOT, 1, 1, 3),
                    new DropEntry(Items.DIAMOND, 1, 1, 1),
                    new DropEntry(Items.EMERALD, 1, 1, 1)
            );
        }
    }

    public record LightningSettings(boolean enabled, float blockBreakChance, float mobHitChance) {
        public static LightningSettings defaults() {
            return new LightningSettings(false, 0.08F, 0.12F);
        }
    }

    public record LeafStormSettings(
            boolean enabled,
            float chance,
            int particleCount,
            int stickMin,
            int stickMax,
            float appleChance
    ) {
        public static LeafStormSettings defaults() {
            return new LeafStormSettings(true, 0.22F, 20, 1, 3, 0.25F);
        }
    }

    public record BeeSwarmSettings(boolean enabled, float chance, int minCount, int maxCount) {
        public static BeeSwarmSettings defaults() {
            return new BeeSwarmSettings(true, 0.08F, 1, 3);
        }
    }

    public record TimberLaunchSettings(boolean enabled, float chance, float upwardBoost, float horizontalBoost) {
        public static TimberLaunchSettings defaults() {
            return new TimberLaunchSettings(true, 0.2F, 0.9F, 0.25F);
        }
    }

    public record LootPinataSettings(
            boolean enabled,
            float chance,
            int minRolls,
            int maxRolls,
            int particleCount,
            List<DropEntry> entries
    ) {
        public static LootPinataSettings defaults() {
            return new LootPinataSettings(
                    true,
                    0.2F,
                    2,
                    4,
                    18,
                    List.of(
                            new DropEntry(Items.STICK, 1, 3, 24),
                            new DropEntry(Items.APPLE, 1, 2, 20),
                            new DropEntry(Items.CHARCOAL, 1, 2, 16),
                            new DropEntry(Items.HONEYCOMB, 1, 2, 12),
                            new DropEntry(Items.SWEET_BERRIES, 1, 3, 12),
                            new DropEntry(Items.GOLD_NUGGET, 1, 2, 10),
                            new DropEntry(Items.EMERALD, 1, 1, 3),
                            new DropEntry(Items.DIAMOND, 1, 1, 1)
                    )
            );
        }
    }

    public record LuckyBlockTransformSettings(boolean enabled, float chance, float luckyBlockChance) {
        public static LuckyBlockTransformSettings defaults() {
            return new LuckyBlockTransformSettings(true, 0.05F, 1.0F);
        }

        public net.minecraft.world.level.block.Block chooseBlock(net.minecraft.util.RandomSource random) {
            if (random.nextFloat() <= luckyBlockChance) {
                return ModBlocks.LUCKY_BLOCK;
            }
            return ModBlocks.MOSTLY_UNLUCKY_BLOCK;
        }
    }

    public record EnchantmentEntry(Identifier id, int weight, int minLevel, int maxLevel) {
    }

    public record TemporaryEnchantSettings(
            boolean enabled,
            float chance,
            int minEnchantments,
            int maxEnchantments,
            int minDurationSeconds,
            int maxDurationSeconds,
            List<EnchantmentEntry> entries,
            TemporaryEnchantParticleSettings effectParticles
    ) {
        public static TemporaryEnchantSettings defaults() {
            return new TemporaryEnchantSettings(
                    true,
                    0.25F,
                    1,
                    2,
                    3,
                    8,
                    List.of(
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "efficiency"), 32, 1, 5),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "unbreaking"), 26, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "fortune"), 22, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "silk_touch"), 10, 1, 1),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "mending"), 10, 1, 1),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 10, 1, 5),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "smite"), 8, 1, 5),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "bane_of_arthropods"), 8, 1, 5),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "fire_aspect"), 6, 1, 2),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "looting"), 6, 1, 3)
                    ),
                    TemporaryEnchantParticleSettings.defaults()
            );
        }
    }

    public record TemporaryEnchantParticleSettings(
            boolean enabled,
            SimpleParticleType applyParticle,
            SimpleParticleType expireParticle,
            int count,
            float spreadX,
            float spreadY,
            float spreadZ,
            float speed
    ) {
        public static TemporaryEnchantParticleSettings defaults() {
            return new TemporaryEnchantParticleSettings(
                    true,
                    ParticleTypes.ENCHANT,
                    ParticleTypes.CRIT,
                    4,
                    0.18F,
                    0.12F,
                    0.18F,
                    0.01F
            );
        }
    }

    public record DropParticleSettings(
            boolean enabled,
            int count,
            float spreadX,
            float spreadY,
            float spreadZ,
            float speed
    ) {
        public static DropParticleSettings defaults() {
            return new DropParticleSettings(true, 10, 0.3F, 0.2F, 0.3F, 0.02F);
        }
    }
}