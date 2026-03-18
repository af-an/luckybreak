package com.luckybreak.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luckybreak.LuckyBreak;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LuckyHoeConfig {

    private static final String CONFIG_PATH = "data/luckybreak/items/lucky_hoe.json";
    private static final int DEFAULT_DURABILITY = 32;
    private static final float BASE_ATTACK_DAMAGE = 1.0F;
    private static final float BASE_ATTACK_SPEED = 4.0F;
    private static final float DEFAULT_ATTACK_DAMAGE = 0.0F;
    private static final float DEFAULT_ATTACK_SPEED = -3.0F;

    private static int durability = DEFAULT_DURABILITY;
    private static float attackDamage = DEFAULT_ATTACK_DAMAGE;
    private static float attackSpeed = DEFAULT_ATTACK_SPEED;
    private static TillEffectSettings tillEffectSettings = TillEffectSettings.defaults();
    private static NuggetDropSettings nuggetDropSettings = NuggetDropSettings.defaults();
    private static MobHitFoodDropSettings mobHitFoodDropSettings = MobHitFoodDropSettings.defaults();
    private static VillagerSpawnSettings villagerSpawnSettings = VillagerSpawnSettings.defaults();
    private static CropBloomSettings cropBloomSettings = CropBloomSettings.defaults();
    private static ChickenParadeSettings chickenParadeSettings = ChickenParadeSettings.defaults();
    private static PinataHitSettings pinataHitSettings = PinataHitSettings.defaults();
    private static boolean loaded;

    private LuckyHoeConfig() {
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

    public static TillEffectSettings tillEffectSettings() {
        ensureLoaded();
        return tillEffectSettings;
    }

    public static NuggetDropSettings nuggetDropSettings() {
        ensureLoaded();
        return nuggetDropSettings;
    }

    public static MobHitFoodDropSettings mobHitFoodDropSettings() {
        ensureLoaded();
        return mobHitFoodDropSettings;
    }

    public static VillagerSpawnSettings villagerSpawnSettings() {
        ensureLoaded();
        return villagerSpawnSettings;
    }

    public static CropBloomSettings cropBloomSettings() {
        ensureLoaded();
        return cropBloomSettings;
    }

    public static ChickenParadeSettings chickenParadeSettings() {
        ensureLoaded();
        return chickenParadeSettings;
    }

    public static PinataHitSettings pinataHitSettings() {
        ensureLoaded();
        return pinataHitSettings;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        durability = DEFAULT_DURABILITY;
        attackDamage = DEFAULT_ATTACK_DAMAGE;
        attackSpeed = DEFAULT_ATTACK_SPEED;
        tillEffectSettings = TillEffectSettings.defaults();
        nuggetDropSettings = NuggetDropSettings.defaults();
        mobHitFoodDropSettings = MobHitFoodDropSettings.defaults();
        villagerSpawnSettings = VillagerSpawnSettings.defaults();
        cropBloomSettings = CropBloomSettings.defaults();
        chickenParadeSettings = ChickenParadeSettings.defaults();
        pinataHitSettings = PinataHitSettings.defaults();

        InputStream input = LuckyHoeConfig.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
        if (input == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Missing Lucky Hoe config: {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            durability = parseInt(root, "durability", DEFAULT_DURABILITY, 1, 10000);
            attackDamage = parseAttackDamageModifier(root);
            attackSpeed = parseAttackSpeedModifier(root);
            tillEffectSettings = parseTillEffectSettings(root);
            nuggetDropSettings = parseNuggetDropSettings(root);
            mobHitFoodDropSettings = parseMobHitFoodDropSettings(root);
            villagerSpawnSettings = parseVillagerSpawnSettings(root);
            cropBloomSettings = parseCropBloomSettings(root);
            chickenParadeSettings = parseChickenParadeSettings(root);
            pinataHitSettings = parsePinataHitSettings(root);
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load Lucky Hoe config", exception);
        }
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

    private static VillagerSpawnSettings parseVillagerSpawnSettings(JsonObject root) {
        if (!root.has("villager_spawn") || !root.get("villager_spawn").isJsonObject()) {
            return VillagerSpawnSettings.defaults();
        }

        VillagerSpawnSettings defaults = VillagerSpawnSettings.defaults();
        JsonObject obj = root.getAsJsonObject("villager_spawn");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int searchRadius = parseInt(obj, "search_radius", defaults.searchRadius(), 1, 12);
        boolean guaranteeEnabled = parseBoolean(obj, "guarantee_enabled", defaults.guaranteeEnabled());
        int guaranteedMinPerHoe = parseInt(obj, "guaranteed_min_per_hoe", defaults.guaranteedMinPerHoe(), 0, 32);
        return new VillagerSpawnSettings(enabled, chance, searchRadius, guaranteeEnabled, guaranteedMinPerHoe);
    }

    private static CropBloomSettings parseCropBloomSettings(JsonObject root) {
        if (!root.has("crop_bloom") || !root.get("crop_bloom").isJsonObject()) {
            return CropBloomSettings.defaults();
        }

        CropBloomSettings defaults = CropBloomSettings.defaults();
        JsonObject obj = root.getAsJsonObject("crop_bloom");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int radius = parseInt(obj, "radius", defaults.radius(), 1, 12);
        int growthMinSteps = parseInt(obj, "growth_min_steps", defaults.growthMinSteps(), 1, 16);
        int growthMaxSteps = parseInt(obj, "growth_max_steps", defaults.growthMaxSteps(), growthMinSteps, 16);
        int particleCount = parseInt(obj, "particle_count", defaults.particleCount(), 0, 256);
        return new CropBloomSettings(enabled, chance, radius, growthMinSteps, growthMaxSteps, particleCount);
    }

    private static ChickenParadeSettings parseChickenParadeSettings(JsonObject root) {
        if (!root.has("chicken_parade") || !root.get("chicken_parade").isJsonObject()) {
            return ChickenParadeSettings.defaults();
        }

        ChickenParadeSettings defaults = ChickenParadeSettings.defaults();
        JsonObject obj = root.getAsJsonObject("chicken_parade");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int minCount = parseInt(obj, "min_count", defaults.minCount(), 1, 32);
        int maxCount = parseInt(obj, "max_count", defaults.maxCount(), minCount, 32);
        int searchRadius = parseInt(obj, "search_radius", defaults.searchRadius(), 1, 12);
        return new ChickenParadeSettings(enabled, chance, minCount, maxCount, searchRadius);
    }

    private static PinataHitSettings parsePinataHitSettings(JsonObject root) {
        if (!root.has("pinata_hit") || !root.get("pinata_hit").isJsonObject()) {
            return PinataHitSettings.defaults();
        }

        PinataHitSettings defaults = PinataHitSettings.defaults();
        JsonObject obj = root.getAsJsonObject("pinata_hit");
        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        int minRolls = parseInt(obj, "min_rolls", defaults.minRolls(), 1, 16);
        int maxRolls = parseInt(obj, "max_rolls", defaults.maxRolls(), minRolls, 16);
        int particleCount = parseInt(obj, "particle_count", defaults.particleCount(), 0, 256);
        List<FoodDropEntry> entries = parseFoodDropEntries(obj, defaults.entries());
        if (entries.isEmpty()) {
            entries = defaults.entries();
        }

        return new PinataHitSettings(enabled, chance, minRolls, maxRolls, particleCount, List.copyOf(entries));
    }

    private static MobHitFoodDropSettings parseMobHitFoodDropSettings(JsonObject root) {
        String section = root.has("mob_hit_food_bonus_drops") && root.get("mob_hit_food_bonus_drops").isJsonObject()
                ? "mob_hit_food_bonus_drops"
                : "mob_hit_food_drop";

        if (!root.has(section) || !root.get(section).isJsonObject()) {
            return MobHitFoodDropSettings.defaults();
        }

        MobHitFoodDropSettings defaults = MobHitFoodDropSettings.defaults();
        JsonObject obj = root.getAsJsonObject(section);

        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        float chance = parseFloat(obj, "chance", defaults.chance(), 0.0F, 1.0F);
        List<FoodDropEntry> entries = parseFoodDropEntries(obj, defaults.entries());
        if (entries.isEmpty()) {
            entries = defaults.entries();
        }

        return new MobHitFoodDropSettings(enabled, chance, List.copyOf(entries));
    }

    private static List<FoodDropEntry> parseFoodDropEntries(JsonObject root, List<FoodDropEntry> fallback) {
        if (!root.has("entries") || !root.get("entries").isJsonArray()) {
            return fallback;
        }

        List<FoodDropEntry> entries = new ArrayList<>();
        JsonArray array = root.getAsJsonArray("entries");
        for (var element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            try {
                JsonObject obj = element.getAsJsonObject();
                String itemId = parseString(obj, "item", "");
                if (itemId.isBlank()) {
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
                if (item == null || item == Items.AIR) {
                    continue;
                }

                int weight = parseInt(obj, "weight", 1, 0, 100000);
                int minCount = parseInt(obj, "min_count", 1, 1, 64);
                int maxCount = parseInt(obj, "max_count", minCount, minCount, 64);
                if (weight <= 0) {
                    continue;
                }

                entries.add(new FoodDropEntry(item, weight, minCount, maxCount));
            } catch (Exception ignored) {
            }
        }

        return entries;
    }

    private static NuggetDropSettings parseNuggetDropSettings(JsonObject root) {
        String section = root.has("till_bonus_drops") && root.get("till_bonus_drops").isJsonObject()
                ? "till_bonus_drops"
                : "nugget_drop";

        if (!root.has(section) || !root.get(section).isJsonObject()) {
            return NuggetDropSettings.defaults();
        }

        JsonObject obj = root.getAsJsonObject(section);
        boolean enabled = parseBoolean(obj, "enabled", true);
        float chance = parseFloat(obj, "chance", 0.35F, 0.0F, 1.0F);
        int minCount = parseInt(obj, "min_count", 1, 1, 64);
        int maxCount = parseInt(obj, "max_count", 2, minCount, 64);
        return new NuggetDropSettings(enabled, chance, minCount, maxCount);
    }

    private static TillEffectSettings parseTillEffectSettings(JsonObject root) {
        if (!root.has("till_effect") || !root.get("till_effect").isJsonObject()) {
            return TillEffectSettings.defaults();
        }

        TillEffectSettings defaults = TillEffectSettings.defaults();
        JsonObject obj = root.getAsJsonObject("till_effect");

        boolean enabled = parseBoolean(obj, "enabled", defaults.enabled());
        boolean waterFarmland = parseBoolean(obj, "water_farmland", defaults.waterFarmland());
        float plantChance = parseFloat(obj, "plant_chance", defaults.plantChance(), 0.0F, 1.0F);
        float growChance = parseFloat(obj, "grow_chance", defaults.growChance(), 0.0F, 1.0F);
        int growthMinSteps = parseInt(obj, "growth_min_steps", defaults.growthMinSteps(), 0, 16);
        int growthMaxSteps = parseInt(obj, "growth_max_steps", defaults.growthMaxSteps(), 0, 16);
        if (growthMaxSteps < growthMinSteps) {
            growthMaxSteps = growthMinSteps;
        }

        boolean includeAllSeeds = parseBoolean(obj, "include_all_seed_items", defaults.includeAllSeedItems());
        boolean includeAllFoods = parseBoolean(obj, "include_all_food_items", defaults.includeAllFoodItems());
        int seedWeight = parseInt(obj, "seed_weight", defaults.seedWeight(), 0, 100000);
        int foodWeight = parseInt(obj, "food_weight", defaults.foodWeight(), 0, 100000);
        boolean plantableOnly = parseBoolean(obj, "plantable_only", defaults.plantableOnly());

        List<PlantEntry> customEntries = parseCustomEntries(obj, plantableOnly);
        List<PlantEntry> entries = buildPlantEntries(customEntries, includeAllSeeds, includeAllFoods, seedWeight, foodWeight, plantableOnly);

        if (entries.isEmpty()) {
            entries = defaults.plantEntries();
        }

        return new TillEffectSettings(
                enabled,
                waterFarmland,
                plantChance,
                growChance,
                growthMinSteps,
                growthMaxSteps,
                includeAllSeeds,
                includeAllFoods,
                seedWeight,
                foodWeight,
                plantableOnly,
                List.copyOf(entries)
        );
    }

    private static List<PlantEntry> buildPlantEntries(
            List<PlantEntry> customEntries,
            boolean includeAllSeeds,
            boolean includeAllFoods,
            int seedWeight,
            int foodWeight,
            boolean plantableOnly
    ) {
        Map<Item, Integer> weights = new LinkedHashMap<>();

        for (PlantEntry entry : customEntries) {
            if (entry.item() == null || entry.item() == Items.AIR) {
                continue;
            }
            weights.put(entry.item(), Math.max(0, entry.weight()));
        }

        if (includeAllSeeds || includeAllFoods) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == null || item == Items.AIR) {
                    continue;
                }

                ItemStack stack = item.getDefaultInstance();
                boolean isSeed = stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS);
                boolean isFood = stack.has(DataComponents.FOOD);

                if (!isSeed && !isFood) {
                    continue;
                }
                if ((!includeAllSeeds && isSeed) || (!includeAllFoods && isFood && !isSeed)) {
                    continue;
                }
                if (plantableOnly && !isPlantable(item)) {
                    continue;
                }

                int weight = isSeed ? seedWeight : foodWeight;
                if (weight <= 0) {
                    continue;
                }

                weights.putIfAbsent(item, weight);
            }
        }

        List<PlantEntry> entries = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : weights.entrySet()) {
            int weight = Math.max(0, entry.getValue());
            if (weight <= 0) {
                continue;
            }
            entries.add(new PlantEntry(entry.getKey(), weight));
        }

        return entries;
    }

    private static List<PlantEntry> parseCustomEntries(JsonObject root, boolean plantableOnly) {
        List<PlantEntry> entries = new ArrayList<>();
        if (!root.has("custom_entries") || !root.get("custom_entries").isJsonArray()) {
            return entries;
        }

        JsonArray array = root.getAsJsonArray("custom_entries");
        for (var element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            try {
                JsonObject obj = element.getAsJsonObject();
                String itemId = parseString(obj, "item", "");
                if (itemId.isBlank()) {
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
                if (item == null || item == Items.AIR) {
                    continue;
                }
                if (plantableOnly && !isPlantable(item)) {
                    continue;
                }

                int weight = parseInt(obj, "weight", 1, 0, 100000);
                if (weight <= 0) {
                    continue;
                }

                entries.add(new PlantEntry(item, weight));
            } catch (Exception ignored) {
            }
        }

        return entries;
    }

    private static boolean isPlantable(Item item) {
        Block block = Block.byItem(item);
        return block != Blocks.AIR;
    }

    private static int parseInt(JsonObject root, String field, int fallback, int min, int max) {
        if (!root.has(field)) {
            return fallback;
        }
        try {
            int value = root.get(field).getAsInt();
            return Math.max(min, Math.min(max, value));
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
            return Math.max(min, Math.min(max, value));
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
            return value == null ? fallback : value.trim();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record PlantEntry(Item item, int weight) {
    }

    public record NuggetDropSettings(boolean enabled, float chance, int minCount, int maxCount) {
        public static NuggetDropSettings defaults() {
            return new NuggetDropSettings(true, 0.35F, 1, 2);
        }
    }

    public record FoodDropEntry(Item item, int weight, int minCount, int maxCount) {
    }

    public record MobHitFoodDropSettings(boolean enabled, float chance, List<FoodDropEntry> entries) {
        public static MobHitFoodDropSettings defaults() {
            return new MobHitFoodDropSettings(
                    true,
                    0.30F,
                    List.of(
                            new FoodDropEntry(Items.APPLE, 24, 1, 1),
                            new FoodDropEntry(Items.POTATO, 22, 1, 2),
                            new FoodDropEntry(Items.POISONOUS_POTATO, 10, 1, 1),
                            new FoodDropEntry(Items.CARROT, 22, 1, 2),
                            new FoodDropEntry(Items.GOLDEN_CARROT, 5, 1, 1),
                            new FoodDropEntry(Items.CAKE, 1, 1, 1)
                    )
            );
        }
    }

    public record VillagerSpawnSettings(boolean enabled, float chance, int searchRadius, boolean guaranteeEnabled, int guaranteedMinPerHoe) {
        public static VillagerSpawnSettings defaults() {
            return new VillagerSpawnSettings(true, 0.20F, 4, false, 1);
        }
    }

    public record CropBloomSettings(
            boolean enabled,
            float chance,
            int radius,
            int growthMinSteps,
            int growthMaxSteps,
            int particleCount
    ) {
        public static CropBloomSettings defaults() {
            return new CropBloomSettings(true, 0.22F, 3, 1, 2, 12);
        }
    }

    public record ChickenParadeSettings(
            boolean enabled,
            float chance,
            int minCount,
            int maxCount,
            int searchRadius
    ) {
        public static ChickenParadeSettings defaults() {
            return new ChickenParadeSettings(true, 0.08F, 1, 3, 4);
        }
    }

    public record PinataHitSettings(
            boolean enabled,
            float chance,
            int minRolls,
            int maxRolls,
            int particleCount,
            List<FoodDropEntry> entries
    ) {
        public static PinataHitSettings defaults() {
            return new PinataHitSettings(
                    true,
                    0.25F,
                    2,
                    4,
                    18,
                    List.of(
                            new FoodDropEntry(Items.WHEAT, 22, 1, 3),
                            new FoodDropEntry(Items.BEETROOT_SEEDS, 16, 1, 3),
                            new FoodDropEntry(Items.PUMPKIN_SEEDS, 14, 1, 2),
                            new FoodDropEntry(Items.MELON_SEEDS, 14, 1, 2),
                            new FoodDropEntry(Items.CARROT, 16, 1, 2),
                            new FoodDropEntry(Items.POTATO, 16, 1, 2),
                            new FoodDropEntry(Items.GOLDEN_CARROT, 2, 1, 1)
                    )
            );
        }
    }

    public record TillEffectSettings(
            boolean enabled,
            boolean waterFarmland,
            float plantChance,
            float growChance,
            int growthMinSteps,
            int growthMaxSteps,
            boolean includeAllSeedItems,
            boolean includeAllFoodItems,
            int seedWeight,
            int foodWeight,
            boolean plantableOnly,
            List<PlantEntry> plantEntries
    ) {
        public static TillEffectSettings defaults() {
            return new TillEffectSettings(
                    true,
                    true,
                    0.90F,
                    0.65F,
                    1,
                    3,
                    true,
                    true,
                    12,
                    4,
                    true,
                    List.of(
                            new PlantEntry(Items.WHEAT_SEEDS, 18),
                            new PlantEntry(Items.BEETROOT_SEEDS, 14),
                            new PlantEntry(Items.CARROT, 14),
                            new PlantEntry(Items.POTATO, 14),
                            new PlantEntry(Items.MELON_SEEDS, 10),
                            new PlantEntry(Items.PUMPKIN_SEEDS, 10)
                    )
            );
        }
    }
}