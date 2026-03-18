package com.luckybreak.item;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.minecraft.world.level.block.Block.popResource;

public class LuckyShovelItem extends ShovelItem {

    private static final String ITEM_ID = "lucky_shovel";
    private static final String TEMP_ENCHANT_IDS_KEY = "luckybreak_shovel_temp_enchant_ids";
    private static final String TEMP_ENCHANT_EXPIRES_AT_KEY = "luckybreak_shovel_temp_enchant_expires_at";

    public LuckyShovelItem(ResourceKey<Item> key) {
        super(ToolMaterial.GOLD, LuckyShovelConfig.attackDamage(), LuckyShovelConfig.attackSpeed(), LuckyItemVisuals.apply(ITEM_ID, new Item.Properties()
                .setId(key)
                .durability(LuckyShovelConfig.durability())
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

        if (level.isClientSide() || !state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return result;
        }

        tickTemporaryEnchantments(stack, level, miningEntity);
        tryTriggerTemporaryEnchantments(stack, level, miningEntity);
        tryTriggerLuckyBlockTransform(level, pos);
        tryTriggerSandstorm(level, pos, miningEntity);
        tryTriggerTreasureBurst(level, pos);

        LuckyShovelConfig.BonusDropSettings settings = LuckyShovelConfig.bonusDropSettings();
        if (!settings.enabled() || level.random.nextFloat() > settings.dropChance()) {
            return result;
        }

        LuckyShovelConfig.DropEntry selected = pickDrop(level, settings);
        if (selected == null || selected.weight() <= 0) {
            return result;
        }

        int amount = selected.minCount();
        if (selected.maxCount() > selected.minCount()) {
            amount += level.random.nextInt(selected.maxCount() - selected.minCount() + 1);
        }

        popResource(level, pos, new ItemStack(selected.item(), amount));
        if (level instanceof ServerLevel serverLevel) {
            LuckyShovelConfig.DropParticleSettings particles = LuckyShovelConfig.dropParticleSettings();
            if (particles.enabled() && particles.count() > 0) {
                serverLevel.sendParticles(
                    particles.particleType(),
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

    public boolean triggerCommandEvent(ItemStack stack, ServerLevel level, Entity user, String eventName, BlockPos blockPos) {
        if (stack == null || stack.isEmpty() || level == null || eventName == null || eventName.isBlank()) {
            return false;
        }

        String normalized = eventName.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "lucky_block_transform" -> {
                if (blockPos == null) {
                    yield false;
                }
                tryTriggerLuckyBlockTransform(level, blockPos, true);
                yield true;
            }
            case "bonus_drops" -> {
                if (blockPos == null) {
                    yield false;
                }
                triggerBonusDropAt(level, blockPos);
                yield true;
            }
            case "sandstorm" -> {
                if (blockPos == null) {
                    yield false;
                }
                tryTriggerSandstorm(level, blockPos, user, true);
                yield true;
            }
            case "treasure_burst" -> {
                if (blockPos == null) {
                    yield false;
                }
                tryTriggerTreasureBurst(level, blockPos, true);
                yield true;
            }
            case "temporary_enchantments" -> {
                tickTemporaryEnchantments(stack, level, user);
                tryTriggerTemporaryEnchantments(stack, level, user, true);
                yield true;
            }
            default -> false;
        };
    }

    private void triggerBonusDropAt(Level level, BlockPos pos) {
        LuckyShovelConfig.BonusDropSettings settings = LuckyShovelConfig.bonusDropSettings();
        LuckyShovelConfig.DropEntry selected = pickDrop(level, settings);
        if (selected == null || selected.weight() <= 0) {
            return;
        }

        int amount = selected.minCount();
        if (selected.maxCount() > selected.minCount()) {
            amount += level.random.nextInt(selected.maxCount() - selected.minCount() + 1);
        }

        popResource(level, pos, new ItemStack(selected.item(), amount));
        if (level instanceof ServerLevel serverLevel) {
            LuckyShovelConfig.DropParticleSettings particles = LuckyShovelConfig.dropParticleSettings();
            if (particles.enabled() && particles.count() > 0) {
                serverLevel.sendParticles(
                        particles.particleType(),
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

    private LuckyShovelConfig.DropEntry pickDrop(Level level, LuckyShovelConfig.BonusDropSettings settings) {
        int totalWeight = 0;
        for (LuckyShovelConfig.DropEntry entry : settings.entries()) {
            totalWeight += Math.max(0, entry.weight());
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyShovelConfig.DropEntry entry : settings.entries()) {
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

    private void tryTriggerLuckyBlockTransform(Level level, BlockPos pos) {
        tryTriggerLuckyBlockTransform(level, pos, false);
    }

    private void tryTriggerSandstorm(Level level, BlockPos pos, Entity source) {
        tryTriggerSandstorm(level, pos, source, false);
    }

    private void tryTriggerSandstorm(Level level, BlockPos pos, Entity source, boolean force) {
        LuckyShovelConfig.SandstormSettings settings = LuckyShovelConfig.sandstormSettings();
        if (!settings.enabled() || (!force && level.random.nextFloat() > settings.chance())) {
            return;
        }

        double radius = Math.max(1.0D, settings.radius());
        AABB area = new AABB(pos).inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != null
                        && entity.isAlive()
                        && entity != source
                        && !(entity instanceof Player));

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, settings.durationTicks(), settings.slownessAmplifier(), false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, settings.durationTicks(), settings.weaknessAmplifier(), false, true, true));
        }

        if (level instanceof ServerLevel serverLevel && settings.particleCount() > 0) {
            serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.8D,
                    pos.getZ() + 0.5D,
                    settings.particleCount(),
                    radius * 0.35D,
                    0.35D,
                    radius * 0.35D,
                    0.03D
            );
            serverLevel.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.9D,
                    pos.getZ() + 0.5D,
                    Math.max(2, settings.particleCount() / 6),
                    radius * 0.25D,
                    0.2D,
                    radius * 0.25D,
                    0.0D
            );
        }
    }

    private void tryTriggerTreasureBurst(Level level, BlockPos pos) {
        tryTriggerTreasureBurst(level, pos, false);
    }

    private void tryTriggerTreasureBurst(Level level, BlockPos pos, boolean force) {
        LuckyShovelConfig.TreasureBurstSettings settings = LuckyShovelConfig.treasureBurstSettings();
        if (!settings.enabled() || (!force && level.random.nextFloat() > settings.chance())) {
            return;
        }

        int rolls = settings.minRolls();
        if (settings.maxRolls() > settings.minRolls()) {
            rolls += level.random.nextInt(settings.maxRolls() - settings.minRolls() + 1);
        }

        for (int i = 0; i < rolls; i++) {
            LuckyShovelConfig.DropEntry selected = pickDrop(level, new LuckyShovelConfig.BonusDropSettings(true, 1.0F, settings.entries()));
            if (selected == null || selected.weight() <= 0) {
                continue;
            }

            int amount = selected.minCount();
            if (selected.maxCount() > selected.minCount()) {
                amount += level.random.nextInt(selected.maxCount() - selected.minCount() + 1);
            }

            popResource(level, pos, new ItemStack(selected.item(), amount));
        }

        if (level instanceof ServerLevel serverLevel && settings.particleCount() > 0) {
            serverLevel.sendParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.8D,
                    pos.getZ() + 0.5D,
                    settings.particleCount(),
                    0.35D,
                    0.25D,
                    0.35D,
                    0.02D
            );
        }
    }

    private void tryTriggerLuckyBlockTransform(Level level, BlockPos pos, boolean force) {
        LuckyShovelConfig.LuckyBlockTransformSettings settings = LuckyShovelConfig.luckyBlockTransformSettings();
        if (!settings.enabled() || (!force && level.random.nextFloat() > settings.chance())) {
            return;
        }

        Block selected = settings.chooseBlock(level.random);
        if (selected == null) {
            return;
        }

        level.setBlock(pos, selected.defaultBlockState(), 3);
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

        LuckyShovelConfig.TemporaryEnchantSettings settings = LuckyShovelConfig.temporaryEnchantSettings();
        if (!settings.enabled() || settings.entries().isEmpty()) {
            return;
        }

        if (!force && level.random.nextFloat() > settings.chance()) {
            return;
        }

        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, Integer> currentLevels = currentEnchantmentLevels(stack, enchantmentRegistry);

        List<LuckyShovelConfig.EnchantmentEntry> candidates = new ArrayList<>();
        for (LuckyShovelConfig.EnchantmentEntry entry : settings.entries()) {
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
        List<LuckyShovelConfig.EnchantmentEntry> pool = new ArrayList<>(candidates);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            LuckyShovelConfig.EnchantmentEntry selected = pickWeightedEnchant(level, pool);
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

    private void spawnTemporaryEnchantParticles(Level level, Entity holderEntity, boolean applied) {
        if (!(level instanceof ServerLevel serverLevel) || holderEntity == null) {
            return;
        }

        LuckyShovelConfig.TemporaryEnchantParticleSettings settings = LuckyShovelConfig.temporaryEnchantSettings().effectParticles();
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

    private LuckyShovelConfig.EnchantmentEntry pickWeightedEnchant(Level level, List<LuckyShovelConfig.EnchantmentEntry> entries) {
        int totalWeight = 0;
        for (LuckyShovelConfig.EnchantmentEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyShovelConfig.EnchantmentEntry entry : entries) {
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

    private record EnchantLevel(Identifier id, int level) {
    }
}
