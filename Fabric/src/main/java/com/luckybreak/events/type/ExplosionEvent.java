package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Creates an explosion centered on the break position.
 *
 * JSON fields:
 *   power:         3.0    (explosion radius, default 3.0)
 *   fire:          false  (set fire, default false)
 *   destroy_blocks: false (destroy terrain, default false)
 */
public class ExplosionEvent implements LuckyEvent {

    private final float power;
    private final boolean fire;
    private final boolean destroyBlocks;

    private ExplosionEvent(float power, boolean fire, boolean destroyBlocks) {
        this.power = power;
        this.fire = fire;
        this.destroyBlocks = destroyBlocks;
    }

    public static ExplosionEvent fromJson(JsonObject obj) {
        float power         = obj.has("power")          ? obj.get("power").getAsFloat()          : 3.0f;
        boolean fire        = obj.has("fire")            && obj.get("fire").getAsBoolean();
        boolean destroy     = obj.has("destroy_blocks")  && obj.get("destroy_blocks").getAsBoolean();
        return new ExplosionEvent(power, fire, destroy);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Level.ExplosionInteraction interaction = destroyBlocks
                ? Level.ExplosionInteraction.BLOCK
                : Level.ExplosionInteraction.NONE;
        level.explode(null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                power, fire, interaction);
    }
}
