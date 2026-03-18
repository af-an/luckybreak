package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/**
 * Spawns primed TNT and launches it upward from the Lucky Block position.
 *
 * JSON fields:
 *   min_count:    3     (minimum TNT spawned, default 3)
 *   max_count:    10    (maximum TNT spawned, default 10)
 *   upward_min:   0.6   (minimum upward velocity, default 0.6)
 *   upward_max:   1.2   (maximum upward velocity, default 1.2)
 */
public class ThrowTntUpEvent implements LuckyEvent {

    private final int minCount;
    private final int maxCount;
    private final double upwardMin;
    private final double upwardMax;

    private ThrowTntUpEvent(int minCount, int maxCount, double upwardMin, double upwardMax) {
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.upwardMin = upwardMin;
        this.upwardMax = upwardMax;
    }

    public static ThrowTntUpEvent fromJson(JsonObject obj) {
        int minCount = obj.has("min_count") ? obj.get("min_count").getAsInt() : 3;
        int maxCount = obj.has("max_count") ? obj.get("max_count").getAsInt() : 10;
        double upwardMin = obj.has("upward_min") ? obj.get("upward_min").getAsDouble() : 0.6;
        double upwardMax = obj.has("upward_max") ? obj.get("upward_max").getAsDouble() : 1.2;

        if (minCount < 1) minCount = 1;
        if (maxCount < minCount) maxCount = minCount;
        if (upwardMax < upwardMin) upwardMax = upwardMin;

        return new ThrowTntUpEvent(minCount, maxCount, upwardMin, upwardMax);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        RandomSource random = level.getRandom();
        int count = minCount + random.nextInt(maxCount - minCount + 1);

        for (int i = 0; i < count; i++) {
            Entity tnt = EntityType.TNT.create(level, EntitySpawnReason.TRIGGERED);
            if (tnt == null) {
                continue;
            }

            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.4;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.4;
            tnt.setPos(x, y, z);
            tnt.setYRot(random.nextFloat() * 360.0F);

            double vx = (random.nextDouble() - 0.5) * 0.35;
            double vy = upwardMin + random.nextDouble() * (upwardMax - upwardMin);
            double vz = (random.nextDouble() - 0.5) * 0.35;
            tnt.setDeltaMovement(vx, vy, vz);

            level.addFreshEntity(tnt);
        }
    }
}
