package com.luckybreak.item;

import com.luckybreak.events.LuckyScheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LuckyBowItem extends BowItem {

    private static final int MAX_IMPACT_CHECK_TICKS = 200;
    public static final String LUCKY_BOW_ARROW_TAG = "luckybreak_lucky_bow_arrow";
    public static final String LUCKY_BOW_MODE_TAG_PREFIX = "luckybreak_lucky_bow_mode_";
    public static final String LUCKY_BOW_LIGHTNING_TRIGGERED_TAG = "luckybreak_lucky_bow_lightning_triggered";
    public static final String LUCKY_BOW_ENTITY_BLOCK_TRANSFORM_TRIGGERED_TAG = "luckybreak_lucky_bow_entity_block_transform_triggered";
    public static final String LUCKY_BOW_ENTITY_POTION_TRIGGERED_TAG = "luckybreak_lucky_bow_entity_potion_triggered";
    private static final String TEMP_ENCHANT_IDS_KEY = "luckybreak_bow_temp_enchant_ids";
    private static final String TEMP_ENCHANT_EXPIRES_AT_KEY = "luckybreak_bow_temp_enchant_expires_at";
    private static final ThreadLocal<Set<LuckyBowConfig.BowEffectMode>> CURRENT_SHOT_MODES = new ThreadLocal<>();

    public LuckyBowItem(ResourceKey<Item> key) {
        super(LuckyItemVisuals.apply("lucky_bow", new Item.Properties()
                .setId(key)
                .durability(LuckyBowConfig.durability())
            .repairable(Items.GOLD_INGOT)));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        tickTemporaryEnchantments(stack, level, entity);
    }

    public boolean triggerCommandEvent(ItemStack stack, ServerPlayer player, String eventName, BlockPos blockPos, LivingEntity targetMob) {
        if (stack == null || stack.isEmpty() || player == null || eventName == null || eventName.isBlank()) {
            return false;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        String normalized = eventName.trim().toLowerCase(Locale.ROOT);
        BlockPos targetPos = blockPos != null ? blockPos : player.blockPosition().relative(player.getDirection());

        return switch (normalized) {
            case "entity_hit_lightning" -> {
                LivingEntity mob = targetMob;
                if (mob == null) {
                    yield false;
                }
                summonLightningAtTarget(level, mob, player);
                yield true;
            }
            case "block_transform" -> {
                LuckyBowConfig.BlockTransformSettings settings = LuckyBowConfig.blockTransformSettings();
                if (!settings.enabled()) {
                    yield false;
                }
                tryTransformHitBlock(level, targetPos, settings);
                yield true;
            }
            case "fire_spread" -> {
                LuckyBowConfig.FireSpreadSettings settings = LuckyBowConfig.fireSpreadSettings();
                if (!settings.enabled()) {
                    yield false;
                }
                tryFireSpreadForced(level, targetPos, settings);
                yield true;
            }
            case "entity_hit_block_transform" -> {
                if (targetMob != null) {
                    LuckyBowEntityHitEffects.tryEntityHitBlockTransformAtPosition(level, targetMob.blockPosition());
                } else {
                    LuckyBowEntityHitEffects.tryEntityHitBlockTransformAtPosition(level, targetPos);
                }
                yield true;
            }
            case "entity_hit_potion" -> {
                BlockPos pos = targetMob != null ? targetMob.blockPosition() : targetPos;
                applyPotionAtPositionForCommand(level, pos, player);
                yield true;
            }
            case "tnt_trail" -> {
                LuckyBowConfig.TntTrailSettings settings = LuckyBowConfig.tntTrailSettings();
                if (!settings.enabled()) {
                    yield false;
                }
                int count = Math.max(1, Math.min(3, settings.maxCount()));
                Vec3 spawn = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
                for (int i = 0; i < count; i++) {
                    spawnTrailTnt(level, spawn, Vec3.ZERO, settings, player);
                }
                yield true;
            }
            case "chicken_rain" -> {
                tryChickenRainAtPosition(level, targetPos, true);
                yield true;
            }
            case "party_pop" -> {
                tryPartyPopAtPosition(level, targetPos, true);
                yield true;
            }
            case "entity_bounce" -> {
                LivingEntity mob = targetMob;
                if (mob == null) {
                    yield false;
                }
                applyEntityBounce(level, mob, true);
                yield true;
            }
            case "temporary_enchantments" -> {
                tickTemporaryEnchantments(stack, level, player);
                tryTriggerTemporaryEnchantments(stack, level, player, true);
                yield true;
            }
            default -> false;
        };
    }

    private void summonLightningAtTarget(ServerLevel level, LivingEntity target, ServerPlayer player) {
        LuckyBowConfig.EntityHitLightningSettings settings = LuckyBowConfig.entityHitLightningSettings();
        if (!settings.enabled()) {
            return;
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }

        bolt.setPos(target.getX(), target.getY(), target.getZ());
        bolt.setVisualOnly(settings.visualOnly());
        if (!settings.visualOnly()) {
            bolt.setCause(player);
        }
        level.addFreshEntity(bolt);
    }

    private void tryFireSpreadForced(ServerLevel level, BlockPos impactPos, LuckyBowConfig.FireSpreadSettings settings) {
        BlockPos center = resolveTargetPos(level, impactPos);
        if (center == null) {
            center = impactPos;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int requested = minCount + level.random.nextInt(maxCount - minCount + 1);

        List<BlockPos> candidates = findFireSpreadTargets(level, center, settings.searchRadius());
        if (candidates.isEmpty()) {
            return;
        }

        Collections.shuffle(candidates);
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

    private void applyPotionAtPositionForCommand(ServerLevel level, BlockPos centerPos, ServerPlayer source) {
        if (centerPos == null) {
            return;
        }

        LuckyBowConfig.EntityHitPotionSettings settings = LuckyBowConfig.entityHitPotionSettings();
        if (!settings.enabled() || settings.possibleEffects().isEmpty()) {
            return;
        }

        LuckyBowConfig.PotionEntry selected = pickWeightedPotion(level, settings.possibleEffects());
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
        var area = new net.minecraft.world.phys.AABB(centerPos).inflate(2.5D);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive() && entity != source);
        if (nearby.isEmpty()) {
            return;
        }

        LivingEntity target = nearby.get(level.random.nextInt(nearby.size()));
        target.addEffect(new MobEffectInstance(holder, selected.durationTicks(), selected.amplifier()));
    }

    private LuckyBowConfig.PotionEntry pickWeightedPotion(ServerLevel level, List<LuckyBowConfig.PotionEntry> entries) {
        int totalWeight = 0;
        for (LuckyBowConfig.PotionEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyBowConfig.PotionEntry entry : entries) {
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry;
            }
            roll -= weight;
        }

        return entries.getFirst();
    }

    @Override
    public Component getName(ItemStack stack) {
        return LuckyItemVisuals.styleName("lucky_bow", super.getName(stack));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (LuckyBowConfig.infinityWithoutArrows()) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) {
            return false;
        }

        boolean infinityEnabled = LuckyBowConfig.infinityWithoutArrows();
        ItemStack inventoryProjectile = player.getProjectile(stack);

        if (!infinityEnabled && inventoryProjectile.isEmpty()) {
            return false;
        }

        ItemStack projectile = new ItemStack(Items.ARROW);
        int useTicks = this.getUseDuration(stack, livingEntity) - timeLeft;
        float power = BowItem.getPowerForTime(useTicks);
        if ((double) power < 0.1D) {
            return false;
        }

        tickTemporaryEnchantments(stack, level, player);
        tryTriggerTemporaryEnchantments(stack, level, player);

        List<ItemStack> ammo = draw(stack, projectile, player);

        if (level instanceof ServerLevel serverLevel && !ammo.isEmpty()) {
            Set<LuckyBowConfig.BowEffectMode> selectedModes = pickEffectModes(level);
            CURRENT_SHOT_MODES.set(selectedModes);
            try {
                this.shoot(
                        serverLevel,
                        player,
                        player.getUsedItemHand(),
                        stack,
                        ammo,
                        power * 3.0F,
                        1.0F,
                        power == 1.0F,
                        null
                );
            } finally {
                CURRENT_SHOT_MODES.remove();
            }
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F
        );
        player.awardStat(Stats.ITEM_USED.get(this));

        if (!infinityEnabled && !player.getAbilities().instabuild) {
            inventoryProjectile.shrink(1);
            if (inventoryProjectile.isEmpty()) {
                player.getInventory().removeItem(inventoryProjectile);
            }
        }

        return true;
    }

    @Override
    protected void shootProjectile(LivingEntity livingEntity, Projectile projectile, int i, float f, float g, float h, LivingEntity livingEntity2) {
        super.shootProjectile(livingEntity, projectile, i, f, g, h, livingEntity2);
        if (projectile instanceof AbstractArrow arrow) {
            arrow.addTag(LUCKY_BOW_ARROW_TAG);
            Set<LuckyBowConfig.BowEffectMode> modes = CURRENT_SHOT_MODES.get();
            if (modes == null || modes.isEmpty()) {
                modes = getArrowModes(arrow);
            }
            if (modes.isEmpty()) {
                modes = EnumSet.of(LuckyBowConfig.BowEffectMode.NORMAL_ARROW);
            }

            for (LuckyBowConfig.BowEffectMode mode : modes) {
                arrow.addTag(LUCKY_BOW_MODE_TAG_PREFIX + mode.id());
            }

            if (LuckyBowConfig.nonCollectableFiredArrows()) {
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            }
            // Apply Power V equivalent base damage (default 2.0 + Power V bonus 3.0 = 5.0)
            arrow.setBaseDamage(5.0);
            if (modes.contains(LuckyBowConfig.BowEffectMode.TNT_TRAIL) && livingEntity instanceof Player player) {
                maybeStartTntTrail(arrow, player);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.BLOCK_TRANSFORM)) {
                scheduleBlockTransformCheck(arrow, 0);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.FIRE_SPREAD)) {
                scheduleFireSpreadCheck(arrow, 0);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.ENTITY_HIT_LIGHTNING)) {
                scheduleEntityHitLightningFallbackCheck(arrow, 0);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.ENTITY_HIT_BLOCK_TRANSFORM)) {
                scheduleEntityHitBlockTransformFallbackCheck(arrow, 0);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.ENTITY_HIT_POTION)) {
                scheduleEntityHitPotionFallbackCheck(arrow, 0);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.CHICKEN_RAIN)) {
                scheduleChickenRainCheck(arrow, 0);
            }
            if (modes.contains(LuckyBowConfig.BowEffectMode.PARTY_POP)) {
                schedulePartyPopCheck(arrow, 0);
            }
        }
    }

    public static boolean hasLightningTriggered(AbstractArrow arrow) {
        return arrow != null && arrow.getTags().contains(LUCKY_BOW_LIGHTNING_TRIGGERED_TAG);
    }

    public static void markLightningTriggered(AbstractArrow arrow) {
        if (arrow != null) {
            arrow.addTag(LUCKY_BOW_LIGHTNING_TRIGGERED_TAG);
        }
    }

    public static boolean hasEntityHitBlockTransformTriggered(AbstractArrow arrow) {
        return arrow != null && arrow.getTags().contains(LUCKY_BOW_ENTITY_BLOCK_TRANSFORM_TRIGGERED_TAG);
    }

    public static void markEntityHitBlockTransformTriggered(AbstractArrow arrow) {
        if (arrow != null) {
            arrow.addTag(LUCKY_BOW_ENTITY_BLOCK_TRANSFORM_TRIGGERED_TAG);
        }
    }

    public static boolean hasEntityHitPotionTriggered(AbstractArrow arrow) {
        return arrow != null && arrow.getTags().contains(LUCKY_BOW_ENTITY_POTION_TRIGGERED_TAG);
    }

    public static void markEntityHitPotionTriggered(AbstractArrow arrow) {
        if (arrow != null) {
            arrow.addTag(LUCKY_BOW_ENTITY_POTION_TRIGGERED_TAG);
        }
    }

    public static LuckyBowConfig.BowEffectMode getArrowMode(AbstractArrow arrow) {
        Set<LuckyBowConfig.BowEffectMode> modes = getArrowModes(arrow);
        if (modes.isEmpty()) {
            return LuckyBowConfig.BowEffectMode.NORMAL_ARROW;
        }
        return modes.iterator().next();
    }

    public static Set<LuckyBowConfig.BowEffectMode> getArrowModes(AbstractArrow arrow) {
        EnumSet<LuckyBowConfig.BowEffectMode> modes = EnumSet.noneOf(LuckyBowConfig.BowEffectMode.class);
        if (arrow == null) {
            return modes;
        }

        for (String tag : arrow.getTags()) {
            if (!tag.startsWith(LUCKY_BOW_MODE_TAG_PREFIX)) {
                continue;
            }

            LuckyBowConfig.BowEffectMode mode = LuckyBowConfig.BowEffectMode.fromId(tag.substring(LUCKY_BOW_MODE_TAG_PREFIX.length()));
            if (mode != null) {
                modes.add(mode);
            }
        }

        return modes;
    }

    private Set<LuckyBowConfig.BowEffectMode> pickEffectModes(Level level) {
        List<LuckyBowConfig.WeightedEffectModeEntry> pool = LuckyBowConfig.effectModePool();
        if (pool == null || pool.isEmpty()) {
            return EnumSet.of(LuckyBowConfig.BowEffectMode.NORMAL_ARROW);
        }

        List<LuckyBowConfig.WeightedEffectModeEntry> available = new ArrayList<>();
        for (LuckyBowConfig.WeightedEffectModeEntry entry : pool) {
            if (entry != null && entry.mode() != null && entry.weight() > 0) {
                available.add(entry);
            }
        }
        if (available.isEmpty()) {
            return EnumSet.of(LuckyBowConfig.BowEffectMode.NORMAL_ARROW);
        }

        LuckyBowConfig.EffectCombinationSettings combination = LuckyBowConfig.effectCombinationSettings();
        int targetCount;
        if (!combination.allowMultiple()) {
            targetCount = 1;
        } else {
            int min = Math.max(0, combination.minEffects());
            int max = Math.max(min, combination.maxEffects());
            min = Math.min(min, available.size());
            max = Math.min(max, available.size());
            targetCount = pickWeightedEffectCount(level, min, max, combination.countWeights());
        }

        if (targetCount <= 0) {
            return EnumSet.of(LuckyBowConfig.BowEffectMode.NORMAL_ARROW);
        }

        EnumSet<LuckyBowConfig.BowEffectMode> selected = EnumSet.noneOf(LuckyBowConfig.BowEffectMode.class);
        for (int picks = 0; picks < targetCount && !available.isEmpty(); picks++) {
            LuckyBowConfig.WeightedEffectModeEntry choice = pickWeightedEffectEntry(level, available);
            if (choice == null) {
                break;
            }

            selected.add(choice.mode());
            available.remove(choice);

            List<LuckyBowConfig.ModePair> pairs = combination.mutuallyExclusivePairs();
            if (combination.enforceMutualExclusions() && pairs != null && !pairs.isEmpty()) {
                EnumSet<LuckyBowConfig.BowEffectMode> blocked = EnumSet.noneOf(LuckyBowConfig.BowEffectMode.class);
                for (LuckyBowConfig.ModePair pair : pairs) {
                    if (pair == null || pair.modeA() == null || pair.modeB() == null) {
                        continue;
                    }
                    if (selected.contains(pair.modeA())) {
                        blocked.add(pair.modeB());
                    }
                    if (selected.contains(pair.modeB())) {
                        blocked.add(pair.modeA());
                    }
                }

                if (!blocked.isEmpty()) {
                    available.removeIf(entry -> blocked.contains(entry.mode()));
                }
            }
        }

        if (selected.isEmpty()) {
            selected.add(LuckyBowConfig.BowEffectMode.NORMAL_ARROW);
        }

        return selected;
    }

    private int pickWeightedEffectCount(Level level, int min, int max, List<LuckyBowConfig.EffectCountWeight> weights) {
        if (max < min) {
            return min;
        }

        List<LuckyBowConfig.EffectCountWeight> valid = new ArrayList<>();
        int totalWeight = 0;
        if (weights != null) {
            for (LuckyBowConfig.EffectCountWeight entry : weights) {
                if (entry == null) {
                    continue;
                }
                if (entry.count() < min || entry.count() > max) {
                    continue;
                }
                int weight = Math.max(0, entry.weight());
                if (weight <= 0) {
                    continue;
                }
                totalWeight += weight;
                valid.add(entry);
            }
        }

        if (totalWeight <= 0 || valid.isEmpty()) {
            return min + level.random.nextInt(max - min + 1);
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyBowConfig.EffectCountWeight entry : valid) {
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry.count();
            }
            roll -= weight;
        }

        return valid.getFirst().count();
    }

    private LuckyBowConfig.WeightedEffectModeEntry pickWeightedEffectEntry(Level level, List<LuckyBowConfig.WeightedEffectModeEntry> entries) {
        int totalWeight = 0;
        for (LuckyBowConfig.WeightedEffectModeEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyBowConfig.WeightedEffectModeEntry entry : entries) {
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry;
            }
            roll -= weight;
        }

        return entries.getFirst();
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity livingEntity, ItemStack bowStack, ItemStack ammoStack, boolean isCrit) {
        Set<LuckyBowConfig.BowEffectMode> modes = CURRENT_SHOT_MODES.get();
        if (modes == null) {
            modes = EnumSet.noneOf(LuckyBowConfig.BowEffectMode.class);
        }
        ItemStack selectedAmmo = pickArrowAmmoForModes(level, modes);
        Projectile projectile = super.createProjectile(level, livingEntity, bowStack, selectedAmmo, isCrit);
        if (projectile instanceof AbstractArrow arrow && LuckyBowConfig.nonCollectableFiredArrows()) {
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        }
        return projectile;
    }

    private ItemStack pickArrowAmmoForModes(Level level, Set<LuckyBowConfig.BowEffectMode> modes) {
        if (modes == null || !modes.contains(LuckyBowConfig.BowEffectMode.RANDOM_ARROW_TYPES)) {
            return new ItemStack(Items.ARROW);
        }

        LuckyBowConfig.RandomArrowTypeSettings settings = LuckyBowConfig.randomArrowTypeSettings();
        if (!settings.enabled()) {
            return new ItemStack(Items.ARROW);
        }

        if (level.getRandom().nextFloat() > settings.chance()) {
            return new ItemStack(Items.ARROW);
        }

        LuckyBowConfig.ArrowType type = pickWeightedArrowType(level, settings.types());
        if (type == LuckyBowConfig.ArrowType.SPECTRAL) {
            return new ItemStack(Items.SPECTRAL_ARROW);
        }

        if (type == LuckyBowConfig.ArrowType.TIPPED) {
            ItemStack tipped = createRandomTippedArrow(level);
            if (!tipped.isEmpty()) {
                return tipped;
            }
        }

        return new ItemStack(Items.ARROW);
    }

    private LuckyBowConfig.ArrowType pickWeightedArrowType(Level level, List<LuckyBowConfig.WeightedArrowTypeEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return LuckyBowConfig.ArrowType.NORMAL;
        }

        int totalWeight = 0;
        for (LuckyBowConfig.WeightedArrowTypeEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return LuckyBowConfig.ArrowType.NORMAL;
        }

        int roll = level.getRandom().nextInt(totalWeight);
        for (LuckyBowConfig.WeightedArrowTypeEntry entry : entries) {
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry.type();
            }
            roll -= weight;
        }

        return LuckyBowConfig.ArrowType.NORMAL;
    }

    private ItemStack createRandomTippedArrow(Level level) {
        List<Holder<Potion>> candidates = new ArrayList<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion);
            if (potionId == null || "empty".equals(potionId.getPath())) {
                continue;
            }
            candidates.add(BuiltInRegistries.POTION.wrapAsHolder(potion));
        }

        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Holder<Potion> potion = candidates.get(level.getRandom().nextInt(candidates.size()));
        return PotionContents.createItemStack(Items.TIPPED_ARROW, potion);
    }

    private void maybeStartTntTrail(AbstractArrow arrow, Player player) {
        if (!(arrow.level() instanceof ServerLevel level)) {
            return;
        }

        LuckyBowConfig.TntTrailSettings settings = LuckyBowConfig.tntTrailSettings();
        if (!settings.enabled()) {
            return;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int count = minCount + level.getRandom().nextInt(maxCount - minCount + 1);
        if (count <= 0) {
            return;
        }

        Vec3 shooterOrigin = player.getEyePosition();
        scheduleTntTrailFollowArrow(arrow, player, settings, shooterOrigin, count, null, 0);
    }

    private void scheduleTntTrailFollowArrow(
            AbstractArrow arrow,
            Player player,
            LuckyBowConfig.TntTrailSettings settings,
            Vec3 shooterOrigin,
            int remaining,
            Vec3 lastSpawn,
            int ticksElapsed
    ) {
        if (!(arrow.level() instanceof ServerLevel level) || remaining <= 0) {
            return;
        }

        double startDistance = Math.max(3.0D, settings.startDistance());
        double spacing = Math.max(0.5D, settings.spacing());
        int maxTicks = 240;

        LuckyScheduler.INSTANCE.schedule(1, () -> {
            if (!arrow.isAlive()) {
                return;
            }

            Vec3 pos = arrow.position();
            boolean onGround = arrow.onGround();
            double traveledFromShooter = pos.distanceTo(shooterOrigin);

            int remainingAfter = remaining;
            Vec3 lastSpawnAfter = lastSpawn;

            boolean canSpawnByDistance = traveledFromShooter >= startDistance;
            boolean canSpawnBySpacing = lastSpawn == null || pos.distanceTo(lastSpawn) >= spacing;
            if (canSpawnByDistance && (canSpawnBySpacing || onGround)) {
                spawnTrailTnt(level, pos, arrow.getDeltaMovement(), settings, player);
                remainingAfter--;
                lastSpawnAfter = pos;
            }

            if (remainingAfter <= 0) {
                return;
            }

            if (onGround || ticksElapsed + 1 >= maxTicks) {
                if (canSpawnByDistance) {
                    for (int i = 0; i < remainingAfter; i++) {
                        spawnTrailTnt(level, pos, arrow.getDeltaMovement(), settings, player);
                    }
                }
                return;
            }

            scheduleTntTrailFollowArrow(arrow, player, settings, shooterOrigin, remainingAfter, lastSpawnAfter, ticksElapsed + 1);
        });
    }

    private void scheduleFireSpreadCheck(Projectile projectile, int ticksElapsed) {
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyBowConfig.FireSpreadSettings settings = LuckyBowConfig.fireSpreadSettings();
        if (!settings.enabled()) {
            return;
        }

        LuckyScheduler.INSTANCE.schedule(1, () -> {
            if (!arrow.isAlive()) {
                return;
            }

            if (hasBlockImpact(arrow, ticksElapsed)) {
                tryFireSpread(serverLevel, arrow.blockPosition(), settings);
                return;
            }

            if (ticksElapsed + 1 < MAX_IMPACT_CHECK_TICKS) {
                scheduleFireSpreadCheck(arrow, ticksElapsed + 1);
            } else {
                tryFireSpread(serverLevel, arrow.blockPosition(), settings);
            }
        });
    }

    private void tryFireSpread(ServerLevel level, BlockPos impactPos, LuckyBowConfig.FireSpreadSettings settings) {
        if (level.getRandom().nextFloat() > settings.chance()) {
            return;
        }

        BlockPos center = resolveTargetPos(level, impactPos);
        if (center == null) {
            center = impactPos;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int requested = minCount + level.getRandom().nextInt(maxCount - minCount + 1);

        List<BlockPos> candidates = findFireSpreadTargets(level, center, settings.searchRadius());
        if (candidates.isEmpty()) {
            return;
        }

        Collections.shuffle(candidates);
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

    private List<BlockPos> findFireSpreadTargets(ServerLevel level, BlockPos center, int radius) {
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

    private void spawnTrailTnt(ServerLevel level, Vec3 pos, Vec3 arrowVelocity, LuckyBowConfig.TntTrailSettings settings, Player owner) {
        Vec3 velocity = arrowVelocity == null ? Vec3.ZERO : arrowVelocity;
        double vx = velocity.x * 0.25 + (level.random.nextDouble() - 0.5) * 0.02;
        double vy = velocity.y * 0.10 - 0.04;
        double vz = velocity.z * 0.25 + (level.random.nextDouble() - 0.5) * 0.02;

        PrimedTnt tnt = new PrimedTnt(level, pos.x, pos.y, pos.z, owner);
        tnt.setFuse(settings.fuseTicks());
        tnt.setDeltaMovement(vx, vy, vz);
        level.addFreshEntity(tnt);
    }

    private void scheduleBlockTransformCheck(Projectile projectile, int ticksElapsed) {
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyBowConfig.BlockTransformSettings settings = LuckyBowConfig.blockTransformSettings();
        if (!settings.enabled()) {
            return;
        }

        LuckyScheduler.INSTANCE.schedule(1, () -> {
            if (!arrow.isAlive()) {
                return;
            }

            if (hasBlockImpact(arrow, ticksElapsed)) {
                tryTransformHitBlock(serverLevel, arrow.blockPosition(), settings);
                return;
            }

            if (ticksElapsed + 1 < MAX_IMPACT_CHECK_TICKS) {
                scheduleBlockTransformCheck(arrow, ticksElapsed + 1);
            } else {
                tryTransformHitBlock(serverLevel, arrow.blockPosition(), settings);
            }
        });
    }

    private void scheduleEntityHitLightningFallbackCheck(Projectile projectile, int ticksElapsed) {
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyBowConfig.EntityHitLightningSettings settings = LuckyBowConfig.entityHitLightningSettings();
        if (!settings.enabled()) {
            return;
        }

        LuckyScheduler.INSTANCE.schedule(1, () -> {
            if (!arrow.isAlive()) {
                return;
            }

            if (hasBlockImpact(arrow, ticksElapsed)) {
                trySummonLightningAtArrowLanding(serverLevel, arrow, settings);
                return;
            }

            if (ticksElapsed + 1 < MAX_IMPACT_CHECK_TICKS) {
                scheduleEntityHitLightningFallbackCheck(arrow, ticksElapsed + 1);
            } else {
                trySummonLightningAtArrowLanding(serverLevel, arrow, settings);
            }
        });
    }

    private void scheduleEntityHitBlockTransformFallbackCheck(Projectile projectile, int ticksElapsed) {
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyBowConfig.EntityHitBlockTransformSettings settings = LuckyBowConfig.entityHitBlockTransformSettings();
        if (!settings.enabled()) {
            return;
        }

        LuckyScheduler.INSTANCE.schedule(1, () -> {
            if (!arrow.isAlive()) {
                return;
            }

            if (hasBlockImpact(arrow, ticksElapsed)) {
                tryEntityHitBlockTransformAtArrowLanding(serverLevel, arrow);
                return;
            }

            if (ticksElapsed + 1 < MAX_IMPACT_CHECK_TICKS) {
                scheduleEntityHitBlockTransformFallbackCheck(arrow, ticksElapsed + 1);
            } else {
                tryEntityHitBlockTransformAtArrowLanding(serverLevel, arrow);
            }
        });
    }

    private void tryEntityHitBlockTransformAtArrowLanding(ServerLevel level, AbstractArrow arrow) {
        if (hasEntityHitBlockTransformTriggered(arrow)) {
            return;
        }

        BlockPos impact = arrow.blockPosition();
        BlockPos strikePos = resolveTargetPos(level, impact);
        if (strikePos == null) {
            strikePos = impact;
        }

        LuckyBowEntityHitEffects.tryEntityHitBlockTransformAtPosition(level, strikePos);
        markEntityHitBlockTransformTriggered(arrow);
    }

    private void scheduleEntityHitPotionFallbackCheck(Projectile projectile, int ticksElapsed) {
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyBowConfig.EntityHitPotionSettings settings = LuckyBowConfig.entityHitPotionSettings();
        if (!settings.enabled()) {
            return;
        }

        LuckyScheduler.INSTANCE.schedule(1, () -> {
            if (!arrow.isAlive()) {
                return;
            }

            if (hasBlockImpact(arrow, ticksElapsed)) {
                tryEntityHitPotionAtArrowLanding(serverLevel, arrow);
                return;
            }

            if (ticksElapsed + 1 < MAX_IMPACT_CHECK_TICKS) {
                scheduleEntityHitPotionFallbackCheck(arrow, ticksElapsed + 1);
            } else {
                tryEntityHitPotionAtArrowLanding(serverLevel, arrow);
            }
        });
    }

    private void tryEntityHitPotionAtArrowLanding(ServerLevel level, AbstractArrow arrow) {
        if (hasEntityHitPotionTriggered(arrow)) {
            return;
        }

        BlockPos impact = arrow.blockPosition();
        BlockPos strikePos = resolveTargetPos(level, impact);
        if (strikePos == null) {
            strikePos = impact;
        }

        LuckyBowEntityHitEffects.tryApplyPotionAtPosition(level, strikePos, arrow);
        markEntityHitPotionTriggered(arrow);
    }

    private void scheduleChickenRainCheck(Projectile projectile, int ticksElapsed) {
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyBowConfig.ChickenRainSettings settings = LuckyBowConfig.chickenRainSettings();
        if (!settings.enabled()) {
            return;
        }

        LuckyScheduler.INSTANCE.schedule(1, () -> {
            if (!arrow.isAlive()) {
                return;
            }

            if (hasBlockImpact(arrow, ticksElapsed)) {
                tryChickenRainAtPosition(serverLevel, arrow.blockPosition(), false);
                return;
            }

            if (ticksElapsed + 1 < MAX_IMPACT_CHECK_TICKS) {
                scheduleChickenRainCheck(arrow, ticksElapsed + 1);
            } else {
                tryChickenRainAtPosition(serverLevel, arrow.blockPosition(), false);
            }
        });
    }

    private void schedulePartyPopCheck(Projectile projectile, int ticksElapsed) {
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyBowConfig.PartyPopSettings settings = LuckyBowConfig.partyPopSettings();
        if (!settings.enabled()) {
            return;
        }

        LuckyScheduler.INSTANCE.schedule(1, () -> {
            if (!arrow.isAlive()) {
                return;
            }

            if (hasBlockImpact(arrow, ticksElapsed)) {
                tryPartyPopAtPosition(serverLevel, arrow.blockPosition(), false);
                return;
            }

            if (ticksElapsed + 1 < MAX_IMPACT_CHECK_TICKS) {
                schedulePartyPopCheck(arrow, ticksElapsed + 1);
            } else {
                tryPartyPopAtPosition(serverLevel, arrow.blockPosition(), false);
            }
        });
    }

    private void applyEntityBounce(ServerLevel level, LivingEntity target, boolean force) {
        LuckyBowConfig.EntityBounceSettings settings = LuckyBowConfig.entityBounceSettings();
        if (!settings.enabled() || (!force && level.getRandom().nextFloat() > settings.chance())) {
            return;
        }

        Vec3 current = target.getDeltaMovement();
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

    private void tryPartyPopAtPosition(ServerLevel level, BlockPos impactPos, boolean force) {
        LuckyBowConfig.PartyPopSettings settings = LuckyBowConfig.partyPopSettings();
        if (!settings.enabled() || (!force && level.getRandom().nextFloat() > settings.chance())) {
            return;
        }

        BlockPos center = resolveTargetPos(level, impactPos);
        if (center == null) {
            center = impactPos;
        }

        double x = center.getX() + 0.5D;
        double y = center.getY() + 0.9D;
        double z = center.getZ() + 0.5D;
        int particleCount = Math.max(0, settings.particleCount());

        if (particleCount > 0) {
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
    }

    private void tryChickenRainAtPosition(ServerLevel level, BlockPos impactPos, boolean force) {
        LuckyBowConfig.ChickenRainSettings settings = LuckyBowConfig.chickenRainSettings();
        if (!settings.enabled() || (!force && level.getRandom().nextFloat() > settings.chance())) {
            return;
        }

        BlockPos center = resolveTargetPos(level, impactPos);
        if (center == null) {
            center = impactPos;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int count = minCount + level.getRandom().nextInt(maxCount - minCount + 1);

        List<BlockPos> spots = findFireSpreadTargets(level, center, settings.searchRadius());
        if (spots.isEmpty()) {
            spots = List.of(center.above());
        }

        Collections.shuffle(spots);
        int spawned = 0;
        for (BlockPos spot : spots) {
            if (spawned >= count) {
                break;
            }

            var chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
            if (chicken == null) {
                continue;
            }

            chicken.setPos(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
            if (!level.noCollision(chicken)) {
                continue;
            }

            level.addFreshEntity(chicken);
            spawned++;
        }

        if (spawned > 0) {
            level.sendParticles(ParticleTypes.CLOUD,
                    center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D,
                    Math.max(8, spawned * 6),
                    0.5D, 0.25D, 0.5D, 0.02D);
        }
    }

    private void trySummonLightningAtArrowLanding(ServerLevel level, AbstractArrow arrow, LuckyBowConfig.EntityHitLightningSettings settings) {
        if (hasLightningTriggered(arrow)) {
            return;
        }

        BlockPos impact = arrow.blockPosition();
        BlockPos strikePos = resolveTargetPos(level, impact);
        if (strikePos == null) {
            strikePos = impact;
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }

        bolt.setPos(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
        bolt.setVisualOnly(settings.visualOnly());
        if (!settings.visualOnly() && arrow.getOwner() instanceof net.minecraft.server.level.ServerPlayer player) {
            bolt.setCause(player);
        }
        level.addFreshEntity(bolt);
        markLightningTriggered(arrow);
    }

    private boolean hasBlockImpact(AbstractArrow arrow, int ticksElapsed) {
        if (arrow.onGround() || arrow.horizontalCollision || arrow.verticalCollision) {
            return true;
        }

        if (ticksElapsed < 2) {
            return false;
        }

        return arrow.getDeltaMovement().lengthSqr() < 1.0E-4D;
    }

    private void tryTransformHitBlock(ServerLevel level, BlockPos impactPos, LuckyBowConfig.BlockTransformSettings settings) {
        if (settings.possibleBlocks().isEmpty()) {
            return;
        }

        BlockPos center = resolveTargetPos(level, impactPos);
        if (center == null) {
            return;
        }

        List<BlockPos> targets = findSpreadTargets(level, center, settings.searchRadius(), settings.transformCount());
        if (targets.isEmpty()) {
            targets = List.of(center);
        }

        for (BlockPos targetPos : targets) {
            BlockState currentState = level.getBlockState(targetPos);
            if (currentState.isAir()) {
                continue;
            }

            Block replacement = pickWeightedBlock(level, settings.possibleBlocks(), currentState.getBlock());
            if (replacement == null || replacement == currentState.getBlock()) {
                continue;
            }

            BlockState replacementState = replacement.defaultBlockState();
            level.setBlock(targetPos, replacementState, 3);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
                    8, 0.4, 0.4, 0.4, 0.05);
        }
    }

    private List<BlockPos> findSpreadTargets(ServerLevel level, BlockPos center, int radius, int targetCount) {
        if (center == null) {
            return List.of();
        }

        int scanRadius = Math.max(0, radius);
        int requested = Math.max(1, targetCount);
        List<BlockPos> result = new ArrayList<>(requested);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        BlockPos start = center.immutable();
        queue.add(start);
        visited.add(start);

        List<Direction> directions = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN));

        while (!queue.isEmpty() && result.size() < requested) {
            BlockPos current = queue.removeFirst();

            int dx = current.getX() - center.getX();
            int dz = current.getZ() - center.getZ();
            if ((dx * dx) + (dz * dz) > (scanRadius * scanRadius)) {
                continue;
            }

            if (!level.getBlockState(current).isAir()) {
                result.add(current);
            }

            Collections.shuffle(directions);
            for (Direction direction : directions) {
                BlockPos next = current.relative(direction).immutable();
                if (!visited.add(next)) {
                    continue;
                }

                int nextDx = next.getX() - center.getX();
                int nextDz = next.getZ() - center.getZ();
                if ((nextDx * nextDx) + (nextDz * nextDz) > (scanRadius * scanRadius)) {
                    continue;
                }

                queue.addLast(next);
            }
        }

        return result;
    }

    private Block pickWeightedBlock(Level level, List<LuckyBowConfig.WeightedBlockEntry> entries, Block disallowed) {
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

        int roll = level.random.nextInt(totalWeight);
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

        for (LuckyBowConfig.WeightedBlockEntry entry : entries) {
            if (entry.block() != disallowed) {
                return entry.block();
            }
        }

        return pickWeightedBlock(level, entries);
    }

    private Block pickWeightedBlock(Level level, List<LuckyBowConfig.WeightedBlockEntry> entries) {
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

        int roll = level.random.nextInt(totalWeight);
        for (LuckyBowConfig.WeightedBlockEntry entry : entries) {
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry.block();
            }
            roll -= weight;
        }

        return entries.getFirst().block();
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

        LuckyBowConfig.TemporaryEnchantSettings settings = LuckyBowConfig.temporaryEnchantSettings();
        if (!settings.enabled() || settings.entries().isEmpty()) {
            return;
        }

        if (!force && level.random.nextFloat() > settings.chance()) {
            return;
        }

        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, Integer> currentLevels = currentEnchantmentLevels(stack, enchantmentRegistry);

        List<LuckyBowConfig.EnchantmentEntry> candidates = new ArrayList<>();
        for (LuckyBowConfig.EnchantmentEntry entry : settings.entries()) {
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
        List<LuckyBowConfig.EnchantmentEntry> pool = new ArrayList<>(candidates);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            LuckyBowConfig.EnchantmentEntry selected = pickWeightedEnchant(level, pool);
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

    private LuckyBowConfig.EnchantmentEntry pickWeightedEnchant(Level level, List<LuckyBowConfig.EnchantmentEntry> entries) {
        int totalWeight = 0;
        for (LuckyBowConfig.EnchantmentEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyBowConfig.EnchantmentEntry entry : entries) {
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
            ResourceLocation id = enchantmentRegistry.getKey(entry.getKey().value());
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
            ResourceLocation id = enchantmentRegistry.getKey(entry.getKey().value());
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

        LuckyBowConfig.TemporaryEnchantParticleSettings settings = LuckyBowConfig.temporaryEnchantSettings().effectParticles();
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

    private record EnchantLevel(ResourceLocation id, int level) {
    }

    private BlockPos resolveTargetPos(Level level, BlockPos impactPos) {
        if (impactPos == null) {
            return null;
        }

        BlockState atImpact = level.getBlockState(impactPos);
        if (!atImpact.isAir()) {
            return impactPos;
        }

        for (Direction direction : Direction.values()) {
            BlockPos adjacent = impactPos.relative(direction);
            if (!level.getBlockState(adjacent).isAir()) {
                return adjacent;
            }
        }

        return null;
    }
}
