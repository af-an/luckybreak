package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Changes the world time of day.
 *
 * JSON fields:
 *   time: 0-24000   (0=dawn, 6000=noon, 12000=dusk, 18000=midnight)
 */
public class SetTimeEvent implements LuckyEvent {

    private final long time;

    private SetTimeEvent(long time) { this.time = time; }

    public static SetTimeEvent fromJson(JsonObject obj) {
        long t = obj.has("time") ? obj.get("time").getAsLong() : 6000L;
        return new SetTimeEvent(t);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        level.setDayTime(time);
    }
}
