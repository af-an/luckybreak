package com.luckybreak.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luckybreak.LuckyBreak;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LuckySwordConfig {

    private static final String CONFIG_PATH = "data/luckybreak/items/lucky_sword.json";
    private static final int DEFAULT_DURABILITY = 32;
    private static final float BASE_ATTACK_DAMAGE = 1.0F;
    private static final float BASE_ATTACK_SPEED = 4.0F;
    private static final float DEFAULT_ATTACK_DAMAGE = 3.0F;
    private static final float DEFAULT_ATTACK_SPEED = -2.4F;

    private static int durability = DEFAULT_DURABILITY;
    private static float attackDamage = DEFAULT_ATTACK_DAMAGE;
    private static float attackSpeed = DEFAULT_ATTACK_SPEED;
    private static LightningSettings lightningSettings = LightningSettings.defaults();
    private static KnockbackFireTrailSettings knockbackFireTrailSettings = KnockbackFireTrailSettings.defaults();
    private static TemporaryEnchantSettings temporaryEnchantSettings = TemporaryEnchantSettings.defaults();
    private static FunEffectSettings funEffectSettings = FunEffectSettings.defaults();
    private static boolean loaded;

    private LuckySwordConfig() {
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

    public static LightningSettings lightningSettings() {
        ensureLoaded();
        return lightningSettings;
    }

    public static KnockbackFireTrailSettings knockbackFireTrailSettings() {
        ensureLoaded();
        return knockbackFireTrailSettings;
    }

    public static TemporaryEnchantSettings temporaryEnchantSettings() {
        ensureLoaded();
        return temporaryEnchantSettings;
    }

    public static FunEffectSettings funEffectSettings() {
        ensureLoaded();
        return funEffectSettings;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        durability = DEFAULT_DURABILITY;
        attackDamage = DEFAULT_ATTACK_DAMAGE;
        attackSpeed = DEFAULT_ATTACK_SPEED;
        lightningSettings = LightningSettings.defaults();
        knockbackFireTrailSettings = KnockbackFireTrailSettings.defaults();
        temporaryEnchantSettings = TemporaryEnchantSettings.defaults();
        funEffectSettings = FunEffectSettings.defaults();

        InputStream input = LuckySwordConfig.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing Lucky Sword config: {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            durability = parseInt(root, "durability", DEFAULT_DURABILITY, 1, 10000);
            attackDamage = parseAttackDamageModifier(root);
            attackSpeed = parseAttackSpeedModifier(root);
            lightningSettings = parseLightning(root);
            knockbackFireTrailSettings = parseKnockbackFireTrail(root);
            temporaryEnchantSettings = parseTemporaryEnchantments(root);
            funEffectSettings = parseFunEffects(root);
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load Lucky Sword config", exception);
        }
    }

    private static FunEffectSettings parseFunEffects(JsonObject root) {
        if (!root.has("fun_effects") || !root.get("fun_effects").isJsonObject()) {
            return FunEffectSettings.defaults();
        }

        FunEffectSettings defaults = FunEffectSettings.defaults();
        JsonObject obj = root.getAsJsonObject("fun_effects");
        EnderSwapSettings enderSwap = parseEnderSwap(obj, defaults.enderSwap());
        SkyLaunchSettings skyLaunch = parseSkyLaunch(obj, defaults.skyLaunch());
        EchoSlashSettings echoSlash = parseEchoSlash(obj, defaults.echoSlash());
        ChainLightningSettings chainLightning = parseChainLightning(obj, defaults.chainLightning());
        ShadowStepSettings shadowStep = parseShadowStep(obj, defaults.shadowStep());
        FrostPrisonSettings frostPrison = parseFrostPrison(obj, defaults.frostPrison());
        StarfallStrikeSettings starfallStrike = parseStarfallStrike(obj, defaults.starfallStrike());
        return new FunEffectSettings(enderSwap, skyLaunch, echoSlash, chainLightning, shadowStep, frostPrison, starfallStrike);
    }

    private static EnderSwapSettings parseEnderSwap(JsonObject root, EnderSwapSettings fallback) {
        if (!root.has("ender_swap") || !root.get("ender_swap").isJsonObject()) {
            return fallback;
        }

        JsonObject obj = root.getAsJsonObject("ender_swap");
        boolean enabled = parseBoolean(obj, "enabled", fallback.enabled());
        float chance = parseFloat(obj, "chance", fallback.chance(), 0.0F, 1.0F);
        float failedSwapIgniteSeconds = parseFloat(obj, "failed_swap_ignite_seconds", fallback.failedSwapIgniteSeconds(), 0.0F, 60.0F);
        return new EnderSwapSettings(enabled, chance, failedSwapIgniteSeconds);
    }

    private static SkyLaunchSettings parseSkyLaunch(JsonObject root, SkyLaunchSettings fallback) {
        if (!root.has("sky_launch") || !root.get("sky_launch").isJsonObject()) {
            return fallback;
        }

        JsonObject obj = root.getAsJsonObject("sky_launch");
        boolean enabled = parseBoolean(obj, "enabled", fallback.enabled());
        float chance = parseFloat(obj, "chance", fallback.chance(), 0.0F, 1.0F);
        float verticalBoost = parseFloat(obj, "vertical_boost", fallback.verticalBoost(), 0.1F, 3.0F);
        int trailDurationTicks = parseInt(obj, "trail_duration_ticks", fallback.trailDurationTicks(), 0, 120);
        int trailIntervalTicks = parseInt(obj, "trail_interval_ticks", fallback.trailIntervalTicks(), 1, 20);
        int particlesPerStep = parseInt(obj, "particles_per_step", fallback.particlesPerStep(), 1, 64);
        return new SkyLaunchSettings(enabled, chance, verticalBoost, trailDurationTicks, trailIntervalTicks, particlesPerStep);
    }

    private static EchoSlashSettings parseEchoSlash(JsonObject root, EchoSlashSettings fallback) {
        if (!root.has("echo_slash") || !root.get("echo_slash").isJsonObject()) {
            return fallback;
        }

        JsonObject obj = root.getAsJsonObject("echo_slash");
        boolean enabled = parseBoolean(obj, "enabled", fallback.enabled());
        float chance = parseFloat(obj, "chance", fallback.chance(), 0.0F, 1.0F);
        int extraHits = parseInt(obj, "extra_hits", fallback.extraHits(), 1, 8);
        int intervalTicks = parseInt(obj, "interval_ticks", fallback.intervalTicks(), 1, 20);
        float damagePerHit = parseFloat(obj, "damage_per_hit", fallback.damagePerHit(), 0.0F, 100.0F);
        return new EchoSlashSettings(enabled, chance, extraHits, intervalTicks, damagePerHit);
    }

    private static ChainLightningSettings parseChainLightning(JsonObject root, ChainLightningSettings fallback) {
        if (!root.has("chain_lightning") || !root.get("chain_lightning").isJsonObject()) {
            return fallback;
        }

        JsonObject obj = root.getAsJsonObject("chain_lightning");
        boolean enabled = parseBoolean(obj, "enabled", fallback.enabled());
        float chance = parseFloat(obj, "chance", fallback.chance(), 0.0F, 1.0F);
        float radius = parseFloat(obj, "radius", fallback.radius(), 1.0F, 256.0F);
        int minTargets = parseInt(obj, "min_targets", fallback.minTargets(), 1, 2048);
        int maxTargets = parseInt(obj, "max_targets", fallback.maxTargets(), minTargets, 2048);
        float damagePerTarget = parseFloat(obj, "damage_per_target", fallback.damagePerTarget(), 0.0F, 100.0F);
        boolean igniteTarget = parseBoolean(obj, "ignite_target", fallback.igniteTarget());
        float igniteSeconds = parseFloat(obj, "ignite_seconds", fallback.igniteSeconds(), 0.0F, 60.0F);
        return new ChainLightningSettings(enabled, chance, radius, minTargets, maxTargets, damagePerTarget, igniteTarget, igniteSeconds);
    }

    private static ShadowStepSettings parseShadowStep(JsonObject root, ShadowStepSettings fallback) {
        if (!root.has("shadow_step") || !root.get("shadow_step").isJsonObject()) {
            return fallback;
        }

        JsonObject obj = root.getAsJsonObject("shadow_step");
        boolean enabled = parseBoolean(obj, "enabled", fallback.enabled());
        float chance = parseFloat(obj, "chance", fallback.chance(), 0.0F, 1.0F);
        float behindDistance = parseFloat(obj, "behind_distance", fallback.behindDistance(), 0.5F, 6.0F);
        float bonusDamage = parseFloat(obj, "bonus_damage", fallback.bonusDamage(), 0.0F, 100.0F);
        return new ShadowStepSettings(enabled, chance, behindDistance, bonusDamage);
    }

    private static FrostPrisonSettings parseFrostPrison(JsonObject root, FrostPrisonSettings fallback) {
        if (!root.has("frost_prison") || !root.get("frost_prison").isJsonObject()) {
            return fallback;
        }

        JsonObject obj = root.getAsJsonObject("frost_prison");
        boolean enabled = parseBoolean(obj, "enabled", fallback.enabled());
        float chance = parseFloat(obj, "chance", fallback.chance(), 0.0F, 1.0F);
        int durationTicks = parseInt(obj, "duration_ticks", fallback.durationTicks(), 1, 200);
        int slownessAmplifier = parseInt(obj, "slowness_amplifier", fallback.slownessAmplifier(), 0, 10);
        int particleCount = parseInt(obj, "particle_count", fallback.particleCount(), 1, 128);
        return new FrostPrisonSettings(enabled, chance, durationTicks, slownessAmplifier, particleCount);
    }

    private static StarfallStrikeSettings parseStarfallStrike(JsonObject root, StarfallStrikeSettings fallback) {
        if (!root.has("starfall_strike") || !root.get("starfall_strike").isJsonObject()) {
            return fallback;
        }

        JsonObject obj = root.getAsJsonObject("starfall_strike");
        boolean enabled = parseBoolean(obj, "enabled", fallback.enabled());
        float chance = parseFloat(obj, "chance", fallback.chance(), 0.0F, 1.0F);
        int delayTicks = parseInt(obj, "delay_ticks", fallback.delayTicks(), 1, 80);
        float damage = parseFloat(obj, "damage", fallback.damage(), 0.0F, 200.0F);
        int particleCount = parseInt(obj, "particle_count", fallback.particleCount(), 1, 128);
        return new StarfallStrikeSettings(enabled, chance, delayTicks, damage, particleCount);
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

    private static LightningSettings parseLightning(JsonObject root) {
        if (!root.has("lightning") || !root.get("lightning").isJsonObject()) {
            return LightningSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject("lightning");
        boolean enabled = parseBoolean(obj, "enabled", true);
        float mobHitChance = parseFloat(obj, "mob_hit_chance", 0.35F, 0.0F, 1.0F);
        boolean visualOnly = parseBoolean(obj, "visual_only", true);
        float damage = parseFloat(obj, "damage", 5.0F, 0.0F, 1000.0F);
        boolean igniteTarget = parseBoolean(obj, "ignite_target", true);
        float igniteSeconds = parseFloat(obj, "ignite_seconds", 5.0F, 0.0F, 60.0F);
        int followDurationTicks = parseInt(obj, "follow_duration_ticks", 6, 0, 200);
        int followIntervalTicks = parseInt(obj, "follow_interval_ticks", 2, 1, 40);
        boolean freezeTargetDuringVisual = parseBoolean(obj, "freeze_target_during_visual", false);
        return new LightningSettings(enabled, mobHitChance, visualOnly, damage, igniteTarget, igniteSeconds, followDurationTicks, followIntervalTicks, freezeTargetDuringVisual);
    }

    private static KnockbackFireTrailSettings parseKnockbackFireTrail(JsonObject root) {
        if (!root.has("knockback_fire_trail") || !root.get("knockback_fire_trail").isJsonObject()) {
            return KnockbackFireTrailSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject("knockback_fire_trail");
        boolean enabled = parseBoolean(obj, "enabled", false);
        float chance = parseFloat(obj, "chance", 0.2F, 0.0F, 1.0F);
        boolean igniteTarget = parseBoolean(obj, "ignite_target", true);
        float igniteSeconds = parseFloat(obj, "ignite_seconds", 4.0F, 0.0F, 60.0F);
        float knockbackStrength = parseFloat(obj, "knockback_strength", 2.25F, 0.1F, 10.0F);
        float upwardBoost = parseFloat(obj, "upward_boost", 0.18F, 0.0F, 2.0F);
        int trailDurationTicks = parseInt(obj, "trail_duration_ticks", 12, 1, 200);
        int trailIntervalTicks = parseInt(obj, "trail_interval_ticks", 1, 1, 40);
        int particlesPerStep = parseInt(obj, "particles_per_step", 7, 1, 200);
        double spread = parseFloat(obj, "spread", 0.18F, 0.0F, 2.0F);
        return new KnockbackFireTrailSettings(
                enabled,
                chance,
                igniteTarget,
                igniteSeconds,
                knockbackStrength,
                upwardBoost,
                trailDurationTicks,
                trailIntervalTicks,
                particlesPerStep,
                spread);
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

    public record LightningSettings(
            boolean enabled,
            float mobHitChance,
            boolean visualOnly,
            float damage,
            boolean igniteTarget,
            float igniteSeconds,
            int followDurationTicks,
            int followIntervalTicks,
            boolean freezeTargetDuringVisual) {
        public static LightningSettings defaults() {
            return new LightningSettings(true, 0.35F, true, 5.0F, true, 5.0F, 6, 2, false);
        }
    }

    public record KnockbackFireTrailSettings(
            boolean enabled,
            float chance,
            boolean igniteTarget,
            float igniteSeconds,
            float knockbackStrength,
            float upwardBoost,
            int trailDurationTicks,
            int trailIntervalTicks,
            int particlesPerStep,
            double spread) {
        public static KnockbackFireTrailSettings defaults() {
            return new KnockbackFireTrailSettings(false, 0.2F, true, 4.0F, 2.25F, 0.18F, 12, 1, 7, 0.18D);
        }
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
        public static TemporaryEnchantSettings defaults() {
            return new TemporaryEnchantSettings(
                    true,
                    0.3F,
                    1,
                    2,
                    3,
                    8,
                    List.of(
                            new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness"), 30, 1, 5),
                            new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "smite"), 18, 1, 5),
                            new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "bane_of_arthropods"), 14, 1, 5),
                            new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "knockback"), 14, 1, 2),
                            new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "fire_aspect"), 12, 1, 2),
                            new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "looting"), 10, 1, 3),
                            new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "sweeping_edge"), 10, 1, 3),
                            new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "unbreaking"), 14, 1, 3),
                            new EnchantmentEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "mending"), 8, 1, 1)
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

    public record FunEffectSettings(
            EnderSwapSettings enderSwap,
            SkyLaunchSettings skyLaunch,
            EchoSlashSettings echoSlash,
            ChainLightningSettings chainLightning,
            ShadowStepSettings shadowStep,
            FrostPrisonSettings frostPrison,
            StarfallStrikeSettings starfallStrike
    ) {
        public static FunEffectSettings defaults() {
            return new FunEffectSettings(
                    EnderSwapSettings.defaults(),
                    SkyLaunchSettings.defaults(),
                EchoSlashSettings.defaults(),
                ChainLightningSettings.defaults(),
                ShadowStepSettings.defaults(),
                FrostPrisonSettings.defaults(),
                StarfallStrikeSettings.defaults()
            );
        }
    }

    public record EnderSwapSettings(boolean enabled, float chance, float failedSwapIgniteSeconds) {
        public static EnderSwapSettings defaults() {
            return new EnderSwapSettings(true, 0.08F, 4.0F);
        }
    }

    public record SkyLaunchSettings(
            boolean enabled,
            float chance,
            float verticalBoost,
            int trailDurationTicks,
            int trailIntervalTicks,
            int particlesPerStep
    ) {
        public static SkyLaunchSettings defaults() {
            return new SkyLaunchSettings(true, 0.14F, 1.25F, 14, 1, 8);
        }
    }

    public record EchoSlashSettings(
            boolean enabled,
            float chance,
            int extraHits,
            int intervalTicks,
            float damagePerHit
    ) {
        public static EchoSlashSettings defaults() {
            return new EchoSlashSettings(true, 0.12F, 2, 3, 2.5F);
        }
    }

    public record ChainLightningSettings(
            boolean enabled,
            float chance,
            float radius,
            int minTargets,
            int maxTargets,
            float damagePerTarget,
            boolean igniteTarget,
            float igniteSeconds
    ) {
        public static ChainLightningSettings defaults() {
            return new ChainLightningSettings(true, 0.1F, 64.0F, 3, 10, 3.0F, true, 4.0F);
        }
    }

    public record ShadowStepSettings(
            boolean enabled,
            float chance,
            float behindDistance,
            float bonusDamage
    ) {
        public static ShadowStepSettings defaults() {
            return new ShadowStepSettings(true, 0.1F, 1.6F, 2.0F);
        }
    }

    public record FrostPrisonSettings(
            boolean enabled,
            float chance,
            int durationTicks,
            int slownessAmplifier,
            int particleCount
    ) {
        public static FrostPrisonSettings defaults() {
            return new FrostPrisonSettings(true, 0.12F, 30, 6, 20);
        }
    }

    public record StarfallStrikeSettings(
            boolean enabled,
            float chance,
            int delayTicks,
            float damage,
            int particleCount
    ) {
        public static StarfallStrikeSettings defaults() {
            return new StarfallStrikeSettings(true, 0.09F, 10, 5.0F, 28);
        }
    }
}