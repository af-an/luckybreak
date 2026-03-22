package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spawns a chest and fills random slots with random weighted loot.
 *
 * JSON fields:
 *   rolls_min / rolls_max: number of loot stacks to place
 *   loot: [
 *     {
 *       "item": "minecraft:book",
 *       "weight": 2,
 *       "enchant_cost_min": 12,
 *       "enchant_cost_max": 30
 *     },
 *     {
 *       "item": "minecraft:potion",
 *       "weight": 2,
 *       "potion": "minecraft:healing"
 *     }
 *   ]
 */
public class LootChestEvent implements LuckyEvent {

    private enum LimitedCategory {
        NONE,
        MUSIC_DISC,
        SPAWN_EGG,
        ENCHANTED_BOOK
    }

    private record LootEntry(
        Item item,
        double weight,
        int countMin,
        int countMax,
        int enchantCostMin,
        int enchantCostMax,
        LimitedCategory limitedCategory,
        Holder<Potion> potion
    ) {}

    private record SpawnBaseEntry(Block block, double weight) {}

    private final int rollsMin;
    private final int rollsMax;
    private final List<LootEntry> lootPool;
    private final boolean allowDuplicateItems;
    private final List<SpawnBaseEntry> spawnBaseBlocks;
    private final int chestYOffset;
    private final int spawnParticleCount;
    private final SoundEvent spawnSound;

    private LootChestEvent(
        int rollsMin,
        int rollsMax,
        List<LootEntry> lootPool,
        boolean allowDuplicateItems,
        List<SpawnBaseEntry> spawnBaseBlocks,
        int chestYOffset,
        int spawnParticleCount,
        SoundEvent spawnSound
    ) {
        this.rollsMin = rollsMin;
        this.rollsMax = rollsMax;
        this.lootPool = lootPool;
        this.allowDuplicateItems = allowDuplicateItems;
        this.spawnBaseBlocks = spawnBaseBlocks;
        this.chestYOffset = chestYOffset;
        this.spawnParticleCount = spawnParticleCount;
        this.spawnSound = spawnSound;
    }

