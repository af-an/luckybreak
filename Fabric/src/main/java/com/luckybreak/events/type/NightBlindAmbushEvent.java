package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/**
 * Switches time to night, applies blindness, and spawns a ring of hostile mobs around the player.
 * If world difficulty is peaceful, it is changed to normal first.
 *
 * JSON fields:
 *   min_count: 8                      (default 8)
 *   max_count: 15                     (default 15)
 *   radius: 5.0                       (default 5.0)
 *   spider_chance: 0.5                (0.0-1.0 chance to spawn spiders instead of zombies)
 *   blindness_duration_ticks: 160     (default 8 seconds)
 *   blindness_amplifier: 0            (default Blindness I)
 *   night_time: 13000                 (default night)
 */
public class NightBlindAmbushEvent implements LuckyEvent {

    private final int minCount;
    private final int maxCount;
    private final double radius;
    private final double spiderChance;
    private final int blindnessDurationTicks;
    private final int blindnessAmplifier;
    private final long nightTime;

    private NightBlindAmbushEvent(int minCount, int maxCount, double radius, double spiderChance,
                                  int blindnessDurationTicks, int blindnessAmplifier, long nightTime) {
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.radius = radius;
        this.spiderChance = spiderChance;
        this.blindnessDurationTicks = blindnessDurationTicks;
        this.blindnessAmplifier = blindnessAmplifier;
        this.nightTime = nightTime;
    }

    public static NightBlindAmbushEvent fromJson(JsonObject obj) {
        int minCount = obj.has("min_count") ? obj.get("min_count").getAsInt() : 8;
        int maxCount = obj.has("max_count") ? obj.get("max_count").getAsInt() : 15;
        double radius = obj.has("radius") ? obj.get("radius").getAsDouble() : 5.0;
        double spiderChance = obj.has("spider_chance") ? obj.get("spider_chance").getAsDouble() : 0.5;
        int blindnessDurationTicks = obj.has("blindness_duration_ticks") ? obj.get("blindness_duration_ticks").getAsInt() : 160;
        int blindnessAmplifier = obj.has("blindness_amplifier") ? obj.get("blindness_amplifier").getAsInt() : 0;
        long nightTime = obj.has("night_time") ? obj.get("night_time").getAsLong() : 13000L;

        if (minCount < 1) minCount = 1;
        if (maxCount < minCount) maxCount = minCount;
        if (radius < 1.0) radius = 1.0;
        if (spiderChance < 0.0) spiderChance = 0.0;
        if (spiderChance > 1.0) spiderChance = 1.0;
        if (blindnessDurationTicks < 1) blindnessDurationTicks = 1;
        if (blindnessAmplifier < 0) blindnessAmplifier = 0;

        return new NightBlindAmbushEvent(
                minCount,
                maxCount,
                radius,
                spiderChance,
                blindnessDurationTicks,
                blindnessAmplifier,
                nightTime
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (level.getServer().getWorldData().getDifficulty() == Difficulty.PEACEFUL) {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
        }

        level.setDayTime(nightTime);

        MobEffect blindness = BuiltInRegistries.MOB_EFFECT.getValue(ResourceLocation.parse("minecraft:blindness"));
        if (blindness != null) {
            Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(blindness);
            player.addEffect(new MobEffectInstance(holder, blindnessDurationTicks, blindnessAmplifier));
        }

        RandomSource random = level.getRandom();
        int count = minCount + random.nextInt(maxCount - minCount + 1);
        EntityType<?> spawnType = random.nextDouble() < spiderChance ? EntityType.SPIDER : EntityType.ZOMBIE;
        BlockPos center = player.blockPosition();

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i / count) + (random.nextDouble() - 0.5) * 0.35;
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = center.getY();

            Entity mob = spawnType.create(level, EntitySpawnReason.TRIGGERED);
            if (mob == null) {
                continue;
            }

            mob.setPos(x, y, z);
            mob.setYRot((float) (angle * 180.0 / Math.PI) + 180.0F);
            level.addFreshEntity(mob);
        }
    }
}
