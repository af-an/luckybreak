package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.ModEntities;
import com.luckybreak.events.LuckyEvent;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

import java.util.List;

public class GoldenHenGiftEvent implements LuckyEvent {

    private final int countMin;
    private final int countMax;
    private final double spawnRadius;
    private final String message;
    private final int messageColor;
    private final int fireworkBurstCount;
    private final int fireworkRocketCount;
    private final int fireworkFlight;
    private final IntList fireworkColors;
    private final IntList fireworkFadeColors;
    private final boolean fireworkFlicker;
    private final boolean fireworkTrail;
    private final float chimeVolume;
    private final float chimePitch;

    private GoldenHenGiftEvent(
            int countMin,
            int countMax,
            double spawnRadius,
            String message,
            int messageColor,
            int fireworkBurstCount,
            int fireworkRocketCount,
            int fireworkFlight,
            IntList fireworkColors,
            IntList fireworkFadeColors,
            boolean fireworkFlicker,
            boolean fireworkTrail,
            float chimeVolume,
            float chimePitch
    ) {
        this.countMin = countMin;
        this.countMax = countMax;
        this.spawnRadius = spawnRadius;
        this.message = message;
        this.messageColor = messageColor;
        this.fireworkBurstCount = fireworkBurstCount;
        this.fireworkRocketCount = fireworkRocketCount;
        this.fireworkFlight = fireworkFlight;
        this.fireworkColors = fireworkColors;
        this.fireworkFadeColors = fireworkFadeColors;
        this.fireworkFlicker = fireworkFlicker;
        this.fireworkTrail = fireworkTrail;
        this.chimeVolume = chimeVolume;
        this.chimePitch = chimePitch;
    }

    public static GoldenHenGiftEvent fromJson(JsonObject obj) {
        int countMin = obj.has("count_min") ? obj.get("count_min").getAsInt() : 1;
        int countMax = obj.has("count_max") ? obj.get("count_max").getAsInt() : 2;
        double spawnRadius = obj.has("spawn_radius") ? obj.get("spawn_radius").getAsDouble() : 2.0;

        String message = obj.has("message")
                ? obj.get("message").getAsString()
                : "You got a Golden Hen! Wait for it to lay something...";
        int messageColor = parseColorField(obj, "message_color", 0xFFD700);

        int fireworkBurstCount = obj.has("firework_burst_count") ? obj.get("firework_burst_count").getAsInt() : 24;
        int fireworkRocketCount = obj.has("firework_rocket_count") ? obj.get("firework_rocket_count").getAsInt() : 2;
        int fireworkFlight = obj.has("firework_flight") ? obj.get("firework_flight").getAsInt() : 1;
        IntList fireworkColors = parseColorList(obj, "firework_colors", List.of(0xFFD700, 0xFFAA00));
        IntList fireworkFadeColors = parseColorList(obj, "firework_fade_colors", List.of(0xFFFFFF));
        boolean fireworkFlicker = !obj.has("firework_flicker") || obj.get("firework_flicker").getAsBoolean();
        boolean fireworkTrail = !obj.has("firework_trail") || obj.get("firework_trail").getAsBoolean();

        float chimeVolume = obj.has("chime_volume") ? obj.get("chime_volume").getAsFloat() : 1.0f;
        float chimePitch = obj.has("chime_pitch") ? obj.get("chime_pitch").getAsFloat() : 1.05f;

        countMin = Mth.clamp(countMin, 1, 16);
        countMax = Mth.clamp(countMax, countMin, 16);
        spawnRadius = Mth.clamp((float) spawnRadius, 0.0f, 8.0f);
        fireworkBurstCount = Mth.clamp(fireworkBurstCount, 0, 200);
        fireworkRocketCount = Mth.clamp(fireworkRocketCount, 0, 16);
        fireworkFlight = Mth.clamp(fireworkFlight, 0, 3);

        return new GoldenHenGiftEvent(
                countMin,
                countMax,
                spawnRadius,
                message,
                messageColor,
                fireworkBurstCount,
                fireworkRocketCount,
                fireworkFlight,
                new IntArrayList(fireworkColors),
                new IntArrayList(fireworkFadeColors),
                fireworkFlicker,
                fireworkTrail,
                chimeVolume,
                chimePitch
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        int spawnCount = Mth.nextInt(level.random, countMin, countMax);

        for (int i = 0; i < spawnCount; i++) {
            var goldenHen = ModEntities.GOLDEN_HEN.create(level, EntitySpawnReason.EVENT);
            if (goldenHen == null) {
                continue;
            }

            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0 * spawnRadius;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0 * spawnRadius;

            goldenHen.setPos(x, y, z);
            goldenHen.setYRot(level.random.nextFloat() * 360f);
            level.addFreshEntity(goldenHen);
        }

        if (!message.isBlank()) {
            player.displayClientMessage(
                    Component.literal(message).withStyle(style -> style.withColor(TextColor.fromRgb(messageColor))),
                    false
            );
        }

        BlockPos fxPos = pos.above();
        double fx = fxPos.getX() + 0.5;
        double fy = fxPos.getY() + 0.5;
        double fz = fxPos.getZ() + 0.5;

        if (fireworkBurstCount > 0) {
            level.sendParticles(ParticleTypes.FIREWORK, fx, fy, fz, fireworkBurstCount, 0.35, 0.35, 0.35, 0.06);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, fx, fy, fz, 1, 0.0, 0.0, 0.0, 0.0);
        }

        spawnColoredFireworks(level, fxPos);
        level.playSound(null, fxPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, chimeVolume, chimePitch);
    }

