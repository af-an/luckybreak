package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

/**
 * Sets time to night, ensures normal difficulty if currently peaceful,
 * and spawns mounted skeleton riders on zombie/skeleton horses.
 *
 * JSON fields:
 *   min_count: 4             (default 4)
 *   max_count: 8             (default 8)
 *   count: 6                 (legacy fallback)
 *   xp_multiplier: 3.0        (default 3.0)
 *   bonus_gold_min: 1         (default 1)
 *   bonus_gold_max: 3         (default 3)
 *   message: "The Night Riders Approach"  (default this value)
 *   message_color: "#FF0000"              (default red)
 *   thunderstorm: true         (default true)
 *   thunder_duration_ticks: 12000 (default 12000)
 *   radius: 12.0             (default 12)
 *   night_time: 13000        (default 13000)
 */
public class NightRidersEvent implements LuckyEvent {

    public static final String NIGHT_RIDER_XP_TAG = "luckybreak_night_rider_xp3";
     public static final String NIGHT_RIDER_XP_MULT_TAG_PREFIX = "luckybreak_night_rider_xp_mult_";
    public static final String NIGHT_RIDER_GOLD_MIN_TAG_PREFIX = "luckybreak_night_rider_gold_min_";
    public static final String NIGHT_RIDER_GOLD_MAX_TAG_PREFIX = "luckybreak_night_rider_gold_max_";
    public static final String NIGHT_RIDER_IRON_MIN_TAG_PREFIX = "luckybreak_night_rider_iron_min_";
    public static final String NIGHT_RIDER_IRON_MAX_TAG_PREFIX = "luckybreak_night_rider_iron_max_";
    public static final String NIGHT_RIDER_EMERALD_MIN_TAG_PREFIX = "luckybreak_night_rider_emerald_min_";
    public static final String NIGHT_RIDER_EMERALD_MAX_TAG_PREFIX = "luckybreak_night_rider_emerald_max_";
    public static final String NIGHT_RIDER_DIAMOND_MIN_TAG_PREFIX = "luckybreak_night_rider_diamond_min_";
    public static final String NIGHT_RIDER_DIAMOND_MAX_TAG_PREFIX = "luckybreak_night_rider_diamond_max_";
    public static final String NIGHT_RIDER_GOLD_WEIGHT_TAG_PREFIX = "luckybreak_night_rider_gold_weight_";
    public static final String NIGHT_RIDER_IRON_WEIGHT_TAG_PREFIX = "luckybreak_night_rider_iron_weight_";
    public static final String NIGHT_RIDER_EMERALD_WEIGHT_TAG_PREFIX = "luckybreak_night_rider_emerald_weight_";
    public static final String NIGHT_RIDER_DIAMOND_WEIGHT_TAG_PREFIX = "luckybreak_night_rider_diamond_weight_";

    private final int minCount;
    private final int maxCount;
    private final double radius;
    private final long nightTime;
    private final double armorChance;
    private final double armorPieceChance;
    private final double armorEnchantChance;
    private final int armorEnchantCostMin;
    private final int armorEnchantCostMax;
    private final double xpMultiplier;
    private final int bonusGoldMin;
    private final int bonusGoldMax;
    private final int bonusIronMin;
    private final int bonusIronMax;
    private final int bonusEmeraldMin;
    private final int bonusEmeraldMax;
    private final int bonusDiamondMin;
    private final int bonusDiamondMax;
    private final int bonusGoldWeight;
    private final int bonusIronWeight;
    private final int bonusEmeraldWeight;
    private final int bonusDiamondWeight;
    private final String message;
    private final int messageColor;
    private final boolean thunderstorm;
    private final int thunderDurationTicks;

