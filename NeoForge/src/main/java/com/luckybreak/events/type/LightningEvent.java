package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

/**
 * Strikes lightning at or around the break position.
 *
 * JSON fields:
 *   count:  1     (number of strikes, default 1)
 *   spread: 4     (random XZ spread per strike, default 0)
 */
public class LightningEvent implements LuckyEvent {

    private final int count;
    private final int spread;

    private LightningEvent(int count, int spread) {
        this.count = count;
        this.spread = spread;
    }

    public static LightningEvent fromJson(JsonObject obj) {
        int count  = obj.has("count")  ? obj.get("count").getAsInt()  : 1;
        int spread = obj.has("spread") ? obj.get("spread").getAsInt() : 0;
        return new LightningEvent(count, spread);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        for (int i = 0; i < count; i++) {
            int ox = spread > 0 ? (level.getRandom().nextInt(spread * 2 + 1) - spread) : 0;
            int oz = spread > 0 ? (level.getRandom().nextInt(spread * 2 + 1) - spread) : 0;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.setPos(pos.getX() + 0.5 + ox, (double) pos.getY(), pos.getZ() + 0.5 + oz);
                level.addFreshEntity(bolt);
            }
        }
    }
}
