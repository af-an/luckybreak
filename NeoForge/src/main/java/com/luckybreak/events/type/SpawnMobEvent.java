package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/**
 * Spawns a number of entities around the break position.
 *
 * JSON fields:
 *   entity:  "minecraft:zombie"
 *   count:   5          (default 1)
 *   radius:  2.0        (random XZ scatter, default 1.5)
 */
public class SpawnMobEvent implements LuckyEvent {

    private final String entityId;
    private final int count;
    private final double radius;

    private SpawnMobEvent(String entityId, int count, double radius) {
        this.entityId = entityId;
        this.count = count;
        this.radius = radius;
    }

    public static SpawnMobEvent fromJson(JsonObject obj) {
        String entity = obj.get("entity").getAsString();
        int count  = obj.has("count")  ? obj.get("count").getAsInt()    : 1;
        double rad = obj.has("radius") ? obj.get("radius").getAsDouble() : 1.5;
        return new SpawnMobEvent(entity, count, rad);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(entityId));
        if (type == null || type == EntityType.PIG) {
            // getValue returns Pig as default when not found; skip if unchanged
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(entityId))) {
                LuckyBreak.LOGGER.warn("[LuckyBreak] Unknown entity: {}", entityId);
                return;
            }
        }

        for (int i = 0; i < count; i++) {
            double ox = (level.getRandom().nextDouble() - 0.5) * 2 * radius;
            double oz = (level.getRandom().nextDouble() - 0.5) * 2 * radius;
            Entity entity = type.create(level, EntitySpawnReason.TRIGGERED);
            if (entity != null) {
                entity.setPos(pos.getX() + 0.5 + ox, (double) pos.getY(), pos.getZ() + 0.5 + oz);
                entity.setYRot(level.getRandom().nextFloat() * 360f);
                level.addFreshEntity(entity);
            }
        }
    }
}
