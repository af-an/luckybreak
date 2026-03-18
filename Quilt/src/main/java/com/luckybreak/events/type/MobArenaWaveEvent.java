package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.luckybreak.events.LuckyEvent;
import com.luckybreak.events.LuckyScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Temporary circular arena with two short enemy waves and a reward.
 */
public class MobArenaWaveEvent implements LuckyEvent {

    private record WaveConfig(String entityId, int countMin, int countMax, String message, int messageColor) {}
    private record SavedBlock(BlockPos pos, BlockState previousState, BlockState arenaState, boolean supportBlock) {}
    private record SupportChestEntry(Item item, int countMin, int countMax, int weight) {}
    private record GuaranteedChestEntry(Item item, int countMin, int countMax) {}
    private record ZombieArmorTier(Item helmet, Item chestplate, Item leggings, Item boots, int weight) {}
    private record RewardLootEntry(Item item, int countMin, int countMax, int baseWeight, int waveWeightBonus, boolean enchanted, int enchantCostMin, int enchantCostMax) {}

    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final long ARENA_NIGHT_TIME = 13000L;
    private static final int DEFAULT_WALL_DEPTH = 15;
    private static final String LUCKY_BLOCK_ITEM_ID = "luckybreak:lucky_block";
    private static final String VERY_LUCKY_BLOCK_ITEM_ID = "luckybreak:very_lucky_block";
    private static final String REWARD_SIGN_TEXT = "Collect your Reward";
    private static final String[] DEFAULT_REWARD_SIGN_LINES = {"", REWARD_SIGN_TEXT, "", ""};
    private static final String[] DEFAULT_SUPPORT_ITEM_IDS = {
        "minecraft:copper_helmet",
        "minecraft:copper_chestplate",
        "minecraft:copper_leggings",
        "minecraft:copper_boots",
        "minecraft:copper_sword",
        "minecraft:copper_axe",
        "minecraft:copper_pickaxe",
        "minecraft:copper_shovel",
        "minecraft:copper_hoe"
    };

    private final int radius;
    private final int checkIntervalTicks;
    private final long nightTime;
    private final int wallHeight;
    private final int wallDepth;
    private final int wallTorchSpacing;
    private final Block wallBlock;
    private final int waveStartMessageColor;
    private final List<WaveConfig> waves;
    private final int[] waveCountWeights;
    private final List<RewardLootEntry> rewardLootTable;
    private final int rewardRollsMin;
    private final int rewardRollsMax;
    private final Item rewardItem;
    private final int rewardCountMin;
    private final int rewardCountMax;
    private final double rewardBonusPerExtraWave;
    private final int bonusDiamondMinExtraWaves;
    private final int bonusDiamondCountMin;
    private final int bonusDiamondCountMax;
    private final int bonusSwordMinExtraWaves;
    private final int bonusSwordEnchantCostMin;
    private final int bonusSwordEnchantCostMax;
    private final int maxDurationTicks;
    private final String timeoutMessage;
    private final int timeoutEmeraldDropMin;
    private final int timeoutEmeraldDropMax;
    private final String arenaMessage;
    private final int arenaMessageColor;
    private final String forfeitMessage;
    private final int forfeitMessageColor;
    private final double ownerForfeitDistanceMargin;
    private final int ownerForfeitGraceTicks;
    private final boolean supportChestEnabled;
    private final boolean protectWallsFromExplosions;
    private final boolean protectSupportChestFromExplosions;
    private final int supportStacksMin;
    private final int supportStacksMax;
    private final List<SupportChestEntry> supportChestEntries;
    private final List<GuaranteedChestEntry> supportGuaranteedItems;
    private final int rewardChestForwardOffset;
    private final String[] rewardSignLines;
    private final int rewardCelebrationParticleCount;
    private final double rewardCelebrationParticleSpreadX;
    private final double rewardCelebrationParticleSpreadY;
    private final double rewardCelebrationParticleSpreadZ;
    private final int rewardFireworksCount;
    private final double rewardFireworksVelocityMin;
    private final double rewardFireworksVelocityMax;
    private final int rewardFireworkBurstDelayMinTicks;
    private final int rewardFireworkBurstDelayMaxTicks;
    private final int rewardFireworkRingPoints;
    private final double rewardFireworkRingRadius;
    private final double rewardFireworkStarOuterRadius;
    private final double rewardFireworkStarInnerRadius;
    private final int rewardFireworkParticlesPerPoint;
    private final double zombieArmorChance;
    private final double zombieArmorPieceChance;
    private final double zombieArmorEnchantChance;
    private final int zombieArmorEnchantCostMin;
    private final int zombieArmorEnchantCostMax;
    private final List<ZombieArmorTier> zombieArmorTiers;

    private MobArenaWaveEvent(
            int radius,
            int checkIntervalTicks,
            long nightTime,
            int wallHeight,
            int wallDepth,
            int wallTorchSpacing,
            Block wallBlock,
            int waveStartMessageColor,
            List<WaveConfig> waves,
            int[] waveCountWeights,
            List<RewardLootEntry> rewardLootTable,
            int rewardRollsMin,
            int rewardRollsMax,
            Item rewardItem,
            int rewardCountMin,
            int rewardCountMax,
            double rewardBonusPerExtraWave,
            int bonusDiamondMinExtraWaves,
            int bonusDiamondCountMin,
            int bonusDiamondCountMax,
            int bonusSwordMinExtraWaves,
            int bonusSwordEnchantCostMin,
            int bonusSwordEnchantCostMax,
            int maxDurationTicks,
            String timeoutMessage,
            int timeoutEmeraldDropMin,
            int timeoutEmeraldDropMax,
            String arenaMessage,
                int arenaMessageColor,
                String forfeitMessage,
                int forfeitMessageColor,
                double ownerForfeitDistanceMargin,
                int ownerForfeitGraceTicks,
                boolean supportChestEnabled,
                boolean protectWallsFromExplosions,
                boolean protectSupportChestFromExplosions,
                int supportStacksMin,
                int supportStacksMax,
                List<SupportChestEntry> supportChestEntries,
                List<GuaranteedChestEntry> supportGuaranteedItems,
                int rewardChestForwardOffset,
                String[] rewardSignLines,
                int rewardCelebrationParticleCount,
                double rewardCelebrationParticleSpreadX,
                double rewardCelebrationParticleSpreadY,
                double rewardCelebrationParticleSpreadZ,
                int rewardFireworksCount,
                double rewardFireworksVelocityMin,
                double rewardFireworksVelocityMax,
                int rewardFireworkBurstDelayMinTicks,
                int rewardFireworkBurstDelayMaxTicks,
                int rewardFireworkRingPoints,
                double rewardFireworkRingRadius,
                double rewardFireworkStarOuterRadius,
                double rewardFireworkStarInnerRadius,
                int rewardFireworkParticlesPerPoint,
                double zombieArmorChance,
                double zombieArmorPieceChance,
                double zombieArmorEnchantChance,
                int zombieArmorEnchantCostMin,
                int zombieArmorEnchantCostMax,
                List<ZombieArmorTier> zombieArmorTiers
    ) {
        this.radius = radius;
        this.checkIntervalTicks = checkIntervalTicks;
        this.nightTime = nightTime;
        this.wallHeight = wallHeight;
        this.wallDepth = wallDepth;
        this.wallTorchSpacing = wallTorchSpacing;
        this.wallBlock = wallBlock;
        this.waveStartMessageColor = waveStartMessageColor;
        this.waves = waves;
        this.waveCountWeights = waveCountWeights;
        this.rewardLootTable = rewardLootTable;
        this.rewardRollsMin = rewardRollsMin;
        this.rewardRollsMax = rewardRollsMax;
        this.rewardItem = rewardItem;
        this.rewardCountMin = rewardCountMin;
        this.rewardCountMax = rewardCountMax;
        this.rewardBonusPerExtraWave = rewardBonusPerExtraWave;
        this.bonusDiamondMinExtraWaves = bonusDiamondMinExtraWaves;
        this.bonusDiamondCountMin = bonusDiamondCountMin;
        this.bonusDiamondCountMax = bonusDiamondCountMax;
        this.bonusSwordMinExtraWaves = bonusSwordMinExtraWaves;
        this.bonusSwordEnchantCostMin = bonusSwordEnchantCostMin;
        this.bonusSwordEnchantCostMax = bonusSwordEnchantCostMax;
        this.maxDurationTicks = maxDurationTicks;
        this.timeoutMessage = timeoutMessage;
        this.timeoutEmeraldDropMin = timeoutEmeraldDropMin;
        this.timeoutEmeraldDropMax = timeoutEmeraldDropMax;
        this.arenaMessage = arenaMessage;
        this.arenaMessageColor = arenaMessageColor;
        this.forfeitMessage = forfeitMessage;
        this.forfeitMessageColor = forfeitMessageColor;
        this.ownerForfeitDistanceMargin = ownerForfeitDistanceMargin;
        this.ownerForfeitGraceTicks = ownerForfeitGraceTicks;
        this.supportChestEnabled = supportChestEnabled;
        this.protectWallsFromExplosions = protectWallsFromExplosions;
        this.protectSupportChestFromExplosions = protectSupportChestFromExplosions;
        this.supportStacksMin = supportStacksMin;
        this.supportStacksMax = supportStacksMax;
        this.supportChestEntries = supportChestEntries;
        this.supportGuaranteedItems = supportGuaranteedItems;
        this.rewardChestForwardOffset = rewardChestForwardOffset;
        this.rewardSignLines = rewardSignLines;
        this.rewardCelebrationParticleCount = rewardCelebrationParticleCount;
        this.rewardCelebrationParticleSpreadX = rewardCelebrationParticleSpreadX;
        this.rewardCelebrationParticleSpreadY = rewardCelebrationParticleSpreadY;
        this.rewardCelebrationParticleSpreadZ = rewardCelebrationParticleSpreadZ;
        this.rewardFireworksCount = rewardFireworksCount;
        this.rewardFireworksVelocityMin = rewardFireworksVelocityMin;
        this.rewardFireworksVelocityMax = rewardFireworksVelocityMax;
        this.rewardFireworkBurstDelayMinTicks = rewardFireworkBurstDelayMinTicks;
        this.rewardFireworkBurstDelayMaxTicks = rewardFireworkBurstDelayMaxTicks;
        this.rewardFireworkRingPoints = rewardFireworkRingPoints;
        this.rewardFireworkRingRadius = rewardFireworkRingRadius;
        this.rewardFireworkStarOuterRadius = rewardFireworkStarOuterRadius;
        this.rewardFireworkStarInnerRadius = rewardFireworkStarInnerRadius;
        this.rewardFireworkParticlesPerPoint = rewardFireworkParticlesPerPoint;
        this.zombieArmorChance = zombieArmorChance;
        this.zombieArmorPieceChance = zombieArmorPieceChance;
        this.zombieArmorEnchantChance = zombieArmorEnchantChance;
        this.zombieArmorEnchantCostMin = zombieArmorEnchantCostMin;
        this.zombieArmorEnchantCostMax = zombieArmorEnchantCostMax;
        this.zombieArmorTiers = zombieArmorTiers;
    }

