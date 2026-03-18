package com.luckybreak.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Locale;

public class LuckyHoeItem extends HoeItem {

    private static final String ITEM_ID = "lucky_hoe";
    private static final String VILLAGER_SPAWN_COUNT_KEY = "luckybreak_villager_spawn_count";
    private static final float BASE_ATTACK_DAMAGE = 0.0F;
    private static final float BASE_ATTACK_SPEED = -3.0F;
    private static final int BASE_DURABILITY = 32;

    public LuckyHoeItem(ResourceKey<Item> key) {
        super(ToolMaterial.GOLD, BASE_ATTACK_DAMAGE, BASE_ATTACK_SPEED, LuckyItemVisuals.apply(ITEM_ID, new Item.Properties()
                .setId(key)
                .durability(BASE_DURABILITY)
            .repairable(Items.GOLD_INGOT)));
    }

    @Override
    public Component getName(ItemStack stack) {
        return LuckyItemVisuals.styleName(ITEM_ID, super.getName(stack));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        maybeDropMobHitFood(target);
        maybeTriggerPinataHit(target);
    }

    public boolean triggerCommandEvent(ItemStack stack, ServerPlayer player, String eventName, BlockPos blockPos, LivingEntity targetMob) {
        if (stack == null || stack.isEmpty() || player == null || eventName == null || eventName.isBlank()) {
            return false;
        }

        String normalized = eventName.trim().toLowerCase(Locale.ROOT);
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos pos = blockPos != null ? blockPos : player.blockPosition().relative(player.getDirection());

        return switch (normalized) {
            case "mob_hit_food_bonus_drops" -> {
                if (targetMob == null) {
                    yield false;
                }
                dropMobHitFoodForced(targetMob);
                yield true;
            }
            case "till_bonus_drops" -> {
                dropGoldNuggetsForced(level, pos);
                yield true;
            }
            case "villager_spawn" -> {
                spawnFarmerVillagerForced(stack, level, pos);
                yield true;
            }
            case "crop_bloom" -> {
                triggerCropBloom(level, pos, true);
                yield true;
            }
            case "chicken_parade" -> {
                triggerChickenParade(level, pos, true);
                yield true;
            }
            case "pinata_hit" -> {
                if (targetMob == null) {
                    yield false;
                }
                triggerPinataHit(targetMob, true);
                yield true;
            }
            default -> false;
        };
    }

    private void maybeTriggerPinataHit(LivingEntity target) {
        triggerPinataHit(target, false);
    }

