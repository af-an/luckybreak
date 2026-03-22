package com.luckybreak.item;

import com.luckybreak.ModBlocks;
import com.luckybreak.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.minecraft.world.level.block.Block.popResource;

public class LuckyPickaxeItem extends Item {

    private static final String ITEM_ID = "lucky_pickaxe";
    private static final String TEMP_ENCHANT_IDS_KEY = "luckybreak_pickaxe_temp_enchant_ids";
    private static final String TEMP_ENCHANT_EXPIRES_AT_KEY = "luckybreak_pickaxe_temp_enchant_expires_at";

    public LuckyPickaxeItem(ResourceKey<Item> key) {
        super(LuckyItemVisuals.apply(ITEM_ID, new Item.Properties()
                .setId(key)
            .pickaxe(LuckyPickaxeConfig.miningTier(), LuckyPickaxeConfig.attackDamage(), LuckyPickaxeConfig.attackSpeed())
            .durability(LuckyPickaxeConfig.durability())
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
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        boolean result = super.mineBlock(stack, level, state, pos, miningEntity);

        if (level.isClientSide() || state.isAir()) {
            return result;
        }

        tickTemporaryEnchantments(stack, level, miningEntity);
        tryTriggerTemporaryEnchantments(stack, level, miningEntity);

        int eventCount = 11;
        int start = level.random.nextInt(eventCount);
        for (int i = 0; i < eventCount; i++) {
            int eventType = (start + i) % eventCount;
            boolean triggered = switch (eventType) {
                case 0 -> tryTriggerTntTransform(level, pos, miningEntity);
                case 1 -> tryTriggerBedrockTransform(level, pos);
                case 2 -> tryTriggerLuckyBlockTransform(level, pos);
                case 3 -> tryTriggerBlockTransform(level, pos);
                case 4 -> tryTriggerGoldenHenSpawn(level, pos, miningEntity);
                case 5 -> tryTriggerBonusDrop(level, pos);
                case 6 -> tryTriggerXpBurst(level, miningEntity);
                case 7 -> tryTriggerHostileSpawn(level, pos, miningEntity);
                case 8 -> tryTriggerFriendlySpawn(level, pos, miningEntity);
                case 9 -> tryTriggerOreVeinBurst(level, pos);
                default -> tryTriggerSeismicBurst(level, pos, miningEntity);
            };

            if (triggered) {
                break;
            }
        }

        return result;
    }

    public boolean triggerCommandEvent(ItemStack stack, ServerPlayer player, String eventName, BlockPos blockPos, LivingEntity targetMob) {
        if (stack == null || stack.isEmpty() || player == null || eventName == null || eventName.isBlank()) {
            return false;
        }

        String normalized = eventName.trim().toLowerCase(Locale.ROOT);
        BlockPos targetPos = blockPos != null ? blockPos : player.blockPosition().relative(player.getDirection());
        return switch (normalized) {
            case "tnt_transform" -> tryTriggerTntTransform(player.level(), targetPos, player);
            case "bedrock_transform" -> tryTriggerBedrockTransform(player.level(), targetPos);
            case "lucky_block_transform" -> tryTriggerLuckyBlockTransform(player.level(), targetPos);
            case "block_transform" -> tryTriggerBlockTransform(player.level(), targetPos);
            case "golden_hen_spawn" -> tryTriggerGoldenHenSpawn(player.level(), targetPos, player);
            case "bonus_drops" -> tryTriggerBonusDrop(player.level(), targetPos);
            case "xp_burst" -> tryTriggerXpBurst(player.level(), player);
            case "hostile_spawn" -> tryTriggerHostileSpawn(player.level(), targetPos, player);
            case "friendly_spawn" -> tryTriggerFriendlySpawn(player.level(), targetPos, player);
            case "ore_vein_burst" -> tryTriggerOreVeinBurst(player.level(), targetPos, true);
            case "seismic_burst" -> tryTriggerSeismicBurst(player.level(), targetPos, player, true);
            case "temporary_enchantments" -> {
                tickTemporaryEnchantments(stack, player.level(), player);
                tryTriggerTemporaryEnchantments(stack, player.level(), player, true);
                yield true;
            }
            default -> false;
        };
    }

    private boolean tryTriggerOreVeinBurst(Level level, BlockPos pos) {
        return tryTriggerOreVeinBurst(level, pos, false);
    }

    private boolean tryTriggerOreVeinBurst(Level level, BlockPos pos, boolean force) {
        LuckyPickaxeConfig.OreVeinBurstSettings settings = LuckyPickaxeConfig.oreVeinBurstSettings();
        if (!settings.enabled() || (!force && level.random.nextFloat() > settings.chance())) {
            return false;
        }

        int rolls = settings.minRolls();
        if (settings.maxRolls() > settings.minRolls()) {
            rolls += level.random.nextInt(settings.maxRolls() - settings.minRolls() + 1);
        }

        boolean droppedAny = false;
        for (int i = 0; i < rolls; i++) {
            LuckyPickaxeConfig.WeightedDropEntry selected = pickWeightedOreEntry(level, settings.entries());
            if (selected == null || selected.weight() <= 0) {
                continue;
            }

            int amount = selected.minCount();
            if (selected.maxCount() > selected.minCount()) {
                amount += level.random.nextInt(selected.maxCount() - selected.minCount() + 1);
            }

            popResource(level, pos, new ItemStack(selected.item(), amount));
            droppedAny = true;
        }

        if (droppedAny) {
            spawnEventParticles(level, pos, "ore_vein_burst");
        }
        return droppedAny;
    }

    private LuckyPickaxeConfig.WeightedDropEntry pickWeightedOreEntry(Level level, List<LuckyPickaxeConfig.WeightedDropEntry> entries) {
        int totalWeight = 0;
        for (LuckyPickaxeConfig.WeightedDropEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyPickaxeConfig.WeightedDropEntry entry : entries) {
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

    private boolean tryTriggerSeismicBurst(Level level, BlockPos pos, LivingEntity source) {
        return tryTriggerSeismicBurst(level, pos, source, false);
    }

    private boolean tryTriggerSeismicBurst(Level level, BlockPos pos, LivingEntity source, boolean force) {
        LuckyPickaxeConfig.SeismicBurstSettings settings = LuckyPickaxeConfig.seismicBurstSettings();
        if (!settings.enabled() || (!force && level.random.nextFloat() > settings.chance())) {
            return false;
        }

        double radius = Math.max(1.0D, settings.radius());
        AABB area = new AABB(pos).inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != null
                        && entity.isAlive()
                        && entity != source
                        && !(entity instanceof Player));

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, settings.slownessTicks(), settings.slownessAmplifier(), false, true, true));

            double dx = target.getX() - (pos.getX() + 0.5D);
            double dz = target.getZ() - (pos.getZ() + 0.5D);
            double length = Math.sqrt((dx * dx) + (dz * dz));
            double horizontalX = length > 1.0E-4D ? (dx / length) * 0.35D : 0.0D;
            double horizontalZ = length > 1.0E-4D ? (dz / length) * 0.35D : 0.0D;
            target.setDeltaMovement(target.getDeltaMovement().add(horizontalX, settings.upwardBoost(), horizontalZ));
            target.hurtMarked = true;
        }

        spawnEventParticles(level, pos, "seismic_burst");
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.8D,
                    pos.getZ() + 0.5D,
                    2,
                    0.18D,
                    0.1D,
                    0.18D,
                    0.0D
            );
        }

        return true;
    }

    private boolean tryTriggerLuckyBlockTransform(Level level, BlockPos pos) {
        LuckyPickaxeConfig.LuckyBlockTransformSettings settings = LuckyPickaxeConfig.luckyBlockTransformSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.chance()) {
            return false;
        }

        Block selected = settings.chooseBlock(level.random);
        if (selected == null) {
            return false;
        }

        level.setBlock(pos, selected.defaultBlockState(), 3);
        if (selected == ModBlocks.LUCKY_BLOCK) {
            spawnEventParticles(level, pos, "lucky_block_transform");
        } else {
            spawnEventParticles(level, pos, "mostly_unlucky_block_transform");
        }
        return true;
    }

    private boolean tryTriggerGoldenHenSpawn(Level level, BlockPos pos, LivingEntity miningEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        LuckyPickaxeConfig.GoldenHenSpawnSettings settings = LuckyPickaxeConfig.goldenHenSpawnSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.chance()) {
            return false;
        }

        var goldenHen = ModEntities.GOLDEN_HEN.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (goldenHen == null) {
            return false;
        }

        int radius = Math.max(0, settings.spawnRadius());
        BlockPos spawnOrigin = preferredSpawnOrigin(pos, miningEntity);
        if (!placeEntityWithoutClipping(serverLevel, goldenHen, spawnOrigin, radius)) {
            return false;
        }

        goldenHen.setYRot(level.random.nextFloat() * 360f);
        serverLevel.addFreshEntity(goldenHen);
        spawnEventParticles(level, pos, "golden_hen_spawn");
        return true;
    }

    private boolean tryTriggerBedrockTransform(Level level, BlockPos pos) {
        LuckyPickaxeConfig.BedrockTransformSettings settings = LuckyPickaxeConfig.bedrockTransformSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.chance()) {
            return false;
        }

        level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
        spawnEventParticles(level, pos, "bedrock_transform");
        return true;
    }

    private boolean tryTriggerBlockTransform(Level level, BlockPos pos) {
        LuckyPickaxeConfig.BlockTransformSettings settings = LuckyPickaxeConfig.blockTransformSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.chance()) {
            return false;
        }

        List<Block> blocks = settings.blocks();
        if (blocks.isEmpty()) {
            return false;
        }

        Block selected = blocks.get(level.random.nextInt(blocks.size()));
        if (selected == null) {
            return false;
        }

        level.setBlock(pos, selected.defaultBlockState(), 3);
        spawnEventParticles(level, pos, "block_transform");
        return true;
    }

    private boolean tryTriggerBonusDrop(Level level, BlockPos pos) {
        LuckyPickaxeConfig.BonusDropSettings settings = LuckyPickaxeConfig.bonusDropSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.dropChance()) {
            return false;
        }

        LuckyPickaxeConfig.DropEntry selected = pickDrop(level, settings);
        if (selected == null || selected.weight() <= 0) {
            return false;
        }

        int amount = selected.minCount();
        if (selected.maxCount() > selected.minCount()) {
            amount += level.random.nextInt(selected.maxCount() - selected.minCount() + 1);
        }

        popResource(level, pos, new ItemStack(selected.item(), amount));
        spawnEventParticles(level, pos, "mine_bonus_drops");
        return true;
    }

    private boolean tryTriggerFriendlySpawn(Level level, BlockPos pos, LivingEntity miningEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        LuckyPickaxeConfig.FriendlySpawnSettings settings = LuckyPickaxeConfig.friendlySpawnSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.chance()) {
            return false;
        }

        List<LuckyPickaxeConfig.FriendlyMobEntry> mobEntries = settings.mobs();
        if (mobEntries.isEmpty()) {
            return false;
        }

        int sumMin = 0;
        int sumMax = 0;
        int[] counts = new int[mobEntries.size()];
        for (int i = 0; i < mobEntries.size(); i++) {
            LuckyPickaxeConfig.FriendlyMobEntry entry = mobEntries.get(i);
            int min = Math.max(0, entry.minCount());
            int max = Math.max(min, entry.maxCount());
            counts[i] = min;
            sumMin += min;
            sumMax += max;
        }

        if (sumMax <= 0) {
            return false;
        }

        int effectiveMin = Math.max(settings.overallMinCount(), sumMin);
        int effectiveMax = Math.min(settings.overallMaxCount(), sumMax);
        if (effectiveMax < effectiveMin) {
            effectiveMin = Math.min(sumMax, effectiveMin);
            effectiveMax = effectiveMin;
        }

        int targetTotal = effectiveMin;
        if (effectiveMax > effectiveMin) {
            targetTotal += level.random.nextInt(effectiveMax - effectiveMin + 1);
        }

        int remaining = Math.max(0, targetTotal - sumMin);
        while (remaining > 0) {
            List<Integer> candidates = new ArrayList<>();
            for (int i = 0; i < mobEntries.size(); i++) {
                if (counts[i] < mobEntries.get(i).maxCount()) {
                    candidates.add(i);
                }
            }

            if (candidates.isEmpty()) {
                break;
            }

            int selectedIndex = candidates.get(level.random.nextInt(candidates.size()));
            counts[selectedIndex]++;
            remaining--;
        }

        int radius = Math.max(0, settings.spawnRadius());
        BlockPos spawnOrigin = preferredSpawnOrigin(pos, miningEntity);
        boolean spawnedAny = false;
        for (int i = 0; i < mobEntries.size(); i++) {
            LuckyPickaxeConfig.FriendlyMobEntry entry = mobEntries.get(i);
            int count = counts[i];
            for (int spawned = 0; spawned < count; spawned++) {
                Mob mob = spawnFriendly(serverLevel, entry.type(), spawnOrigin, radius);
                if (mob != null) {
                    serverLevel.addFreshEntity(mob);
                    spawnedAny = true;
                }
            }
        }
        if (spawnedAny) {
            spawnEventParticles(level, pos, "friendly_spawn");
        }
        return spawnedAny;
    }

    private boolean tryTriggerHostileSpawn(Level level, BlockPos pos, LivingEntity miningEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        LuckyPickaxeConfig.HostileSpawnSettings settings = LuckyPickaxeConfig.hostileSpawnSettings();
        if (settings.forceNormalOnPeaceful() && serverLevel.getServer() != null && serverLevel.getServer().getWorldData().getDifficulty() == Difficulty.PEACEFUL) {
            serverLevel.getServer().setDifficulty(Difficulty.NORMAL, true);
        }

        if (!settings.enabled() || level.random.nextFloat() > settings.chance()) {
            return false;
        }

        List<LuckyPickaxeConfig.HostileMobEntry> mobEntries = settings.mobs();
        if (mobEntries.isEmpty()) {
            return false;
        }

        int sumMin = 0;
        int sumMax = 0;
        int[] counts = new int[mobEntries.size()];
        for (int i = 0; i < mobEntries.size(); i++) {
            LuckyPickaxeConfig.HostileMobEntry entry = mobEntries.get(i);
            int min = Math.max(0, entry.minCount());
            int max = Math.max(min, entry.maxCount());
            counts[i] = min;
            sumMin += min;
            sumMax += max;
        }

        if (sumMax <= 0) {
            return false;
        }

        int effectiveMin = Math.max(settings.overallMinCount(), sumMin);
        int effectiveMax = Math.min(settings.overallMaxCount(), sumMax);
        if (effectiveMax < effectiveMin) {
            effectiveMin = Math.min(sumMax, effectiveMin);
            effectiveMax = effectiveMin;
        }

        int targetTotal = effectiveMin;
        if (effectiveMax > effectiveMin) {
            targetTotal += level.random.nextInt(effectiveMax - effectiveMin + 1);
        }

        int remaining = Math.max(0, targetTotal - sumMin);
        while (remaining > 0) {
            List<Integer> candidates = new ArrayList<>();
            for (int i = 0; i < mobEntries.size(); i++) {
                if (counts[i] < mobEntries.get(i).maxCount()) {
                    candidates.add(i);
                }
            }

            if (candidates.isEmpty()) {
                break;
            }

            int selectedIndex = candidates.get(level.random.nextInt(candidates.size()));
            counts[selectedIndex]++;
            remaining--;
        }

        int radius = Math.max(0, settings.spawnRadius());
        BlockPos spawnOrigin = preferredSpawnOrigin(pos, miningEntity);
        boolean spawnedAny = false;
        for (int i = 0; i < mobEntries.size(); i++) {
            LuckyPickaxeConfig.HostileMobEntry entry = mobEntries.get(i);
            int count = counts[i];
            for (int spawned = 0; spawned < count; spawned++) {
                Mob mob = spawnHostile(serverLevel, entry.type(), spawnOrigin, radius);
                if (mob != null) {
                    serverLevel.addFreshEntity(mob);
                    spawnedAny = true;
                }
            }
        }
        if (spawnedAny) {
            spawnEventParticles(level, pos, "hostile_spawn");
        }
        return spawnedAny;
    }

    private BlockPos randomSpawnPos(ServerLevel level, BlockPos origin, int radius) {
        int dx = radius <= 0 ? 0 : level.random.nextInt(radius * 2 + 1) - radius;
        int dz = radius <= 0 ? 0 : level.random.nextInt(radius * 2 + 1) - radius;
        int spawnX = origin.getX() + dx;
        int spawnZ = origin.getZ() + dz;
        int spawnY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);
        return new BlockPos(spawnX, Math.max(level.getMinY() + 1, spawnY), spawnZ);
    }

    private Mob spawnHostile(ServerLevel level, EntityType<?> type, BlockPos origin, int radius) {
        if (type == null) {
            return null;
        }

        var entity = type.create(level, EntitySpawnReason.TRIGGERED);
        if (!(entity instanceof Mob mob) || !(mob instanceof Enemy)) {
            return null;
        }

        if (!placeEntityWithoutClipping(level, mob, origin, radius)) {
            return null;
        }

        BlockPos spawnPos = mob.blockPosition();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, null);
        if (!level.noCollision(mob)) {
            return null;
        }
        return mob;
    }

    private Mob spawnFriendly(ServerLevel level, EntityType<?> type, BlockPos origin, int radius) {
        if (type == null) {
            return null;
        }

        var entity = type.create(level, EntitySpawnReason.TRIGGERED);
        if (!(entity instanceof Mob mob) || mob instanceof Enemy) {
            return null;
        }

        if (!placeEntityWithoutClipping(level, mob, origin, radius)) {
            return null;
        }

        BlockPos spawnPos = mob.blockPosition();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, null);
        if (!level.noCollision(mob)) {
            return null;
        }
        return mob;
    }

    private boolean placeEntityWithoutClipping(ServerLevel level, Mob mob, BlockPos origin, int radius) {
        int scanRadius = Math.max(0, radius);

        for (int attempt = 0; attempt < 20; attempt++) {
            BlockPos base = randomSpawnPos(level, origin, scanRadius);

            for (int dy = -1; dy <= 2; dy++) {
                BlockPos feet = base.offset(0, dy, 0);
                BlockPos below = feet.below();

                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    continue;
                }

                mob.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                if (level.noCollision(mob)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean tryTriggerXpBurst(Level level, LivingEntity miningEntity) {
        if (!(level instanceof ServerLevel serverLevel) || miningEntity == null) {
            return false;
        }

        LuckyPickaxeConfig.XpBurstSettings settings = LuckyPickaxeConfig.xpBurstSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.chance()) {
            return false;
        }

        int xp = settings.minXp();
        if (settings.maxXp() > settings.minXp()) {
            xp += level.random.nextInt(settings.maxXp() - settings.minXp() + 1);
        }

        ExperienceOrb.award(serverLevel, miningEntity.position(), xp);
        spawnEventParticles(level, miningEntity.blockPosition(), "xp_burst");
        return true;
    }

    private boolean tryTriggerTntTransform(Level level, BlockPos pos, LivingEntity miningEntity) {
        LuckyPickaxeConfig.TntTransformSettings settings = LuckyPickaxeConfig.tntTransformSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.chance()) {
            return false;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int count = minCount + level.random.nextInt(maxCount - minCount + 1);

        for (int i = 0; i < count; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 2.0;
            double oz = (level.random.nextDouble() - 0.5) * 2.0;
            PrimedTnt tnt = new PrimedTnt(
                    level,
                    pos.getX() + 0.5 + ox,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5 + oz,
                    miningEntity
            );
            tnt.setFuse(settings.fuseTicks());
            level.addFreshEntity(tnt);
        }

        spawnEventParticles(level, pos, "tnt_transform");
        return true;
    }

    private BlockPos preferredSpawnOrigin(BlockPos fallback, LivingEntity miningEntity) {
        if (miningEntity == null) {
            return fallback;
        }

        var look = miningEntity.getLookAngle();
        double ahead = 2.0;
        int x = (int) Math.floor(miningEntity.getX() + (look.x * ahead));
        int y = (int) Math.floor(miningEntity.getY());
        int z = (int) Math.floor(miningEntity.getZ() + (look.z * ahead));
        return new BlockPos(x, y, z);
    }

    private void spawnEventParticles(Level level, BlockPos pos, String eventKey) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) {
            return;
        }

        LuckyPickaxeConfig.EventParticleSettings settings = LuckyPickaxeConfig.eventParticleSettings();
        if (!settings.enabled()) {
            return;
        }

        boolean goodEvent = settings.isGoodEvent(eventKey);
        boolean badEvent = settings.isBadEvent(eventKey);
        if (!goodEvent && !badEvent) {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.7;
        double z = pos.getZ() + 0.5;

        if (goodEvent) {
            if (settings.goodCount() > 0) {
                serverLevel.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        x,
                        y,
                        z,
                        settings.goodCount(),
                        settings.spreadX(),
                        settings.spreadY(),
                        settings.spreadZ(),
                        settings.goodSpeed()
                );
            }
        } else {
            if (settings.badCount() > 0) {
                serverLevel.sendParticles(
                        ParticleTypes.SMOKE,
                        x,
                        y,
                        z,
                        settings.badCount(),
                        settings.spreadX(),
                        settings.spreadY(),
                        settings.spreadZ(),
                        settings.badSpeed()
                );
            }
        }
    }

    private LuckyPickaxeConfig.DropEntry pickDrop(Level level, LuckyPickaxeConfig.BonusDropSettings settings) {
        int nuggetWeight = Math.max(0, settings.goldNugget().weight());
        int charcoalWeight = Math.max(0, settings.charcoal().weight());
        int ingotWeight = Math.max(0, settings.goldIngot().weight());
        int diamondWeight = Math.max(0, settings.diamond().weight());
        int emeraldWeight = Math.max(0, settings.emerald().weight());
        int totalWeight = nuggetWeight + charcoalWeight + ingotWeight + diamondWeight + emeraldWeight;

        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        if (roll < nuggetWeight) {
            return settings.goldNugget();
        }
        roll -= nuggetWeight;
        if (roll < charcoalWeight) {
            return settings.charcoal();
        }
        roll -= charcoalWeight;
        if (roll < ingotWeight) {
            return settings.goldIngot();
        }
        roll -= ingotWeight;
        if (roll < diamondWeight) {
            return settings.diamond();
        }
        return settings.emerald();
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

        LuckyPickaxeConfig.TemporaryEnchantSettings settings = LuckyPickaxeConfig.temporaryEnchantSettings();
        if (!settings.enabled() || settings.entries().isEmpty()) {
            return;
        }

        if (!force && level.random.nextFloat() > settings.chance()) {
            return;
        }

        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, Integer> currentLevels = currentEnchantmentLevels(stack, enchantmentRegistry);

        List<LuckyPickaxeConfig.EnchantmentEntry> candidates = new ArrayList<>();
        for (LuckyPickaxeConfig.EnchantmentEntry entry : settings.entries()) {
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
        List<LuckyPickaxeConfig.EnchantmentEntry> pool = new ArrayList<>(candidates);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            LuckyPickaxeConfig.EnchantmentEntry selected = pickWeightedEnchant(level, pool);
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

    private LuckyPickaxeConfig.EnchantmentEntry pickWeightedEnchant(Level level, List<LuckyPickaxeConfig.EnchantmentEntry> entries) {
        int totalWeight = 0;
        for (LuckyPickaxeConfig.EnchantmentEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyPickaxeConfig.EnchantmentEntry entry : entries) {
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

        LuckyPickaxeConfig.TemporaryEnchantParticleSettings settings = LuckyPickaxeConfig.temporaryEnchantSettings().effectParticles();
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
}