    public static MobArenaWaveEvent fromJson(JsonObject obj) {
        int radius = obj.has("radius") ? obj.get("radius").getAsInt() : 8;
        int checkIntervalTicks = obj.has("check_interval_ticks") ? obj.get("check_interval_ticks").getAsInt() : CHECK_INTERVAL_TICKS;
        long nightTime = obj.has("night_time") ? obj.get("night_time").getAsLong() : ARENA_NIGHT_TIME;
        int wallHeight = obj.has("wall_height") ? obj.get("wall_height").getAsInt() : 3;
        int wallDepth = obj.has("wall_depth") ? obj.get("wall_depth").getAsInt() : DEFAULT_WALL_DEPTH;
        int wallTorchSpacing = obj.has("wall_torch_spacing") ? obj.get("wall_torch_spacing").getAsInt() : 4;
        Block wallBlock = parseBlock(obj, "wall_block", Blocks.COBBLESTONE);

        int waveStartMessageColor = parseRgbColor(obj, "wave_start_message_color", 0xFFAA55);
        List<WaveConfig> waves = parseWaves(obj, waveStartMessageColor);
        int[] waveCountWeights = parseWaveCountWeights(obj);
        List<RewardLootEntry> rewardLootTable = parseRewardLootTable(obj);
        int rewardRollsMin = obj.has("reward_rolls_min") ? obj.get("reward_rolls_min").getAsInt() : 4;
        int rewardRollsMax = obj.has("reward_rolls_max") ? obj.get("reward_rolls_max").getAsInt() : 8;

        Item rewardItem = parseItem(obj, "reward_item", Items.EMERALD);
        int rewardCountMin = obj.has("reward_count_min") ? obj.get("reward_count_min").getAsInt() : 3;
        int rewardCountMax = obj.has("reward_count_max") ? obj.get("reward_count_max").getAsInt() : 6;
        double rewardBonusPerExtraWave = obj.has("reward_bonus_per_extra_wave")
            ? obj.get("reward_bonus_per_extra_wave").getAsDouble()
            : 0.5;
        int bonusDiamondMinExtraWaves = obj.has("bonus_diamond_min_extra_waves")
            ? obj.get("bonus_diamond_min_extra_waves").getAsInt()
            : 1;
        int bonusDiamondCountMin = obj.has("bonus_diamond_count_min")
            ? obj.get("bonus_diamond_count_min").getAsInt()
            : 2;
        int bonusDiamondCountMax = obj.has("bonus_diamond_count_max")
            ? obj.get("bonus_diamond_count_max").getAsInt()
            : 6;
        int bonusSwordMinExtraWaves = obj.has("bonus_sword_min_extra_waves")
            ? obj.get("bonus_sword_min_extra_waves").getAsInt()
            : 2;
        int bonusSwordEnchantCostMin = obj.has("bonus_sword_enchant_cost_min")
            ? obj.get("bonus_sword_enchant_cost_min").getAsInt()
            : 18;
        int bonusSwordEnchantCostMax = obj.has("bonus_sword_enchant_cost_max")
            ? obj.get("bonus_sword_enchant_cost_max").getAsInt()
            : 28;

        int maxDurationTicks = obj.has("wave_timeout_ticks")
            ? obj.get("wave_timeout_ticks").getAsInt()
            : (obj.has("max_duration_ticks") ? obj.get("max_duration_ticks").getAsInt() : 20 * 60);
        String timeoutMessage = obj.has("timeout_message")
            ? obj.get("timeout_message").getAsString()
            : "You took too long! Challenge ended. Here's a reward for trying";
        int timeoutEmeraldDropMin = obj.has("timeout_emerald_drop_min") ? obj.get("timeout_emerald_drop_min").getAsInt() : 1;
        int timeoutEmeraldDropMax = obj.has("timeout_emerald_drop_max") ? obj.get("timeout_emerald_drop_max").getAsInt() : 3;
        String arenaMessage = obj.has("arena_message")
                ? obj.get("arena_message").getAsString()
                : "Arena challenge! Defeat both waves for a reward.";
        int arenaMessageColor = parseRgbColor(obj, "arena_message_color", 0xFFD76A);
        String forfeitMessage = obj.has("forfeit_message")
            ? obj.get("forfeit_message").getAsString()
            : "Arena challenge ended, you forfeit";
        int forfeitMessageColor = parseRgbColor(obj, "forfeit_message_color", 0xFF5555);
        double ownerForfeitDistanceMargin = obj.has("owner_forfeit_distance_margin")
            ? obj.get("owner_forfeit_distance_margin").getAsDouble()
            : 1.5;
        int ownerForfeitGraceTicks = obj.has("owner_forfeit_grace_ticks")
            ? obj.get("owner_forfeit_grace_ticks").getAsInt()
            : 60;

        SupportChestConfig supportChest = parseSupportChest(obj);
        boolean protectWallsFromExplosions = obj.has("arena_protect_walls_from_explosions")
            ? obj.get("arena_protect_walls_from_explosions").getAsBoolean()
            : true;
        boolean protectSupportChestFromExplosions = obj.has("arena_protect_support_chest_from_explosions")
            ? obj.get("arena_protect_support_chest_from_explosions").getAsBoolean()
            : true;
        List<GuaranteedChestEntry> supportGuaranteedItems = parseSupportGuaranteedItems(obj);
        int rewardChestForwardOffset = obj.has("reward_chest_forward_offset") ? obj.get("reward_chest_forward_offset").getAsInt() : 1;
        String[] rewardSignLines = parseSignLines(obj, "reward_sign_lines", DEFAULT_REWARD_SIGN_LINES);
        if (obj.has("reward_sign_text") && !obj.get("reward_sign_text").getAsString().isBlank()) {
            rewardSignLines = new String[] {"", obj.get("reward_sign_text").getAsString(), "", ""};
        }
        int rewardCelebrationParticleCount = obj.has("reward_particle_count") ? obj.get("reward_particle_count").getAsInt() : 36;
        double rewardCelebrationParticleSpreadX = obj.has("reward_particle_spread_x") ? obj.get("reward_particle_spread_x").getAsDouble() : 0.8;
        double rewardCelebrationParticleSpreadY = obj.has("reward_particle_spread_y") ? obj.get("reward_particle_spread_y").getAsDouble() : 0.5;
        double rewardCelebrationParticleSpreadZ = obj.has("reward_particle_spread_z") ? obj.get("reward_particle_spread_z").getAsDouble() : 0.8;
        int rewardFireworksCount = obj.has("reward_fireworks_count") ? obj.get("reward_fireworks_count").getAsInt() : 3;
        double rewardFireworksVelocityMin = obj.has("reward_fireworks_velocity_min") ? obj.get("reward_fireworks_velocity_min").getAsDouble() : 0.8;
        double rewardFireworksVelocityMax = obj.has("reward_fireworks_velocity_max") ? obj.get("reward_fireworks_velocity_max").getAsDouble() : 1.05;
        int rewardFireworkBurstDelayMinTicks = obj.has("reward_firework_burst_delay_min_ticks") ? obj.get("reward_firework_burst_delay_min_ticks").getAsInt() : 16;
        int rewardFireworkBurstDelayMaxTicks = obj.has("reward_firework_burst_delay_max_ticks") ? obj.get("reward_firework_burst_delay_max_ticks").getAsInt() : 23;
        int rewardFireworkRingPoints = obj.has("reward_firework_ring_points") ? obj.get("reward_firework_ring_points").getAsInt() : 18;
        double rewardFireworkRingRadius = obj.has("reward_firework_ring_radius") ? obj.get("reward_firework_ring_radius").getAsDouble() : 0.9;
        double rewardFireworkStarOuterRadius = obj.has("reward_firework_star_outer_radius") ? obj.get("reward_firework_star_outer_radius").getAsDouble() : 1.0;
        double rewardFireworkStarInnerRadius = obj.has("reward_firework_star_inner_radius") ? obj.get("reward_firework_star_inner_radius").getAsDouble() : 0.45;
        int rewardFireworkParticlesPerPoint = obj.has("reward_firework_particles_per_point") ? obj.get("reward_firework_particles_per_point").getAsInt() : 2;
        ZombieArmorConfig zombieArmor = parseZombieArmor(obj);

        if (radius < 4) radius = 4;
        if (checkIntervalTicks < 1) checkIntervalTicks = 1;
        if (wallHeight < 2) wallHeight = 2;
        if (wallDepth < 0) wallDepth = 0;
        if (wallTorchSpacing < 1) wallTorchSpacing = 1;
        if (waves.isEmpty()) {
            waves.add(new WaveConfig("minecraft:zombie", 3, 5, "Wave 1 started!", waveStartMessageColor));
            waves.add(new WaveConfig("minecraft:skeleton", 4, 6, "Wave 2 started!", waveStartMessageColor));
        }
        if (rewardCountMin < 1) rewardCountMin = 1;
        if (rewardCountMax < rewardCountMin) rewardCountMax = rewardCountMin;
        if (rewardBonusPerExtraWave < 0.0) rewardBonusPerExtraWave = 0.0;
        if (bonusDiamondMinExtraWaves < 0) bonusDiamondMinExtraWaves = 0;
        if (bonusDiamondCountMin < 1) bonusDiamondCountMin = 1;
        if (bonusDiamondCountMax < bonusDiamondCountMin) bonusDiamondCountMax = bonusDiamondCountMin;
        if (bonusSwordMinExtraWaves < 0) bonusSwordMinExtraWaves = 0;
        if (bonusSwordEnchantCostMin < 1) bonusSwordEnchantCostMin = 1;
        if (bonusSwordEnchantCostMax < bonusSwordEnchantCostMin) bonusSwordEnchantCostMax = bonusSwordEnchantCostMin;
        if (rewardChestForwardOffset < 1) rewardChestForwardOffset = 1;
        if (rewardCelebrationParticleCount < 0) rewardCelebrationParticleCount = 0;
        if (rewardFireworksCount < 0) rewardFireworksCount = 0;
        if (rewardFireworksVelocityMin < 0.0) rewardFireworksVelocityMin = 0.0;
        if (rewardFireworksVelocityMax < rewardFireworksVelocityMin) rewardFireworksVelocityMax = rewardFireworksVelocityMin;
        if (rewardFireworkBurstDelayMinTicks < 1) rewardFireworkBurstDelayMinTicks = 1;
        if (rewardFireworkBurstDelayMaxTicks < rewardFireworkBurstDelayMinTicks) rewardFireworkBurstDelayMaxTicks = rewardFireworkBurstDelayMinTicks;
        if (rewardFireworkRingPoints < 4) rewardFireworkRingPoints = 4;
        if (rewardFireworkRingRadius < 0.1) rewardFireworkRingRadius = 0.1;
        if (rewardFireworkStarOuterRadius < 0.1) rewardFireworkStarOuterRadius = 0.1;
        if (rewardFireworkStarInnerRadius < 0.05) rewardFireworkStarInnerRadius = 0.05;
        if (rewardFireworkParticlesPerPoint < 1) rewardFireworkParticlesPerPoint = 1;
        if (rewardRollsMin < 1) rewardRollsMin = 1;
        if (rewardRollsMax < rewardRollsMin) rewardRollsMax = rewardRollsMin;
        if (maxDurationTicks < 100) maxDurationTicks = 100;
        if (timeoutEmeraldDropMin < 0) timeoutEmeraldDropMin = 0;
        if (timeoutEmeraldDropMax < timeoutEmeraldDropMin) timeoutEmeraldDropMax = timeoutEmeraldDropMin;
        if (ownerForfeitDistanceMargin < 0.0) ownerForfeitDistanceMargin = 0.0;
        if (ownerForfeitGraceTicks < 0) ownerForfeitGraceTicks = 0;

        return new MobArenaWaveEvent(
                radius,
            checkIntervalTicks,
            nightTime,
                wallHeight,
                wallDepth,
            wallTorchSpacing,
                wallBlock,
            waveStartMessageColor,
                waves,
                waveCountWeights,
            rewardLootTable,
            rewardRollsMin,
            rewardRollsMax,
                rewardItem,
                rewardCountMin,
                rewardCountMax,
                rewardBonusPerExtraWave,
                bonusDiamondMinExtraWaves,
                bonusDiamondCountMin,
                bonusDiamondCountMax,
                bonusSwordMinExtraWaves,
                bonusSwordEnchantCostMin,
                bonusSwordEnchantCostMax,
                maxDurationTicks,
                timeoutMessage,
                timeoutEmeraldDropMin,
                timeoutEmeraldDropMax,
                arenaMessage,
                arenaMessageColor,
                forfeitMessage,
                forfeitMessageColor,
                ownerForfeitDistanceMargin,
                ownerForfeitGraceTicks,
                supportChest.enabled,
                protectWallsFromExplosions,
                protectSupportChestFromExplosions,
                supportChest.stacksMin,
                supportChest.stacksMax,
                supportChest.entries,
                supportGuaranteedItems,
                rewardChestForwardOffset,
                rewardSignLines,
                rewardCelebrationParticleCount,
                rewardCelebrationParticleSpreadX,
                rewardCelebrationParticleSpreadY,
                rewardCelebrationParticleSpreadZ,
                rewardFireworksCount,
                rewardFireworksVelocityMin,
                rewardFireworksVelocityMax,
                rewardFireworkBurstDelayMinTicks,
                rewardFireworkBurstDelayMaxTicks,
                rewardFireworkRingPoints,
                rewardFireworkRingRadius,
                rewardFireworkStarOuterRadius,
                rewardFireworkStarInnerRadius,
                rewardFireworkParticlesPerPoint,
                zombieArmor.chance,
                zombieArmor.pieceChance,
                zombieArmor.enchantChance,
                zombieArmor.enchantCostMin,
                zombieArmor.enchantCostMax,
                zombieArmor.tiers
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        executeInternal(level, pos, player, null);
    }

    public void executeWithForcedWaveCount(ServerLevel level, BlockPos pos, ServerPlayer player, int forcedWaveCount) {
        executeInternal(level, pos, player, forcedWaveCount);
    }

    private void executeInternal(ServerLevel level, BlockPos pos, ServerPlayer player, Integer forcedWaveCount) {
        if (level.getServer().getWorldData().getDifficulty() == Difficulty.PEACEFUL) {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
        }

        level.setDayTime(nightTime);

        BlockPos center = player.blockPosition();
        List<SavedBlock> savedBlocks = placeArenaWall(level, center);
        if (supportChestEnabled) {
            placeSupportChest(level, center, savedBlocks);
        }

        player.displayClientMessage(
                Component.literal(arenaMessage).withStyle(s -> s.withColor(TextColor.fromRgb(arenaMessageColor))),
                false
        );

        ArenaRun run = new ArenaRun(level, player.getUUID(), center, savedBlocks);
        run.waveExpiresAtGameTime = level.getGameTime() + maxDurationTicks;
        int maxWaves = Math.min(5, waves.size());
        run.totalWaves = forcedWaveCount != null
            ? Math.max(1, Math.min(forcedWaveCount, maxWaves))
            : chooseWaveCount(level.getRandom(), maxWaves);
        run.activeWaveNumber = 1;
        WaveConfig firstWave = waves.get(0);
        run.activeMobs = spawnWave(level, center, firstWave);
        sendToOwner(run, resolveWaveStartMessage(firstWave, run.activeWaveNumber), firstWave.messageColor);
        scheduleCheck(run);
    }

    private void scheduleCheck(ArenaRun run) {
        LuckyScheduler.INSTANCE.schedule(checkIntervalTicks, () -> tickRun(run));
    }

    private void tickRun(ArenaRun run) {
        if (run.finished) {
            return;
        }

        maintainArenaIntegrity(run);
        containActiveMobs(run);
        clearIntrudingMobs(run);

        if (ownerLeftArena(run)) {
            cleanupArena(run);
            sendToOwner(run, forfeitMessage, forfeitMessageColor);
            run.finished = true;
            return;
        }

        if (run.level.getGameTime() >= run.waveExpiresAtGameTime) {
            cleanupArena(run);
            dropTimeoutConsolationReward(run);
            sendToOwner(run, timeoutMessage, 0xFFAA55);
            run.finished = true;
            return;
        }

        if (!allDead(run.level, run.activeMobs)) {
            scheduleCheck(run);
            return;
        }

        if (run.activeWaveNumber < run.totalWaves) {
            run.activeWaveNumber++;
            WaveConfig nextWave = waves.get(Math.min(run.activeWaveNumber - 1, waves.size() - 1));
            run.activeMobs = spawnWave(run.level, run.center, nextWave);
            run.waveExpiresAtGameTime = run.level.getGameTime() + maxDurationTicks;
            sendToOwner(run, resolveWaveStartMessage(nextWave, run.activeWaveNumber), nextWave.messageColor);
            scheduleCheck(run);
            return;
        }

        // Completed all waves
        rewardPlayer(run);
        cleanupArena(run);
        sendToOwner(run, "Arena cleared! Reward granted.", 0x55FF55);
        run.finished = true;
    }

    private void containActiveMobs(ArenaRun run) {
        double maxDistance = Math.max(2.0, radius - 0.75);
        double maxDistanceSq = maxDistance * maxDistance;

        for (UUID uuid : run.activeMobs) {
            Entity entity = run.level.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }

            double dx = living.getX() - (run.center.getX() + 0.5);
            double dz = living.getZ() - (run.center.getZ() + 0.5);
            if ((dx * dx + dz * dz) <= maxDistanceSq) {
                continue;
            }

            moveEntityBackInside(run, living);
        }
    }

