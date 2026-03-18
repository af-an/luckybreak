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

public final class LuckyShovelConfig {

    private static final String CONFIG_PATH = "data/luckybreak/items/lucky_shovel.json";
    private static final int DEFAULT_DURABILITY = 32;
    private static final float BASE_ATTACK_DAMAGE = 1.0F;
    private static final float BASE_ATTACK_SPEED = 4.0F;
    private static final float DEFAULT_ATTACK_DAMAGE = 1.5F;
    private static final float DEFAULT_ATTACK_SPEED = -3.0F;

    private static int durability = DEFAULT_DURABILITY;
    private static float attackDamage = DEFAULT_ATTACK_DAMAGE;
    private static float attackSpeed = DEFAULT_ATTACK_SPEED;
    private static BonusDropSettings bonusDropSettings = BonusDropSettings.defaults();
    private static SandstormSettings sandstormSettings = SandstormSettings.defaults();
    private static TreasureBurstSettings treasureBurstSettings = TreasureBurstSettings.defaults();
    private static LuckyBlockTransformSettings luckyBlockTransformSettings = LuckyBlockTransformSettings.defaults();
    private static TemporaryEnchantSettings temporaryEnchantSettings = TemporaryEnchantSettings.defaults();
    private static DropParticleSettings dropParticleSettings = DropParticleSettings.defaults();
    private static boolean loaded;

