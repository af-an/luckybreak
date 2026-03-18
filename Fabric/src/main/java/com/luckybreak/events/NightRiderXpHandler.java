package com.luckybreak.events;

import com.luckybreak.events.type.NightRidersEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Grants extra XP for night rider skeleton kills so total XP follows configured multiplier.
 */
public final class NightRiderXpHandler {

    private static final int BASE_SKELETON_XP = 5;

    private NightRiderXpHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) -> {
            if (!(entity.level() instanceof ServerLevel level)) {
                return;
            }
            if (!entity.getTags().contains(NightRidersEvent.NIGHT_RIDER_XP_TAG)) {
                return;
            }

            double multiplier = readMultiplierFromTags(entity);
            int bonusXp = (int) Math.round(BASE_SKELETON_XP * (multiplier - 1.0));
            if (bonusXp <= 0) {
                return;
            }

            String command = String.format(
                    "summon minecraft:experience_orb %.2f %.2f %.2f {Value:%d}",
                    entity.getX(),
                    entity.getY() + 0.25,
                    entity.getZ(),
                    bonusXp
            );
            level.getServer().getCommands().performPrefixedCommand(
                    level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level),
                    command
            );

                int goldWeight = Math.max(0, readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_GOLD_WEIGHT_TAG_PREFIX, 40));
                int ironWeight = Math.max(0, readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_IRON_WEIGHT_TAG_PREFIX, 40));
                int emeraldWeight = Math.max(0, readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_EMERALD_WEIGHT_TAG_PREFIX, 12));
                int diamondWeight = Math.max(0, readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_DIAMOND_WEIGHT_TAG_PREFIX, 8));

                int totalWeight = goldWeight + ironWeight + emeraldWeight + diamondWeight;
                if (totalWeight <= 0) {
                    return;
                }

                int pick = level.getRandom().nextInt(totalWeight);
                String dropId;
                int countMin;
                int countMax;
                if (pick < goldWeight) {
                    dropId = "minecraft:gold_ingot";
                    countMin = readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_GOLD_MIN_TAG_PREFIX, 1);
                    countMax = readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_GOLD_MAX_TAG_PREFIX, 3);
                } else if ((pick -= goldWeight) < ironWeight) {
                    dropId = "minecraft:iron_ingot";
                    countMin = readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_IRON_MIN_TAG_PREFIX, 1);
                    countMax = readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_IRON_MAX_TAG_PREFIX, 3);
                } else if ((pick -= ironWeight) < emeraldWeight) {
                    dropId = "minecraft:emerald";
                    countMin = readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_EMERALD_MIN_TAG_PREFIX, 1);
                    countMax = readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_EMERALD_MAX_TAG_PREFIX, 1);
                } else {
                    dropId = "minecraft:diamond";
                    countMin = readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_DIAMOND_MIN_TAG_PREFIX, 1);
                    countMax = readIntTagWithPrefix(entity, NightRidersEvent.NIGHT_RIDER_DIAMOND_MAX_TAG_PREFIX, 1);
                }

                if (countMin < 0) countMin = 0;
                if (countMax < countMin) countMax = countMin;
                int dropCount = countMin + level.getRandom().nextInt(Math.max(1, countMax - countMin + 1));
                if (dropCount <= 0) {
                    return;
                }

                String goldDropCommand = String.format(
                        "summon minecraft:item %.2f %.2f %.2f {Item:{id:\"%s\",Count:%db}}",
                    entity.getX(),
                    entity.getY() + 0.25,
                    entity.getZ(),
                        dropId,
                        dropCount
                );
                level.getServer().getCommands().performPrefixedCommand(
                    level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level),
                    goldDropCommand
                );
        });
    }

    private static double readMultiplierFromTags(LivingEntity entity) {
        for (String tag : entity.getTags()) {
            if (!tag.startsWith(NightRidersEvent.NIGHT_RIDER_XP_MULT_TAG_PREFIX)) {
                continue;
            }
            String suffix = tag.substring(NightRidersEvent.NIGHT_RIDER_XP_MULT_TAG_PREFIX.length());
            try {
                int hundredths = Integer.parseInt(suffix);
                return Math.max(1.0, hundredths / 100.0);
            } catch (Exception ignored) {
            }
        }
        return 1.0;
    }

    private static int readIntTagWithPrefix(LivingEntity entity, String prefix, int fallback) {
        for (String tag : entity.getTags()) {
            if (!tag.startsWith(prefix)) {
                continue;
            }
            String suffix = tag.substring(prefix.length());
            try {
                return Integer.parseInt(suffix);
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }
}
