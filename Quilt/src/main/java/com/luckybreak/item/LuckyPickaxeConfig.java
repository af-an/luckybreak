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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LuckyPickaxeConfig {

    private static final String CONFIG_PATH = "data/luckybreak/items/lucky_pickaxe.json";
    private static final int DEFAULT_DURABILITY = 32;
    private static final ToolMaterial DEFAULT_MINING_TIER = ToolMaterial.GOLD;
    private static final float BASE_ATTACK_DAMAGE = 1.0F;
    private static final float BASE_ATTACK_SPEED = 4.0F;
    private static final float DEFAULT_ATTACK_DAMAGE = 1.0F;
    private static final float DEFAULT_ATTACK_SPEED = -2.8F;

    private static int durability = DEFAULT_DURABILITY;
    private static ToolMaterial miningTier = DEFAULT_MINING_TIER;
    private static float attackDamage = DEFAULT_ATTACK_DAMAGE;
    private static float attackSpeed = DEFAULT_ATTACK_SPEED;
    private static BonusDropSettings bonusDropSettings = BonusDropSettings.defaults();
    private static TntTransformSettings tntTransformSettings = TntTransformSettings.defaults();
    private static BedrockTransformSettings bedrockTransformSettings = BedrockTransformSettings.defaults();
    private static LuckyBlockTransformSettings luckyBlockTransformSettings = LuckyBlockTransformSettings.defaults();
    private static BlockTransformSettings blockTransformSettings = BlockTransformSettings.defaults();
    private static GoldenHenSpawnSettings goldenHenSpawnSettings = GoldenHenSpawnSettings.defaults();
    private static EventParticleSettings eventParticleSettings = EventParticleSettings.defaults();
    private static TemporaryEnchantSettings temporaryEnchantSettings = TemporaryEnchantSettings.defaults();
    private static XpBurstSettings xpBurstSettings = XpBurstSettings.defaults();
    private static OreVeinBurstSettings oreVeinBurstSettings = OreVeinBurstSettings.defaults();
    private static SeismicBurstSettings seismicBurstSettings = SeismicBurstSettings.defaults();
    private static HostileSpawnSettings hostileSpawnSettings = HostileSpawnSettings.defaults();
    private static FriendlySpawnSettings friendlySpawnSettings = FriendlySpawnSettings.defaults();
    private static boolean loaded;

    private LuckyPickaxeConfig() {
    }

    public static int durability() {
        ensureLoaded();
        return durability;
    }

    public static ToolMaterial miningTier() {
        ensureLoaded();
        return miningTier;
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

    public static TntTransformSettings tntTransformSettings() {
        ensureLoaded();
        return tntTransformSettings;
    }

    public static BedrockTransformSettings bedrockTransformSettings() {
        ensureLoaded();
        return bedrockTransformSettings;
    }

    public static LuckyBlockTransformSettings luckyBlockTransformSettings() {
        ensureLoaded();
        return luckyBlockTransformSettings;
    }

    public static BlockTransformSettings blockTransformSettings() {
        ensureLoaded();
        return blockTransformSettings;
    }

    public static GoldenHenSpawnSettings goldenHenSpawnSettings() {
        ensureLoaded();
        return goldenHenSpawnSettings;
    }

    public static EventParticleSettings eventParticleSettings() {
        ensureLoaded();
        return eventParticleSettings;
    }

    public static TemporaryEnchantSettings temporaryEnchantSettings() {
        ensureLoaded();
        return temporaryEnchantSettings;
    }

    public static XpBurstSettings xpBurstSettings() {
        ensureLoaded();
        return xpBurstSettings;
    }

    public static OreVeinBurstSettings oreVeinBurstSettings() {
        ensureLoaded();
        return oreVeinBurstSettings;
    }

    public static SeismicBurstSettings seismicBurstSettings() {
        ensureLoaded();
        return seismicBurstSettings;
    }

    public static HostileSpawnSettings hostileSpawnSettings() {
        ensureLoaded();
        return hostileSpawnSettings;
    }

    public static FriendlySpawnSettings friendlySpawnSettings() {
        ensureLoaded();
        return friendlySpawnSettings;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        durability = DEFAULT_DURABILITY;
        miningTier = DEFAULT_MINING_TIER;
        attackDamage = DEFAULT_ATTACK_DAMAGE;
        attackSpeed = DEFAULT_ATTACK_SPEED;
        bonusDropSettings = BonusDropSettings.defaults();
        tntTransformSettings = TntTransformSettings.defaults();
        bedrockTransformSettings = BedrockTransformSettings.defaults();
        luckyBlockTransformSettings = LuckyBlockTransformSettings.defaults();
        blockTransformSettings = BlockTransformSettings.defaults();
        goldenHenSpawnSettings = GoldenHenSpawnSettings.defaults();
        eventParticleSettings = EventParticleSettings.defaults();
        temporaryEnchantSettings = TemporaryEnchantSettings.defaults();
        xpBurstSettings = XpBurstSettings.defaults();
        oreVeinBurstSettings = OreVeinBurstSettings.defaults();
        seismicBurstSettings = SeismicBurstSettings.defaults();
        hostileSpawnSettings = HostileSpawnSettings.defaults();
        friendlySpawnSettings = FriendlySpawnSettings.defaults();

        InputStream input = LuckyPickaxeConfig.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing Lucky Pickaxe config: {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            durability = parseInt(root, "durability", DEFAULT_DURABILITY, 1, 10000);
            miningTier = parseMiningTier(root);
            attackDamage = parseAttackDamageModifier(root);
            attackSpeed = parseAttackSpeedModifier(root);
            bonusDropSettings = parseBonusDrops(root);
            tntTransformSettings = parseTntTransform(root);
            bedrockTransformSettings = parseBedrockTransform(root);
            luckyBlockTransformSettings = parseLuckyBlockTransform(root);
            blockTransformSettings = parseBlockTransform(root);
            goldenHenSpawnSettings = parseGoldenHenSpawn(root);
            eventParticleSettings = parseEventParticles(root);
            temporaryEnchantSettings = parseTemporaryEnchantments(root);
            xpBurstSettings = parseXpBurst(root);
            oreVeinBurstSettings = parseOreVeinBurst(root);
            seismicBurstSettings = parseSeismicBurst(root);
            hostileSpawnSettings = parseHostileSpawn(root);
            friendlySpawnSettings = parseFriendlySpawn(root);
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load Lucky Pickaxe config", exception);
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

    private static ToolMaterial parseMiningTier(JsonObject root) {
        String value = parseString(root, "mining_tier", "gold").trim().toLowerCase();
        return switch (value) {
            case "wood", "wooden" -> ToolMaterial.WOOD;
            case "stone" -> ToolMaterial.STONE;
            case "iron" -> ToolMaterial.IRON;
            case "diamond" -> ToolMaterial.DIAMOND;
            case "netherite" -> ToolMaterial.NETHERITE;
            case "gold", "golden" -> ToolMaterial.GOLD;
            default -> DEFAULT_MINING_TIER;
        };
    }

    private static BedrockTransformSettings parseBedrockTransform(JsonObject root) {
        if (!root.has("bedrock_transform") || !root.get("bedrock_transform").isJsonObject()) {
            return BedrockTransformSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject("bedrock_transform");
        boolean enabled = parseBoolean(obj, "enabled", true);
        float chance = parseFloat(obj, "chance", 0.02F, 0.0F, 1.0F);
        return new BedrockTransformSettings(enabled, chance);
    }

    private static LuckyBlockTransformSettings parseLuckyBlockTransform(JsonObject root) {
        if (!root.has("lucky_block_transform") || !root.get("lucky_block_transform").isJsonObject()) {
            return LuckyBlockTransformSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject("lucky_block_transform");
        boolean enabled = parseBoolean(obj, "enabled", true);
        float chance = parseFloat(obj, "chance", 0.015F, 0.0F, 1.0F);
        float luckyBlockChance = parseFloat(obj, "lucky_block_chance", 0.2F, 0.0F, 1.0F);
        return new LuckyBlockTransformSettings(enabled, chance, luckyBlockChance);
    }

    private static BlockTransformSettings parseBlockTransform(JsonObject root) {
        if (!root.has("block_transform") || !root.get("block_transform").isJsonObject()) {
            return BlockTransformSettings.defaults();
        }

        BlockTransformSettings defaults = BlockTransformSettings.defaults();
        JsonObject obj = root.getAsJsonObject("block_transform");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        List<Block> blocks = parseTransformBlocks(obj, defaults.blocks());
        if (blocks.isEmpty()) {
            blocks = defaults.blocks();
        }
        return new BlockTransformSettings(enabled, chance, List.copyOf(blocks));
    }

    private static GoldenHenSpawnSettings parseGoldenHenSpawn(JsonObject root) {
        if (!root.has("golden_hen_spawn") || !root.get("golden_hen_spawn").isJsonObject()) {
            return GoldenHenSpawnSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject("golden_hen_spawn");
        boolean enabled = parseBoolean(obj, "enabled", true);
        float chance = parseFloat(obj, "chance", 0.005F, 0.0F, 1.0F);
        int spawnRadius = parseInt(obj, "spawn_radius", 2, 0, 12);
        return new GoldenHenSpawnSettings(enabled, chance, spawnRadius);
    }

    private static EventParticleSettings parseEventParticles(JsonObject root) {
        if (!root.has("event_particles") || !root.get("event_particles").isJsonObject()) {
            return EventParticleSettings.defaults();
        }

        EventParticleSettings defaults = EventParticleSettings.defaults();
        JsonObject obj = root.getAsJsonObject("event_particles");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        int goodCount = parseInt(obj, "good_count", defaults.goodCount(), 0, 256);
        int badCount = parseInt(obj, "bad_count", defaults.badCount(), 0, 256);
        float spreadX = parseFloat(obj, "spread_x", defaults.spreadX(), 0.0F, 4.0F);
        float spreadY = parseFloat(obj, "spread_y", defaults.spreadY(), 0.0F, 4.0F);
        float spreadZ = parseFloat(obj, "spread_z", defaults.spreadZ(), 0.0F, 4.0F);
        float goodSpeed = parseFloat(obj, "good_speed", defaults.goodSpeed(), 0.0F, 1.0F);
        float badSpeed = parseFloat(obj, "bad_speed", defaults.badSpeed(), 0.0F, 1.0F);
        List<String> goodEvents = parseStringList(obj, "good_events", defaults.goodEvents());
        List<String> badEvents = parseStringList(obj, "bad_events", defaults.badEvents());

        return new EventParticleSettings(enabled, goodCount, badCount, spreadX, spreadY, spreadZ, goodSpeed, badSpeed, List.copyOf(goodEvents), List.copyOf(badEvents));
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

    private static List<String> parseStringList(JsonObject root, String field, List<String> fallback) {
        if (!root.has(field) || !root.get(field).isJsonArray()) {
            return fallback;
        }

        List<String> values = new ArrayList<>();
        var array = root.getAsJsonArray(field);
        for (var element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                continue;
            }

            try {
                String value = element.getAsString();
                if (value == null) {
                    continue;
                }
                value = value.trim().toLowerCase();
                if (!value.isBlank()) {
                    values.add(value);
                }
            } catch (Exception ignored) {
            }
        }

        return values.isEmpty() ? fallback : values;
    }

    private static List<Block> parseTransformBlocks(JsonObject obj, List<Block> fallback) {
        if (!obj.has("blocks") || !obj.get("blocks").isJsonArray()) {
            return fallback;
        }

        List<Block> result = new ArrayList<>();
        var array = obj.getAsJsonArray("blocks");
        for (var element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                continue;
            }

            try {
                String id = element.getAsString();
                if (id == null || id.isBlank()) {
                    continue;
                }

                Identifier identifier = Identifier.parse(id);
                Block block = BuiltInRegistries.BLOCK.getValue(identifier);
                if (block == null || block == Blocks.AIR) {
                    continue;
                }
                result.add(block);
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private static FriendlySpawnSettings parseFriendlySpawn(JsonObject root) {
        if (!root.has("friendly_spawn") || !root.get("friendly_spawn").isJsonObject()) {
            return FriendlySpawnSettings.defaults();
        }

        FriendlySpawnSettings defaults = FriendlySpawnSettings.defaults();
        JsonObject obj = root.getAsJsonObject("friendly_spawn");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int spawnRadius = parseInt(obj, "spawn_radius", defaults.spawnRadius(), 0, 12);
        int overallMinCount = parseInt(obj, "overall_min_count", defaults.overallMinCount(), 0, 64);
        int overallMaxCount = parseInt(obj, "overall_max_count", defaults.overallMaxCount(), 0, 64);
        if (overallMaxCount < overallMinCount) {
            overallMaxCount = overallMinCount;
        }

        List<FriendlyMobEntry> mobs = parseFriendlyMobEntries(obj, defaults.mobs());
        if (mobs.isEmpty()) {
            mobs = defaults.mobs();
        }

        return new FriendlySpawnSettings(enabled, chance, spawnRadius, overallMinCount, overallMaxCount, List.copyOf(mobs));
    }

    private static List<FriendlyMobEntry> parseFriendlyMobEntries(JsonObject obj, List<FriendlyMobEntry> fallback) {
        if (!obj.has("mobs") || !obj.get("mobs").isJsonArray()) {
            return fallback;
        }

        List<FriendlyMobEntry> result = new ArrayList<>();
        var array = obj.getAsJsonArray("mobs");
        for (var element : array) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                try {
                    String id = element.getAsString();
                    if (id == null || id.isBlank()) {
                        continue;
                    }
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
                    if (type != null) {
                        result.add(new FriendlyMobEntry(type, 0, 1));
                    }
                } catch (Exception ignored) {
                }
                continue;
            }

            if (!element.isJsonObject()) {
                continue;
            }

            try {
                JsonObject mobObj = element.getAsJsonObject();
                String id = parseString(mobObj, "id", "");
                if (id.isBlank()) {
                    continue;
                }
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
                if (type == null) {
                    continue;
                }

                int minCount = parseInt(mobObj, "min_count", 0, 0, 64);
                int maxCount = parseInt(mobObj, "max_count", Math.max(1, minCount), 0, 64);
                if (maxCount < minCount) {
                    maxCount = minCount;
                }
                if (maxCount <= 0) {
                    continue;
                }

                result.add(new FriendlyMobEntry(type, minCount, maxCount));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private static HostileSpawnSettings parseHostileSpawn(JsonObject root) {
        if (!root.has("hostile_spawn") || !root.get("hostile_spawn").isJsonObject()) {
            return HostileSpawnSettings.defaults();
        }

        HostileSpawnSettings defaults = HostileSpawnSettings.defaults();
        JsonObject obj = root.getAsJsonObject("hostile_spawn");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int spawnRadius = parseInt(obj, "spawn_radius", defaults.spawnRadius(), 0, 12);
        int overallMinCount = parseInt(obj, "overall_min_count", defaults.overallMinCount(), 0, 64);
        int overallMaxCount = parseInt(obj, "overall_max_count", defaults.overallMaxCount(), 0, 64);
        boolean forceNormalOnPeaceful = parseBoolean(obj, "force_normal_on_peaceful", defaults.forceNormalOnPeaceful());
        if (overallMaxCount < overallMinCount) {
            overallMaxCount = overallMinCount;
        }

        List<HostileMobEntry> mobs = parseHostileMobEntries(obj, defaults.mobs());
        if (mobs.isEmpty()) {
            mobs = defaults.mobs();
        }

        return new HostileSpawnSettings(enabled, chance, spawnRadius, overallMinCount, overallMaxCount, forceNormalOnPeaceful, List.copyOf(mobs));
    }

    private static List<HostileMobEntry> parseHostileMobEntries(JsonObject obj, List<HostileMobEntry> fallback) {
        if (!obj.has("mobs") || !obj.get("mobs").isJsonArray()) {
            return fallback;
        }

        List<HostileMobEntry> result = new ArrayList<>();
        var array = obj.getAsJsonArray("mobs");
        for (var element : array) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                try {
                    String id = element.getAsString();
                    if (id == null || id.isBlank()) {
                        continue;
                    }
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
                    if (type != null) {
                        result.add(new HostileMobEntry(type, 0, 1));
                    }
                } catch (Exception ignored) {
                }
                continue;
            }

            if (!element.isJsonObject()) {
                continue;
            }

            try {
                JsonObject mobObj = element.getAsJsonObject();
                String id = parseString(mobObj, "id", "");
                if (id.isBlank()) {
                    continue;
                }
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
                if (type == null) {
                    continue;
                }

                int minCount = parseInt(mobObj, "min_count", 0, 0, 64);
                int maxCount = parseInt(mobObj, "max_count", Math.max(1, minCount), 0, 64);
                if (maxCount < minCount) {
                    maxCount = minCount;
                }
                if (maxCount <= 0) {
                    continue;
                }

                result.add(new HostileMobEntry(type, minCount, maxCount));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private static XpBurstSettings parseXpBurst(JsonObject root) {
        if (!root.has("xp_burst") || !root.get("xp_burst").isJsonObject()) {
            return XpBurstSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject("xp_burst");
        boolean enabled = parseBoolean(obj, "enabled", true);
        float chance = parseFloat(obj, "chance", 0.12F, 0.0F, 1.0F);
        int minXp = parseInt(obj, "min_xp", 1, 1, 1000);
        int maxXp = parseInt(obj, "max_xp", 6, minXp, 1000);
        return new XpBurstSettings(enabled, chance, minXp, maxXp);
    }

    private static OreVeinBurstSettings parseOreVeinBurst(JsonObject root) {
        if (!root.has("ore_vein_burst") || !root.get("ore_vein_burst").isJsonObject()) {
            return OreVeinBurstSettings.defaults();
        }

        OreVeinBurstSettings defaults = OreVeinBurstSettings.defaults();
        JsonObject obj = root.getAsJsonObject("ore_vein_burst");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int minRolls = parseInt(obj, "min_rolls", defaults.minRolls(), 1, 16);
        int maxRolls = parseInt(obj, "max_rolls", defaults.maxRolls(), minRolls, 16);
        List<WeightedDropEntry> entries = parseWeightedDropEntries(obj, defaults.entries());
        if (entries.isEmpty()) {
            entries = defaults.entries();
        }
        return new OreVeinBurstSettings(enabled, chance, minRolls, maxRolls, List.copyOf(entries));
    }

    private static SeismicBurstSettings parseSeismicBurst(JsonObject root) {
        if (!root.has("seismic_burst") || !root.get("seismic_burst").isJsonObject()) {
            return SeismicBurstSettings.defaults();
        }

        SeismicBurstSettings defaults = SeismicBurstSettings.defaults();
        JsonObject obj = root.getAsJsonObject("seismic_burst");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        float radius = parseFloat(obj, "radius", defaults.radius(), 1.0F, 24.0F);
        int slownessTicks = parseInt(obj, "slowness_ticks", defaults.slownessTicks(), 1, 20 * 60);
        int slownessAmplifier = parseInt(obj, "slowness_amplifier", defaults.slownessAmplifier(), 0, 10);
        float upwardBoost = parseFloat(obj, "upward_boost", defaults.upwardBoost(), 0.0F, 2.0F);
        return new SeismicBurstSettings(enabled, chance, radius, slownessTicks, slownessAmplifier, upwardBoost);
    }

    private static List<WeightedDropEntry> parseWeightedDropEntries(JsonObject parent, List<WeightedDropEntry> fallback) {
        if (!parent.has("entries") || !parent.get("entries").isJsonArray()) {
            return fallback;
        }

        List<WeightedDropEntry> entries = new ArrayList<>();
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

                entries.add(new WeightedDropEntry(item, minCount, maxCount, weight));
            } catch (Exception ignored) {
            }
        }

        return entries.isEmpty() ? fallback : entries;
    }

    private static TntTransformSettings parseTntTransform(JsonObject root) {
        if (!root.has("tnt_transform") || !root.get("tnt_transform").isJsonObject()) {
            return TntTransformSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject("tnt_transform");
        boolean enabled = parseBoolean(obj, "enabled", true);
        float chance = parseFloat(obj, "chance", 0.03F, 0.0F, 1.0F);
        int fuseTicks = parseInt(obj, "fuse_ticks", 80, 10, 200);
        int minCount = parseInt(obj, "min_count", 1, 1, 64);
        int maxCount = parseInt(obj, "max_count", 6, minCount, 64);
        return new TntTransformSettings(enabled, chance, fuseTicks, minCount, maxCount);
    }

    private static BonusDropSettings parseBonusDrops(JsonObject root) {
        String section = root.has("pickaxe_bonus_drops") && root.get("pickaxe_bonus_drops").isJsonObject()
                ? "pickaxe_bonus_drops"
                : "mine_bonus_drops";

        if (!root.has(section) || !root.get(section).isJsonObject()) {
            return BonusDropSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject(section);
        boolean enabled = parseBoolean(obj, "enabled", true);
        float dropChance = parseFloat(obj, "drop_chance", 0.25F, 0.0F, 1.0F);

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
                    0.25F,
                    new DropEntry(Items.GOLD_NUGGET, 1, 2, 80),
                    new DropEntry(Items.CHARCOAL, 1, 2, 16),
                    new DropEntry(Items.GOLD_INGOT, 1, 1, 3),
                    new DropEntry(Items.DIAMOND, 1, 1, 1),
                    new DropEntry(Items.EMERALD, 1, 1, 1)
            );
        }
    }

    public record TntTransformSettings(boolean enabled, float chance, int fuseTicks, int minCount, int maxCount) {
        public static TntTransformSettings defaults() {
            return new TntTransformSettings(true, 0.03F, 80, 1, 6);
        }
    }

    public record BedrockTransformSettings(boolean enabled, float chance) {
        public static BedrockTransformSettings defaults() {
            return new BedrockTransformSettings(true, 0.02F);
        }
    }

    public record LuckyBlockTransformSettings(boolean enabled, float chance, float luckyBlockChance) {
        public static LuckyBlockTransformSettings defaults() {
            return new LuckyBlockTransformSettings(true, 0.015F, 0.2F);
        }

        public Block chooseBlock(net.minecraft.util.RandomSource random) {
            if (random.nextFloat() <= luckyBlockChance) {
                return ModBlocks.LUCKY_BLOCK;
            }
            return ModBlocks.MOSTLY_UNLUCKY_BLOCK;
        }
    }

    public record BlockTransformSettings(boolean enabled, float chance, List<Block> blocks) {
        public static BlockTransformSettings defaults() {
            return new BlockTransformSettings(
                    true,
                    0.04F,
                    List.of(
                            Blocks.STONE,
                            Blocks.COBBLESTONE,
                            Blocks.DIRT,
                            Blocks.GRASS_BLOCK,
                            Blocks.SAND,
                            Blocks.RED_SAND,
                            Blocks.GRAVEL,
                            Blocks.CLAY,
                            Blocks.OAK_LOG,
                            Blocks.BIRCH_LOG,
                            Blocks.SPRUCE_LOG,
                            Blocks.JUNGLE_LOG,
                            Blocks.ACACIA_LOG,
                            Blocks.DARK_OAK_LOG,
                            Blocks.MANGROVE_LOG,
                            Blocks.CHERRY_LOG,
                            Blocks.OAK_PLANKS,
                            Blocks.BIRCH_PLANKS,
                            Blocks.SPRUCE_PLANKS,
                            Blocks.BRICKS,
                            Blocks.MOSSY_COBBLESTONE,
                            Blocks.NETHERRACK,
                            Blocks.END_STONE,
                            Blocks.DEEPSLATE,
                            Blocks.TUFF,
                            Blocks.CALCITE,
                            Blocks.BASALT,
                            Blocks.BLACKSTONE,
                            Blocks.SANDSTONE,
                            Blocks.TERRACOTTA
                    )
            );
        }
    }

    public record GoldenHenSpawnSettings(boolean enabled, float chance, int spawnRadius) {
        public static GoldenHenSpawnSettings defaults() {
            return new GoldenHenSpawnSettings(true, 0.005F, 2);
        }
    }

    public record EventParticleSettings(
            boolean enabled,
            int goodCount,
            int badCount,
            float spreadX,
            float spreadY,
            float spreadZ,
            float goodSpeed,
            float badSpeed,
            List<String> goodEvents,
            List<String> badEvents
    ) {
        public static EventParticleSettings defaults() {
            return new EventParticleSettings(
                    true,
                    12,
                    12,
                    0.35F,
                    0.25F,
                    0.35F,
                    0.02F,
                    0.01F,
                    List.of(
                            "lucky_block_transform",
                            "block_transform",
                            "golden_hen_spawn",
                            "mine_bonus_drops",
                            "ore_vein_burst",
                            "xp_burst",
                            "friendly_spawn"
                    ),
                    List.of(
                            "tnt_transform",
                            "bedrock_transform",
                            "mostly_unlucky_block_transform",
                            "seismic_burst",
                            "hostile_spawn"
                    )
            );
        }

        public boolean isGoodEvent(String eventKey) {
            if (eventKey == null) {
                return false;
            }
            return goodEvents.contains(eventKey.toLowerCase());
        }

        public boolean isBadEvent(String eventKey) {
            if (eventKey == null) {
                return false;
            }
            return badEvents.contains(eventKey.toLowerCase());
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
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "looting"), 24, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 22, 1, 5),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "smite"), 16, 1, 5),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "bane_of_arthropods"), 16, 1, 5),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "fire_aspect"), 10, 1, 2),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "knockback"), 10, 1, 2),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "sweeping_edge"), 8, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "thorns"), 8, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "depth_strider"), 7, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "feather_falling"), 7, 1, 4),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "respiration"), 6, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "aqua_affinity"), 6, 1, 1),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "soul_speed"), 4, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "swift_sneak"), 4, 1, 3),
                            new EnchantmentEntry(Identifier.fromNamespaceAndPath("minecraft", "frost_walker"), 4, 1, 2)
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

    public record XpBurstSettings(boolean enabled, float chance, int minXp, int maxXp) {
        public static XpBurstSettings defaults() {
            return new XpBurstSettings(true, 0.12F, 1, 6);
        }
    }

    public record WeightedDropEntry(Item item, int minCount, int maxCount, int weight) {
    }

    public record OreVeinBurstSettings(
            boolean enabled,
            float chance,
            int minRolls,
            int maxRolls,
            List<WeightedDropEntry> entries
    ) {
        public static OreVeinBurstSettings defaults() {
            return new OreVeinBurstSettings(
                    true,
                    0.1F,
                    2,
                    4,
                    List.of(
                            new WeightedDropEntry(Items.COAL, 1, 3, 28),
                            new WeightedDropEntry(Items.REDSTONE, 1, 4, 24),
                            new WeightedDropEntry(Items.LAPIS_LAZULI, 1, 3, 18),
                            new WeightedDropEntry(Items.IRON_NUGGET, 1, 4, 16),
                            new WeightedDropEntry(Items.GOLD_NUGGET, 1, 3, 10),
                            new WeightedDropEntry(Items.DIAMOND, 1, 1, 2),
                            new WeightedDropEntry(Items.EMERALD, 1, 1, 2)
                    )
            );
        }
    }

    public record SeismicBurstSettings(
            boolean enabled,
            float chance,
            float radius,
            int slownessTicks,
            int slownessAmplifier,
            float upwardBoost
    ) {
        public static SeismicBurstSettings defaults() {
            return new SeismicBurstSettings(true, 0.07F, 4.5F, 60, 1, 0.42F);
        }
    }

    public record HostileMobEntry(EntityType<?> type, int minCount, int maxCount) {
    }

    public record HostileSpawnSettings(
            boolean enabled,
            float chance,
            int spawnRadius,
            int overallMinCount,
            int overallMaxCount,
            boolean forceNormalOnPeaceful,
            List<HostileMobEntry> mobs
    ) {
        public static HostileSpawnSettings defaults() {
            return new HostileSpawnSettings(
                    true,
                    0.03F,
                    2,
                    1,
                    2,
                true,
                    List.of(
                            new HostileMobEntry(EntityType.ZOMBIE, 0, 1),
                            new HostileMobEntry(EntityType.SKELETON, 0, 1),
                            new HostileMobEntry(EntityType.SPIDER, 0, 1),
                            new HostileMobEntry(EntityType.CREEPER, 0, 1)
                    )
            );
        }
    }

    public record FriendlyMobEntry(EntityType<?> type, int minCount, int maxCount) {
    }

    public record FriendlySpawnSettings(
            boolean enabled,
            float chance,
            int spawnRadius,
            int overallMinCount,
            int overallMaxCount,
            List<FriendlyMobEntry> mobs
    ) {
        public static FriendlySpawnSettings defaults() {
            return new FriendlySpawnSettings(
                    true,
                    0.03F,
                    2,
                    1,
                    2,
                    List.of(
                            new FriendlyMobEntry(EntityType.COW, 0, 1),
                            new FriendlyMobEntry(EntityType.SHEEP, 0, 1),
                            new FriendlyMobEntry(EntityType.PIG, 0, 1),
                            new FriendlyMobEntry(EntityType.CHICKEN, 0, 1)
                    )
            );
        }
    }
}