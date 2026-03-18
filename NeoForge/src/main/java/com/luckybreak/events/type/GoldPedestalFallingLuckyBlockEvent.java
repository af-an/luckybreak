package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.luckybreak.ModBlocks;
import com.luckybreak.events.LuckyEvent;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Places a gold block at the trigger position, then drops a falling Lucky Block above it.
 *
 * JSON fields:
 *   offset_x: 0
 *   offset_y: 0
 *   offset_z: 0
 *   falling_height: 8
 *   clear_column: true
 *   firework_burst_count: 24
 *   firework_rocket_count: 2
 *   firework_flight: 1
 *   firework_colors: ["#FFD700", "#FFAA00"]
 *   firework_fade_colors: ["#FFFFFF"]
 *   firework_flicker: true
 *   firework_trail: true
 *   villager_trade_particle_count: 20
 *   amethyst_sound_volume: 1.0
 *   amethyst_sound_pitch: 1.05
 */
public class GoldPedestalFallingLuckyBlockEvent implements LuckyEvent {

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final int fallingHeight;
    private final boolean clearColumn;
    private final int fireworkBurstCount;
    private final int fireworkRocketCount;
    private final int fireworkFlight;
    private final IntList fireworkColors;
    private final IntList fireworkFadeColors;
    private final boolean fireworkFlicker;
    private final boolean fireworkTrail;
    private final int villagerTradeParticleCount;
    private final float amethystSoundVolume;
    private final float amethystSoundPitch;

    private GoldPedestalFallingLuckyBlockEvent(
            int offsetX,
            int offsetY,
            int offsetZ,
            int fallingHeight,
            boolean clearColumn,
            int fireworkBurstCount,
            int fireworkRocketCount,
            int fireworkFlight,
            IntList fireworkColors,
            IntList fireworkFadeColors,
            boolean fireworkFlicker,
            boolean fireworkTrail,
            int villagerTradeParticleCount,
            float amethystSoundVolume,
            float amethystSoundPitch
    ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.fallingHeight = fallingHeight;
        this.clearColumn = clearColumn;
        this.fireworkBurstCount = fireworkBurstCount;
        this.fireworkRocketCount = fireworkRocketCount;
        this.fireworkFlight = fireworkFlight;
        this.fireworkColors = fireworkColors;
        this.fireworkFadeColors = fireworkFadeColors;
        this.fireworkFlicker = fireworkFlicker;
        this.fireworkTrail = fireworkTrail;
        this.villagerTradeParticleCount = villagerTradeParticleCount;
        this.amethystSoundVolume = amethystSoundVolume;
        this.amethystSoundPitch = amethystSoundPitch;
    }

    public static GoldPedestalFallingLuckyBlockEvent fromJson(JsonObject obj) {
        int offsetX = obj.has("offset_x") ? obj.get("offset_x").getAsInt() : 0;
        int offsetY = obj.has("offset_y") ? obj.get("offset_y").getAsInt() : 0;
        int offsetZ = obj.has("offset_z") ? obj.get("offset_z").getAsInt() : 0;
        int fallingHeight = obj.has("falling_height") ? obj.get("falling_height").getAsInt() : 8;
        boolean clearColumn = !obj.has("clear_column") || obj.get("clear_column").getAsBoolean();
        int fireworkBurstCount = obj.has("firework_burst_count") ? obj.get("firework_burst_count").getAsInt() : 24;
        int fireworkRocketCount = obj.has("firework_rocket_count") ? obj.get("firework_rocket_count").getAsInt() : 2;
        int fireworkFlight = obj.has("firework_flight") ? obj.get("firework_flight").getAsInt() : 1;
        IntList fireworkColors = parseColorList(obj, "firework_colors", List.of(0xFFD700, 0xFFAA00));
        IntList fireworkFadeColors = parseColorList(obj, "firework_fade_colors", List.of(0xFFFFFF));
        boolean fireworkFlicker = !obj.has("firework_flicker") || obj.get("firework_flicker").getAsBoolean();
        boolean fireworkTrail = !obj.has("firework_trail") || obj.get("firework_trail").getAsBoolean();
        int villagerTradeParticleCount = obj.has("villager_trade_particle_count") ? obj.get("villager_trade_particle_count").getAsInt() : 20;
        float amethystSoundVolume = obj.has("amethyst_sound_volume") ? obj.get("amethyst_sound_volume").getAsFloat() : 1.0f;
        float amethystSoundPitch = obj.has("amethyst_sound_pitch") ? obj.get("amethyst_sound_pitch").getAsFloat() : 1.05f;

        if (fallingHeight < 2) fallingHeight = 2;
        if (fireworkBurstCount < 0) fireworkBurstCount = 0;
        if (fireworkRocketCount < 0) fireworkRocketCount = 0;
        if (fireworkFlight < 0) fireworkFlight = 0;
        if (fireworkFlight > 3) fireworkFlight = 3;
        if (villagerTradeParticleCount < 0) villagerTradeParticleCount = 0;

        return new GoldPedestalFallingLuckyBlockEvent(
            offsetX,
            offsetY,
            offsetZ,
            fallingHeight,
            clearColumn,
            fireworkBurstCount,
            fireworkRocketCount,
            fireworkFlight,
            new IntArrayList(fireworkColors),
            new IntArrayList(fireworkFadeColors),
            fireworkFlicker,
            fireworkTrail,
            villagerTradeParticleCount,
            amethystSoundVolume,
            amethystSoundPitch
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockPos basePos = pos.offset(offsetX, offsetY, offsetZ);
        BlockState air = Blocks.AIR.defaultBlockState();

        if (clearColumn) {
            for (int y = 1; y <= fallingHeight; y++) {
                level.setBlock(basePos.above(y), air, 3);
            }
        }

        level.setBlock(basePos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);

        BlockPos spawnPos = basePos.above(fallingHeight);
        FallingBlockEntity.fall(level, spawnPos, ModBlocks.LUCKY_BLOCK.defaultBlockState());

        double fx = spawnPos.getX() + 0.5;
        double fy = spawnPos.getY() + 0.5;
        double fz = spawnPos.getZ() + 0.5;

        if (fireworkBurstCount > 0) {
            level.sendParticles(ParticleTypes.FIREWORK, fx, fy, fz, fireworkBurstCount, 0.35, 0.35, 0.35, 0.06);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, fx, fy, fz, 1, 0.0, 0.0, 0.0, 0.0);
        }

        spawnColoredFireworks(level, spawnPos);

        if (villagerTradeParticleCount > 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, fx, fy - 0.3, fz, villagerTradeParticleCount, 0.45, 0.25, 0.45, 0.03);
        }

        level.playSound(null, spawnPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, amethystSoundVolume, amethystSoundPitch);
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

    private static Integer parseColor(JsonElement element) {
        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt() & 0xFFFFFF;
            }

            String value = element.getAsString().trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            if (value.length() == 6) {
                return Integer.parseInt(value, 16) & 0xFFFFFF;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
