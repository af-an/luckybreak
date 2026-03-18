package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/**
 * Spawns primed TNT entities, launches them upward, and applies slowness to the triggering player.
 *
 * JSON fields:
 *   min_count:             10    (minimum TNT to spawn, default 10)
 *   max_count:             10    (maximum TNT to spawn, default 10)
 *   tnt_count:             10    (legacy fixed count alias for min/max)
 *   spawn_range:           0.7   (horizontal spawn range from center, default 0.7)
 *   upward_min:            0.6   (minimum upward velocity, default 0.6)
 *   upward_max:            1.2   (maximum upward velocity, default 1.2)
 *   slowness_duration_ticks: 200 (default 10 seconds)
 *   slowness_amplifier:    0     (Slowness I by default)
 */
public class ThrowTntUpSlownessEvent implements LuckyEvent {

    private final int minCount;
    private final int maxCount;
    private final double spawnRange;
    private final double upwardMin;
    private final double upwardMax;
    private final int slownessDurationTicks;
    private final int slownessAmplifier;

    private ThrowTntUpSlownessEvent(int minCount, int maxCount, double spawnRange, double upwardMin, double upwardMax, int slownessDurationTicks, int slownessAmplifier) {
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.spawnRange = spawnRange;
        this.upwardMin = upwardMin;
        this.upwardMax = upwardMax;
        this.slownessDurationTicks = slownessDurationTicks;
        this.slownessAmplifier = slownessAmplifier;
    }

    public static ThrowTntUpSlownessEvent fromJson(JsonObject obj) {
        int minCount;
        int maxCount;
        if (obj.has("min_count") || obj.has("max_count")) {
            minCount = obj.has("min_count") ? obj.get("min_count").getAsInt() : 10;
            maxCount = obj.has("max_count") ? obj.get("max_count").getAsInt() : minCount;
        } else {
            int legacyCount = obj.has("tnt_count") ? obj.get("tnt_count").getAsInt() : 10;
            minCount = legacyCount;
            maxCount = legacyCount;
        }
        double spawnRange = obj.has("spawn_range") ? obj.get("spawn_range").getAsDouble() : 0.7;
        double upwardMin = obj.has("upward_min") ? obj.get("upward_min").getAsDouble() : 0.6;
        double upwardMax = obj.has("upward_max") ? obj.get("upward_max").getAsDouble() : 1.2;
        int duration = obj.has("slowness_duration_ticks") ? obj.get("slowness_duration_ticks").getAsInt() : 200;
        int amplifier = obj.has("slowness_amplifier") ? obj.get("slowness_amplifier").getAsInt() : 0;

        if (minCount < 1) {
            minCount = 1;
        }
        if (maxCount < minCount) {
            maxCount = minCount;
        }
        if (upwardMax < upwardMin) {
            upwardMax = upwardMin;
        }
        if (spawnRange < 0.0) {
            spawnRange = 0.0;
        }
        if (duration < 1) {
            duration = 1;
        }
        if (amplifier < 0) {
            amplifier = 0;
        }

        return new ThrowTntUpSlownessEvent(minCount, maxCount, spawnRange, upwardMin, upwardMax, duration, amplifier);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        RandomSource random = level.getRandom();
        int tntCount = minCount + random.nextInt(maxCount - minCount + 1);

        for (int i = 0; i < tntCount; i++) {
            Entity tnt = EntityType.TNT.create(level, EntitySpawnReason.TRIGGERED);
            if (tnt == null) {
                continue;
            }

            double x = pos.getX() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * spawnRange;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * spawnRange;
            tnt.setPos(x, y, z);
            tnt.setYRot(random.nextFloat() * 360.0F);

            double vx = (random.nextDouble() - 0.5) * 0.35;
            double vy = upwardMin + random.nextDouble() * (upwardMax - upwardMin);
            double vz = (random.nextDouble() - 0.5) * 0.35;
            tnt.setDeltaMovement(vx, vy, vz);

            level.addFreshEntity(tnt);
        }

        MobEffect slowness = BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse("minecraft:slowness"));
        if (slowness != null) {
            Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(slowness);
            player.addEffect(new MobEffectInstance(holder, slownessDurationTicks, slownessAmplifier));
        }
    }
}
