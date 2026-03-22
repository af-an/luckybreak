package com.luckybreak.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luckybreak.LuckyBreak;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LuckyBowConfig {

    private static final String CONFIG_PATH = "data/luckybreak/items/lucky_bow.json";
    private static final boolean DEFAULT_INFINITY_WITHOUT_ARROWS = false;
    private static final boolean DEFAULT_NON_COLLECTABLE_FIRED_ARROWS = true;
    private static final int DEFAULT_DURABILITY = 64;
    private static final RandomArrowTypeSettings DEFAULT_RANDOM_ARROW_TYPE_SETTINGS = new RandomArrowTypeSettings(
        true,
        1.00F,
        List.of(
            new WeightedArrowTypeEntry(ArrowType.NORMAL, 1),
            new WeightedArrowTypeEntry(ArrowType.SPECTRAL, 4),
            new WeightedArrowTypeEntry(ArrowType.TIPPED, 8)
        )
    );
    private static final List<WeightedEffectModeEntry> DEFAULT_EFFECT_MODE_POOL = List.of(
        new WeightedEffectModeEntry(BowEffectMode.TNT_TRAIL, 2),
        new WeightedEffectModeEntry(BowEffectMode.BLOCK_TRANSFORM, 2),
        new WeightedEffectModeEntry(BowEffectMode.FIRE_SPREAD, 2),
        new WeightedEffectModeEntry(BowEffectMode.ENTITY_HIT_BLOCK_TRANSFORM, 2),
        new WeightedEffectModeEntry(BowEffectMode.ENTITY_HIT_POTION, 2),
        new WeightedEffectModeEntry(BowEffectMode.ENTITY_HIT_LIGHTNING, 2),
        new WeightedEffectModeEntry(BowEffectMode.CHICKEN_RAIN, 2),
        new WeightedEffectModeEntry(BowEffectMode.PARTY_POP, 2),
        new WeightedEffectModeEntry(BowEffectMode.ENTITY_BOUNCE, 2),
        new WeightedEffectModeEntry(BowEffectMode.RANDOM_ARROW_TYPES, 2)
    );
    private static final EffectCombinationSettings DEFAULT_EFFECT_COMBINATION_SETTINGS = new EffectCombinationSettings(
        true,
        1,
        10,
        true,
        List.of(
            new EffectCountWeight(1, 2),
            new EffectCountWeight(2, 8),
            new EffectCountWeight(3, 8),
            new EffectCountWeight(4, 3),
            new EffectCountWeight(5, 2),
            new EffectCountWeight(6, 1),
            new EffectCountWeight(7, 1),
            new EffectCountWeight(8, 1),
            new EffectCountWeight(9, 1),
            new EffectCountWeight(10, 1)
        ),
        List.of(new ModePair(BowEffectMode.TNT_TRAIL, BowEffectMode.BLOCK_TRANSFORM))
    );
    private static final TntTrailSettings DEFAULT_TNT_TRAIL_SETTINGS = new TntTrailSettings(true, 0.60F, 2, 5, 5.0F, 2.0F, 35);
    private static final FireSpreadSettings DEFAULT_FIRE_SPREAD_SETTINGS = new FireSpreadSettings(true, 0.75F, 3, 8, 3);
        private static final BlockTransformSettings DEFAULT_BLOCK_TRANSFORM_SETTINGS = new BlockTransformSettings(
            true,
            0.60F,
            2,
            6,
                List.of(
                    new WeightedBlockEntry(Blocks.STONE, 1),
                    new WeightedBlockEntry(Blocks.COBBLESTONE, 1),
                    new WeightedBlockEntry(Blocks.OAK_LOG, 1),
                    new WeightedBlockEntry(Blocks.DEEPSLATE_IRON_ORE, 1),
                    new WeightedBlockEntry(Blocks.DEEPSLATE_GOLD_ORE, 1),
                    new WeightedBlockEntry(Blocks.DEEPSLATE_EMERALD_ORE, 1),
                    new WeightedBlockEntry(Blocks.DEEPSLATE_DIAMOND_ORE, 1)
                )
        );
        private static final EntityHitBlockTransformSettings DEFAULT_ENTITY_HIT_BLOCK_TRANSFORM_SETTINGS = new EntityHitBlockTransformSettings(
            true,
            0.60F,
            2,
                4,
                List.of(
                    new WeightedBlockEntry(Blocks.WATER, 1),
                    new WeightedBlockEntry(Blocks.LAVA, 1)
                )
        );
            private static final EntityHitPotionSettings DEFAULT_ENTITY_HIT_POTION_SETTINGS = new EntityHitPotionSettings(
                true,
                0.60F,
                    List.of(
                        new PotionEntry("minecraft:slowness", 120, 1, 1),
                        new PotionEntry("minecraft:weakness", 120, 0, 1),
                        new PotionEntry("minecraft:poison", 100, 0, 1),
                        new PotionEntry("minecraft:blindness", 80, 0, 1),
                        new PotionEntry("minecraft:levitation", 60, 0, 1),
                        new PotionEntry("minecraft:glowing", 200, 0, 1),
                        new PotionEntry("minecraft:regeneration", 80, 0, 1),
                        new PotionEntry("minecraft:speed", 120, 1, 1),
                        new PotionEntry("minecraft:jump_boost", 120, 1, 1),
                        new PotionEntry("minecraft:resistance", 100, 0, 1)
                    )
            );
            private static final EntityHitLightningSettings DEFAULT_ENTITY_HIT_LIGHTNING_SETTINGS = new EntityHitLightningSettings(
                true,
                0.60F,
                false
            );
                private static final TemporaryEnchantSettings DEFAULT_TEMPORARY_ENCHANT_SETTINGS = new TemporaryEnchantSettings(
                    true,
                    0.25F,
                    1,
                    2,
                    3,
                    8,
                    List.of(
                        new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "power"), 32, 1, 5),
                        new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "punch"), 18, 1, 2),
                        new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "flame"), 10, 1, 1),
                        new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "infinity"), 8, 1, 1),
                        new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 20, 1, 3),
                        new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "mending"), 8, 1, 1)
                    ),
                    TemporaryEnchantParticleSettings.defaults()
                );
    private static final ChickenRainSettings DEFAULT_CHICKEN_RAIN_SETTINGS = new ChickenRainSettings(true, 0.55F, 2, 5, 4);
    private static final PartyPopSettings DEFAULT_PARTY_POP_SETTINGS = new PartyPopSettings(true, 0.65F, 3.5F, 24);
    private static final EntityBounceSettings DEFAULT_ENTITY_BOUNCE_SETTINGS = new EntityBounceSettings(true, 0.65F, 0.85F, 0.25F);

    private static boolean infinityWithoutArrows = DEFAULT_INFINITY_WITHOUT_ARROWS;
    private static boolean nonCollectableFiredArrows = DEFAULT_NON_COLLECTABLE_FIRED_ARROWS;
    private static int durability = DEFAULT_DURABILITY;
    private static RandomArrowTypeSettings randomArrowTypeSettings = DEFAULT_RANDOM_ARROW_TYPE_SETTINGS;
    private static EffectCombinationSettings effectCombinationSettings = DEFAULT_EFFECT_COMBINATION_SETTINGS;
    private static List<WeightedEffectModeEntry> effectModePool = DEFAULT_EFFECT_MODE_POOL;
    private static TntTrailSettings tntTrailSettings = DEFAULT_TNT_TRAIL_SETTINGS;
    private static FireSpreadSettings fireSpreadSettings = DEFAULT_FIRE_SPREAD_SETTINGS;
    private static BlockTransformSettings blockTransformSettings = DEFAULT_BLOCK_TRANSFORM_SETTINGS;
    private static EntityHitBlockTransformSettings entityHitBlockTransformSettings = DEFAULT_ENTITY_HIT_BLOCK_TRANSFORM_SETTINGS;
    private static EntityHitPotionSettings entityHitPotionSettings = DEFAULT_ENTITY_HIT_POTION_SETTINGS;
    private static EntityHitLightningSettings entityHitLightningSettings = DEFAULT_ENTITY_HIT_LIGHTNING_SETTINGS;
    private static TemporaryEnchantSettings temporaryEnchantSettings = DEFAULT_TEMPORARY_ENCHANT_SETTINGS;
    private static ChickenRainSettings chickenRainSettings = DEFAULT_CHICKEN_RAIN_SETTINGS;
    private static PartyPopSettings partyPopSettings = DEFAULT_PARTY_POP_SETTINGS;
    private static EntityBounceSettings entityBounceSettings = DEFAULT_ENTITY_BOUNCE_SETTINGS;
    private static boolean loaded;

    private LuckyBowConfig() {
    }

    public static boolean infinityWithoutArrows() {
        ensureLoaded();
        return infinityWithoutArrows;
    }

    public static boolean nonCollectableFiredArrows() {
        ensureLoaded();
        return nonCollectableFiredArrows;
    }

    public static int durability() {
        ensureLoaded();
        return durability;
    }

    public static RandomArrowTypeSettings randomArrowTypeSettings() {
        ensureLoaded();
        return randomArrowTypeSettings;
    }

    public static EffectCombinationSettings effectCombinationSettings() {
        ensureLoaded();
        return effectCombinationSettings;
    }

    public static List<WeightedEffectModeEntry> effectModePool() {
        ensureLoaded();
        return effectModePool;
    }

    public static TntTrailSettings tntTrailSettings() {
        ensureLoaded();
        return tntTrailSettings;
    }

    public static FireSpreadSettings fireSpreadSettings() {
        ensureLoaded();
        return fireSpreadSettings;
    }

    public static BlockTransformSettings blockTransformSettings() {
        ensureLoaded();
        return blockTransformSettings;
    }

    public static EntityHitBlockTransformSettings entityHitBlockTransformSettings() {
        ensureLoaded();
        return entityHitBlockTransformSettings;
    }

    public static EntityHitPotionSettings entityHitPotionSettings() {
        ensureLoaded();
        return entityHitPotionSettings;
    }

    public static EntityHitLightningSettings entityHitLightningSettings() {
        ensureLoaded();
        return entityHitLightningSettings;
    }

    public static TemporaryEnchantSettings temporaryEnchantSettings() {
        ensureLoaded();
        return temporaryEnchantSettings;
    }

    public static ChickenRainSettings chickenRainSettings() {
        ensureLoaded();
        return chickenRainSettings;
    }

    public static PartyPopSettings partyPopSettings() {
        ensureLoaded();
        return partyPopSettings;
    }

    public static EntityBounceSettings entityBounceSettings() {
        ensureLoaded();
        return entityBounceSettings;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        infinityWithoutArrows = DEFAULT_INFINITY_WITHOUT_ARROWS;
        nonCollectableFiredArrows = DEFAULT_NON_COLLECTABLE_FIRED_ARROWS;
        durability = DEFAULT_DURABILITY;
        randomArrowTypeSettings = DEFAULT_RANDOM_ARROW_TYPE_SETTINGS;
        effectCombinationSettings = DEFAULT_EFFECT_COMBINATION_SETTINGS;
        effectModePool = DEFAULT_EFFECT_MODE_POOL;
        tntTrailSettings = DEFAULT_TNT_TRAIL_SETTINGS;
        fireSpreadSettings = DEFAULT_FIRE_SPREAD_SETTINGS;
        blockTransformSettings = DEFAULT_BLOCK_TRANSFORM_SETTINGS;
        entityHitBlockTransformSettings = DEFAULT_ENTITY_HIT_BLOCK_TRANSFORM_SETTINGS;
        entityHitPotionSettings = DEFAULT_ENTITY_HIT_POTION_SETTINGS;
        entityHitLightningSettings = DEFAULT_ENTITY_HIT_LIGHTNING_SETTINGS;
        temporaryEnchantSettings = DEFAULT_TEMPORARY_ENCHANT_SETTINGS;
        chickenRainSettings = DEFAULT_CHICKEN_RAIN_SETTINGS;
        partyPopSettings = DEFAULT_PARTY_POP_SETTINGS;
        entityBounceSettings = DEFAULT_ENTITY_BOUNCE_SETTINGS;

        InputStream input = LuckyBowConfig.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing Lucky Bow config: {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("infinity_enabled")) {
                infinityWithoutArrows = root.get("infinity_enabled").getAsBoolean();
            }
            if (root.has("infinity_without_arrows")) {
                infinityWithoutArrows = root.get("infinity_without_arrows").getAsBoolean();
            }
            if (root.has("non_collectable_fired_arrows")) {
                nonCollectableFiredArrows = root.get("non_collectable_fired_arrows").getAsBoolean();
            }
            durability = parseInt(root, "durability", DEFAULT_DURABILITY, 1, 5000);
            randomArrowTypeSettings = parseRandomArrowTypeSettings(root);
            effectCombinationSettings = parseEffectCombinationSettings(root);
            effectModePool = parseEffectModePool(root);
            tntTrailSettings = parseTntTrailSettings(root);
            fireSpreadSettings = parseFireSpreadSettings(root);
            blockTransformSettings = parseBlockTransformSettings(root);
            entityHitBlockTransformSettings = parseEntityHitBlockTransformSettings(root);
            entityHitPotionSettings = parseEntityHitPotionSettings(root);
            entityHitLightningSettings = parseEntityHitLightningSettings(root);
            temporaryEnchantSettings = parseTemporaryEnchantSettings(root);
            chickenRainSettings = parseChickenRainSettings(root);
            partyPopSettings = parsePartyPopSettings(root);
            entityBounceSettings = parseEntityBounceSettings(root);
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load Lucky Bow config", exception);
        }
    }

    private static EntityHitPotionSettings parseEntityHitPotionSettings(JsonObject root) {
        if (!root.has("entity_hit_potion") || !root.get("entity_hit_potion").isJsonObject()) {
            return DEFAULT_ENTITY_HIT_POTION_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("entity_hit_potion");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_ENTITY_HIT_POTION_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_ENTITY_HIT_POTION_SETTINGS.chance(), 0.0F, 1.0F);

        List<PotionEntry> possibleEffects = parsePotionEntries(obj);
        if (possibleEffects.isEmpty()) {
            String effectId = parseString(obj, "effect", "minecraft:slowness");
            int durationTicks = parseInt(obj, "duration_ticks", 120, 1, 2400);
            int amplifier = parseInt(obj, "amplifier", 1, 0, 10);
            possibleEffects = List.of(new PotionEntry(effectId, durationTicks, amplifier, 1));
        }

        return new EntityHitPotionSettings(enabled, chance, possibleEffects);
    }

    private static List<PotionEntry> parsePotionEntries(JsonObject obj) {
        List<PotionEntry> entries = new ArrayList<>();
        if (!obj.has("possible_effects") || !obj.get("possible_effects").isJsonArray()) {
            return entries;
        }

        for (var element : obj.getAsJsonArray("possible_effects")) {
            try {
                if (element.isJsonPrimitive()) {
                    String effectId = element.getAsString().trim();
                    if (!effectId.isEmpty()) {
                        entries.add(new PotionEntry(effectId, 120, 0, 1));
                    }
                    continue;
                }

                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject item = element.getAsJsonObject();
                String effectId = parseString(item, "effect", "");
                if (effectId.isBlank()) {
                    continue;
                }

                int durationTicks = parseInt(item, "duration_ticks", 120, 1, 2400);
                int amplifier = parseInt(item, "amplifier", 0, 0, 10);
                int weight = parseInt(item, "weight", 1, 0, 100000);
                entries.add(new PotionEntry(effectId, durationTicks, amplifier, weight));
            } catch (Exception ignored) {
            }
        }

        return entries;
    }

    private static EntityHitLightningSettings parseEntityHitLightningSettings(JsonObject root) {
        if (!root.has("entity_hit_lightning") || !root.get("entity_hit_lightning").isJsonObject()) {
            return DEFAULT_ENTITY_HIT_LIGHTNING_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("entity_hit_lightning");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_ENTITY_HIT_LIGHTNING_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_ENTITY_HIT_LIGHTNING_SETTINGS.chance(), 0.0F, 1.0F);
        boolean visualOnly = parseBoolean(obj, "visual_only", DEFAULT_ENTITY_HIT_LIGHTNING_SETTINGS.visualOnly());
        return new EntityHitLightningSettings(enabled, chance, visualOnly);
    }

    private static TemporaryEnchantSettings parseTemporaryEnchantSettings(JsonObject root) {
        if (!root.has("temporary_enchantments") || !root.get("temporary_enchantments").isJsonObject()) {
            return DEFAULT_TEMPORARY_ENCHANT_SETTINGS;
        }

        TemporaryEnchantSettings defaults = DEFAULT_TEMPORARY_ENCHANT_SETTINGS;
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

        return new TemporaryEnchantSettings(
                enabled,
                chance,
                minEnchantments,
                maxEnchantments,
                minDurationSeconds,
                maxDurationSeconds,
                List.copyOf(entries),
                effectParticles
        );
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

                ResourceLocation id = ResourceLocation.parse(idValue);
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

            var particle = BuiltInRegistries.PARTICLE_TYPE.getValue(ResourceLocation.parse(value));
            if (particle instanceof SimpleParticleType simpleParticle) {
                return simpleParticle;
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private static ChickenRainSettings parseChickenRainSettings(JsonObject root) {
        if (!root.has("chicken_rain") || !root.get("chicken_rain").isJsonObject()) {
            return DEFAULT_CHICKEN_RAIN_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("chicken_rain");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_CHICKEN_RAIN_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_CHICKEN_RAIN_SETTINGS.chance(), 0.0F, 1.0F);
        int minCount = parseInt(obj, "min_count", DEFAULT_CHICKEN_RAIN_SETTINGS.minCount(), 1, 32);
        int maxCount = parseInt(obj, "max_count", DEFAULT_CHICKEN_RAIN_SETTINGS.maxCount(), minCount, 32);
        int searchRadius = parseInt(obj, "search_radius", DEFAULT_CHICKEN_RAIN_SETTINGS.searchRadius(), 1, 8);
        return new ChickenRainSettings(enabled, chance, minCount, maxCount, searchRadius);
    }

    private static PartyPopSettings parsePartyPopSettings(JsonObject root) {
        if (!root.has("party_pop") || !root.get("party_pop").isJsonObject()) {
            return DEFAULT_PARTY_POP_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("party_pop");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_PARTY_POP_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_PARTY_POP_SETTINGS.chance(), 0.0F, 1.0F);
        float radius = parseFloat(obj, "radius", DEFAULT_PARTY_POP_SETTINGS.radius(), 0.5F, 16.0F);
        int particleCount = parseInt(obj, "particle_count", DEFAULT_PARTY_POP_SETTINGS.particleCount(), 0, 512);
        return new PartyPopSettings(enabled, chance, radius, particleCount);
    }

    private static EntityBounceSettings parseEntityBounceSettings(JsonObject root) {
        if (!root.has("entity_bounce") || !root.get("entity_bounce").isJsonObject()) {
            return DEFAULT_ENTITY_BOUNCE_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("entity_bounce");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_ENTITY_BOUNCE_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_ENTITY_BOUNCE_SETTINGS.chance(), 0.0F, 1.0F);
        float upwardBoost = parseFloat(obj, "upward_boost", DEFAULT_ENTITY_BOUNCE_SETTINGS.upwardBoost(), 0.0F, 3.0F);
        float horizontalBoost = parseFloat(obj, "horizontal_boost", DEFAULT_ENTITY_BOUNCE_SETTINGS.horizontalBoost(), 0.0F, 2.0F);
        return new EntityBounceSettings(enabled, chance, upwardBoost, horizontalBoost);
    }

    private static EntityHitBlockTransformSettings parseEntityHitBlockTransformSettings(JsonObject root) {
        if (!root.has("entity_hit_block_transform") || !root.get("entity_hit_block_transform").isJsonObject()) {
            return DEFAULT_ENTITY_HIT_BLOCK_TRANSFORM_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("entity_hit_block_transform");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_ENTITY_HIT_BLOCK_TRANSFORM_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_ENTITY_HIT_BLOCK_TRANSFORM_SETTINGS.chance(), 0.0F, 1.0F);
        int searchRadius = parseInt(obj, "search_radius", DEFAULT_ENTITY_HIT_BLOCK_TRANSFORM_SETTINGS.searchRadius(), 0, 6);
        int transformCount = parseInt(obj, "transform_count", DEFAULT_ENTITY_HIT_BLOCK_TRANSFORM_SETTINGS.transformCount(), 1, 32);

        List<WeightedBlockEntry> possibleBlocks = parseWeightedBlockList(obj, "possible_blocks", DEFAULT_ENTITY_HIT_BLOCK_TRANSFORM_SETTINGS.possibleBlocks());
        return new EntityHitBlockTransformSettings(enabled, chance, searchRadius, transformCount, possibleBlocks);
    }

    private static BlockTransformSettings parseBlockTransformSettings(JsonObject root) {
        if (!root.has("block_transform") || !root.get("block_transform").isJsonObject()) {
            return DEFAULT_BLOCK_TRANSFORM_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("block_transform");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_BLOCK_TRANSFORM_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_BLOCK_TRANSFORM_SETTINGS.chance(), 0.0F, 1.0F);
        int searchRadius = parseInt(obj, "search_radius", DEFAULT_BLOCK_TRANSFORM_SETTINGS.searchRadius(), 0, 6);
        int transformCount = parseInt(obj, "transform_count", DEFAULT_BLOCK_TRANSFORM_SETTINGS.transformCount(), 1, 64);

        List<WeightedBlockEntry> possibleBlocks = parseWeightedBlockList(obj, "possible_blocks", DEFAULT_BLOCK_TRANSFORM_SETTINGS.possibleBlocks());

        return new BlockTransformSettings(enabled, chance, searchRadius, transformCount, List.copyOf(possibleBlocks));
    }

    private static List<WeightedBlockEntry> parseWeightedBlockList(JsonObject obj, String field, List<WeightedBlockEntry> fallback) {
        List<WeightedBlockEntry> possibleBlocks = new ArrayList<>();
        if (obj.has(field) && obj.get(field).isJsonArray()) {
            for (var element : obj.getAsJsonArray(field)) {
                try {
                    if (element.isJsonPrimitive()) {
                        ResourceLocation id = ResourceLocation.parse(element.getAsString());
                        Block block = BuiltInRegistries.BLOCK.getValue(id);
                        if (block != null && block != Blocks.AIR) {
                            possibleBlocks.add(new WeightedBlockEntry(block, 1));
                        }
                        continue;
                    }

                    if (!element.isJsonObject()) {
                        continue;
                    }

                    JsonObject item = element.getAsJsonObject();
                    String blockId = parseString(item, "block", "");
                    if (blockId.isBlank()) {
                        continue;
                    }

                    Block block = BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse(blockId));
                    if (block == null || block == Blocks.AIR) {
                        continue;
                    }

                    int weight = parseInt(item, "weight", 1, 0, 100000);
                    possibleBlocks.add(new WeightedBlockEntry(block, weight));
                } catch (Exception ignored) {
                }
            }
        }

        if (possibleBlocks.isEmpty()) {
            possibleBlocks = new ArrayList<>(fallback);
        }
        return List.copyOf(possibleBlocks);
    }

    private static TntTrailSettings parseTntTrailSettings(JsonObject root) {
        if (!root.has("tnt_trail") || !root.get("tnt_trail").isJsonObject()) {
            return DEFAULT_TNT_TRAIL_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("tnt_trail");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_TNT_TRAIL_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_TNT_TRAIL_SETTINGS.chance(), 0.0F, 1.0F);
        int legacyCount = parseInt(obj, "count", DEFAULT_TNT_TRAIL_SETTINGS.minCount(), 1, 64);
        int minCount = parseInt(obj, "min_count", legacyCount, 1, 64);
        int maxCount = parseInt(obj, "max_count", legacyCount, minCount, 64);
        float startDistance = parseFloat(obj, "start_distance", DEFAULT_TNT_TRAIL_SETTINGS.startDistance(), 3.0F, 64.0F);
        float spacing = parseFloat(obj, "spacing", DEFAULT_TNT_TRAIL_SETTINGS.spacing(), 0.5F, 16.0F);
        int fuseTicks = parseInt(obj, "fuse_ticks", DEFAULT_TNT_TRAIL_SETTINGS.fuseTicks(), 10, 200);
        return new TntTrailSettings(enabled, chance, minCount, maxCount, startDistance, spacing, fuseTicks);
    }

    private static FireSpreadSettings parseFireSpreadSettings(JsonObject root) {
        if (!root.has("fire_spread") || !root.get("fire_spread").isJsonObject()) {
            return DEFAULT_FIRE_SPREAD_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("fire_spread");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_FIRE_SPREAD_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_FIRE_SPREAD_SETTINGS.chance(), 0.0F, 1.0F);
        int legacyCount = parseInt(obj, "count", DEFAULT_FIRE_SPREAD_SETTINGS.minCount(), 1, 64);
        int minCount = parseInt(obj, "min_count", legacyCount, 1, 64);
        int maxCount = parseInt(obj, "max_count", legacyCount, minCount, 64);
        int searchRadius = parseInt(obj, "search_radius", DEFAULT_FIRE_SPREAD_SETTINGS.searchRadius(), 1, 8);
        return new FireSpreadSettings(enabled, chance, minCount, maxCount, searchRadius);
    }

    private static List<WeightedEffectModeEntry> parseEffectModePool(JsonObject root) {
        if (!root.has("effect_modes") || !root.get("effect_modes").isJsonArray()) {
            return DEFAULT_EFFECT_MODE_POOL;
        }

        List<WeightedEffectModeEntry> entries = new ArrayList<>();
        for (var element : root.getAsJsonArray("effect_modes")) {
            try {
                if (element.isJsonPrimitive()) {
                    BowEffectMode mode = BowEffectMode.fromId(element.getAsString());
                    if (mode != null) {
                        entries.add(new WeightedEffectModeEntry(mode, 1));
                    }
                    continue;
                }

                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject obj = element.getAsJsonObject();
                BowEffectMode mode = BowEffectMode.fromId(parseString(obj, "mode", ""));
                if (mode == null) {
                    continue;
                }

                int weight = parseInt(obj, "weight", 1, 0, 100000);
                entries.add(new WeightedEffectModeEntry(mode, weight));
            } catch (Exception ignored) {
            }
        }

        if (entries.isEmpty()) {
            return DEFAULT_EFFECT_MODE_POOL;
        }

        return List.copyOf(entries);
    }

    private static EffectCombinationSettings parseEffectCombinationSettings(JsonObject root) {
        if (!root.has("effect_combination") || !root.get("effect_combination").isJsonObject()) {
            return DEFAULT_EFFECT_COMBINATION_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("effect_combination");
        boolean allowMultiple = parseBoolean(obj, "allow_multiple", DEFAULT_EFFECT_COMBINATION_SETTINGS.allowMultiple());
        int minEffects = parseInt(obj, "min_effects", DEFAULT_EFFECT_COMBINATION_SETTINGS.minEffects(), 0, 16);
        int maxEffects = parseInt(obj, "max_effects", DEFAULT_EFFECT_COMBINATION_SETTINGS.maxEffects(), minEffects, 16);
        boolean enforceMutualExclusions = parseBoolean(obj, "enforce_mutual_exclusions", DEFAULT_EFFECT_COMBINATION_SETTINGS.enforceMutualExclusions());
        List<EffectCountWeight> countWeights = parseEffectCountWeights(obj, minEffects, maxEffects, DEFAULT_EFFECT_COMBINATION_SETTINGS.countWeights());
        List<ModePair> mutuallyExclusivePairs = parseModePairs(obj, DEFAULT_EFFECT_COMBINATION_SETTINGS.mutuallyExclusivePairs());
        return new EffectCombinationSettings(allowMultiple, minEffects, maxEffects, enforceMutualExclusions, countWeights, mutuallyExclusivePairs);
    }

    private static List<EffectCountWeight> parseEffectCountWeights(JsonObject obj, int minEffects, int maxEffects, List<EffectCountWeight> fallback) {
        if (!obj.has("count_weights") || !obj.get("count_weights").isJsonArray()) {
            return fallback;
        }

        List<EffectCountWeight> parsed = new ArrayList<>();
        for (var element : obj.getAsJsonArray("count_weights")) {
            if (!element.isJsonObject()) {
                continue;
            }

            try {
                JsonObject item = element.getAsJsonObject();
                int count = parseInt(item, "count", minEffects, 0, 32);
                int weight = parseInt(item, "weight", 1, 0, 100000);
                if (count < minEffects || count > maxEffects || weight <= 0) {
                    continue;
                }
                parsed.add(new EffectCountWeight(count, weight));
            } catch (Exception ignored) {
            }
        }

        if (parsed.isEmpty()) {
            return fallback;
        }
        return List.copyOf(parsed);
    }

    private static List<ModePair> parseModePairs(JsonObject obj, List<ModePair> fallback) {
        if (!obj.has("mutually_exclusive_pairs") || !obj.get("mutually_exclusive_pairs").isJsonArray()) {
            return fallback;
        }

        List<ModePair> parsed = new ArrayList<>();
        for (var element : obj.getAsJsonArray("mutually_exclusive_pairs")) {
            if (!element.isJsonObject()) {
                continue;
            }

            try {
                JsonObject pair = element.getAsJsonObject();
                BowEffectMode a = BowEffectMode.fromId(parseString(pair, "mode_a", ""));
                BowEffectMode b = BowEffectMode.fromId(parseString(pair, "mode_b", ""));
                if (a == null || b == null || a == b) {
                    continue;
                }
                parsed.add(new ModePair(a, b));
            } catch (Exception ignored) {
            }
        }

        if (parsed.isEmpty()) {
            return fallback;
        }

        return List.copyOf(parsed);
    }

    private static RandomArrowTypeSettings parseRandomArrowTypeSettings(JsonObject root) {
        if (!root.has("random_arrow_types") || !root.get("random_arrow_types").isJsonObject()) {
            return DEFAULT_RANDOM_ARROW_TYPE_SETTINGS;
        }

        JsonObject obj = root.getAsJsonObject("random_arrow_types");
        boolean enabled = parseBoolean(obj, "enabled", DEFAULT_RANDOM_ARROW_TYPE_SETTINGS.enabled());
        float chance = parseFloat(obj, "chance", DEFAULT_RANDOM_ARROW_TYPE_SETTINGS.chance(), 0.0F, 1.0F);

        List<WeightedArrowTypeEntry> entries = new ArrayList<>();
        if (obj.has("types") && obj.get("types").isJsonArray()) {
            for (var element : obj.getAsJsonArray("types")) {
                try {
                    if (element.isJsonPrimitive()) {
                        ArrowType type = ArrowType.fromId(element.getAsString());
                        if (type != null) {
                            entries.add(new WeightedArrowTypeEntry(type, 1));
                        }
                        continue;
                    }

                    if (!element.isJsonObject()) {
                        continue;
                    }

                    JsonObject item = element.getAsJsonObject();
                    ArrowType type = ArrowType.fromId(parseString(item, "type", ""));
                    if (type == null) {
                        continue;
                    }

                    int weight = parseInt(item, "weight", 1, 0, 100000);
                    entries.add(new WeightedArrowTypeEntry(type, weight));
                } catch (Exception ignored) {
                }
            }
        }

        if (entries.isEmpty()) {
            entries = DEFAULT_RANDOM_ARROW_TYPE_SETTINGS.types();
        }

        return new RandomArrowTypeSettings(enabled, chance, List.copyOf(entries));
    }

    private static boolean parseBoolean(JsonObject obj, String field, boolean fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            return obj.get(field).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseInt(JsonObject obj, String field, int fallback, int min, int max) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            int value = obj.get(field).getAsInt();
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float parseFloat(JsonObject obj, String field, float fallback, float min, float max) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            float value = obj.get(field).getAsFloat();
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return fallback;
            }
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String parseString(JsonObject obj, String field, String fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            String value = obj.get(field).getAsString();
            return value == null || value.isBlank() ? fallback : value.trim();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record TntTrailSettings(boolean enabled, float chance, int minCount, int maxCount, float startDistance, float spacing, int fuseTicks) {
    }

    public record FireSpreadSettings(boolean enabled, float chance, int minCount, int maxCount, int searchRadius) {
    }

    public enum ArrowType {
        NORMAL,
        SPECTRAL,
        TIPPED;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static ArrowType fromId(String id) {
            if (id == null || id.isBlank()) {
                return null;
            }

            String normalized = id.trim().toLowerCase(Locale.ROOT);
            for (ArrowType type : values()) {
                if (type.id().equals(normalized)) {
                    return type;
                }
            }

            return null;
        }
    }

    public record WeightedArrowTypeEntry(ArrowType type, int weight) {
    }

    public record RandomArrowTypeSettings(boolean enabled, float chance, List<WeightedArrowTypeEntry> types) {
    }

        public record EffectCombinationSettings(
            boolean allowMultiple,
            int minEffects,
            int maxEffects,
                boolean enforceMutualExclusions,
            List<EffectCountWeight> countWeights,
            List<ModePair> mutuallyExclusivePairs
        ) {
    }

    public record EffectCountWeight(int count, int weight) {
    }

        public record ModePair(BowEffectMode modeA, BowEffectMode modeB) {
        }

    public enum BowEffectMode {
        NORMAL_ARROW,
        TNT_TRAIL,
        BLOCK_TRANSFORM,
        FIRE_SPREAD,
        ENTITY_HIT_BLOCK_TRANSFORM,
        ENTITY_HIT_POTION,
        ENTITY_HIT_LIGHTNING,
        CHICKEN_RAIN,
        PARTY_POP,
        ENTITY_BOUNCE,
        RANDOM_ARROW_TYPES;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static BowEffectMode fromId(String id) {
            if (id == null || id.isBlank()) {
                return null;
            }

            String normalized = id.trim().toLowerCase(Locale.ROOT);
            for (BowEffectMode mode : values()) {
                if (mode.id().equals(normalized)) {
                    return mode;
                }
            }

            return null;
        }
    }

    public record WeightedEffectModeEntry(BowEffectMode mode, int weight) {
    }

    public record BlockTransformSettings(boolean enabled, float chance, int searchRadius, int transformCount, List<WeightedBlockEntry> possibleBlocks) {
    }

    public record EntityHitBlockTransformSettings(boolean enabled, float chance, int searchRadius, int transformCount, List<WeightedBlockEntry> possibleBlocks) {
    }

    public record EntityHitPotionSettings(boolean enabled, float chance, List<PotionEntry> possibleEffects) {
    }

    public record PotionEntry(String effectId, int durationTicks, int amplifier, int weight) {
    }

    public record EntityHitLightningSettings(boolean enabled, float chance, boolean visualOnly) {
    }

        public record EnchantmentEntry(ResourceLocation id, int weight, int minLevel, int maxLevel) {
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

    public record ChickenRainSettings(boolean enabled, float chance, int minCount, int maxCount, int searchRadius) {
    }

    public record PartyPopSettings(boolean enabled, float chance, float radius, int particleCount) {
    }

    public record EntityBounceSettings(boolean enabled, float chance, float upwardBoost, float horizontalBoost) {
    }

    public record WeightedBlockEntry(Block block, int weight) {
    }
}