    private NightRidersEvent(int minCount, int maxCount, double radius, long nightTime,
                             double armorChance, double armorPieceChance, double armorEnchantChance,
                             int armorEnchantCostMin, int armorEnchantCostMax,
                             double xpMultiplier,
                             int bonusGoldMin, int bonusGoldMax,
                             int bonusIronMin, int bonusIronMax,
                             int bonusEmeraldMin, int bonusEmeraldMax,
                             int bonusDiamondMin, int bonusDiamondMax,
                             int bonusGoldWeight, int bonusIronWeight,
                             int bonusEmeraldWeight, int bonusDiamondWeight,
                             String message, int messageColor,
                             boolean thunderstorm, int thunderDurationTicks) {
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.radius = radius;
        this.nightTime = nightTime;
        this.armorChance = armorChance;
        this.armorPieceChance = armorPieceChance;
        this.armorEnchantChance = armorEnchantChance;
        this.armorEnchantCostMin = armorEnchantCostMin;
        this.armorEnchantCostMax = armorEnchantCostMax;
        this.xpMultiplier = xpMultiplier;
        this.bonusGoldMin = bonusGoldMin;
        this.bonusGoldMax = bonusGoldMax;
        this.bonusIronMin = bonusIronMin;
        this.bonusIronMax = bonusIronMax;
        this.bonusEmeraldMin = bonusEmeraldMin;
        this.bonusEmeraldMax = bonusEmeraldMax;
        this.bonusDiamondMin = bonusDiamondMin;
        this.bonusDiamondMax = bonusDiamondMax;
        this.bonusGoldWeight = bonusGoldWeight;
        this.bonusIronWeight = bonusIronWeight;
        this.bonusEmeraldWeight = bonusEmeraldWeight;
        this.bonusDiamondWeight = bonusDiamondWeight;
        this.message = message;
        this.messageColor = messageColor;
        this.thunderstorm = thunderstorm;
        this.thunderDurationTicks = thunderDurationTicks;
    }