    public static LootChestEvent fromJson(JsonObject obj) {
        int rollsMin = obj.has("rolls_min") ? obj.get("rolls_min").getAsInt() : 5;
        int rollsMax = obj.has("rolls_max") ? obj.get("rolls_max").getAsInt() : 9;
        boolean allowDuplicateItems = !obj.has("allow_duplicate_items") || obj.get("allow_duplicate_items").getAsBoolean();

        if (rollsMin < 1) rollsMin = 1;
        if (rollsMax < rollsMin) rollsMax = rollsMin;

        int chestYOffset = obj.has("chest_y_offset") ? obj.get("chest_y_offset").getAsInt() : 0;
        if (chestYOffset < 0) chestYOffset = 0;

        int spawnParticleCount = obj.has("spawn_particle_count") ? obj.get("spawn_particle_count").getAsInt() : 0;
        if (spawnParticleCount < 0) spawnParticleCount = 0;

        SoundEvent spawnSound = null;
        if (obj.has("spawn_sound")) {
            try {
                ResourceLocation soundId = ResourceLocation.parse(obj.get("spawn_sound").getAsString());
                if (BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
                    spawnSound = BuiltInRegistries.SOUND_EVENT.getValue(soundId);
                }
            } catch (Exception ignored) {
            }
        }

        List<SpawnBaseEntry> spawnBaseBlocks = new ArrayList<>();
        if (obj.has("spawn_base_blocks") && obj.get("spawn_base_blocks").isJsonArray()) {
            JsonArray baseBlocksArray = obj.getAsJsonArray("spawn_base_blocks");
            for (JsonElement baseElement : baseBlocksArray) {
                ResourceLocation blockId = null;
                double weight = 1.0;

                try {
                    if (baseElement.isJsonPrimitive()) {
                        blockId = ResourceLocation.parse(baseElement.getAsString());
                    } else if (baseElement.isJsonObject()) {
                        JsonObject baseObj = baseElement.getAsJsonObject();
                        if (!baseObj.has("block")) {
                            continue;
                        }
                        blockId = ResourceLocation.parse(baseObj.get("block").getAsString());
                        if (baseObj.has("weight")) {
                            weight = baseObj.get("weight").getAsDouble();
                        }
                    }

                    if (blockId == null || weight <= 0.0) {
                        continue;
                    }

                    Block block = BuiltInRegistries.BLOCK.getValue(blockId);
                    if (block != null && block != Blocks.AIR) {
                        spawnBaseBlocks.add(new SpawnBaseEntry(block, weight));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        List<LootEntry> pool = new ArrayList<>();
        if (obj.has("loot") && obj.get("loot").isJsonArray()) {
            JsonArray loot = obj.getAsJsonArray("loot");
            for (JsonElement element : loot) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject lo = element.getAsJsonObject();
                if (!lo.has("item")) {
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(lo.get("item").getAsString()));
                if (item == null || item == Items.AIR) {
                    continue;
                }

                double weight = lo.has("weight") ? lo.get("weight").getAsDouble() : 1.0;
                int countMin = lo.has("count_min") ? lo.get("count_min").getAsInt() : 1;
                int countMax = lo.has("count_max") ? lo.get("count_max").getAsInt() : countMin;
                int enchantCostMin = lo.has("enchant_cost_min") ? lo.get("enchant_cost_min").getAsInt() : 0;
                int enchantCostMax = lo.has("enchant_cost_max") ? lo.get("enchant_cost_max").getAsInt() : enchantCostMin;

                Holder<Potion> potion = null;
                if (lo.has("potion")) {
                    Potion p = BuiltInRegistries.POTION.getValue(ResourceLocation.parse(lo.get("potion").getAsString()));
                    if (p != null) {
                        potion = BuiltInRegistries.POTION.wrapAsHolder(p);
                    }
                }

                if (weight <= 0.0) continue;
                if (countMin < 1) countMin = 1;
                if (countMax < countMin) countMax = countMin;
                if (enchantCostMin < 0) enchantCostMin = 0;
                if (enchantCostMax < enchantCostMin) enchantCostMax = enchantCostMin;

                LimitedCategory limitedCategory = classifyLimitedCategory(item, enchantCostMax);
                pool.add(new LootEntry(item, weight, countMin, countMax, enchantCostMin, enchantCostMax, limitedCategory, potion));
            }
        }

        if (pool.isEmpty()) {
            pool.add(new LootEntry(Items.IRON_INGOT, 10, 2, 8, 0, 0, LimitedCategory.NONE, null));
            pool.add(new LootEntry(Items.GOLD_INGOT, 8, 2, 6, 0, 0, LimitedCategory.NONE, null));
            pool.add(new LootEntry(Items.EMERALD, 5, 1, 4, 0, 0, LimitedCategory.NONE, null));
            pool.add(new LootEntry(Items.DIAMOND, 3, 1, 2, 0, 0, LimitedCategory.NONE, null));
        }

        return new LootChestEvent(rollsMin, rollsMax, pool, allowDuplicateItems, spawnBaseBlocks, chestYOffset, spawnParticleCount, spawnSound);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockPos basePos = pos;
        BlockPos chestPos = pos.above(chestYOffset);

        if (!spawnBaseBlocks.isEmpty()) {
            Block baseBlock = chooseSpawnBaseBlock(level.getRandom());
            if (baseBlock == null) {
                baseBlock = spawnBaseBlocks.get(level.getRandom().nextInt(spawnBaseBlocks.size())).block;
            }
            level.setBlock(basePos, baseBlock.defaultBlockState(), 3);
            chestPos = basePos.above(Math.max(1, chestYOffset));
        }

        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, player.getDirection().getOpposite()), 3);

        if (spawnParticleCount > 0) {
            level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                chestPos.getX() + 0.5,
                chestPos.getY() + 1.0,
                chestPos.getZ() + 0.5,
                spawnParticleCount,
                0.35,
                0.3,
                0.35,
                0.02
            );
        }

        if (spawnSound != null) {
            level.playSound(
                null,
                chestPos,
                spawnSound,
                SoundSource.BLOCKS,
                1.0f,
                1.0f
            );
        }

        var be = level.getBlockEntity(chestPos);
        if (!(be instanceof Container container)) {
            return;
        }

        RandomSource random = level.getRandom();
        int rolls = rollsMin + random.nextInt(rollsMax - rollsMin + 1);

        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            availableSlots.add(i);
        }
        EnumSet<LimitedCategory> usedLimitedCategories = EnumSet.noneOf(LimitedCategory.class);
        Set<Item> usedItems = new HashSet<>();