    private void moveEntityBackInside(ArenaRun run, LivingEntity entity) {
        RandomSource random = run.level.getRandom();
        double minRadius = Math.max(0.8, radius * 0.2);
        double maxRadius = Math.max(minRadius + 0.5, radius - 2.5);

        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = random.nextDouble() * (Math.PI * 2.0);
            double distance = minRadius + random.nextDouble() * Math.max(0.0, maxRadius - minRadius);
            double x = run.center.getX() + 0.5 + Math.cos(angle) * distance;
            double z = run.center.getZ() + 0.5 + Math.sin(angle) * distance;
            int y = run.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            double spawnY = Math.max(run.level.getMinY() + 1, y);

            entity.setPos(x, spawnY, z);
            if (run.level.noCollision(entity)) {
                entity.setDeltaMovement(0.0, 0.1, 0.0);
                return;
            }
        }

        int y = run.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, run.center.getX(), run.center.getZ());
        entity.setPos(run.center.getX() + 0.5, Math.max(run.level.getMinY() + 1, y), run.center.getZ() + 0.5);
        entity.setDeltaMovement(0.0, 0.1, 0.0);
    }

    private void clearIntrudingMobs(ArenaRun run) {
        double minX = run.center.getX() + 0.5 - radius;
        double minY = run.level.getMinY();
        double minZ = run.center.getZ() + 0.5 - radius;
        double maxX = run.center.getX() + 0.5 + radius;
        double maxY = run.level.getMaxY();
        double maxZ = run.center.getZ() + 0.5 + radius;
        AABB arenaBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        run.level.getEntitiesOfClass(Mob.class, arenaBox, mob -> mob.isAlive()).forEach(mob -> {
            if (mob.getUUID().equals(run.ownerId)) {
                return;
            }
            if (run.activeMobs.contains(mob.getUUID())) {
                return;
            }
            mob.discard();
        });
    }

    private boolean ownerLeftArena(ArenaRun run) {
        ServerPlayer owner = run.level.getServer().getPlayerList().getPlayer(run.ownerId);
        if (owner == null) {
            return true;
        }

        double centerX = run.center.getX() + 0.5;
        double centerZ = run.center.getZ() + 0.5;
        double dx = owner.getX() - centerX;
        double dz = owner.getZ() - centerZ;
        double maxDistance = Math.max(1.5, radius - 0.5 + ownerForfeitDistanceMargin);
        boolean outside = (dx * dx + dz * dz) > (maxDistance * maxDistance);

        if (!outside) {
            run.ownerOutsideSinceTick = -1L;
            return false;
        }

        long now = run.level.getGameTime();
        if (run.ownerOutsideSinceTick < 0L) {
            run.ownerOutsideSinceTick = now;
            return false;
        }

        return (now - run.ownerOutsideSinceTick) >= ownerForfeitGraceTicks;
    }

    private void maintainArenaIntegrity(ArenaRun run) {
        for (SavedBlock saved : run.savedBlocks) {
            if (saved.supportBlock && !protectSupportChestFromExplosions) {
                continue;
            }
            if (!saved.supportBlock && !protectWallsFromExplosions) {
                continue;
            }

            BlockState current = run.level.getBlockState(saved.pos);
            if (!current.equals(saved.arenaState)) {
                run.level.setBlock(saved.pos, saved.arenaState, 3);
            }
        }
    }

    private int chooseWaveCount(RandomSource random, int maxWaves) {
        if (maxWaves < 1) {
            return 1;
        }

        int limit = Math.min(5, maxWaves);
        int total = 0;
        for (int i = 1; i <= limit; i++) {
            total += Math.max(0, waveCountWeights[i - 1]);
        }
        if (total <= 0) {
            return Math.min(2, limit);
        }

        int pick = random.nextInt(total);
        int running = 0;
        for (int i = 1; i <= limit; i++) {
            running += Math.max(0, waveCountWeights[i - 1]);
            if (pick < running) {
                return i;
            }
        }

        return limit;
    }

    private String resolveWaveStartMessage(WaveConfig wave, int waveNumber) {
        if (wave.message != null && !wave.message.isBlank()) {
            return wave.message;
        }
        return "Wave " + waveNumber + " started!";
    }

    private void rewardPlayer(ArenaRun run) {
        spawnRewardChest(run);
    }

    private void dropTimeoutConsolationReward(ArenaRun run) {
        ServerPlayer owner = run.level.getServer().getPlayerList().getPlayer(run.ownerId);
        Direction facing = owner != null ? owner.getDirection() : Direction.NORTH;
        BlockPos base = owner != null ? owner.blockPosition() : run.center;

        BlockPos dropPos = base.relative(facing, 1);
        int dropY = Math.max(run.level.getMinY() + 1,
            run.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dropPos.getX(), dropPos.getZ()));

        int emeraldCount = timeoutEmeraldDropMin
            + run.level.getRandom().nextInt(Math.max(1, timeoutEmeraldDropMax - timeoutEmeraldDropMin + 1));
        ItemEntity reward = new ItemEntity(
            run.level,
            dropPos.getX() + 0.5,
            dropY + 0.9,
            dropPos.getZ() + 0.5,
            new ItemStack(Items.EMERALD, emeraldCount)
        );
        run.level.addFreshEntity(reward);
    }

    private void spawnRewardChest(ArenaRun run) {
        ServerPlayer owner = run.level.getServer().getPlayerList().getPlayer(run.ownerId);
        Direction facing = owner != null ? owner.getDirection() : Direction.NORTH;
        BlockPos base = owner != null ? owner.blockPosition() : run.center;

        BlockPos chestPos = base.relative(facing, rewardChestForwardOffset);
        int chestY = Math.max(run.level.getMinY() + 1,
                run.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, chestPos.getX(), chestPos.getZ()));
        chestPos = new BlockPos(chestPos.getX(), chestY, chestPos.getZ());

        BlockState chestState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing.getOpposite());
        run.level.setBlock(chestPos, chestState, 3);

        var be = run.level.getBlockEntity(chestPos);
        if (be instanceof Container container) {
            RandomSource random = run.level.getRandom();
            List<ItemStack> rewards = buildRewardStacks(run, random);
            for (ItemStack stack : rewards) {
                placeItemInRandomEmptySlot(container, random, stack);
            }
        }

        spawnRewardSign(run.level, chestPos, facing);
        spawnRewardCelebration(run.level, chestPos);
    }

    private List<ItemStack> buildRewardStacks(ArenaRun run, RandomSource random) {
        List<ItemStack> rewards = new ArrayList<>();

        if (!rewardLootTable.isEmpty()) {
            List<RewardLootEntry> pool = new ArrayList<>(rewardLootTable);
            int rolls = rewardRollsMin + random.nextInt(Math.max(1, rewardRollsMax - rewardRollsMin + 1));
            rolls = Math.min(rolls, pool.size());
            int waveFactor = Math.max(0, run.activeWaveNumber - 1);

            for (int i = 0; i < rolls; i++) {
                RewardLootEntry selected = chooseRewardLootEntry(random, pool, waveFactor);
                if (selected == null) {
                    break;
                }
                pool.remove(selected);
                removeMutuallyExclusiveRewards(pool, selected);

                int amount = selected.countMin + random.nextInt(Math.max(1, selected.countMax - selected.countMin + 1));
                ItemStack stack = new ItemStack(selected.item, amount);
                if (selected.enchanted) {
                    int cost = selected.enchantCostMin + random.nextInt(Math.max(1, selected.enchantCostMax - selected.enchantCostMin + 1));
                    try {
                        stack = EnchantmentHelper.enchantItem(random, stack, cost, run.level.registryAccess(), Optional.empty());
                    } catch (Exception ignored) {
                    }
                }
                rewards.add(stack);
            }

            if (!rewards.isEmpty()) {
                return rewards;
            }
        }

        if (run.activeWaveNumber >= 4) {
            rewards.add(new ItemStack(Items.EMERALD, 8));
            rewards.add(new ItemStack(Items.DIAMOND, 5));
            rewards.add(new ItemStack(resolveItem(VERY_LUCKY_BLOCK_ITEM_ID, Items.EMERALD), 1));
            rewards.add(new ItemStack(Items.NETHERITE_SWORD, 1));
            return rewards;
        }

        if (run.activeWaveNumber == 3) {
            rewards.add(new ItemStack(Items.EMERALD, 6));
            rewards.add(new ItemStack(Items.DIAMOND, 3));
            rewards.add(new ItemStack(resolveItem(LUCKY_BLOCK_ITEM_ID, Items.EMERALD), 1));
            rewards.add(new ItemStack(Items.DIAMOND_SWORD, 1));
            return rewards;
        }

        int emeralds = 3 + random.nextInt(3); // 3-5
        int diamonds = 2 + random.nextInt(4); // 2-5
        int gold = 3 + random.nextInt(6); // 3-8
        rewards.add(new ItemStack(Items.EMERALD, emeralds));
        rewards.add(new ItemStack(Items.DIAMOND, diamonds));
        rewards.add(new ItemStack(Items.GOLD_INGOT, gold));
        return rewards;
    }

    private RewardLootEntry chooseRewardLootEntry(RandomSource random, List<RewardLootEntry> pool, int waveFactor) {
        int total = 0;
        for (RewardLootEntry entry : pool) {
            int weight = entry.baseWeight + (entry.waveWeightBonus * waveFactor);
            if (weight > 0) {
                total += weight;
            }
        }
        if (total <= 0) {
            return null;
        }

        int pick = random.nextInt(total);
        int running = 0;
        for (RewardLootEntry entry : pool) {
            int weight = entry.baseWeight + (entry.waveWeightBonus * waveFactor);
            if (weight <= 0) {
                continue;
            }
            running += weight;
            if (pick < running) {
                return entry;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private void removeMutuallyExclusiveRewards(List<RewardLootEntry> pool, RewardLootEntry selected) {
        Identifier selectedId = BuiltInRegistries.ITEM.getKey(selected.item);
        if (selectedId == null) {
            return;
        }

        String selectedKey = selectedId.toString();
        if (LUCKY_BLOCK_ITEM_ID.equals(selectedKey)) {
            pool.removeIf(entry -> {
                Identifier id = BuiltInRegistries.ITEM.getKey(entry.item);
                return id != null && VERY_LUCKY_BLOCK_ITEM_ID.equals(id.toString());
            });
            return;
        }

        if (VERY_LUCKY_BLOCK_ITEM_ID.equals(selectedKey)) {
            pool.removeIf(entry -> {
                Identifier id = BuiltInRegistries.ITEM.getKey(entry.item);
                return id != null && LUCKY_BLOCK_ITEM_ID.equals(id.toString());
            });
        }
    }

    private void spawnRewardSign(ServerLevel level, BlockPos chestPos, Direction playerFacing) {
        Direction signFacing = playerFacing.getOpposite();
        BlockPos[] candidates = new BlockPos[] {
                chestPos.relative(signFacing),
                chestPos.relative(signFacing.getClockWise()),
                chestPos.relative(signFacing.getCounterClockWise()),
                chestPos.above()
        };

        BlockPos signPos = null;
        for (BlockPos candidate : candidates) {
            if (level.isEmptyBlock(candidate)) {
                signPos = candidate;
                break;
            }
        }
        if (signPos == null) {
            return;
        }

        int rotation = RotationSegment.convertToSegment(signFacing);
        level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation), 3);

        BlockPos finalSignPos = signPos;
        LuckyScheduler.INSTANCE.schedule(1, () -> applySignText(level, finalSignPos, rewardSignLines));
    }

    private void applySignText(ServerLevel level, BlockPos signPos, String[] lines) {
        if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity sign)) {
            return;
        }

        SignText text = new SignText();
        for (int i = 0; i < 4; i++) {
            String line = i < lines.length ? lines[i] : "";
            text = text.setMessage(i, Component.literal(line));
        }
        sign.setText(text, true);
        sign.setChanged();
        BlockState state = level.getBlockState(signPos);
        level.sendBlockUpdated(signPos, state, state, 3);
    }

    private void spawnRewardCelebration(ServerLevel level, BlockPos chestPos) {
        double x = chestPos.getX() + 0.5;
        double y = chestPos.getY() + 1.0;
        double z = chestPos.getZ() + 0.5;

        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                x,
                y,
                z,
                rewardCelebrationParticleCount,
                rewardCelebrationParticleSpreadX,
                rewardCelebrationParticleSpreadY,
                rewardCelebrationParticleSpreadZ,
                0.01
        );

        RandomSource random = level.getRandom();
        for (int i = 0; i < rewardFireworksCount; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET, 1);
            FireworkRocketEntity entity = new FireworkRocketEntity(level, x, y, z, rocket);
            double yVel = rewardFireworksVelocityMin + random.nextDouble() * Math.max(0.0, rewardFireworksVelocityMax - rewardFireworksVelocityMin);
            double xVel = (random.nextDouble() - 0.5) * 0.15;
            double zVel = (random.nextDouble() - 0.5) * 0.15;
            entity.setDeltaMovement(xVel, yVel, zVel);
            level.addFreshEntity(entity);

                int burstDelay = rewardFireworkBurstDelayMinTicks
                    + random.nextInt(Math.max(1, rewardFireworkBurstDelayMaxTicks - rewardFireworkBurstDelayMinTicks + 1));
            double burstX = x + (xVel * burstDelay);
            double burstY = y + (yVel * burstDelay);
            double burstZ = z + (zVel * burstDelay);
            LuckyScheduler.INSTANCE.schedule(burstDelay, () -> spawnFireworkBurst(level, burstX, burstY, burstZ));
        }
    }

    private void spawnFireworkBurst(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

        // Ring burst.
        int ringPoints = rewardFireworkRingPoints;
        double ringRadius = rewardFireworkRingRadius;
        for (int i = 0; i < ringPoints; i++) {
            double angle = (Math.PI * 2.0 * i) / ringPoints;
            double ox = Math.cos(angle) * ringRadius;
            double oz = Math.sin(angle) * ringRadius;
            level.sendParticles(ParticleTypes.FIREWORK, x + ox, y, z + oz, rewardFireworkParticlesPerPoint, 0.02, 0.02, 0.02, 0.02);
        }

        // Star-like spokes.
        for (int i = 0; i < 10; i++) {
            double angle = (Math.PI * 2.0 * i) / 5.0;
            double radius = (i % 2 == 0) ? rewardFireworkStarOuterRadius : rewardFireworkStarInnerRadius;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.FIREWORK, x + ox, y + 0.1, z + oz, rewardFireworkParticlesPerPoint, 0.02, 0.02, 0.02, 0.02);
        }
    }

    private Item resolveItem(String itemId, Item fallback) {
        try {
            Identifier id = Identifier.parse(itemId);
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                return fallback;
            }
            Item item = BuiltInRegistries.ITEM.getValue(id);
            return item != null && item != Items.AIR ? item : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void placeSupportChest(ServerLevel level, BlockPos center, List<SavedBlock> savedBlocks) {
        RandomSource random = level.getRandom();
        int chestY = Math.max(level.getMinY() + 1, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ()));
        BlockPos chestPos = new BlockPos(center.getX(), chestY, center.getZ());

        saveAndSet(level, savedBlocks, chestPos, Blocks.CHEST.defaultBlockState(), true);
        placeTorchIfPossible(level, chestPos.north(), savedBlocks, true);
        placeTorchIfPossible(level, chestPos.south(), savedBlocks, true);
        placeTorchIfPossible(level, chestPos.east(), savedBlocks, true);
        placeTorchIfPossible(level, chestPos.west(), savedBlocks, true);

        if (supportChestEntries.isEmpty()) {
            ItemStack fallback = new ItemStack(Items.SHIELD);
            ItemEntity itemEntity = new ItemEntity(level, chestPos.getX() + 0.5, chestPos.getY() + 1.1, chestPos.getZ() + 0.5, fallback);
            level.addFreshEntity(itemEntity);
            return;
        }

        var be = level.getBlockEntity(chestPos);
        if (be instanceof Container container) {
            int stacksToPlace = supportStacksMin + random.nextInt(Math.max(1, supportStacksMax - supportStacksMin + 1));
            int targetStacks = Math.max(supportGuaranteedItems.size(), stacksToPlace);
            int placed = 0;

            for (GuaranteedChestEntry guaranteed : supportGuaranteedItems) {
                int amount = guaranteed.countMin + random.nextInt(Math.max(1, guaranteed.countMax - guaranteed.countMin + 1));
                if (placeItemInRandomEmptySlot(container, random, new ItemStack(guaranteed.item, amount))) {
                    placed++;
                }
            }

            for (int i = 0; i < stacksToPlace; i++) {
                if (placed >= targetStacks) {
                    break;
                }
                SupportChestEntry entry = chooseSupportEntry(random, supportChestEntries);
                if (entry == null) {
                    continue;
                }
                int amount = entry.countMin + random.nextInt(Math.max(1, entry.countMax - entry.countMin + 1));
                if (amount < 1) {
                    amount = 1;
                }

                if (placeItemInRandomEmptySlot(container, random, new ItemStack(entry.item, amount))) {
                    placed++;
                }
            }

            if (placed == 0) {
                placeItemInRandomEmptySlot(container, random, new ItemStack(Items.SHIELD));
            }
        }
    }

    private boolean placeItemInRandomEmptySlot(Container container, RandomSource random, ItemStack stack) {
        int size = container.getContainerSize();
        int slot = random.nextInt(size);
        int tries = 0;
        while (!container.getItem(slot).isEmpty() && tries < size) {
            slot = (slot + 1) % size;
            tries++;
        }
        if (!container.getItem(slot).isEmpty()) {
            return false;
        }
        container.setItem(slot, stack);
        return true;
    }

    private SupportChestEntry chooseSupportEntry(RandomSource random, List<SupportChestEntry> entries) {
        int totalWeight = 0;
        for (SupportChestEntry entry : entries) {
            totalWeight += Math.max(1, entry.weight);
        }

        if (totalWeight <= 0) {
            return null;
        }

        int pick = random.nextInt(totalWeight);
        int running = 0;
        for (SupportChestEntry entry : entries) {
            running += Math.max(1, entry.weight);
            if (pick < running) {
                return entry;
            }
        }

        return entries.get(entries.size() - 1);
    }

    private void placeTorchIfPossible(ServerLevel level, BlockPos pos, List<SavedBlock> savedBlocks, boolean supportBlock) {
        if (!level.isEmptyBlock(pos)) {
            return;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender()) {
            return;
        }
        saveAndSet(level, savedBlocks, pos, Blocks.TORCH.defaultBlockState(), supportBlock);
    }

    private void sendToOwner(ArenaRun run, String message, int color) {
        ServerPlayer owner = run.level.getServer().getPlayerList().getPlayer(run.ownerId);
        if (owner != null) {
            owner.displayClientMessage(Component.literal(message).withStyle(s -> s.withColor(TextColor.fromRgb(color))), false);
        }
    }

    private List<UUID> spawnWave(ServerLevel level, BlockPos center, WaveConfig wave) {
        List<UUID> spawned = new ArrayList<>();
        Identifier id;
        try {
            id = Identifier.parse(wave.entityId);
        } catch (Exception ignored) {
            return spawned;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return spawned;
        }

        RandomSource random = level.getRandom();
        int count = wave.countMin + random.nextInt(Math.max(1, wave.countMax - wave.countMin + 1));
        double maxSpawnRadius = Math.max(1.5, radius - 2.5);
        double minSpawnRadius = Math.min(2.5, Math.max(0.8, maxSpawnRadius * 0.35));
        double interiorLimitSq = Math.max(1.0, (radius - 1.25) * (radius - 1.25));

        for (int i = 0; i < count; i++) {
            Entity entity = type.create(level, EntitySpawnReason.TRIGGERED);
            if (entity == null) {
                continue;
            }

            boolean placed = false;
            for (int attempt = 0; attempt < 14; attempt++) {
                double angle = random.nextDouble() * (Math.PI * 2.0);
                double distance = minSpawnRadius + random.nextDouble() * Math.max(0.0, maxSpawnRadius - minSpawnRadius);
                double x = center.getX() + 0.5 + Math.cos(angle) * distance;
                double z = center.getZ() + 0.5 + Math.sin(angle) * distance;

                double dx = x - (center.getX() + 0.5);
                double dz = z - (center.getZ() + 0.5);
                if ((dx * dx + dz * dz) > interiorLimitSq) {
                    continue;
                }

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                double spawnY = Math.max(level.getMinY() + 1, y);

                entity.setPos(x, spawnY, z);
                if (level.noCollision(entity)) {
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                continue;
            }

            if ("minecraft:skeleton".equals(wave.entityId) && entity instanceof LivingEntity living) {
                living.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            }
            if ("minecraft:zombie".equals(wave.entityId) && entity instanceof LivingEntity living) {
                tryEquipZombieArmor(level, random, living);
            }

            level.addFreshEntity(entity);
            spawned.add(entity.getUUID());
        }

        return spawned;
    }

    private boolean allDead(ServerLevel level, List<UUID> entities) {
        for (UUID uuid : entities) {
            Entity entity = level.getEntity(uuid);
            if (entity != null && entity.isAlive()) {
                return false;
            }
        }
        return true;
    }

    private List<SavedBlock> placeArenaWall(ServerLevel level, BlockPos center) {
        Map<BlockPos, SavedBlock> saved = new HashMap<>();
        List<BlockPos> topColumns = new ArrayList<>();

        double minRing = radius - 0.75;
        double maxRing = radius + 0.75;
        double minSq = minRing * minRing;
        double maxSq = maxRing * maxRing;

        int centerGroundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
        int baseY = Math.max(level.getMinY() + 1, centerGroundY);
        int bottomY = Math.max(level.getMinY() + 1, baseY - wallDepth);
        int topY = baseY + wallHeight - 1;

        for (int x = center.getX() - radius - 1; x <= center.getX() + radius + 1; x++) {
            for (int z = center.getZ() - radius - 1; z <= center.getZ() + radius + 1; z++) {
                double dx = (x + 0.5) - (center.getX() + 0.5);
                double dz = (z + 0.5) - (center.getZ() + 0.5);
                double distSq = dx * dx + dz * dz;
                if (distSq < minSq || distSq > maxSq) {
                    continue;
                }

                for (int y = bottomY; y <= topY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    saveAndSet(level, saved, p, wallBlock.defaultBlockState(), false);
                }
                topColumns.add(new BlockPos(x, topY, z));
            }
        }

        int torchSpacing = wallTorchSpacing;
        int index = 0;
        for (BlockPos topPos : topColumns) {
            index++;
            if (index % torchSpacing != 0) {
                continue;
            }

            placeInteriorWallTorch(level, saved, topPos, center);
        }

        List<SavedBlock> result = new ArrayList<>(saved.size());
        for (Map.Entry<BlockPos, SavedBlock> entry : saved.entrySet()) {
            result.add(entry.getValue());
        }
        return result;
    }

    private void placeInteriorWallTorch(ServerLevel level, Map<BlockPos, SavedBlock> saved, BlockPos wallPos, BlockPos center) {
        Direction inward = inwardDirection(wallPos, center);
        BlockPos torchPos = wallPos.relative(inward);
        if (!level.isEmptyBlock(torchPos)) {
            return;
        }

        BlockState inwardFacing = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, inward);
        if (inwardFacing.canSurvive(level, torchPos)) {
            saveAndSet(level, saved, torchPos, inwardFacing, false);
            return;
        }

        BlockState oppositeFacing = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, inward.getOpposite());
        if (oppositeFacing.canSurvive(level, torchPos)) {
            saveAndSet(level, saved, torchPos, oppositeFacing, false);
        }
    }

    private Direction inwardDirection(BlockPos wallPos, BlockPos center) {
        int dx = center.getX() - wallPos.getX();
        int dz = center.getZ() - wallPos.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static void saveAndSet(ServerLevel level, Map<BlockPos, SavedBlock> saved, BlockPos pos, BlockState newState, boolean supportBlock) {
        BlockPos key = pos.immutable();
        saved.putIfAbsent(key, new SavedBlock(key, level.getBlockState(key), newState, supportBlock));
        SavedBlock existing = saved.get(key);
        saved.put(key, new SavedBlock(key, existing.previousState, newState, existing.supportBlock || supportBlock));
        level.setBlock(key, newState, 3);
    }

    private static void saveAndSet(ServerLevel level, List<SavedBlock> saved, BlockPos pos, BlockState newState, boolean supportBlock) {
        BlockPos key = pos.immutable();
        for (SavedBlock existing : saved) {
            if (existing.pos.equals(key)) {
                int index = saved.indexOf(existing);
                saved.set(index, new SavedBlock(key, existing.previousState, newState, existing.supportBlock || supportBlock));
                level.setBlock(key, newState, 3);
                return;
            }
        }
        saved.add(new SavedBlock(key, level.getBlockState(key), newState, supportBlock));
        level.setBlock(key, newState, 3);
    }

    private void cleanupArena(ArenaRun run) {
        // Restore torches first so they are explicitly removed before support blocks revert.
        for (SavedBlock saved : run.savedBlocks) {
            if (saved.arenaState.is(Blocks.TORCH)) {
                run.level.setBlock(saved.pos, saved.previousState, 3);
            }
        }

        for (SavedBlock saved : run.savedBlocks) {
            if (saved.arenaState.is(Blocks.TORCH)) {
                continue;
            }
            var be = run.level.getBlockEntity(saved.pos);
            if (be instanceof Container container) {
                // Remove temporary chest contents so they do not drop when the arena is reverted.
                container.clearContent();
            }
            run.level.setBlock(saved.pos, saved.previousState, 3);
        }
    }

    private static List<WaveConfig> parseWaves(JsonObject root, int defaultMessageColor) {
        List<WaveConfig> result = new ArrayList<>();

        if (root.has("waves") && root.get("waves").isJsonArray()) {
            JsonArray wavesArray = root.getAsJsonArray("waves");
            for (JsonElement element : wavesArray) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject waveObj = element.getAsJsonObject();
                String entityId = waveObj.has("entity") ? waveObj.get("entity").getAsString() : "minecraft:zombie";
                int min = waveObj.has("count_min") ? waveObj.get("count_min").getAsInt() : 3;
                int max = waveObj.has("count_max") ? waveObj.get("count_max").getAsInt() : min;
                String message = waveObj.has("message") ? waveObj.get("message").getAsString() : null;
                int messageColor = parseRgbColor(waveObj, "message_color", defaultMessageColor);

                if (min < 1) min = 1;
                if (max < min) max = min;
                result.add(new WaveConfig(entityId, min, max, message, messageColor));
            }
        }

        return result;
    }

    private static int[] parseWaveCountWeights(JsonObject root) {
        int[] weights = new int[] {8, 34, 30, 18, 10};
        if (root.has("wave_count_weights") && root.get("wave_count_weights").isJsonObject()) {
            JsonObject weightObj = root.getAsJsonObject("wave_count_weights");
            for (int i = 1; i <= 5; i++) {
                String key = Integer.toString(i);
                if (weightObj.has(key)) {
                    int value = weightObj.get(key).getAsInt();
                    weights[i - 1] = Math.max(0, value);
                }
            }
        }
        return weights;
    }

    private static List<RewardLootEntry> parseRewardLootTable(JsonObject root) {
        List<RewardLootEntry> entries = new ArrayList<>();
        if (!root.has("reward_loot_table") || !root.get("reward_loot_table").isJsonArray()) {
            return entries;
        }

        JsonArray arr = root.getAsJsonArray("reward_loot_table");
        for (JsonElement element : arr) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject entryObj = element.getAsJsonObject();
            Item item = parseItem(entryObj, "item", Items.AIR);
            if (item == Items.AIR) {
                continue;
            }

            int countMin = entryObj.has("count_min") ? entryObj.get("count_min").getAsInt() : 1;
            int countMax = entryObj.has("count_max") ? entryObj.get("count_max").getAsInt() : countMin;
            int baseWeight = entryObj.has("weight") ? entryObj.get("weight").getAsInt() : 1;
            int waveWeightBonus = entryObj.has("wave_weight_bonus") ? entryObj.get("wave_weight_bonus").getAsInt() : 0;
            boolean enchanted = entryObj.has("enchanted") && entryObj.get("enchanted").getAsBoolean();
            int enchantCostMin = entryObj.has("enchant_cost_min") ? entryObj.get("enchant_cost_min").getAsInt() : 8;
            int enchantCostMax = entryObj.has("enchant_cost_max") ? entryObj.get("enchant_cost_max").getAsInt() : 20;

            if (countMin < 1) countMin = 1;
            if (countMax < countMin) countMax = countMin;
            if (baseWeight < 0) baseWeight = 0;
            if (enchantCostMin < 1) enchantCostMin = 1;
            if (enchantCostMax < enchantCostMin) enchantCostMax = enchantCostMin;

            entries.add(new RewardLootEntry(item, countMin, countMax, baseWeight, waveWeightBonus, enchanted, enchantCostMin, enchantCostMax));
        }

        return entries;
    }

    private static List<GuaranteedChestEntry> parseSupportGuaranteedItems(JsonObject obj) {
        List<GuaranteedChestEntry> result = new ArrayList<>();
        if (obj.has("support_chest") && obj.get("support_chest").isJsonObject()) {
            JsonObject supportObj = obj.getAsJsonObject("support_chest");
            if (supportObj.has("guaranteed_items") && supportObj.get("guaranteed_items").isJsonArray()) {
                JsonArray array = supportObj.getAsJsonArray("guaranteed_items");
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) {
                        continue;
                    }

                    JsonObject entryObj = element.getAsJsonObject();
                    Item item = parseItem(entryObj, "item", Items.AIR);
                    if (item == Items.AIR) {
                        continue;
                    }

                    int countMin = entryObj.has("count_min") ? entryObj.get("count_min").getAsInt() : 1;
                    int countMax = entryObj.has("count_max") ? entryObj.get("count_max").getAsInt() : countMin;
                    if (countMin < 1) countMin = 1;
                    if (countMax < countMin) countMax = countMin;

                    result.add(new GuaranteedChestEntry(item, countMin, countMax));
                }
            }
        }

        if (result.isEmpty()) {
            result.add(new GuaranteedChestEntry(Items.STONE_SWORD, 1, 1));
            result.add(new GuaranteedChestEntry(Items.SHIELD, 1, 1));
        }

        return result;
    }

    private static Block parseBlock(JsonObject obj, String field, Block fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(obj.get(field).getAsString()));
            return block != null && block != Blocks.AIR ? block : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Item parseItem(JsonObject obj, String field, Item fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(obj.get(field).getAsString()));
            return item != null && item != Items.AIR ? item : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseRgbColor(JsonObject obj, String field, int fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            var element = obj.get(field);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt() & 0xFFFFFF;
            }

            String value = element.getAsString().trim();
            try {
                ChatFormatting formatting = ChatFormatting.valueOf(value.toUpperCase());
                Integer named = formatting.getColor();
                if (named != null) {
                    return named & 0xFFFFFF;
                }
            } catch (Exception ignored) {
            }

            if (value.startsWith("#")) {
                value = value.substring(1);
            } else if (value.startsWith("0x") || value.startsWith("0X")) {
                value = value.substring(2);
            }
            if (value.length() == 6) {
                return Integer.parseInt(value, 16) & 0xFFFFFF;
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private static String[] parseSignLines(JsonObject obj, String field, String[] fallback) {
        if (!obj.has(field) || !obj.get(field).isJsonArray()) {
            return fallback.clone();
        }

        String[] lines = fallback.clone();
        JsonArray array = obj.getAsJsonArray(field);
        for (int i = 0; i < 4; i++) {
            if (i < array.size() && array.get(i).isJsonPrimitive()) {
                lines[i] = array.get(i).getAsString();
            }
        }
        return lines;
    }

    private void tryEquipZombieArmor(ServerLevel level, RandomSource random, LivingEntity living) {
        if (zombieArmorTiers.isEmpty()) {
            return;
        }
        if (random.nextDouble() >= zombieArmorChance) {
            return;
        }

        ZombieArmorTier tier = chooseZombieArmorTier(random, zombieArmorTiers);
        if (tier == null) {
            return;
        }

        if (tier.helmet != Items.AIR && random.nextDouble() < zombieArmorPieceChance) {
            living.setItemSlot(EquipmentSlot.HEAD, createZombieArmorPiece(level, random, tier.helmet));
        }
        if (tier.chestplate != Items.AIR && random.nextDouble() < zombieArmorPieceChance) {
            living.setItemSlot(EquipmentSlot.CHEST, createZombieArmorPiece(level, random, tier.chestplate));
        }
        if (tier.leggings != Items.AIR && random.nextDouble() < zombieArmorPieceChance) {
            living.setItemSlot(EquipmentSlot.LEGS, createZombieArmorPiece(level, random, tier.leggings));
        }
        if (tier.boots != Items.AIR && random.nextDouble() < zombieArmorPieceChance) {
            living.setItemSlot(EquipmentSlot.FEET, createZombieArmorPiece(level, random, tier.boots));
        }
    }

    private ItemStack createZombieArmorPiece(ServerLevel level, RandomSource random, Item item) {
        ItemStack stack = new ItemStack(item);
        if (random.nextDouble() >= zombieArmorEnchantChance) {
            return stack;
        }

        int cost = zombieArmorEnchantCostMin + random.nextInt(Math.max(1, zombieArmorEnchantCostMax - zombieArmorEnchantCostMin + 1));
        try {
            return EnchantmentHelper.enchantItem(random, stack, cost, level.registryAccess(), Optional.empty());
        } catch (Exception ignored) {
            return stack;
        }
    }

    private ZombieArmorTier chooseZombieArmorTier(RandomSource random, List<ZombieArmorTier> tiers) {
        int totalWeight = 0;
        for (ZombieArmorTier tier : tiers) {
            totalWeight += Math.max(1, tier.weight);
        }
        if (totalWeight <= 0) {
            return null;
        }

        int pick = random.nextInt(totalWeight);
        int running = 0;
        for (ZombieArmorTier tier : tiers) {
            running += Math.max(1, tier.weight);
            if (pick < running) {
                return tier;
            }
        }

        return tiers.get(tiers.size() - 1);
    }

    private static SupportChestConfig parseSupportChest(JsonObject obj) {
        boolean enabled = true;
        int stacksMin = 1;
        int stacksMax = 1;
        List<SupportChestEntry> entries = new ArrayList<>();

        if (obj.has("support_chest") && obj.get("support_chest").isJsonObject()) {
            JsonObject supportObj = obj.getAsJsonObject("support_chest");
            if (supportObj.has("enabled")) {
                enabled = supportObj.get("enabled").getAsBoolean();
            }
            if (supportObj.has("stacks_min")) {
                stacksMin = supportObj.get("stacks_min").getAsInt();
            }
            if (supportObj.has("stacks_max")) {
                stacksMax = supportObj.get("stacks_max").getAsInt();
            }

            if (supportObj.has("items") && supportObj.get("items").isJsonArray()) {
                JsonArray itemsArray = supportObj.getAsJsonArray("items");
                for (JsonElement element : itemsArray) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject itemObj = element.getAsJsonObject();
                    Item item = parseItem(itemObj, "item", Items.AIR);
                    if (item == Items.AIR) {
                        continue;
                    }

                    int countMin = itemObj.has("count_min") ? itemObj.get("count_min").getAsInt() : 1;
                    int countMax = itemObj.has("count_max") ? itemObj.get("count_max").getAsInt() : countMin;
                    int weight = itemObj.has("weight") ? itemObj.get("weight").getAsInt() : 1;

                    if (countMin < 1) countMin = 1;
                    if (countMax < countMin) countMax = countMin;
                    if (weight < 1) weight = 1;

                    entries.add(new SupportChestEntry(item, countMin, countMax, weight));
                }
            }
        }

        if (stacksMin < 1) stacksMin = 1;
        if (stacksMax < stacksMin) stacksMax = stacksMin;

        if (entries.isEmpty()) {
            for (String idString : DEFAULT_SUPPORT_ITEM_IDS) {
                try {
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(idString));
                    if (item != null && item != Items.AIR) {
                        entries.add(new SupportChestEntry(item, 1, 1, 1));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return new SupportChestConfig(enabled, stacksMin, stacksMax, entries);
    }

    private static ZombieArmorConfig parseZombieArmor(JsonObject obj) {
        double chance = obj.has("zombie_armor_chance") ? obj.get("zombie_armor_chance").getAsDouble() : 0.35;
        double pieceChance = obj.has("zombie_armor_piece_chance") ? obj.get("zombie_armor_piece_chance").getAsDouble() : 0.75;
        double enchantChance = obj.has("zombie_armor_enchant_chance") ? obj.get("zombie_armor_enchant_chance").getAsDouble() : 0.05;
        int enchantCostMin = obj.has("zombie_armor_enchant_cost_min") ? obj.get("zombie_armor_enchant_cost_min").getAsInt() : 5;
        int enchantCostMax = obj.has("zombie_armor_enchant_cost_max") ? obj.get("zombie_armor_enchant_cost_max").getAsInt() : 12;
        List<ZombieArmorTier> tiers = new ArrayList<>();

        if (obj.has("zombie_armor_tiers") && obj.get("zombie_armor_tiers").isJsonArray()) {
            JsonArray tiersArray = obj.getAsJsonArray("zombie_armor_tiers");
            for (JsonElement element : tiersArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject tierObj = element.getAsJsonObject();
                Item helmet = parseItem(tierObj, "helmet", Items.AIR);
                Item chestplate = parseItem(tierObj, "chestplate", Items.AIR);
                Item leggings = parseItem(tierObj, "leggings", Items.AIR);
                Item boots = parseItem(tierObj, "boots", Items.AIR);
                int weight = tierObj.has("weight") ? tierObj.get("weight").getAsInt() : 1;
                if (weight < 1) {
                    weight = 1;
                }
                tiers.add(new ZombieArmorTier(helmet, chestplate, leggings, boots, weight));
            }
        }

        if (tiers.isEmpty()) {
            tiers.add(new ZombieArmorTier(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, 5));
            tiers.add(new ZombieArmorTier(Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, 3));
            tiers.add(new ZombieArmorTier(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS, 2));
            tiers.add(new ZombieArmorTier(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, 1));
        }

        if (chance < 0.0) chance = 0.0;
        if (chance > 1.0) chance = 1.0;
        if (pieceChance < 0.0) pieceChance = 0.0;
        if (pieceChance > 1.0) pieceChance = 1.0;
        if (enchantChance < 0.0) enchantChance = 0.0;
        if (enchantChance > 1.0) enchantChance = 1.0;
        if (enchantCostMin < 1) enchantCostMin = 1;
        if (enchantCostMax < enchantCostMin) enchantCostMax = enchantCostMin;

        return new ZombieArmorConfig(chance, pieceChance, enchantChance, enchantCostMin, enchantCostMax, tiers);
    }

    private static class SupportChestConfig {
        private final boolean enabled;
        private final int stacksMin;
        private final int stacksMax;
        private final List<SupportChestEntry> entries;

        private SupportChestConfig(boolean enabled, int stacksMin, int stacksMax, List<SupportChestEntry> entries) {
            this.enabled = enabled;
            this.stacksMin = stacksMin;
            this.stacksMax = stacksMax;
            this.entries = entries;
        }
    }

    private static class ZombieArmorConfig {
        private final double chance;
        private final double pieceChance;
        private final double enchantChance;
        private final int enchantCostMin;
        private final int enchantCostMax;
        private final List<ZombieArmorTier> tiers;

        private ZombieArmorConfig(double chance, double pieceChance, double enchantChance, int enchantCostMin, int enchantCostMax, List<ZombieArmorTier> tiers) {
            this.chance = chance;
            this.pieceChance = pieceChance;
            this.enchantChance = enchantChance;
            this.enchantCostMin = enchantCostMin;
            this.enchantCostMax = enchantCostMax;
            this.tiers = tiers;
        }
    }

    private static class ArenaRun {
        private final ServerLevel level;
        private final UUID ownerId;
        private final BlockPos center;
        private final List<SavedBlock> savedBlocks;
        private long waveExpiresAtGameTime;
        private int totalWaves;
        private int activeWaveNumber;
        private List<UUID> activeMobs = List.of();
        private long ownerOutsideSinceTick = -1L;
        private boolean finished;

        private ArenaRun(ServerLevel level, UUID ownerId, BlockPos center, List<SavedBlock> savedBlocks) {
            this.level = level;
            this.ownerId = ownerId;
            this.center = center;
            this.savedBlocks = savedBlocks;
        }
    }
}
