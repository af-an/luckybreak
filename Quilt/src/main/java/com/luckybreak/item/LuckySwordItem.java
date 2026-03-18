package com.luckybreak.item;

import com.luckybreak.events.LuckyScheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LuckySwordItem extends Item {

    private static final String ITEM_ID = "lucky_sword";
    private static final String TEMP_ENCHANT_IDS_KEY = "luckybreak_sword_temp_enchant_ids";
    private static final String TEMP_ENCHANT_EXPIRES_AT_KEY = "luckybreak_sword_temp_enchant_expires_at";

    public LuckySwordItem(ResourceKey<Item> key) {
        super(LuckyItemVisuals.apply(ITEM_ID, new Item.Properties()
                .setId(key)
                .sword(ToolMaterial.GOLD, LuckySwordConfig.attackDamage(), LuckySwordConfig.attackSpeed())
                .durability(LuckySwordConfig.durability())
            .repairable(Items.GOLD_INGOT)));
    }

    @Override
    public Component getName(ItemStack stack) {
        return LuckyItemVisuals.styleName(ITEM_ID, super.getName(stack));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        tickTemporaryEnchantments(stack, level, entity);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        tickTemporaryEnchantments(stack, target == null ? null : target.level(), attacker);
        tryTriggerTemporaryEnchantments(stack, target == null ? null : target.level(), attacker);
        tryTriggerEnderSwap(target, attacker);
        tryTriggerSkyLaunch(target, attacker);
        tryTriggerEchoSlash(target, attacker);
        tryTriggerChainLightning(target, attacker);
        tryTriggerShadowStep(target, attacker);
        tryTriggerFrostPrison(target, attacker);
        tryTriggerStarfallStrike(target, attacker);
        tryTriggerMobHitLightning(target, attacker);
        tryTriggerKnockbackFireTrail(target, attacker);
    }

    public boolean triggerCommandEvent(ItemStack stack, ServerPlayer player, String eventName) {
        if (stack == null || stack.isEmpty() || player == null || eventName == null || eventName.isBlank()) {
            return false;
        }

        String normalized = eventName.trim().toLowerCase(java.util.Locale.ROOT);
        if ("temporary_enchantments".equals(normalized)) {
            tickTemporaryEnchantments(stack, player.level(), player);
            tryTriggerTemporaryEnchantments(stack, player.level(), player, true);
            return true;
        }

        LivingEntity target = findClosestMob(player, 48.0D);
        if (target == null) {
            return false;
        }

        return switch (normalized) {
            case "ender_swap" -> {
                tryTriggerEnderSwap(target, player, true);
                yield true;
            }
            case "sky_launch" -> {
                tryTriggerSkyLaunch(target, player, true);
                yield true;
            }
            case "echo_slash" -> {
                tryTriggerEchoSlash(target, player, true);
                yield true;
            }
            case "chain_lightning" -> {
                tryTriggerChainLightning(target, player, true);
                yield true;
            }
            case "shadow_step" -> {
                tryTriggerShadowStep(target, player, true);
                yield true;
            }
            case "frost_prison" -> {
                tryTriggerFrostPrison(target, player, true);
                yield true;
            }
            case "starfall_strike" -> {
                tryTriggerStarfallStrike(target, player, true);
                yield true;
            }
            case "lightning" -> {
                tryTriggerMobHitLightning(target, player, true);
                yield true;
            }
            case "knockback_fire_trail" -> {
                tryTriggerKnockbackFireTrail(target, player, true);
                yield true;
            }
            default -> false;
        };
    }

    private LivingEntity findClosestMob(ServerPlayer player, double radius) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != null && entity.isAlive() && entity != player);

        LivingEntity closest = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (LivingEntity entity : nearby) {
            double distSq = entity.distanceToSqr(player);
            if (distSq < bestDistanceSq) {
                bestDistanceSq = distSq;
                closest = entity;
            }
        }
        return closest;
    }

    private void tryTriggerChainLightning(LivingEntity target, LivingEntity attacker) {
        tryTriggerChainLightning(target, attacker, false);
    }

    private void tryTriggerChainLightning(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || attacker == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckySwordConfig.ChainLightningSettings settings = LuckySwordConfig.funEffectSettings().chainLightning();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        AABB area = target.getBoundingBox().inflate(settings.radius());
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != null
                        && entity.isAlive()
                        && entity != target
                        && entity != attacker
                        && !(entity instanceof Player));
        if (nearby.isEmpty()) {
            return;
        }

        Collections.shuffle(nearby, new java.util.Random(serverLevel.random.nextLong()));

        int minTargets = Math.max(1, settings.minTargets());
        int maxTargets = Math.max(minTargets, settings.maxTargets());
        int desiredTargets = minTargets;
        if (maxTargets > minTargets) {
            desiredTargets += serverLevel.random.nextInt(maxTargets - minTargets + 1);
        }

        int targetCount = Math.min(desiredTargets, nearby.size());
        int hits = 0;
        for (LivingEntity candidate : nearby) {
            if (hits >= targetCount) {
                break;
            }

            if (settings.damagePerTarget() > 0.0F) {
                candidate.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(attacker), settings.damagePerTarget());
            }
            if (settings.igniteTarget() && settings.igniteSeconds() > 0.0F) {
                candidate.igniteForSeconds(settings.igniteSeconds());
            }

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, candidate.getX(), candidate.getY(0.7D), candidate.getZ(), 14, 0.2D, 0.25D, 0.2D, 0.03D);
            hits++;
        }
    }

    private void tryTriggerShadowStep(LivingEntity target, LivingEntity attacker) {
        tryTriggerShadowStep(target, attacker, false);
    }

    private void tryTriggerShadowStep(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || attacker == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckySwordConfig.ShadowStepSettings settings = LuckySwordConfig.funEffectSettings().shadowStep();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        Vec3 look = target.getLookAngle();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double dirX = horizontal <= 0.0001D ? 0.0D : look.x / horizontal;
        double dirZ = horizontal <= 0.0001D ? 1.0D : look.z / horizontal;

        double oldX = attacker.getX();
        double oldY = attacker.getY();
        double oldZ = attacker.getZ();

        double newX = target.getX() - dirX * settings.behindDistance();
        double newY = target.getY();
        double newZ = target.getZ() - dirZ * settings.behindDistance();

        attacker.teleportTo(newX, newY, newZ);
        serverLevel.sendParticles(ParticleTypes.PORTAL, oldX, oldY + 0.9D, oldZ, 18, 0.25D, 0.35D, 0.25D, 0.1D);
        serverLevel.sendParticles(ParticleTypes.PORTAL, newX, newY + 0.9D, newZ, 18, 0.25D, 0.35D, 0.25D, 0.1D);

        if (settings.bonusDamage() > 0.0F) {
            target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(attacker), settings.bonusDamage());
        }
    }

    private void tryTriggerFrostPrison(LivingEntity target, LivingEntity attacker) {
        tryTriggerFrostPrison(target, attacker, false);
    }

    private void tryTriggerFrostPrison(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || attacker == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckySwordConfig.FrostPrisonSettings settings = LuckySwordConfig.funEffectSettings().frostPrison();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, settings.durationTicks(), settings.slownessAmplifier(), false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, settings.durationTicks(), 1, false, true, true));
        target.setDeltaMovement(0.0D, Math.min(target.getDeltaMovement().y, 0.0D), 0.0D);
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.8D), target.getZ(), settings.particleCount(), 0.35D, 0.45D, 0.35D, 0.01D);
    }

    private void tryTriggerStarfallStrike(LivingEntity target, LivingEntity attacker) {
        tryTriggerStarfallStrike(target, attacker, false);
    }

    private void tryTriggerStarfallStrike(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || attacker == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckySwordConfig.StarfallStrikeSettings settings = LuckySwordConfig.funEffectSettings().starfallStrike();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 2.2D, target.getZ(), 12, 0.2D, 0.7D, 0.2D, 0.03D);
        LuckyScheduler.INSTANCE.schedule(settings.delayTicks(), () -> {
            if (!target.isAlive() || target.isRemoved() || target.level() != serverLevel) {
                return;
            }

            if (settings.damage() > 0.0F) {
                target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(attacker), settings.damage());
            }
            target.push(0.0D, 0.18D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(0.9D), target.getZ(), 2, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.9D), target.getZ(), settings.particleCount(), 0.3D, 0.5D, 0.3D, 0.03D);
        });
    }

    private void tryTriggerEnderSwap(LivingEntity target, LivingEntity attacker) {
        tryTriggerEnderSwap(target, attacker, false);
    }

    private void tryTriggerEnderSwap(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || attacker == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckySwordConfig.EnderSwapSettings settings = LuckySwordConfig.funEffectSettings().enderSwap();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        double attackerX = attacker.getX();
        double attackerY = attacker.getY();
        double attackerZ = attacker.getZ();
        float attackerYaw = attacker.getYRot();
        float attackerPitch = attacker.getXRot();

        double targetX = target.getX();
        double targetY = target.getY();
        double targetZ = target.getZ();
        float targetYaw = target.getYRot();
        float targetPitch = target.getXRot();

        Vec3 safeAttackerDest = findSafeTeleportSpot(serverLevel, attacker, targetX, targetY, targetZ);
        Vec3 safeTargetDest = findSafeTeleportSpot(serverLevel, target, attackerX, attackerY, attackerZ);
        if (safeAttackerDest == null || safeTargetDest == null) {
            if (settings.failedSwapIgniteSeconds() > 0.0F) {
                target.igniteForSeconds(settings.failedSwapIgniteSeconds());
            }
            return;
        }

        attacker.teleportTo(safeAttackerDest.x, safeAttackerDest.y, safeAttackerDest.z);

        target.teleportTo(safeTargetDest.x, safeTargetDest.y, safeTargetDest.z);
        target.setYRot(attackerYaw);
        target.setXRot(attackerPitch);

        faceEntity(attacker, target);
        if (!(attacker instanceof net.minecraft.world.entity.player.Player)) {
            attacker.setYRot(targetYaw);
            attacker.setXRot(targetPitch);
        }

        serverLevel.sendParticles(ParticleTypes.PORTAL, attackerX, attackerY + 0.9D, attackerZ, 24, 0.3D, 0.4D, 0.3D, 0.1D);
        serverLevel.sendParticles(ParticleTypes.PORTAL, targetX, targetY + 0.9D, targetZ, 24, 0.3D, 0.4D, 0.3D, 0.1D);
    }

    private Vec3 findSafeTeleportSpot(ServerLevel level, LivingEntity entity, double baseX, double baseY, double baseZ) {
        double originalX = entity.getX();
        double originalY = entity.getY();
        double originalZ = entity.getZ();

        Vec3[] verticalCandidates = new Vec3[] {
                new Vec3(baseX, baseY, baseZ),
                new Vec3(baseX, baseY + 0.5D, baseZ),
                new Vec3(baseX, baseY + 1.0D, baseZ),
                new Vec3(baseX, baseY - 0.5D, baseZ)
        };

        for (Vec3 candidate : verticalCandidates) {
            Vec3 safe = tryCandidate(level, entity, candidate.x, candidate.y, candidate.z);
            if (safe != null) {
                entity.setPos(originalX, originalY, originalZ);
                return safe;
            }
        }

        for (int radius = 1; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (double dyOffset : new double[] {0.0D, 0.5D, 1.0D, -0.5D}) {
                        double x = baseX + dx;
                        double y = baseY + dyOffset;
                        double z = baseZ + dz;
                        Vec3 safe = tryCandidate(level, entity, x, y, z);
                        if (safe != null) {
                            entity.setPos(originalX, originalY, originalZ);
                            return safe;
                        }
                    }
                }
            }
        }

        entity.setPos(originalX, originalY, originalZ);
        return null;
    }

    private Vec3 tryCandidate(ServerLevel level, LivingEntity entity, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return null;
        }

        entity.setPos(x, y, z);
        return level.noCollision(entity) ? new Vec3(x, y, z) : null;
    }

    private void faceEntity(LivingEntity viewer, LivingEntity observed) {
        Vec3 delta = observed.getEyePosition().subtract(viewer.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal <= 0.0001D) {
            return;
        }

        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(delta.y, horizontal)));
        viewer.setYRot(yaw);
        viewer.setXRot(pitch);
        viewer.setYHeadRot(yaw);
    }

    private void tryTriggerSkyLaunch(LivingEntity target, LivingEntity attacker) {
        tryTriggerSkyLaunch(target, attacker, false);
    }

    private void tryTriggerSkyLaunch(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || attacker == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckySwordConfig.SkyLaunchSettings settings = LuckySwordConfig.funEffectSettings().skyLaunch();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        Vec3 current = target.getDeltaMovement();
        target.setDeltaMovement(current.x, Math.max(current.y, settings.verticalBoost()), current.z);
        target.hurtMarked = true;
        target.removeEffect(MobEffects.SLOW_FALLING);

        serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.35D), target.getZ(), Math.max(10, settings.particlesPerStep() * 2), 0.3D, 0.08D, 0.3D, 0.03D);
        serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5D), target.getZ(), Math.max(8, settings.particlesPerStep()), 0.25D, 0.2D, 0.25D, 0.02D);

        spawnSkyTrail(serverLevel, target, settings);
    }

    private void spawnSkyTrail(ServerLevel level, LivingEntity target, LuckySwordConfig.SkyLaunchSettings settings) {
        if (settings.trailDurationTicks() <= 0 || settings.particlesPerStep() <= 0) {
            return;
        }

        int interval = Math.max(1, settings.trailIntervalTicks());
        for (int delay = 0; delay <= settings.trailDurationTicks(); delay += interval) {
            LuckyScheduler.INSTANCE.schedule(delay, () -> {
                if (target.isAlive() && !target.isRemoved() && target.level() == level) {
                    level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5D), target.getZ(), settings.particlesPerStep(), 0.2D, 0.35D, 0.2D, 0.02D);
                    level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.3D), target.getZ(), Math.max(1, settings.particlesPerStep() / 2), 0.2D, 0.15D, 0.2D, 0.01D);
                }
            });
        }
    }

    private void tryTriggerEchoSlash(LivingEntity target, LivingEntity attacker) {
        tryTriggerEchoSlash(target, attacker, false);
    }

    private void tryTriggerEchoSlash(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || attacker == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckySwordConfig.EchoSlashSettings settings = LuckySwordConfig.funEffectSettings().echoSlash();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        int interval = Math.max(1, settings.intervalTicks());
        for (int i = 1; i <= settings.extraHits(); i++) {
            int delay = i * interval;
            LuckyScheduler.INSTANCE.schedule(delay, () -> {
                if (!target.isAlive() || target.isRemoved() || target.level() != serverLevel) {
                    return;
                }

                if (settings.damagePerHit() > 0.0F) {
                    target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(attacker), settings.damagePerHit());
                }

                serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.7D), target.getZ(), 12, 0.28D, 0.2D, 0.28D, 0.02D);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6D), target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            });
        }
    }

    private void tickTemporaryEnchantments(ItemStack stack, Level level, Entity holderEntity) {
        if (stack == null || stack.isEmpty() || level == null || level.isClientSide()) {
            return;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }

        CompoundTag tag = customData.copyTag();
        long expiresAt = tag.getLong(TEMP_ENCHANT_EXPIRES_AT_KEY).orElse(0L);
        if (expiresAt <= 0L || level.getGameTime() < expiresAt) {
            return;
        }

        String ids = tag.getString(TEMP_ENCHANT_IDS_KEY).orElse("");
        Set<String> removeIds = parseIdSet(ids);
        if (!removeIds.isEmpty()) {
            removeEnchantments(stack, level, removeIds);
            spawnTemporaryEnchantParticles(level, holderEntity, false);
        }

        CustomData base = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        stack.set(DataComponents.CUSTOM_DATA, base.update(t -> {
            t.remove(TEMP_ENCHANT_EXPIRES_AT_KEY);
            t.remove(TEMP_ENCHANT_IDS_KEY);
        }));
    }

    private void tryTriggerTemporaryEnchantments(ItemStack stack, Level level, Entity holderEntity) {
        tryTriggerTemporaryEnchantments(stack, level, holderEntity, false);
    }

    private void tryTriggerTemporaryEnchantments(ItemStack stack, Level level, Entity holderEntity, boolean force) {
        if (stack == null || stack.isEmpty() || level == null || level.isClientSide()) {
            return;
        }

        LuckySwordConfig.TemporaryEnchantSettings settings = LuckySwordConfig.temporaryEnchantSettings();
        if (!settings.enabled() || settings.entries().isEmpty()) {
            return;
        }

        if (!force && level.random.nextFloat() > settings.chance()) {
            return;
        }

        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, Integer> currentLevels = currentEnchantmentLevels(stack, enchantmentRegistry);

        List<LuckySwordConfig.EnchantmentEntry> candidates = new ArrayList<>();
        for (LuckySwordConfig.EnchantmentEntry entry : settings.entries()) {
            if (entry == null || entry.id() == null || entry.weight() <= 0) {
                continue;
            }

            if (currentLevels.containsKey(entry.id().toString())) {
                continue;
            }

            Enchantment enchantment = enchantmentRegistry.getValue(entry.id());
            if (enchantment == null) {
                continue;
            }

            candidates.add(entry);
        }

        if (candidates.isEmpty()) {
            return;
        }

        int minEnchants = Math.max(1, settings.minEnchantments());
        int maxEnchants = Math.max(minEnchants, settings.maxEnchantments());
        maxEnchants = Math.min(maxEnchants, candidates.size());
        minEnchants = Math.min(minEnchants, maxEnchants);
        int count = minEnchants;
        if (maxEnchants > minEnchants) {
            count += level.random.nextInt(maxEnchants - minEnchants + 1);
        }

        Set<String> appliedIds = new HashSet<>();
        List<LuckySwordConfig.EnchantmentEntry> pool = new ArrayList<>(candidates);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            LuckySwordConfig.EnchantmentEntry selected = pickWeightedEnchant(level, pool);
            if (selected == null) {
                break;
            }

            int minLevel = Math.max(1, selected.minLevel());
            int maxLevel = Math.max(minLevel, selected.maxLevel());
            int levelValue = minLevel;
            if (maxLevel > minLevel) {
                levelValue += level.random.nextInt(maxLevel - minLevel + 1);
            }

            Enchantment enchantment = enchantmentRegistry.getValue(selected.id());
            if (enchantment != null) {
                Holder<Enchantment> holder = enchantmentRegistry.wrapAsHolder(enchantment);
                stack.enchant(holder, levelValue);
                appliedIds.add(selected.id().toString());
            }

            pool.remove(selected);
        }

        if (appliedIds.isEmpty()) {
            return;
        }

        int minSeconds = Math.max(1, settings.minDurationSeconds());
        int maxSeconds = Math.max(minSeconds, settings.maxDurationSeconds());
        int durationSeconds = minSeconds;
        if (maxSeconds > minSeconds) {
            durationSeconds += level.random.nextInt(maxSeconds - minSeconds + 1);
        }
        long expiresAt = level.getGameTime() + (durationSeconds * 20L);

        CustomData base = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        stack.set(DataComponents.CUSTOM_DATA, base.update(tag -> {
            Set<String> allIds = parseIdSet(tag.getString(TEMP_ENCHANT_IDS_KEY).orElse(""));
            allIds.addAll(appliedIds);
            tag.putString(TEMP_ENCHANT_IDS_KEY, String.join(",", allIds));

            long currentExpire = tag.getLong(TEMP_ENCHANT_EXPIRES_AT_KEY).orElse(0L);
            tag.putLong(TEMP_ENCHANT_EXPIRES_AT_KEY, Math.max(currentExpire, expiresAt));
        }));

        spawnTemporaryEnchantParticles(level, holderEntity, true);
    }

    private LuckySwordConfig.EnchantmentEntry pickWeightedEnchant(Level level, List<LuckySwordConfig.EnchantmentEntry> entries) {
        int totalWeight = 0;
        for (LuckySwordConfig.EnchantmentEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckySwordConfig.EnchantmentEntry entry : entries) {
            int weight = Math.max(0, entry.weight());
            if (weight <= 0) {
                continue;
            }

            if (roll < weight) {
                return entry;
            }
            roll -= weight;
        }

        return null;
    }

    private Map<String, Integer> currentEnchantmentLevels(ItemStack stack, net.minecraft.core.Registry<Enchantment> enchantmentRegistry) {
        Map<String, Integer> levels = new HashMap<>();
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            Identifier id = enchantmentRegistry.getKey(entry.getKey().value());
            if (id != null) {
                levels.put(id.toString(), Math.max(0, entry.getIntValue()));
            }
        }
        return levels;
    }

    private void removeEnchantments(ItemStack stack, Level level, Set<String> removeIds) {
        if (removeIds == null || removeIds.isEmpty()) {
            return;
        }

        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        List<EnchantLevel> keep = new ArrayList<>();
        for (var entry : current.entrySet()) {
            Identifier id = enchantmentRegistry.getKey(entry.getKey().value());
            if (id == null) {
                continue;
            }
            if (!removeIds.contains(id.toString())) {
                keep.add(new EnchantLevel(id, Math.max(0, entry.getIntValue())));
            }
        }

        stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (EnchantLevel keepEntry : keep) {
            Enchantment enchantment = enchantmentRegistry.getValue(keepEntry.id());
            if (enchantment == null || keepEntry.level() <= 0) {
                continue;
            }

            stack.enchant(enchantmentRegistry.wrapAsHolder(enchantment), keepEntry.level());
        }
    }

    private Set<String> parseIdSet(String csv) {
        Set<String> ids = new HashSet<>();
        if (csv == null || csv.isBlank()) {
            return ids;
        }

        String[] parts = csv.split(",");
        for (String part : parts) {
            String value = part == null ? "" : part.trim();
            if (!value.isBlank()) {
                ids.add(value);
            }
        }
        return ids;
    }

    private void spawnTemporaryEnchantParticles(Level level, Entity holderEntity, boolean applied) {
        if (!(level instanceof ServerLevel serverLevel) || holderEntity == null) {
            return;
        }

        LuckySwordConfig.TemporaryEnchantParticleSettings settings = LuckySwordConfig.temporaryEnchantSettings().effectParticles();
        if (!settings.enabled() || settings.count() <= 0) {
            return;
        }

        serverLevel.sendParticles(
                applied ? settings.applyParticle() : settings.expireParticle(),
                holderEntity.getX(),
                holderEntity.getY() + 0.9,
                holderEntity.getZ(),
                settings.count(),
                settings.spreadX(),
                settings.spreadY(),
                settings.spreadZ(),
                settings.speed()
        );
    }

    private void tryTriggerKnockbackFireTrail(LivingEntity target, LivingEntity attacker) {
        tryTriggerKnockbackFireTrail(target, attacker, false);
    }

    private void tryTriggerKnockbackFireTrail(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || attacker == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckySwordConfig.KnockbackFireTrailSettings settings = LuckySwordConfig.knockbackFireTrailSettings();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        if (settings.igniteTarget() && settings.igniteSeconds() > 0.0F) {
            target.igniteForSeconds(settings.igniteSeconds());
        }

        Vec3 direction = target.position().subtract(attacker.position());
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double dirX;
        double dirZ;
        if (horizontalLength > 0.0001D) {
            dirX = direction.x / horizontalLength;
            dirZ = direction.z / horizontalLength;
        } else {
            Vec3 look = attacker.getLookAngle();
            double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
            if (lookHorizontal <= 0.0001D) {
                dirX = 0.0D;
                dirZ = 1.0D;
            } else {
                dirX = look.x / lookHorizontal;
                dirZ = look.z / lookHorizontal;
            }
        }

        target.push(dirX * settings.knockbackStrength(), settings.upwardBoost(), dirZ * settings.knockbackStrength());
        spawnFireTrailOnTarget(serverLevel, target, settings);
    }

    private void spawnFireTrailOnTarget(ServerLevel level, LivingEntity target, LuckySwordConfig.KnockbackFireTrailSettings settings) {
        double[] previousPos = new double[] { target.getX(), target.getY(0.2D), target.getZ() };
        spawnFireTrailSegment(level, target, settings, previousPos);

        int interval = Math.max(1, settings.trailIntervalTicks());
        for (int delay = interval; delay <= settings.trailDurationTicks(); delay += interval) {
            LuckyScheduler.INSTANCE.schedule(delay, () -> spawnFireTrailSegment(level, target, settings, previousPos));
        }
    }

    private void spawnFireTrailSegment(ServerLevel level, LivingEntity target, LuckySwordConfig.KnockbackFireTrailSettings settings, double[] previousPos) {
        if (target == null || !target.isAlive() || target.isRemoved() || target.level() != level) {
            return;
        }

        double currentX = target.getX();
        double currentY = target.getY(0.2D);
        double currentZ = target.getZ();

        double deltaX = currentX - previousPos[0];
        double deltaY = currentY - previousPos[1];
        double deltaZ = currentZ - previousPos[2];
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        int steps = Math.max(1, (int) Math.ceil(distance * 4.0D));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double x = previousPos[0] + deltaX * t;
            double y = previousPos[1] + deltaY * t;
            double z = previousPos[2] + deltaZ * t;

            level.sendParticles(
                    ParticleTypes.FLAME,
                    x,
                    y,
                    z,
                    settings.particlesPerStep(),
                    settings.spread(),
                    0.08D,
                    settings.spread(),
                    0.01D);

            level.sendParticles(
                    ParticleTypes.SMOKE,
                    x,
                    y,
                    z,
                    Math.max(1, settings.particlesPerStep() / 2),
                    settings.spread() * 0.5D,
                    0.03D,
                    settings.spread() * 0.5D,
                    0.005D);
        }

        previousPos[0] = currentX;
        previousPos[1] = currentY;
        previousPos[2] = currentZ;
    }

    private void tryTriggerMobHitLightning(LivingEntity target, LivingEntity attacker) {
        tryTriggerMobHitLightning(target, attacker, false);
    }

    private void tryTriggerMobHitLightning(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckySwordConfig.LightningSettings settings = LuckySwordConfig.lightningSettings();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.mobHitChance())) {
            return;
        }

        if (settings.igniteTarget() && settings.igniteSeconds() > 0.0F) {
            target.igniteForSeconds(settings.igniteSeconds());
        }

        strikeFollowLightning(serverLevel, target, settings, attacker);
        applyFreezeIfEnabled(target, settings);
        if (settings.damage() > 0.0F) {
            target.hurtServer(serverLevel, serverLevel.damageSources().lightningBolt(), settings.damage());
        }
    }

    private void strikeFollowLightning(ServerLevel level, LivingEntity target, LuckySwordConfig.LightningSettings settings, LivingEntity attacker) {
        strikeLightningOnTarget(level, target, settings.visualOnly(), attacker);

        int followDuration = settings.followDurationTicks();
        int interval = Math.max(1, settings.followIntervalTicks());
        if (followDuration <= 0) {
            return;
        }

        for (int delay = interval; delay <= followDuration; delay += interval) {
            LuckyScheduler.INSTANCE.schedule(delay, () -> strikeLightningOnTarget(level, target, settings.visualOnly(), attacker));
        }
    }

    private void strikeLightningOnTarget(ServerLevel level, LivingEntity target, boolean visualOnly, LivingEntity source) {
        if (target == null || !target.isAlive() || target.isRemoved() || target.level() != level) {
            return;
        }
        strikeLightning(level, target.getX(), target.getY(), target.getZ(), visualOnly, source);
    }

    private void applyFreezeIfEnabled(LivingEntity target, LuckySwordConfig.LightningSettings settings) {
        if (!settings.freezeTargetDuringVisual() || !settings.visualOnly()) {
            return;
        }

        int freezeTicks = Math.max(1, settings.followDurationTicks() + 2);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, freezeTicks, 10, false, false, false));
        target.setDeltaMovement(0.0D, target.getDeltaMovement().y, 0.0D);
    }

    private void strikeLightning(ServerLevel level, double x, double y, double z, boolean visualOnly, LivingEntity source) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }

        bolt.setPos(x, y, z);
        bolt.setVisualOnly(visualOnly);
        if (!visualOnly && source instanceof ServerPlayer player) {
            bolt.setCause(player);
        }
        level.addFreshEntity(bolt);
    }

    private record EnchantLevel(Identifier id, int level) {
    }
}