        for (int i = 0; i < rolls && !availableSlots.isEmpty(); i++) {
            LootEntry entry = chooseWeighted(random, usedLimitedCategories, usedItems);
            if (entry == null) {
                break;
            }

            int slotIndex = random.nextInt(availableSlots.size());
            int slot = availableSlots.remove(slotIndex);
            container.setItem(slot, createStack(level, random, entry));
            if (entry.limitedCategory != LimitedCategory.NONE) {
                usedLimitedCategories.add(entry.limitedCategory);
            }
            if (!allowDuplicateItems) {
                usedItems.add(entry.item);
            }
        }
    }

    private ItemStack createStack(ServerLevel level, RandomSource random, LootEntry entry) {
        int count = entry.countMin + random.nextInt(entry.countMax - entry.countMin + 1);

        ItemStack stack;
        if (entry.potion != null && isPotionItem(entry.item)) {
            stack = PotionContents.createItemStack(entry.item, entry.potion);
            stack.setCount(count);
        } else {
            stack = new ItemStack(entry.item, count);
        }

        if (entry.enchantCostMax > 0) {
            int cost = entry.enchantCostMin + random.nextInt(entry.enchantCostMax - entry.enchantCostMin + 1);
            stack = EnchantmentHelper.enchantItem(random, stack, cost, level.registryAccess(), Optional.empty());
        }

        return stack;
    }

    private boolean isPotionItem(Item item) {
        return item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
    }

    private static LimitedCategory classifyLimitedCategory(Item item, int enchantCostMax) {
        if (item == Items.ENCHANTED_BOOK || (item == Items.BOOK && enchantCostMax > 0)) {
            return LimitedCategory.ENCHANTED_BOOK;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return LimitedCategory.NONE;
        }

        String path = id.getPath();
        if (path.startsWith("music_disc_")) {
            return LimitedCategory.MUSIC_DISC;
        }
        if (path.endsWith("_spawn_egg")) {
            return LimitedCategory.SPAWN_EGG;
        }
        return LimitedCategory.NONE;
    }

    private LootEntry chooseWeighted(RandomSource random, EnumSet<LimitedCategory> usedLimitedCategories, Set<Item> usedItems) {
        double totalWeight = 0.0;
        for (LootEntry entry : lootPool) {
            if (entry.limitedCategory != LimitedCategory.NONE && usedLimitedCategories.contains(entry.limitedCategory)) {
                continue;
            }
            if (!allowDuplicateItems && usedItems.contains(entry.item)) {
                continue;
            }
            totalWeight += entry.weight;
        }

        if (totalWeight <= 0.0) {
            return null;
        }

        double pick = random.nextDouble() * totalWeight;
        double running = 0.0;
        for (LootEntry entry : lootPool) {
            if (entry.limitedCategory != LimitedCategory.NONE && usedLimitedCategories.contains(entry.limitedCategory)) {
                continue;
            }
            if (!allowDuplicateItems && usedItems.contains(entry.item)) {
                continue;
            }
            running += entry.weight;
            if (pick < running) {
                return entry;
            }
        }

        return null;
    }

    private Block chooseSpawnBaseBlock(RandomSource random) {
        double totalWeight = 0.0;
        for (SpawnBaseEntry entry : spawnBaseBlocks) {
            totalWeight += entry.weight;
        }
        if (totalWeight <= 0.0) {
            return null;
        }

        double pick = random.nextDouble() * totalWeight;
        double running = 0.0;
        for (SpawnBaseEntry entry : spawnBaseBlocks) {
            running += entry.weight;
            if (pick < running) {
                return entry.block;
            }
        }
        return spawnBaseBlocks.get(spawnBaseBlocks.size() - 1).block;
    }
}
