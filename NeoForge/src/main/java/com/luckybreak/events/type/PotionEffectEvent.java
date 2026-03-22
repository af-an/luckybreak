package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies one or more potion effects to all players within a radius.
 *
 * JSON fields:
 *   radius:  8.0    (default 6.0)
 *   effects: array of { "effect": "minecraft:speed", "duration_ticks": 400, "amplifier": 1 }
 */
public class PotionEffectEvent implements LuckyEvent {

    private record EffectEntry(Holder<MobEffect> effect, int duration, int amplifier) {}

    private final double radius;
    private final List<EffectEntry> effects;

    private PotionEffectEvent(double radius, List<EffectEntry> effects) {
        this.radius = radius;
        this.effects = effects;
    }

    public static PotionEffectEvent fromJson(JsonObject obj) {
        double radius = obj.has("radius") ? obj.get("radius").getAsDouble() : 6.0;
        List<EffectEntry> effects = new ArrayList<>();

        if (obj.has("effects")) {
            JsonArray arr = obj.getAsJsonArray("effects");
            for (JsonElement el : arr) {
                JsonObject e = el.getAsJsonObject();
                String id = e.get("effect").getAsString();
                int duration  = e.has("duration_ticks") ? e.get("duration_ticks").getAsInt() : 200;
                int amplifier = e.has("amplifier")      ? e.get("amplifier").getAsInt()      : 0;
                MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.getValue(ResourceLocation.parse(id));
                if (mobEffect != null) {
                    Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect);
                    effects.add(new EffectEntry(holder, duration, amplifier));
                } else {
                    LuckyBreak.LOGGER.warn("[LuckyBreak] Unknown mob effect: {}", id);
                }
            }
        }
        return new PotionEffectEvent(radius, effects);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        AABB area = AABB.unitCubeFromLowerCorner(pos.getCenter()).inflate(radius);
        List<Player> nearby = level.getEntitiesOfClass(Player.class, area, p -> true);
        for (Player p : nearby) {
            for (EffectEntry entry : effects) {
                p.addEffect(new MobEffectInstance(entry.effect(), entry.duration(), entry.amplifier()));
            }
        }
    }
}