    private void triggerPinataHit(LivingEntity target, boolean force) {
        if (target == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }

        LuckyHoeConfig.PinataHitSettings settings = LuckyHoeConfig.pinataHitSettings();
        if (!settings.enabled() || settings.entries().isEmpty()) {
            return;
        }

        RandomSource random = level.getRandom();
        if (!force && random.nextFloat() > settings.chance()) {
            return;
        }

        int rolls = settings.minRolls();
        if (settings.maxRolls() > settings.minRolls()) {
            rolls += random.nextInt(settings.maxRolls() - settings.minRolls() + 1);
        }

        for (int i = 0; i < rolls; i++) {
            LuckyHoeConfig.FoodDropEntry selected = pickFoodDrop(settings.entries(), random);
            if (selected == null) {
                continue;
            }
            int minCount = Math.max(1, selected.minCount());
            int maxCount = Math.max(minCount, selected.maxCount());
            int count = minCount + random.nextInt(maxCount - minCount + 1);
            Block.popResource(level, target.blockPosition(), new ItemStack(selected.item(), count));
        }

        if (settings.particleCount() > 0) {
            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    target.getX(),
                    target.getY() + 0.8,
                    target.getZ(),
                    settings.particleCount(),
                    0.35,
                    0.35,
                    0.35,
                    0.03
            );
        }
    }

    private void dropMobHitFoodForced(LivingEntity target) {
        if (target == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }

        LuckyHoeConfig.MobHitFoodDropSettings settings = LuckyHoeConfig.mobHitFoodDropSettings();
        if (!settings.enabled() || settings.entries().isEmpty()) {
            return;
        }

        RandomSource random = level.getRandom();
        LuckyHoeConfig.FoodDropEntry selected = pickFoodDrop(settings.entries(), random);
        if (selected == null) {
            return;
        }

        int minCount = Math.max(1, selected.minCount());
        int maxCount = Math.max(minCount, selected.maxCount());
        int count = minCount + random.nextInt(maxCount - minCount + 1);
        Block.popResource(level, target.blockPosition(), new ItemStack(selected.item(), count));
    }

    private void dropGoldNuggetsForced(ServerLevel level, BlockPos pos) {
        LuckyHoeConfig.NuggetDropSettings settings = LuckyHoeConfig.nuggetDropSettings();
        if (!settings.enabled()) {
            return;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int count = minCount + level.getRandom().nextInt(maxCount - minCount + 1);
        Block.popResource(level, pos.above(), new ItemStack(Items.GOLD_NUGGET, count));
    }

    private void spawnFarmerVillagerForced(ItemStack hoeStack, ServerLevel level, BlockPos origin) {
        LuckyHoeConfig.VillagerSpawnSettings settings = LuckyHoeConfig.villagerSpawnSettings();
        if (!settings.enabled()) {
            return;
        }

        BlockPos spawnPos = findSafeVillagerSpawnPos(level, origin, settings.searchRadius());
        if (spawnPos == null) {
            return;
        }

        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
        if (villager == null) {
            return;
        }

        villager.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        villager.setVillagerData(villager.getVillagerData().withProfession(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.FARMER)));
        if (!level.noCollision(villager)) {
            return;
        }

        level.addFreshEntity(villager);
        incrementVillagerSpawnCount(hoeStack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var clickedPos = context.getClickedPos();
        BlockState beforeState = level.getBlockState(clickedPos);

        InteractionResult result = super.useOn(context);
        if (!result.consumesAction()) {
            return result;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return result;
        }

        if (!isTillSource(beforeState)) {
            return result;
        }

        BlockState afterState = serverLevel.getBlockState(clickedPos);
        if (!afterState.is(Blocks.FARMLAND)) {
            return result;
        }

        RandomSource random = serverLevel.getRandom();
        maybeDropGoldNuggets(serverLevel, clickedPos, random);
        maybeSpawnFarmerVillager(context.getItemInHand(), serverLevel, clickedPos, random);
        triggerCropBloom(serverLevel, clickedPos, false);
        triggerChickenParade(serverLevel, clickedPos, false);

        LuckyHoeConfig.TillEffectSettings settings = LuckyHoeConfig.tillEffectSettings();
        if (!settings.enabled()) {
            return result;
        }

        if (settings.waterFarmland() && afterState.hasProperty(FarmBlock.MOISTURE)) {
            serverLevel.setBlock(clickedPos, afterState.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), 3);
        }

        if (random.nextFloat() > settings.plantChance()) {
            return result;
        }

        var plantPos = clickedPos.above();
        if (!serverLevel.getBlockState(plantPos).canBeReplaced()) {
            return result;
        }

        LuckyHoeConfig.PlantEntry selectedEntry = pickPlantEntry(settings.plantEntries(), random);
        if (selectedEntry == null) {
            return result;
        }

        if (!tryPlant(serverLevel, plantPos, selectedEntry.item())) {
            return result;
        }

        if (random.nextFloat() <= settings.growChance()) {
            int minSteps = Math.max(0, settings.growthMinSteps());
            int maxSteps = Math.max(minSteps, settings.growthMaxSteps());
            int steps = minSteps + random.nextInt(maxSteps - minSteps + 1);
            applyGrowth(serverLevel, plantPos, steps, random);
        }

        return result;
    }

    private void triggerCropBloom(ServerLevel level, BlockPos center, boolean force) {
        LuckyHoeConfig.CropBloomSettings settings = LuckyHoeConfig.cropBloomSettings();
        if (!settings.enabled()) {
            return;
        }

        RandomSource random = level.getRandom();
        if (!force && random.nextFloat() > settings.chance()) {
            return;
        }

        int radius = Math.max(1, settings.radius());
        int minSteps = Math.max(1, settings.growthMinSteps());
        int maxSteps = Math.max(minSteps, settings.growthMaxSteps());

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos cropPos = center.offset(dx, 1, dz);
                BlockState state = level.getBlockState(cropPos);
                if (!(state.getBlock() instanceof BonemealableBlock)) {
                    continue;
                }

                int steps = minSteps + random.nextInt(maxSteps - minSteps + 1);
                applyGrowth(level, cropPos, steps, random);
            }
        }

        if (settings.particleCount() > 0) {
            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    center.getX() + 0.5,
                    center.getY() + 1.0,
                    center.getZ() + 0.5,
                    settings.particleCount(),
                    radius * 0.3,
                    0.35,
                    radius * 0.3,
                    0.02
            );
        }
    }

    private void triggerChickenParade(ServerLevel level, BlockPos center, boolean force) {
        LuckyHoeConfig.ChickenParadeSettings settings = LuckyHoeConfig.chickenParadeSettings();
        if (!settings.enabled()) {
            return;
        }

        RandomSource random = level.getRandom();
        if (!force && random.nextFloat() > settings.chance()) {
            return;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int count = minCount + random.nextInt(maxCount - minCount + 1);

        for (int i = 0; i < count; i++) {
            BlockPos spawnPos = findSafeVillagerSpawnPos(level, center, settings.searchRadius());
            if (spawnPos == null) {
                continue;
            }

            var chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
            if (chicken == null) {
                continue;
            }

            chicken.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            if (!level.noCollision(chicken)) {
                continue;
            }

            level.addFreshEntity(chicken);
        }

        level.sendParticles(
                ParticleTypes.CLOUD,
                center.getX() + 0.5,
                center.getY() + 1.0,
                center.getZ() + 0.5,
                Math.max(8, count * 6),
                0.45,
                0.25,
                0.45,
                0.02
        );
    }

    private void maybeDropGoldNuggets(ServerLevel level, net.minecraft.core.BlockPos pos, RandomSource random) {
        LuckyHoeConfig.NuggetDropSettings settings = LuckyHoeConfig.nuggetDropSettings();
        if (!settings.enabled()) {
            return;
        }

        if (random.nextFloat() > settings.chance()) {
            return;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int count = minCount + random.nextInt(maxCount - minCount + 1);
        Block.popResource(level, pos.above(), new ItemStack(Items.GOLD_NUGGET, count));
    }

    private void maybeSpawnFarmerVillager(ItemStack hoeStack, ServerLevel level, BlockPos origin, RandomSource random) {
        if (hoeStack == null || hoeStack.isEmpty()) {
            return;
        }

        LuckyHoeConfig.VillagerSpawnSettings settings = LuckyHoeConfig.villagerSpawnSettings();
        if (!settings.enabled()) {
            return;
        }

        int currentSpawnCount = getVillagerSpawnCount(hoeStack);
        int guaranteedMin = Math.max(0, settings.guaranteedMinPerHoe());
        boolean guaranteedPending = settings.guaranteeEnabled() && currentSpawnCount < guaranteedMin;

        if (!guaranteedPending && random.nextFloat() > settings.chance()) {
            return;
        }

        BlockPos spawnPos = findSafeVillagerSpawnPos(level, origin, settings.searchRadius());
        if (spawnPos == null) {
            return;
        }

        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
        if (villager == null) {
            return;
        }

        villager.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        villager.setVillagerData(
            villager.getVillagerData().withProfession(
                BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.FARMER)
            )
        );

        if (!level.noCollision(villager)) {
            return;
        }

        level.addFreshEntity(villager);
        incrementVillagerSpawnCount(hoeStack);
    }

    private int getVillagerSpawnCount(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }

        CompoundTag tag = data.copyTag();
        return tag.getInt(VILLAGER_SPAWN_COUNT_KEY).orElse(0);
    }

    private void incrementVillagerSpawnCount(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int current = getVillagerSpawnCount(stack);
        stack.set(DataComponents.CUSTOM_DATA, data.update(tag -> tag.putInt(VILLAGER_SPAWN_COUNT_KEY, current + 1)));
    }

    private BlockPos findSafeVillagerSpawnPos(ServerLevel level, BlockPos origin, int radius) {
        int scanRadius = Math.max(1, radius);

        for (int ring = 0; ring <= scanRadius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (ring > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }

                    BlockPos base = origin.offset(dx, 0, dz);
                    for (int dy = -1; dy <= 2; dy++) {
                        BlockPos feet = base.offset(0, dy, 0);
                        BlockPos head = feet.above();
                        BlockPos below = feet.below();

                        if (!level.getBlockState(feet).isAir() || !level.getBlockState(head).isAir()) {
                            continue;
                        }

                        BlockState belowState = level.getBlockState(below);
                        if (!belowState.isFaceSturdy(level, below, Direction.UP)) {
                            continue;
                        }

                        return feet;
                    }
                }
            }
        }

        return null;
    }

    private void maybeDropMobHitFood(LivingEntity target) {
        if (target == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }

        LuckyHoeConfig.MobHitFoodDropSettings settings = LuckyHoeConfig.mobHitFoodDropSettings();
        if (!settings.enabled() || settings.entries().isEmpty()) {
            return;
        }

        RandomSource random = level.getRandom();
        if (random.nextFloat() > settings.chance()) {
            return;
        }

        LuckyHoeConfig.FoodDropEntry selected = pickFoodDrop(settings.entries(), random);
        if (selected == null) {
            return;
        }

        int minCount = Math.max(1, selected.minCount());
        int maxCount = Math.max(minCount, selected.maxCount());
        int count = minCount + random.nextInt(maxCount - minCount + 1);
        Block.popResource(level, target.blockPosition(), new ItemStack(selected.item(), count));
    }

    private LuckyHoeConfig.FoodDropEntry pickFoodDrop(List<LuckyHoeConfig.FoodDropEntry> entries, RandomSource random) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (LuckyHoeConfig.FoodDropEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        for (LuckyHoeConfig.FoodDropEntry entry : entries) {
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry;
            }
            roll -= weight;
        }

        return entries.getFirst();
    }

    private boolean isTillSource(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT);
    }

    private LuckyHoeConfig.PlantEntry pickPlantEntry(List<LuckyHoeConfig.PlantEntry> entries, RandomSource random) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (LuckyHoeConfig.PlantEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        for (LuckyHoeConfig.PlantEntry entry : entries) {
            int weight = Math.max(0, entry.weight());
            if (roll < weight) {
                return entry;
            }
            roll -= weight;
        }

        return entries.getFirst();
    }

    private boolean tryPlant(ServerLevel level, net.minecraft.core.BlockPos plantPos, Item item) {
        if (item == null || item == Items.AIR) {
            return false;
        }

        BlockState state = resolvePlantState(item);
        if (state == null) {
            return false;
        }

        if (!state.canSurvive(level, plantPos)) {
            return false;
        }

        level.setBlock(plantPos, state, 3);
        return true;
    }

    private BlockState resolvePlantState(Item item) {
        if (item == Items.WHEAT_SEEDS) {
            return Blocks.WHEAT.defaultBlockState();
        }
        if (item == Items.BEETROOT_SEEDS) {
            return Blocks.BEETROOTS.defaultBlockState();
        }
        if (item == Items.CARROT) {
            return Blocks.CARROTS.defaultBlockState();
        }
        if (item == Items.POTATO) {
            return Blocks.POTATOES.defaultBlockState();
        }
        if (item == Items.MELON_SEEDS) {
            return Blocks.MELON_STEM.defaultBlockState();
        }
        if (item == Items.PUMPKIN_SEEDS) {
            return Blocks.PUMPKIN_STEM.defaultBlockState();
        }

        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block != null && block != Blocks.AIR) {
                return block.defaultBlockState();
            }
        }

        Block fallbackBlock = Block.byItem(item);
        if (fallbackBlock == null || fallbackBlock == Blocks.AIR) {
            return null;
        }

        return fallbackBlock.defaultBlockState();
    }

    private void applyGrowth(ServerLevel level, net.minecraft.core.BlockPos plantPos, int steps, RandomSource random) {
        for (int i = 0; i < steps; i++) {
            BlockState state = level.getBlockState(plantPos);
            if (!(state.getBlock() instanceof BonemealableBlock bonemealable)) {
                return;
            }

            if (!bonemealable.isValidBonemealTarget(level, plantPos, state)) {
                return;
            }

            if (!bonemealable.isBonemealSuccess(level, random, plantPos, state)) {
                continue;
            }

            bonemealable.performBonemeal(level, random, plantPos, state);
        }
    }
}