    public static NightRidersEvent fromJson(JsonObject obj) {
        int fallbackCount = obj.has("count") ? obj.get("count").getAsInt() : 6;
        int minCount = obj.has("min_count") ? obj.get("min_count").getAsInt() : fallbackCount;
        int maxCount = obj.has("max_count") ? obj.get("max_count").getAsInt() : fallbackCount;
        double radius = obj.has("radius") ? obj.get("radius").getAsDouble() : 12.0;
        long nightTime = obj.has("night_time") ? obj.get("night_time").getAsLong() : 13000L;
        double armorChance = obj.has("armor_chance") ? obj.get("armor_chance").getAsDouble() : 0.55;
        double armorPieceChance = obj.has("armor_piece_chance") ? obj.get("armor_piece_chance").getAsDouble() : 0.5;
        double armorEnchantChance = obj.has("armor_enchant_chance") ? obj.get("armor_enchant_chance").getAsDouble() : 0.4;
        int armorEnchantCostMin = obj.has("armor_enchant_cost_min") ? obj.get("armor_enchant_cost_min").getAsInt() : 8;
        int armorEnchantCostMax = obj.has("armor_enchant_cost_max") ? obj.get("armor_enchant_cost_max").getAsInt() : 20;
        double xpMultiplier = obj.has("xp_multiplier") ? obj.get("xp_multiplier").getAsDouble() : 3.0;
        int bonusGoldMin = obj.has("bonus_gold_min") ? obj.get("bonus_gold_min").getAsInt() : 1;
        int bonusGoldMax = obj.has("bonus_gold_max") ? obj.get("bonus_gold_max").getAsInt() : 3;
        int bonusIronMin = obj.has("bonus_iron_min") ? obj.get("bonus_iron_min").getAsInt() : 1;
        int bonusIronMax = obj.has("bonus_iron_max") ? obj.get("bonus_iron_max").getAsInt() : 3;
        int bonusEmeraldMin = obj.has("bonus_emerald_min") ? obj.get("bonus_emerald_min").getAsInt() : 1;
        int bonusEmeraldMax = obj.has("bonus_emerald_max") ? obj.get("bonus_emerald_max").getAsInt() : 1;
        int bonusDiamondMin = obj.has("bonus_diamond_min") ? obj.get("bonus_diamond_min").getAsInt() : 1;
        int bonusDiamondMax = obj.has("bonus_diamond_max") ? obj.get("bonus_diamond_max").getAsInt() : 1;
        int bonusGoldWeight = obj.has("bonus_gold_weight") ? obj.get("bonus_gold_weight").getAsInt() : 40;
        int bonusIronWeight = obj.has("bonus_iron_weight") ? obj.get("bonus_iron_weight").getAsInt() : 40;
        int bonusEmeraldWeight = obj.has("bonus_emerald_weight") ? obj.get("bonus_emerald_weight").getAsInt() : 12;
        int bonusDiamondWeight = obj.has("bonus_diamond_weight") ? obj.get("bonus_diamond_weight").getAsInt() : 8;
        String message = obj.has("message") ? obj.get("message").getAsString() : "The Night Riders Approach";
        int messageColor = parseColor(obj, "message_color", 0xFF0000);
        boolean thunderstorm = !obj.has("thunderstorm") || obj.get("thunderstorm").getAsBoolean();
        int thunderDurationTicks = obj.has("thunder_duration_ticks") ? obj.get("thunder_duration_ticks").getAsInt() : 12000;

        if (minCount < 1) minCount = 1;
        if (maxCount < minCount) maxCount = minCount;
        if (radius < 2.0) radius = 2.0;
        armorChance = clamp01(armorChance);
        armorPieceChance = clamp01(armorPieceChance);
        armorEnchantChance = clamp01(armorEnchantChance);
        if (armorEnchantCostMin < 1) armorEnchantCostMin = 1;
        if (armorEnchantCostMax < armorEnchantCostMin) armorEnchantCostMax = armorEnchantCostMin;
        if (xpMultiplier < 1.0) xpMultiplier = 1.0;
        if (bonusGoldMin < 0) bonusGoldMin = 0;
        if (bonusGoldMax < bonusGoldMin) bonusGoldMax = bonusGoldMin;
        if (bonusIronMin < 0) bonusIronMin = 0;
        if (bonusIronMax < bonusIronMin) bonusIronMax = bonusIronMin;
        if (bonusEmeraldMin < 0) bonusEmeraldMin = 0;
        if (bonusEmeraldMax < bonusEmeraldMin) bonusEmeraldMax = bonusEmeraldMin;
        if (bonusDiamondMin < 0) bonusDiamondMin = 0;
        if (bonusDiamondMax < bonusDiamondMin) bonusDiamondMax = bonusDiamondMin;
        if (bonusGoldWeight < 0) bonusGoldWeight = 0;
        if (bonusIronWeight < 0) bonusIronWeight = 0;
        if (bonusEmeraldWeight < 0) bonusEmeraldWeight = 0;
        if (bonusDiamondWeight < 0) bonusDiamondWeight = 0;
        if (thunderDurationTicks < 1) thunderDurationTicks = 1;

        return new NightRidersEvent(
            minCount,
            maxCount,
            radius,
            nightTime,
            armorChance,
            armorPieceChance,
            armorEnchantChance,
            armorEnchantCostMin,
                armorEnchantCostMax,
                xpMultiplier,
                bonusGoldMin,
                bonusGoldMax,
                bonusIronMin,
                bonusIronMax,
                bonusEmeraldMin,
                bonusEmeraldMax,
                bonusDiamondMin,
                bonusDiamondMax,
                bonusGoldWeight,
                bonusIronWeight,
                bonusEmeraldWeight,
                bonusDiamondWeight,
                message,
                messageColor,
                thunderstorm,
                thunderDurationTicks
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (level.getServer().getWorldData().getDifficulty() == Difficulty.PEACEFUL) {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
        }

        level.setDayTime(nightTime);
        if (thunderstorm) {
            level.setWeatherParameters(0, thunderDurationTicks, true, true);
        }
        if (message != null && !message.isBlank()) {
            player.displayClientMessage(
                    Component.literal(message).withStyle(style -> style.withColor(TextColor.fromRgb(messageColor))),
                    false
            );
        }

        RandomSource random = level.getRandom();
        int count = minCount + random.nextInt(maxCount - minCount + 1);
        BlockPos center = player.blockPosition();

        for (int i = 0; i < count; i++) {
            boolean useSkeletonHorse = (i % 2) == 0;
            spawnMountedRider(level, center, random, useSkeletonHorse);
        }
    }

    private void spawnMountedRider(ServerLevel level, BlockPos center, RandomSource random, boolean useSkeletonHorse) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = radius * (0.5 + random.nextDouble() * 0.5);
            int x = (int) Math.floor(center.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(center.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= level.getMinY()) {
                continue;
            }

            Entity horse = useSkeletonHorse
                    ? EntityType.SKELETON_HORSE.create(level, EntitySpawnReason.TRIGGERED)
                    : EntityType.ZOMBIE_HORSE.create(level, EntitySpawnReason.TRIGGERED);
            if (horse == null) {
                continue;
            }
            horse.setPos(x + 0.5, y, z + 0.5);
            horse.setYRot(random.nextFloat() * 360.0f);
            if (!level.noCollision(horse)) {
                continue;
            }

            Entity rider = EntityType.SKELETON.create(level, EntitySpawnReason.TRIGGERED);
            if (rider == null) {
                continue;
            }
            rider.setPos(x + 0.5, y + 1.0, z + 0.5);
            rider.setYRot(horse.getYRot());
            if (!level.noCollision(rider)) {
                continue;
            }

            if (rider instanceof LivingEntity living) {
                living.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                tryEquipRandomArmor(level, random, living);
            }
            rider.addTag(NIGHT_RIDER_XP_TAG);
            int multiplierHundredths = (int) Math.round(xpMultiplier * 100.0);
            rider.addTag(NIGHT_RIDER_XP_MULT_TAG_PREFIX + multiplierHundredths);
            rider.addTag(NIGHT_RIDER_GOLD_MIN_TAG_PREFIX + bonusGoldMin);
            rider.addTag(NIGHT_RIDER_GOLD_MAX_TAG_PREFIX + bonusGoldMax);
            rider.addTag(NIGHT_RIDER_IRON_MIN_TAG_PREFIX + bonusIronMin);
            rider.addTag(NIGHT_RIDER_IRON_MAX_TAG_PREFIX + bonusIronMax);
            rider.addTag(NIGHT_RIDER_EMERALD_MIN_TAG_PREFIX + bonusEmeraldMin);
            rider.addTag(NIGHT_RIDER_EMERALD_MAX_TAG_PREFIX + bonusEmeraldMax);
            rider.addTag(NIGHT_RIDER_DIAMOND_MIN_TAG_PREFIX + bonusDiamondMin);
            rider.addTag(NIGHT_RIDER_DIAMOND_MAX_TAG_PREFIX + bonusDiamondMax);
            rider.addTag(NIGHT_RIDER_GOLD_WEIGHT_TAG_PREFIX + bonusGoldWeight);
            rider.addTag(NIGHT_RIDER_IRON_WEIGHT_TAG_PREFIX + bonusIronWeight);
            rider.addTag(NIGHT_RIDER_EMERALD_WEIGHT_TAG_PREFIX + bonusEmeraldWeight);
            rider.addTag(NIGHT_RIDER_DIAMOND_WEIGHT_TAG_PREFIX + bonusDiamondWeight);

            level.addFreshEntity(horse);
            level.addFreshEntity(rider);
            boolean mounted = rider.startRiding(horse, true, true);
            if (mounted) {
                return;
            }

            // Keep retrying until we get a valid mounted pair.
            rider.discard();
            horse.discard();
        }
    }

