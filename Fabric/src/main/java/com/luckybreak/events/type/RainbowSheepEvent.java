package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Spawns one sheep for every dye color around the player.
 */
public class RainbowSheepEvent implements LuckyEvent {

    private final double radius;

    private RainbowSheepEvent(double radius) {
        this.radius = radius;
    }

    public static RainbowSheepEvent fromJson(JsonObject obj) {
        double radius = obj.has("radius") ? obj.get("radius").getAsDouble() : 6.0;
        if (radius < 1.0) {
            radius = 1.0;
        }
        return new RainbowSheepEvent(radius);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        RandomSource random = level.getRandom();

        for (DyeColor color : DyeColor.values()) {
            Sheep sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level, EntitySpawnReason.TRIGGERED);
            if (sheep == null) {
                continue;
            }

            double ox = (random.nextDouble() - 0.5) * 2.0 * radius;
            double oz = (random.nextDouble() - 0.5) * 2.0 * radius;
            double x = player.getX() + ox;
            double z = player.getZ() + oz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));

            sheep.setPos(x, Math.max(level.getMinY() + 1, y), z);
            sheep.setColor(color);
            level.addFreshEntity(sheep);
        }
    }
}