    private void spawnColoredFireworks(ServerLevel level, BlockPos spawnPos) {
        if (fireworkRocketCount <= 0) {
            return;
        }

        for (int i = 0; i < fireworkRocketCount; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET, 1);
            FireworkExplosion explosion = new FireworkExplosion(
                    FireworkExplosion.Shape.LARGE_BALL,
                    fireworkColors,
                    fireworkFadeColors,
                    fireworkTrail,
                    fireworkFlicker
            );
            Fireworks fireworks = new Fireworks(fireworkFlight, List.of(explosion));
            rocket.set(DataComponents.FIREWORKS, fireworks);

            double x = spawnPos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.4;
            double y = spawnPos.getY() + 0.2;
            double z = spawnPos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.4;
            FireworkRocketEntity entity = new FireworkRocketEntity(level, x, y, z, rocket);
            entity.setDeltaMovement(
                    (level.getRandom().nextDouble() - 0.5) * 0.08,
                    0.35 + level.getRandom().nextDouble() * 0.15,
                    (level.getRandom().nextDouble() - 0.5) * 0.08
            );
            level.addFreshEntity(entity);
        }
    }

    private static IntList parseColorList(JsonObject obj, String field, List<Integer> fallback) {
        if (!obj.has(field) || !obj.get(field).isJsonArray()) {
            return new IntArrayList(fallback);
        }

        IntList result = new IntArrayList();
        JsonArray arr = obj.getAsJsonArray(field);
        for (JsonElement el : arr) {
            Integer color = parseColor(el);
            if (color != null) {
                result.add(color.intValue());
            }
        }

        if (result.isEmpty()) {
            result.addAll(fallback);
        }
        return result;
    }

    private static int parseColorField(JsonObject obj, String field, int fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        Integer color = parseColor(obj.get(field));
        return color != null ? color : fallback;
    }

    private static Integer parseColor(JsonElement element) {
        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt() & 0xFFFFFF;
            }

            String value = element.getAsString().trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            } else if (value.startsWith("0x") || value.startsWith("0X")) {
                value = value.substring(2);
            }

            if (value.length() != 6) {
                return null;
            }
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return null;
        }
    }
}