    private void tryEquipRandomArmor(ServerLevel level, RandomSource random, LivingEntity living) {
        if (random.nextDouble() >= armorChance) {
            return;
        }

        equipPiece(level, random, living, EquipmentSlot.HEAD, randomHelmet(random));
        equipPiece(level, random, living, EquipmentSlot.CHEST, randomChestplate(random));
        equipPiece(level, random, living, EquipmentSlot.LEGS, randomLeggings(random));
        equipPiece(level, random, living, EquipmentSlot.FEET, randomBoots(random));
    }

    private void equipPiece(ServerLevel level, RandomSource random, LivingEntity living, EquipmentSlot slot, ItemStack stack) {
        if (random.nextDouble() >= armorPieceChance) {
            return;
        }

        if (random.nextDouble() < armorEnchantChance) {
            int cost = armorEnchantCostMin + random.nextInt(Math.max(1, armorEnchantCostMax - armorEnchantCostMin + 1));
            try {
                stack = EnchantmentHelper.enchantItem(random, stack, cost, level.registryAccess(), Optional.empty());
            } catch (Exception ignored) {
            }
        }

        living.setItemSlot(slot, stack);
    }

    private static ItemStack randomHelmet(RandomSource random) {
        return new ItemStack(pick(random,
                Items.LEATHER_HELMET,
                Items.GOLDEN_HELMET,
                Items.CHAINMAIL_HELMET,
                Items.IRON_HELMET,
                Items.DIAMOND_HELMET));
    }

    private static ItemStack randomChestplate(RandomSource random) {
        return new ItemStack(pick(random,
                Items.LEATHER_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE));
    }

    private static ItemStack randomLeggings(RandomSource random) {
        return new ItemStack(pick(random,
                Items.LEATHER_LEGGINGS,
                Items.GOLDEN_LEGGINGS,
                Items.CHAINMAIL_LEGGINGS,
                Items.IRON_LEGGINGS,
                Items.DIAMOND_LEGGINGS));
    }

    private static ItemStack randomBoots(RandomSource random) {
        return new ItemStack(pick(random,
                Items.LEATHER_BOOTS,
                Items.GOLDEN_BOOTS,
                Items.CHAINMAIL_BOOTS,
                Items.IRON_BOOTS,
                Items.DIAMOND_BOOTS));
    }

    private static net.minecraft.world.item.Item pick(RandomSource random, net.minecraft.world.item.Item... items) {
        return items[random.nextInt(items.length)];
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    private static int parseColor(JsonObject obj, String field, int fallback) {
        if (!obj.has(field)) {
            return fallback;
        }

        try {
            String value = obj.get(field).getAsString().trim();
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
}
