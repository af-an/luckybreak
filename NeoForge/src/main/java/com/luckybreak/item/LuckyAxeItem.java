package com.luckybreak.item;

import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.minecraft.world.level.block.Block.popResource;

public class LuckyAxeItem extends AxeItem {

    private static final String ITEM_ID = "lucky_axe";
    private static final String TEMP_ENCHANT_IDS_KEY = "luckybreak_axe_temp_enchant_ids";
    private static final String TEMP_ENCHANT_EXPIRES_AT_KEY = "luckybreak_axe_temp_enchant_expires_at";

    public LuckyAxeItem(ResourceKey<Item> key) {
        super(ToolMaterial.GOLD, LuckyAxeConfig.attackDamage(), LuckyAxeConfig.attackSpeed(), LuckyItemVisuals.apply(ITEM_ID, new Item.Properties()
                .setId(key)
                .durability(LuckyAxeConfig.durability())
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

        tryTriggerBlockBreakLightning(level, pos, miningEntity);

        if (level.isClientSide()) {
            return result;
        }

        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            tryTriggerLuckyBlockTransform(level, pos);
            tryTriggerLeafStorm(level, pos);
            tryTriggerBeeSwarm(level, pos);
        }

        if (!state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return result;
        }

        tickTemporaryEnchantments(stack, level, miningEntity);
        tryTriggerTemporaryEnchantments(stack, level, miningEntity);

        LuckyAxeConfig.BonusDropSettings settings = LuckyAxeConfig.bonusDropSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.dropChance()) {
            return result;
        }

        LuckyAxeConfig.DropEntry selected = pickDrop(level, settings);
        if (selected == null || selected.weight() <= 0) {
            return result;
        }

        int amount = selected.minCount();
        if (selected.maxCount() > selected.minCount()) {
            amount += level.random.nextInt(selected.maxCount() - selected.minCount() + 1);
        }

        popResource(level, pos, new ItemStack(selected.item(), amount));
        if (level instanceof ServerLevel serverLevel) {
            LuckyAxeConfig.DropParticleSettings particles = LuckyAxeConfig.dropParticleSettings();
            if (particles.enabled() && particles.count() > 0) {
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5,
                        pos.getY() + 0.7,
                        pos.getZ() + 0.5,
                        particles.count(),
                        particles.spreadX(),
                        particles.spreadY(),
                        particles.spreadZ(),
                        particles.speed()
                );
            }
        }
        return result;
    }

    public boolean triggerCommandEvent(ItemStack stack, ServerPlayer player, String eventName, BlockPos blockPos, LivingEntity targetMob) {
        if (stack == null || stack.isEmpty() || player == null || eventName == null || eventName.isBlank()) {
            return false;
        }

        String normalized = eventName.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "block_break_lightning" -> {
                if (blockPos == null) {
                    yield false;
                }
                tryTriggerBlockBreakLightning(player.level(), blockPos, player, true);
                yield true;
            }
            case "mob_hit_lightning" -> {
                if (targetMob == null) {
                    yield false;
                }
                tryTriggerMobHitLightning(targetMob, player, true);
                yield true;
            }
            case "lucky_block_transform" -> {
                if (blockPos == null) {
                    yield false;
                }
                tryTriggerLuckyBlockTransform(player.level(), blockPos, true);
                yield true;
            }
            case "bonus_drops" -> {
                if (blockPos == null) {
                    yield false;
                }
                triggerBonusDropAt(player.level(), blockPos);
                yield true;
            }
            case "temporary_enchantments" -> {
                tickTemporaryEnchantments(stack, player.level(), player);
                tryTriggerTemporaryEnchantments(stack, player.level(), player, true);
                yield true;
            }
            case "leaf_storm" -> {
                if (blockPos == null) {
                    yield false;
                }
                tryTriggerLeafStorm(player.level(), blockPos, true);
                yield true;
            }
            case "bee_swarm" -> {
                if (blockPos == null) {
                    yield false;
                }
                tryTriggerBeeSwarm(player.level(), blockPos, true);
                yield true;
            }
            case "timber_launch" -> {
                if (targetMob == null) {
                    yield false;
                }
                tryTriggerTimberLaunch(targetMob, true);
                yield true;
            }
            case "loot_pinata" -> {
                if (targetMob == null) {
                    yield false;
                }
                tryTriggerLootPinata(targetMob, true);
                yield true;
            }
            default -> false;
        };
    }

    private void triggerBonusDropAt(Level level, BlockPos pos) {
        LuckyAxeConfig.BonusDropSettings settings = LuckyAxeConfig.bonusDropSettings();
        LuckyAxeConfig.DropEntry selected = pickDrop(level, settings);
        if (selected == null || selected.weight() <= 0) {
            return;
        }

        int amount = selected.minCount();
        if (selected.maxCount() > selected.minCount()) {
            amount += level.random.nextInt(selected.maxCount() - selected.minCount() + 1);
        }

        popResource(level, pos, new ItemStack(selected.item(), amount));
        if (level instanceof ServerLevel serverLevel) {
            LuckyAxeConfig.DropParticleSettings particles = LuckyAxeConfig.dropParticleSettings();
            if (particles.enabled() && particles.count() > 0) {
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5,
                        pos.getY() + 0.7,
                        pos.getZ() + 0.5,
                        particles.count(),
                        particles.spreadX(),
                        particles.spreadY(),
                        particles.spreadZ(),
                        particles.speed()
                );
            }
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

        LuckyAxeConfig.TemporaryEnchantSettings settings = LuckyAxeConfig.temporaryEnchantSettings();
        if (!settings.enabled() || settings.entries().isEmpty()) {
            return;
        }

        if (!force && level.random.nextFloat() > settings.chance()) {
            return;
        }

        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, Integer> currentLevels = currentEnchantmentLevels(stack, enchantmentRegistry);

        List<LuckyAxeConfig.EnchantmentEntry> candidates = new ArrayList<>();
        for (LuckyAxeConfig.EnchantmentEntry entry : settings.entries()) {
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
        List<LuckyAxeConfig.EnchantmentEntry> pool = new ArrayList<>(candidates);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            LuckyAxeConfig.EnchantmentEntry selected = pickWeightedEnchant(level, pool);
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

    private LuckyAxeConfig.EnchantmentEntry pickWeightedEnchant(Level level, List<LuckyAxeConfig.EnchantmentEntry> entries) {
        int totalWeight = 0;
        for (LuckyAxeConfig.EnchantmentEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyAxeConfig.EnchantmentEntry entry : entries) {
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

    private void tryTriggerLuckyBlockTransform(Level level, BlockPos pos) {
        tryTriggerLuckyBlockTransform(level, pos, false);
    }

    private void tryTriggerLuckyBlockTransform(Level level, BlockPos pos, boolean force) {
        LuckyAxeConfig.LuckyBlockTransformSettings settings = LuckyAxeConfig.luckyBlockTransformSettings();
        if (!settings.enabled() || (!force && level.random.nextFloat() > settings.chance())) {
            return;
        }

        Block selected = settings.chooseBlock(level.random);
        if (selected == null) {
            return;
        }

        level.setBlock(pos, selected.defaultBlockState(), 3);
    }

    private void spawnTemporaryEnchantParticles(Level level, Entity holderEntity, boolean applied) {
        if (!(level instanceof ServerLevel serverLevel) || holderEntity == null) {
            return;
        }

        LuckyAxeConfig.TemporaryEnchantParticleSettings settings = LuckyAxeConfig.temporaryEnchantSettings().effectParticles();
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

    private record EnchantLevel(Identifier id, int level) {
    }

    private LuckyAxeConfig.DropEntry pickDrop(Level level, LuckyAxeConfig.BonusDropSettings settings) {
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

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        tryTriggerMobHitLightning(target, attacker);
        tryTriggerTimberLaunch(target);
        tryTriggerLootPinata(target);
    }

    private void tryTriggerLeafStorm(Level level, BlockPos pos) {
        tryTriggerLeafStorm(level, pos, false);
    }

    private void tryTriggerLeafStorm(Level level, BlockPos pos, boolean force) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) {
            return;
        }

        LuckyAxeConfig.LeafStormSettings settings = LuckyAxeConfig.leafStormSettings();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        int stickMin = Math.max(0, settings.stickMin());
        int stickMax = Math.max(stickMin, settings.stickMax());
        int stickCount = stickMin + serverLevel.random.nextInt(stickMax - stickMin + 1);
        if (stickCount > 0) {
            popResource(serverLevel, pos, new ItemStack(Items.STICK, stickCount));
        }

        if (serverLevel.random.nextFloat() <= settings.appleChance()) {
            popResource(serverLevel, pos, new ItemStack(Items.APPLE, 1));
        }

        if (settings.particleCount() > 0) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                    pos.getX() + 0.5,
                    pos.getY() + 0.8,
                    pos.getZ() + 0.5,
                    settings.particleCount(),
                    0.35,
                    0.35,
                    0.35,
                    0.02
            );
        }
    }

    private void tryTriggerBeeSwarm(Level level, BlockPos pos) {
        tryTriggerBeeSwarm(level, pos, false);
    }

    private void tryTriggerBeeSwarm(Level level, BlockPos pos, boolean force) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) {
            return;
        }

        LuckyAxeConfig.BeeSwarmSettings settings = LuckyAxeConfig.beeSwarmSettings();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        int minCount = Math.max(1, settings.minCount());
        int maxCount = Math.max(minCount, settings.maxCount());
        int count = minCount + serverLevel.random.nextInt(maxCount - minCount + 1);

        for (int i = 0; i < count; i++) {
            var bee = EntityType.BEE.create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (bee == null) {
                continue;
            }

            double ox = (serverLevel.random.nextDouble() - 0.5D) * 2.0D;
            double oz = (serverLevel.random.nextDouble() - 0.5D) * 2.0D;
            bee.setPos(pos.getX() + 0.5D + ox, pos.getY() + 1.0D, pos.getZ() + 0.5D + oz);
            if (!serverLevel.noCollision(bee)) {
                continue;
            }

            serverLevel.addFreshEntity(bee);
        }
    }

    private void tryTriggerTimberLaunch(LivingEntity target) {
        tryTriggerTimberLaunch(target, false);
    }

    private void tryTriggerTimberLaunch(LivingEntity target, boolean force) {
        if (target == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyAxeConfig.TimberLaunchSettings settings = LuckyAxeConfig.timberLaunchSettings();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        Vec3 current = target.getDeltaMovement();
        double x = (serverLevel.random.nextDouble() - 0.5D) * settings.horizontalBoost();
        double z = (serverLevel.random.nextDouble() - 0.5D) * settings.horizontalBoost();
        target.setDeltaMovement(current.x + x, Math.max(current.y, settings.upwardBoost()), current.z + z);
        target.hurtMarked = true;
    }

    private void tryTriggerLootPinata(LivingEntity target) {
        tryTriggerLootPinata(target, false);
    }

    private void tryTriggerLootPinata(LivingEntity target, boolean force) {
        if (target == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyAxeConfig.LootPinataSettings settings = LuckyAxeConfig.lootPinataSettings();
        if (!settings.enabled() || settings.entries().isEmpty() || (!force && serverLevel.random.nextFloat() > settings.chance())) {
            return;
        }

        int minRolls = Math.max(1, settings.minRolls());
        int maxRolls = Math.max(minRolls, settings.maxRolls());
        int rolls = minRolls + serverLevel.random.nextInt(maxRolls - minRolls + 1);

        for (int i = 0; i < rolls; i++) {
            LuckyAxeConfig.DropEntry entry = pickLootPinataDrop(serverLevel, settings.entries());
            if (entry == null || entry.weight() <= 0) {
                continue;
            }

            int amount = entry.minCount();
            if (entry.maxCount() > entry.minCount()) {
                amount += serverLevel.random.nextInt(entry.maxCount() - entry.minCount() + 1);
            }

            popResource(serverLevel, target.blockPosition(), new ItemStack(entry.item(), amount));
        }

        if (settings.particleCount() > 0) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    target.getX(),
                    target.getY() + 0.8,
                    target.getZ(),
                    settings.particleCount(),
                    0.35,
                    0.35,
                    0.35,
                    0.02
            );
        }
    }

    private LuckyAxeConfig.DropEntry pickLootPinataDrop(Level level, List<LuckyAxeConfig.DropEntry> entries) {
        int totalWeight = 0;
        for (LuckyAxeConfig.DropEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyAxeConfig.DropEntry entry : entries) {
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

    private void tryTriggerBlockBreakLightning(Level level, BlockPos pos, LivingEntity miningEntity) {
        tryTriggerBlockBreakLightning(level, pos, miningEntity, false);
    }

    private void tryTriggerBlockBreakLightning(Level level, BlockPos pos, LivingEntity miningEntity, boolean force) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) {
            return;
        }

        LuckyAxeConfig.LightningSettings settings = LuckyAxeConfig.lightningSettings();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.blockBreakChance())) {
            return;
        }

        strikeLightning(serverLevel, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, true, miningEntity);
    }

    private void tryTriggerMobHitLightning(LivingEntity target, LivingEntity attacker) {
        tryTriggerMobHitLightning(target, attacker, false);
    }

    private void tryTriggerMobHitLightning(LivingEntity target, LivingEntity attacker, boolean force) {
        if (target == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LuckyAxeConfig.LightningSettings settings = LuckyAxeConfig.lightningSettings();
        if (!settings.enabled() || (!force && serverLevel.random.nextFloat() > settings.mobHitChance())) {
            return;
        }

        strikeLightning(serverLevel, target.getX(), target.getY(), target.getZ(), false, attacker);
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
}
