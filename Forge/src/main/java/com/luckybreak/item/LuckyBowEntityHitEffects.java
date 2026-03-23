package com.luckybreak.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class LuckyBowEntityHitEffects {

    private LuckyBowEntityHitEffects() {
    }

    public static void register() {
    }

    @Mod.EventBusSubscriber(modid = com.luckybreak.LuckyBreak.MOD_ID)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            LivingEntity entity = event.getEntity();
            DamageSource source = event.getSource();
            float damageTaken = event.getAmount();
            if (!(entity.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (damageTaken <= 0.0F) {
                return;
            }

            DamageSource damageSource = source;
            Entity directEntity = damageSource.getDirectEntity();
            if (!(directEntity instanceof AbstractArrow arrow)) {
                return;
            }
            if (!arrow.getTags().contains(LuckyBowItem.LUCKY_BOW_ARROW_TAG)) {
                return;
            }

            Set<LuckyBowConfig.BowEffectMode> modes = LuckyBowItem.getArrowModes(arrow);
            if (modes.contains(LuckyBowConfig.BowEffectMode.ENTITY_HIT_BLOCK_TRANSFORM)) {
                tryEntityHitBlockTransform(entity, serverLevel);
                LuckyBowItem.markEntityHitBlockTransformTriggered(arrow);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.ENTITY_HIT_POTION)) {
                tryApplyPotionEffect(entity, serverLevel);
                LuckyBowItem.markEntityHitPotionTriggered(arrow);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.ENTITY_HIT_LIGHTNING)) {
                trySummonLightning(entity, serverLevel, arrow);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.ENTITY_BOUNCE)) {
                tryEntityBounce(entity, serverLevel);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.PARTY_POP)) {
                tryPartyPopAtPosition(serverLevel, entity.blockPosition());
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.CHICKEN_RAIN)) {
                tryChickenRainAtPosition(serverLevel, entity.blockPosition());
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.BLOCK_TRANSFORM)) {
                tryBlockTransformAtPosition(serverLevel, entity.blockPosition());
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.FIRE_SPREAD)) {
                tryFireSpreadAtPosition(serverLevel, entity.blockPosition());
            }
        }
    }

    private static void tryEntityHitBlockTransform(net.minecraft.world.entity.LivingEntity target, ServerLevel level) {
        tryEntityHitBlockTransformAtPosition(level, target.blockPosition());
    }

    public static void tryEntityHitBlockTransformAtPosition(ServerLevel level, BlockPos centerPos) {
        LuckyBowConfig.EntityHitBlockTransformSettings settings = LuckyBowConfig.entityHitBlockTransformSettings();
        if (!settings.enabled() || settings.possibleBlocks().isEmpty()) {
            return;
        }

        List<BlockPos> targets = findTransformTargets(level, centerPos, settings.searchRadius(), settings.transformCount());
        if (targets.isEmpty()) {
            return;
        }

        for (BlockPos targetPos : targets) {
            Block replacement = pickWeightedBlock(level, settings.possibleBlocks());
            if (replacement == null) {
                return;
            }
            BlockState replacementState = replacement.defaultBlockState();
            level.setBlock(targetPos, replacementState, 3);
        }
    }

    public static void tryApplyPotionAtPosition(ServerLevel level, BlockPos centerPos, AbstractArrow arrow) {
        if (centerPos == null) {
            return;
        }

        LuckyBowConfig.EntityHitPotionSettings settings = LuckyBowConfig.entityHitPotionSettings();
        if (!settings.enabled()) {
            return;
        }

        LuckyBowConfig.PotionEntry selected = pickPotion(level, settings);
        if (selected == null) {
            return;
        }

        MobEffect effect = null;
        try {
            effect = BuiltInRegistries.MOB_EFFECT.getValue(ResourceLocation.parse(selected.effectId()));
        } catch (Exception ignored) {
        }
        if (effect == null) {
            return;
        }

        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        AABB area = new AABB(centerPos).inflate(2.5D);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive() && entity != arrow.getOwner());
        if (nearby.isEmpty()) {
            return;
        }

        LivingEntity target = nearby.get(level.getRandom().nextInt(nearby.size()));
        target.addEffect(new MobEffectInstance(holder, selected.durationTicks(), selected.amplifier()));
    }

    private static void tryBlockTransformAtPosition(ServerLevel level, BlockPos impactPos) {
        LuckyBowConfig.BlockTransformSettings settings = LuckyBowConfig.blockTransformSettings();
        if (!settings.enabled() || settings.possibleBlocks().isEmpty()) {
            return;
        }

        BlockPos targetPos = resolveTargetPos(level, impactPos);
        if (targetPos == null) {
            return;
        }

        BlockState current = level.getBlockState(targetPos);
        Block replacement = pickWeightedBlock(level, settings.possibleBlocks(), current.getBlock());
        if (replacement == null || replacement == current.getBlock()) {
            return;
        }

        level.setBlock(targetPos, replacement.defaultBlockState(), 3);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
                8, 0.4, 0.4, 0.4, 0.05);
    }

    private static void tryFireSpreadAtPosition(ServerLevel level, BlockPos centerPos) {
        LuckyBowConfig.FireSpreadSettings settings = LuckyBowConfig.fireSpreadSettings();
        if (!settings.enabled()) {
            return;
        }
        if (level.getRandom().nextFloat() > settings.chance()) {
            return;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int requested = minCount + level.getRandom().nextInt(maxCount - minCount + 1);
        List<BlockPos> candidates = findFireSpreadTargets(level, centerPos, settings.searchRadius());
        if (candidates.isEmpty()) {
            return;
        }

        Collections.shuffle(candidates, new java.util.Random(level.getRandom().nextLong()));
        int placed = 0;
        for (BlockPos pos : candidates) {
            if (placed >= requested) {
                break;
            }

            if (!level.getBlockState(pos).isAir()) {
                continue;
            }

            BlockState fireState = BaseFireBlock.getState(level, pos);
            if (fireState == null || !fireState.is(Blocks.FIRE)) {
                continue;
            }

            level.setBlock(pos, fireState, 3);
            level.sendParticles(ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                    12, 0.25, 0.15, 0.25, 0.01);
            placed++;
        }
    }

    private static List<BlockPos> findFireSpreadTargets(ServerLevel level, BlockPos center, int radius) {
        int scanRadius = Math.max(1, radius);
        List<BlockPos> result = new ArrayList<>();

        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                if ((dx * dx) + (dz * dz) > (scanRadius * scanRadius)) {
                    continue;
                }

                BlockPos base = center.offset(dx, 0, dz);
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos candidate = base.offset(0, dy, 0);
                    BlockPos below = candidate.below();

                    if (!level.getBlockState(candidate).isAir()) {
                        continue;
                    }
                    if (level.getBlockState(below).isAir()) {
                        continue;
                    }

                    result.add(candidate.immutable());
                    break;
                }
            }
        }

        return result;
    }

    private static Block pickWeightedBlock(ServerLevel level, java.util.List<LuckyBowConfig.WeightedBlockEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (LuckyBowConfig.WeightedBlockEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.getRandom().nextInt(totalWeight);
        for (LuckyBowConfig.WeightedBlockEntry entry : entries) {
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry.block();
            }
            roll -= weight;
        }

        return entries.getFirst().block();
    }

    private static Block pickWeightedBlock(ServerLevel level, java.util.List<LuckyBowConfig.WeightedBlockEntry> entries, Block disallowed) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (LuckyBowConfig.WeightedBlockEntry entry : entries) {
            if (entry.block() == disallowed) {
                continue;
            }
            totalWeight += Math.max(0, entry.weight());
        }

        if (totalWeight <= 0) {
            return pickWeightedBlock(level, entries);
        }

        int roll = level.getRandom().nextInt(totalWeight);
        for (LuckyBowConfig.WeightedBlockEntry entry : entries) {
            if (entry.block() == disallowed) {
                continue;
            }
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry.block();
            }
            roll -= weight;
        }

        return pickWeightedBlock(level, entries);
    }

    private static BlockPos resolveTargetPos(ServerLevel level, BlockPos impactPos) {
        if (impactPos == null) {
            return null;
        }

        if (!level.getBlockState(impactPos).isAir()) {
            return impactPos;
        }

        BlockPos below = impactPos.below();
        if (!level.getBlockState(below).isAir()) {
            return below;
        }

        return impactPos;
    }

    private static void tryApplyPotionEffect(net.minecraft.world.entity.LivingEntity target, ServerLevel level) {
        LuckyBowConfig.EntityHitPotionSettings settings = LuckyBowConfig.entityHitPotionSettings();
        if (!settings.enabled()) {
            return;
        }

        LuckyBowConfig.PotionEntry selected = pickPotion(level, settings);
        if (selected == null) {
            return;
        }

        MobEffect effect = null;
        try {
            effect = BuiltInRegistries.MOB_EFFECT.getValue(ResourceLocation.parse(selected.effectId()));
        } catch (Exception ignored) {
        }
        if (effect == null) {
            return;
        }

        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        target.addEffect(new MobEffectInstance(holder, selected.durationTicks(), selected.amplifier()));
    }

    private static LuckyBowConfig.PotionEntry pickPotion(ServerLevel level, LuckyBowConfig.EntityHitPotionSettings settings) {
        if (settings.possibleEffects() == null || settings.possibleEffects().isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (LuckyBowConfig.PotionEntry entry : settings.possibleEffects()) {
            totalWeight += Math.max(0, entry.weight());
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.getRandom().nextInt(totalWeight);
        for (LuckyBowConfig.PotionEntry entry : settings.possibleEffects()) {
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry;
            }
            roll -= weight;
        }

        return settings.possibleEffects().getFirst();
    }

    private static void trySummonLightning(net.minecraft.world.entity.LivingEntity target, ServerLevel level, AbstractArrow arrow) {
        LuckyBowConfig.EntityHitLightningSettings settings = LuckyBowConfig.entityHitLightningSettings();
        if (!settings.enabled()) {
            return;
        }

        if (LuckyBowItem.hasLightningTriggered(arrow)) {
            return;
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }

        bolt.setPos(target.getX(), target.getY(), target.getZ());
        bolt.setVisualOnly(settings.visualOnly());
        if (!settings.visualOnly() && arrow.getOwner() instanceof net.minecraft.server.level.ServerPlayer player) {
            bolt.setCause(player);
        }
        level.addFreshEntity(bolt);
        LuckyBowItem.markLightningTriggered(arrow);
    }

    private static void tryEntityBounce(net.minecraft.world.entity.LivingEntity target, ServerLevel level) {
        LuckyBowConfig.EntityBounceSettings settings = LuckyBowConfig.entityBounceSettings();
        if (!settings.enabled() || level.getRandom().nextFloat() > settings.chance()) {
            return;
        }

        var current = target.getDeltaMovement();
        double x = (level.getRandom().nextDouble() - 0.5D) * settings.horizontalBoost();
        double z = (level.getRandom().nextDouble() - 0.5D) * settings.horizontalBoost();
        target.setDeltaMovement(current.x + x, Math.max(current.y, settings.upwardBoost()), current.z + z);
        target.hurtMarked = true;

        level.sendParticles(ParticleTypes.CLOUD,
                target.getX(), target.getY() + 0.8D, target.getZ(),
                14, 0.25D, 0.2D, 0.25D, 0.03D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                target.getX(), target.getY() + 0.8D, target.getZ(),
                10, 0.3D, 0.25D, 0.3D, 0.02D);
    }

    private static void tryPartyPopAtPosition(ServerLevel level, BlockPos centerPos) {
        LuckyBowConfig.PartyPopSettings settings = LuckyBowConfig.partyPopSettings();
        if (!settings.enabled() || level.getRandom().nextFloat() > settings.chance()) {
            return;
        }

        double x = centerPos.getX() + 0.5D;
        double y = centerPos.getY() + 0.9D;
        double z = centerPos.getZ() + 0.5D;
        int particleCount = Math.max(0, settings.particleCount());
        if (particleCount <= 0) {
            return;
        }

        level.sendParticles(ParticleTypes.FIREWORK,
                x, y, z,
                particleCount,
                settings.radius() * 0.2D,
                0.35D,
                settings.radius() * 0.2D,
                0.08D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                x, y, z,
                Math.max(8, particleCount / 2),
                settings.radius() * 0.18D,
                0.3D,
                settings.radius() * 0.18D,
                0.03D);
    }

    private static void tryChickenRainAtPosition(ServerLevel level, BlockPos centerPos) {
        LuckyBowConfig.ChickenRainSettings settings = LuckyBowConfig.chickenRainSettings();
        if (!settings.enabled() || level.getRandom().nextFloat() > settings.chance()) {
            return;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int count = minCount + level.getRandom().nextInt(maxCount - minCount + 1);

        List<BlockPos> candidates = findFireSpreadTargets(level, centerPos, settings.searchRadius());
        if (candidates.isEmpty()) {
            candidates = List.of(centerPos.above());
        }

        Collections.shuffle(candidates, new java.util.Random(level.getRandom().nextLong()));
        int spawned = 0;
        for (BlockPos pos : candidates) {
            if (spawned >= count) {
                break;
            }

            var chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
            if (chicken == null) {
                continue;
            }

            chicken.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            if (!level.noCollision(chicken)) {
                continue;
            }

            level.addFreshEntity(chicken);
            spawned++;
        }

        if (spawned > 0) {
            level.sendParticles(ParticleTypes.CLOUD,
                    centerPos.getX() + 0.5D, centerPos.getY() + 1.0D, centerPos.getZ() + 0.5D,
                    Math.max(8, spawned * 6),
                    0.5D, 0.25D, 0.5D, 0.02D);
        }
    }

    private static List<BlockPos> findTransformTargets(ServerLevel level, BlockPos center, int radius, int targetCount) {
        if (center == null) {
            return List.of();
        }

        int requested = Math.max(1, targetCount);
        List<BlockPos> candidates = new ArrayList<>();

        int scanRadius = Math.max(0, radius);
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                BlockPos candidate = center.offset(dx, 0, dz);
                if (!level.getBlockState(candidate).isAir()) {
                    candidates.add(candidate.immutable());
                }

                BlockPos candidateBelow = candidate.below();
                if (!level.getBlockState(candidateBelow).isAir()) {
                    candidates.add(candidateBelow.immutable());
                }
            }
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(candidates, new java.util.Random(level.getRandom().nextLong()));
        if (candidates.size() <= requested) {
            return candidates;
        }

        return new ArrayList<>(candidates.subList(0, requested));
    }
}
