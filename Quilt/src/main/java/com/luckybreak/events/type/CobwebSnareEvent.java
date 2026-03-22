package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;

public class CobwebSnareEvent implements LuckyEvent {

    private final int webRadius;
    private final int webHeight;
    private final int silverfishMin;
    private final int silverfishMax;
    private final int silverfishSpawnRadius;
    private final int slownessDurationTicks;
    private final int slownessAmplifier;
    private final int weaknessDurationTicks;
    private final int weaknessAmplifier;
    private final int smokeParticleCount;
    private final SoundEvent snareSound;
    private final float snareSoundVolume;
    private final float snareSoundPitch;

    private CobwebSnareEvent(
            int webRadius,
            int webHeight,
            int silverfishMin,
            int silverfishMax,
            int silverfishSpawnRadius,
            int slownessDurationTicks,
            int slownessAmplifier,
            int weaknessDurationTicks,
            int weaknessAmplifier,
            int smokeParticleCount,
            SoundEvent snareSound,
            float snareSoundVolume,
            float snareSoundPitch
    ) {
        this.webRadius = webRadius;
        this.webHeight = webHeight;
        this.silverfishMin = silverfishMin;
        this.silverfishMax = silverfishMax;
        this.silverfishSpawnRadius = silverfishSpawnRadius;
        this.slownessDurationTicks = slownessDurationTicks;
        this.slownessAmplifier = slownessAmplifier;
        this.weaknessDurationTicks = weaknessDurationTicks;
        this.weaknessAmplifier = weaknessAmplifier;
        this.smokeParticleCount = smokeParticleCount;
        this.snareSound = snareSound;
        this.snareSoundVolume = snareSoundVolume;
        this.snareSoundPitch = snareSoundPitch;
    }

    public static CobwebSnareEvent fromJson(JsonObject obj) {
        int webRadius = obj.has("web_radius") ? obj.get("web_radius").getAsInt() : 3;
        int webHeight = obj.has("web_height") ? obj.get("web_height").getAsInt() : 2;
        int silverfishMin = obj.has("silverfish_min") ? obj.get("silverfish_min").getAsInt() : 4;
        int silverfishMax = obj.has("silverfish_max") ? obj.get("silverfish_max").getAsInt() : 7;
        int silverfishSpawnRadius = obj.has("silverfish_spawn_radius") ? obj.get("silverfish_spawn_radius").getAsInt() : 5;
        int slownessDurationTicks = obj.has("slowness_duration_ticks") ? obj.get("slowness_duration_ticks").getAsInt() : 180;
        int slownessAmplifier = obj.has("slowness_amplifier") ? obj.get("slowness_amplifier").getAsInt() : 1;
        int weaknessDurationTicks = obj.has("weakness_duration_ticks") ? obj.get("weakness_duration_ticks").getAsInt() : 180;
        int weaknessAmplifier = obj.has("weakness_amplifier") ? obj.get("weakness_amplifier").getAsInt() : 0;
        int smokeParticleCount = obj.has("smoke_particle_count") ? obj.get("smoke_particle_count").getAsInt() : 32;
        SoundEvent snareSound = parseSoundEvent(obj, "snare_sound", SoundEvents.COBWEB_PLACE);
        float snareSoundVolume = obj.has("snare_sound_volume") ? obj.get("snare_sound_volume").getAsFloat() : 1.0f;
        float snareSoundPitch = obj.has("snare_sound_pitch") ? obj.get("snare_sound_pitch").getAsFloat() : 0.85f;

        if (webRadius < 1) webRadius = 1;
        if (webHeight < 1) webHeight = 1;
        if (silverfishMin < 0) silverfishMin = 0;
        if (silverfishMax < silverfishMin) silverfishMax = silverfishMin;
        if (silverfishSpawnRadius < 2) silverfishSpawnRadius = 2;
        if (slownessDurationTicks < 20) slownessDurationTicks = 20;
        if (weaknessDurationTicks < 20) weaknessDurationTicks = 20;
        if (slownessAmplifier < 0) slownessAmplifier = 0;
        if (weaknessAmplifier < 0) weaknessAmplifier = 0;
        if (smokeParticleCount < 0) smokeParticleCount = 0;

        return new CobwebSnareEvent(
                webRadius,
                webHeight,
                silverfishMin,
                silverfishMax,
                silverfishSpawnRadius,
                slownessDurationTicks,
                slownessAmplifier,
                weaknessDurationTicks,
                weaknessAmplifier,
                smokeParticleCount,
                snareSound,
                snareSoundVolume,
                snareSoundPitch
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (level.getServer() != null && level.getServer().getWorldData().getDifficulty() == Difficulty.PEACEFUL) {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
        }

        applyEffect(level, player, "minecraft:slowness", slownessDurationTicks, slownessAmplifier);
        applyEffect(level, player, "minecraft:weakness", weaknessDurationTicks, weaknessAmplifier);

        BlockPos center = player.blockPosition();
        placeCobwebRing(level, center);

        RandomSource random = level.getRandom();
        int silverfishCount = silverfishMin + random.nextInt(Math.max(1, silverfishMax - silverfishMin + 1));
        spawnSilverfish(level, center, silverfishCount, random);

        if (smokeParticleCount > 0) {
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    center.getX() + 0.5,
                    center.getY() + 0.8,
                    center.getZ() + 0.5,
                    smokeParticleCount,
                    0.9,
                    0.5,
                    0.9,
                    0.01
            );
        }
        level.playSound(null, center, snareSound, SoundSource.HOSTILE, snareSoundVolume, snareSoundPitch);
    }

    private void placeCobwebRing(ServerLevel level, BlockPos center) {
        int minY = center.getY();
        int maxY = center.getY() + webHeight - 1;

        for (int y = minY; y <= maxY; y++) {
            for (int dx = -webRadius; dx <= webRadius; dx++) {
                for (int dz = -webRadius; dz <= webRadius; dz++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance < webRadius - 0.75 || distance > webRadius + 0.35) {
                        continue;
                    }
                    BlockPos target = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                    if (level.getBlockState(target).isAir() && level.getFluidState(target).isEmpty()) {
                        level.setBlock(target, Blocks.COBWEB.defaultBlockState(), 3);
                    }
                }
            }
        }

        BlockPos playerFeet = center;
        if (level.getBlockState(playerFeet).isAir() && level.getFluidState(playerFeet).isEmpty()) {
            level.setBlock(playerFeet, Blocks.COBWEB.defaultBlockState(), 3);
        }
    }

    private void spawnSilverfish(ServerLevel level, BlockPos center, int count, RandomSource random) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 2.0 + random.nextDouble() * Math.max(0.5, silverfishSpawnRadius - 1.5);
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
            int y = center.getY();

            Mob silverfish = (Mob) EntityType.SILVERFISH.create(level, EntitySpawnReason.TRIGGERED);
            if (silverfish == null) {
                continue;
            }
            silverfish.setPos(x, y, z);
            silverfish.setYRot(random.nextFloat() * 360.0f);
            level.addFreshEntity(silverfish);
        }
    }

    private static void applyEffect(ServerLevel level, ServerPlayer player, String effectId, int durationTicks, int amplifier) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(ResourceLocation.parse(effectId));
        if (effect == null) {
            return;
        }
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        player.addEffect(new MobEffectInstance(holder, durationTicks, amplifier));
    }

    private static SoundEvent parseSoundEvent(JsonObject obj, String field, SoundEvent fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(obj.get(field).getAsString()));
            return sound != null ? sound : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
