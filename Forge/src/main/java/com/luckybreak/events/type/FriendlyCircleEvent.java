package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public class FriendlyCircleEvent implements LuckyEvent {

    private record MobEntry(EntityType<?> type, double weight) {}

    private final int countMin;
    private final int countMax;
    private final double radius;
    private final List<MobEntry> mobPool;
    private final ParticleOptions primaryParticle;
    private final int primaryParticleCount;
    private final ParticleOptions secondaryParticle;
    private final int secondaryParticleCount;
    private final int heartParticleCount;
    private final SoundEvent circleSound;
    private final float circleSoundVolume;
    private final float circleSoundPitch;

    private FriendlyCircleEvent(
            int countMin,
            int countMax,
            double radius,
            List<MobEntry> mobPool,
            ParticleOptions primaryParticle,
            int primaryParticleCount,
            ParticleOptions secondaryParticle,
            int secondaryParticleCount,
            int heartParticleCount,
            SoundEvent circleSound,
            float circleSoundVolume,
            float circleSoundPitch
    ) {
        this.countMin = countMin;
        this.countMax = countMax;
        this.radius = radius;
        this.mobPool = mobPool;
        this.primaryParticle = primaryParticle;
        this.primaryParticleCount = primaryParticleCount;
        this.secondaryParticle = secondaryParticle;
        this.secondaryParticleCount = secondaryParticleCount;
        this.heartParticleCount = heartParticleCount;
        this.circleSound = circleSound;
        this.circleSoundVolume = circleSoundVolume;
        this.circleSoundPitch = circleSoundPitch;
    }

    public static FriendlyCircleEvent fromJson(JsonObject obj) {
        int countMin = obj.has("count_min") ? obj.get("count_min").getAsInt() : 4;
        int countMax = obj.has("count_max") ? obj.get("count_max").getAsInt() : 7;
        double radius = obj.has("radius") ? obj.get("radius").getAsDouble() : 5.0;

        if (countMin < 1) countMin = 1;
        if (countMax < countMin) countMax = countMin;
        if (radius < 1.5) radius = 1.5;

        ParticleOptions primaryParticle = parseParticle(obj, "primary_particle", ParticleTypes.HEART);
        int primaryParticleCount = obj.has("primary_particle_count") ? obj.get("primary_particle_count").getAsInt() : 24;
        ParticleOptions secondaryParticle = parseParticle(obj, "secondary_particle", ParticleTypes.HAPPY_VILLAGER);
        int secondaryParticleCount = obj.has("secondary_particle_count") ? obj.get("secondary_particle_count").getAsInt() : 20;
        int heartParticleCount = obj.has("heart_particle_count") ? obj.get("heart_particle_count").getAsInt() : 20;
        SoundEvent circleSound = parseSoundEvent(obj, "circle_sound", SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM);
        float circleSoundVolume = obj.has("circle_sound_volume") ? obj.get("circle_sound_volume").getAsFloat() : 0.9f;
        float circleSoundPitch = obj.has("circle_sound_pitch") ? obj.get("circle_sound_pitch").getAsFloat() : 1.1f;

        if (primaryParticleCount < 0) primaryParticleCount = 0;
        if (secondaryParticleCount < 0) secondaryParticleCount = 0;
        if (heartParticleCount < 0) heartParticleCount = 0;

        List<MobEntry> pool = new ArrayList<>();
        if (obj.has("mobs") && obj.get("mobs").isJsonArray()) {
            JsonArray mobs = obj.getAsJsonArray("mobs");
            for (JsonElement element : mobs) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject mob = element.getAsJsonObject();
                if (!mob.has("entity")) {
                    continue;
                }

                ResourceLocation id = ResourceLocation.parse(mob.get("entity").getAsString());
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                    continue;
                }
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
                if (type == null) {
                    continue;
                }
                double weight = mob.has("weight") ? mob.get("weight").getAsDouble() : 1.0;
                if (weight <= 0.0) {
                    continue;
                }
                pool.add(new MobEntry(type, weight));
            }
        }

        if (pool.isEmpty()) {
            pool.add(new MobEntry(EntityType.WOLF, 2.0));
            pool.add(new MobEntry(EntityType.CAT, 2.0));
            pool.add(new MobEntry(EntityType.FOX, 1.6));
            pool.add(new MobEntry(EntityType.RABBIT, 2.0));
            pool.add(new MobEntry(EntityType.PARROT, 1.5));
            pool.add(new MobEntry(EntityType.SHEEP, 2.2));
        }

        return new FriendlyCircleEvent(
            countMin,
            countMax,
            radius,
            List.copyOf(pool),
            primaryParticle,
            primaryParticleCount,
            secondaryParticle,
            secondaryParticleCount,
            heartParticleCount,
            circleSound,
            circleSoundVolume,
            circleSoundPitch
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        RandomSource random = level.getRandom();
        int count = countMin + random.nextInt(Math.max(1, countMax - countMin + 1));

        double centerX = player.getX();
        double centerZ = player.getZ();

        for (int i = 0; i < count; i++) {
            MobEntry entry = chooseWeighted(random);
            Entity entity = entry.type.create(level, EntitySpawnReason.TRIGGERED);
            if (entity == null) {
                continue;
            }

            double angle = (Math.PI * 2.0 * i / Math.max(1, count)) + random.nextDouble() * 0.4;
            double distance = Math.max(1.8, radius + (random.nextDouble() - 0.5) * 1.3);
            double x = centerX + Math.cos(angle) * distance;
            double z = centerZ + Math.sin(angle) * distance;
            BlockPos safePos = findSafeSpawn(level, entity, x, z);
            if (safePos == null) {
                continue;
            }

            entity.setPos(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
            entity.setYRot(random.nextFloat() * 360.0f);
            entity.fallDistance = 0.0f;
            level.addFreshEntity(entity);
        }

        if (heartParticleCount > 0) {
            level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.1, player.getZ(), heartParticleCount, 1.4, 0.8, 1.4, 0.02);
        }
        if (primaryParticleCount > 0) {
            level.sendParticles(primaryParticle, player.getX(), player.getY() + 1.0, player.getZ(), primaryParticleCount, 1.4, 0.8, 1.4, 0.02);
        }
        if (secondaryParticleCount > 0) {
            level.sendParticles(secondaryParticle, player.getX(), player.getY() + 0.9, player.getZ(), secondaryParticleCount, 1.4, 0.6, 1.4, 0.02);
        }
        level.playSound(null, player.blockPosition(), circleSound, SoundSource.PLAYERS, circleSoundVolume, circleSoundPitch);
    }

    private static BlockPos findSafeSpawn(ServerLevel level, Entity entity, double x, double z) {
        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        int minY = Math.max(level.getMinY() + 1, baseY - 1);
        int maxY = Math.min(level.getMaxY() - 2, baseY + 3);

        for (int y = minY; y <= maxY; y++) {
            entity.setPos(x + 0.5, y, z + 0.5);
            if (level.noCollision(entity)) {
                return BlockPos.containing(x, y, z);
            }
        }

        return null;
    }

    private static ParticleOptions parseParticle(JsonObject obj, String field, ParticleOptions fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            var particleType = BuiltInRegistries.PARTICLE_TYPE.getValue(ResourceLocation.parse(obj.get(field).getAsString()));
            if (particleType instanceof ParticleOptions options) {
                return options;
            }
        } catch (Exception ignored) {
        }
        return fallback;
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

    private MobEntry chooseWeighted(RandomSource random) {
        double total = 0.0;
        for (MobEntry entry : mobPool) {
            total += entry.weight;
        }

        double pick = random.nextDouble() * Math.max(0.0001, total);
        double running = 0.0;
        for (MobEntry entry : mobPool) {
            running += entry.weight;
            if (pick <= running) {
                return entry;
            }
        }
        return mobPool.get(mobPool.size() - 1);
    }
}