    private LuckyShovelConfig() {
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

    public static SandstormSettings sandstormSettings() {
        ensureLoaded();
        return sandstormSettings;
    }

    public static TreasureBurstSettings treasureBurstSettings() {
        ensureLoaded();
        return treasureBurstSettings;
    }

    public static TemporaryEnchantSettings temporaryEnchantSettings() {
        ensureLoaded();
        return temporaryEnchantSettings;
    }

    public static LuckyBlockTransformSettings luckyBlockTransformSettings() {
        ensureLoaded();
        return luckyBlockTransformSettings;
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
        sandstormSettings = SandstormSettings.defaults();
        treasureBurstSettings = TreasureBurstSettings.defaults();
        luckyBlockTransformSettings = LuckyBlockTransformSettings.defaults();
        temporaryEnchantSettings = TemporaryEnchantSettings.defaults();
        dropParticleSettings = DropParticleSettings.defaults();

        InputStream input = LuckyShovelConfig.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing Lucky Shovel config: {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            durability = parseInt(root, "durability", DEFAULT_DURABILITY, 1, 10000);
            attackDamage = parseAttackDamageModifier(root);
            attackSpeed = parseAttackSpeedModifier(root);
            bonusDropSettings = parseBonusDrops(root);
            sandstormSettings = parseSandstorm(root);
            treasureBurstSettings = parseTreasureBurst(root);
            luckyBlockTransformSettings = parseLuckyBlockTransform(root);
            temporaryEnchantSettings = parseTemporaryEnchantments(root);
            dropParticleSettings = parseDropParticles(root);
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load Lucky Shovel config", exception);
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

    private static DropParticleSettings parseDropParticles(JsonObject root) {
        if (!root.has("drop_particles") || !root.get("drop_particles").isJsonObject()) {
            return DropParticleSettings.defaults();
        }

        DropParticleSettings defaults = DropParticleSettings.defaults();
        JsonObject obj = root.getAsJsonObject("drop_particles");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        SimpleParticleType particleType = parseSimpleParticle(obj, "particle", defaults.particleType());
        int count = parseInt(obj, "count", defaults.count(), 0, 256);
        float spreadX = parseFloat(obj, "spread_x", defaults.spreadX(), 0.0F, 4.0F);
        float spreadY = parseFloat(obj, "spread_y", defaults.spreadY(), 0.0F, 4.0F);
        float spreadZ = parseFloat(obj, "spread_z", defaults.spreadZ(), 0.0F, 4.0F);
        float speed = parseFloat(obj, "speed", defaults.speed(), 0.0F, 1.0F);
        return new DropParticleSettings(enabled, particleType, count, spreadX, spreadY, spreadZ, speed);
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

    private static BonusDropSettings parseBonusDrops(JsonObject root) {
        if (!root.has("shovel_bonus_drops") || !root.get("shovel_bonus_drops").isJsonObject()) {
            return BonusDropSettings.defaults();
        }

        BonusDropSettings defaults = BonusDropSettings.defaults();
        JsonObject obj = root.getAsJsonObject("shovel_bonus_drops");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float dropChance = parseFloat(obj, "drop_chance", defaults.dropChance(), 0.0F, 1.0F);

        List<DropEntry> entries = parseDropEntries(obj, defaults.entries());
        if (entries.isEmpty()) {
            entries = defaults.entries();
        }

        int totalWeight = 0;
        for (DropEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            entries = defaults.entries();
        }

        return new BonusDropSettings(enabled, dropChance, List.copyOf(entries));
    }

    private static SandstormSettings parseSandstorm(JsonObject root) {
        if (!root.has("sandstorm") || !root.get("sandstorm").isJsonObject()) {
            return SandstormSettings.defaults();
        }

        SandstormSettings defaults = SandstormSettings.defaults();
        JsonObject obj = root.getAsJsonObject("sandstorm");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        float radius = parseFloat(obj, "radius", defaults.radius(), 1.0F, 24.0F);
        int durationTicks = parseInt(obj, "duration_ticks", defaults.durationTicks(), 1, 20 * 60);
        int slownessAmplifier = parseInt(obj, "slowness_amplifier", defaults.slownessAmplifier(), 0, 10);
        int weaknessAmplifier = parseInt(obj, "weakness_amplifier", defaults.weaknessAmplifier(), 0, 10);
        int particleCount = parseInt(obj, "particle_count", defaults.particleCount(), 0, 256);
        return new SandstormSettings(enabled, chance, radius, durationTicks, slownessAmplifier, weaknessAmplifier, particleCount);
    }

    private static TreasureBurstSettings parseTreasureBurst(JsonObject root) {
        if (!root.has("treasure_burst") || !root.get("treasure_burst").isJsonObject()) {
            return TreasureBurstSettings.defaults();
        }

        TreasureBurstSettings defaults = TreasureBurstSettings.defaults();
        JsonObject obj = root.getAsJsonObject("treasure_burst");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int minRolls = parseInt(obj, "min_rolls", defaults.minRolls(), 1, 16);
        int maxRolls = parseInt(obj, "max_rolls", defaults.maxRolls(), minRolls, 16);
        int particleCount = parseInt(obj, "particle_count", defaults.particleCount(), 0, 256);
        List<DropEntry> entries = parseDropEntries(obj, defaults.entries());
        if (entries.isEmpty()) {
            entries = defaults.entries();
        }

        int totalWeight = 0;
        for (DropEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            entries = defaults.entries();
        }

        return new TreasureBurstSettings(enabled, chance, minRolls, maxRolls, particleCount, List.copyOf(entries));
    }

    private static List<DropEntry> parseDropEntries(JsonObject parent, List<DropEntry> fallback) {
        if (parent.has("entries") && parent.get("entries").isJsonArray()) {
            List<DropEntry> entries = new ArrayList<>();
            var array = parent.getAsJsonArray("entries");
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

            if (!entries.isEmpty()) {
                return entries;
            }
        }

        DropEntry nugget = parseDropEntry(parent, "gold_nugget", Items.GOLD_NUGGET, 80, 1, 2);
        DropEntry flint = parseDropEntry(parent, "flint", Items.FLINT, 16, 1, 2);
        DropEntry goldIngot = parseDropEntry(parent, "gold_ingot", Items.GOLD_INGOT, 3, 1, 1);
        DropEntry diamond = parseDropEntry(parent, "diamond", Items.DIAMOND, 1, 1, 1);
        DropEntry emerald = parseDropEntry(parent, "emerald", Items.EMERALD, 1, 1, 1);
        return List.of(nugget, flint, goldIngot, diamond, emerald);
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
            List<DropEntry> entries
    ) {
        public static BonusDropSettings defaults() {
            return new BonusDropSettings(
                    true,
                    0.12F,
                List.of(
                    new DropEntry(Items.GOLD_NUGGET, 1, 2, 80),
                    new DropEntry(Items.FLINT, 1, 2, 16),
                    new DropEntry(Items.GOLD_INGOT, 1, 1, 3),
                    new DropEntry(Items.DIAMOND, 1, 1, 1),
                    new DropEntry(Items.EMERALD, 1, 1, 1)
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

    public record SandstormSettings(
            boolean enabled,
            float chance,
            float radius,
            int durationTicks,
            int slownessAmplifier,
            int weaknessAmplifier,
            int particleCount
    ) {
        public static SandstormSettings defaults() {
            return new SandstormSettings(true, 0.12F, 4.0F, 80, 1, 0, 22);
        }
    }

    public record TreasureBurstSettings(
            boolean enabled,
            float chance,
            int minRolls,
            int maxRolls,
            int particleCount,
            List<DropEntry> entries
    ) {
        public static TreasureBurstSettings defaults() {
            return new TreasureBurstSettings(
                    true,
                    0.08F,
                    2,
                    4,
                    16,
                    List.of(
                            new DropEntry(Items.IRON_NUGGET, 1, 3, 28),
                            new DropEntry(Items.COPPER_INGOT, 1, 2, 20),
                            new DropEntry(Items.QUARTZ, 1, 2, 14),
                            new DropEntry(Items.AMETHYST_SHARD, 1, 2, 12),
                            new DropEntry(Items.GOLD_INGOT, 1, 2, 8),
                            new DropEntry(Items.PRISMARINE_SHARD, 1, 2, 8),
                            new DropEntry(Items.DIAMOND, 1, 1, 3),
                            new DropEntry(Items.EMERALD, 1, 1, 3),
                            new DropEntry(Items.HEART_OF_THE_SEA, 1, 1, 1)
                    )
            );
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
                    0.1F,
                    1,
                    2,
                    3,
                    8,
                    List.of(
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "efficiency"), 36, 1, 5),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "unbreaking"), 26, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "fortune"), 22, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "mending"), 8, 1, 1),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "silk_touch"), 8, 1, 1)
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
            SimpleParticleType particleType,
            int count,
            float spreadX,
            float spreadY,
            float spreadZ,
            float speed
    ) {
        public static DropParticleSettings defaults() {
            return new DropParticleSettings(true, ParticleTypes.HAPPY_VILLAGER, 10, 0.3F, 0.2F, 0.3F, 0.02F);
        }
    }
}