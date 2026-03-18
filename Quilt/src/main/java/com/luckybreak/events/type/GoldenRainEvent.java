package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class GoldenRainEvent implements LuckyEvent {

    private record RainDrop(Item item, double weight, int countMin, int countMax) {}

    private final int dropCountMin;
    private final int dropCountMax;
    private final double radius;
    private final double minHeight;
    private final double maxHeight;
    private final double itemMotionXZ;
    private final double itemMotionYBase;
    private final double itemMotionYRandom;
    private final int itemPickupDelay;
    private final ParticleOptions primaryParticle;
    private final int primaryParticleCount;
    private final double primaryParticleYOffset;
    private final double primaryParticleSpreadX;
    private final double primaryParticleSpreadY;
    private final double primaryParticleSpreadZ;
    private final double primaryParticleSpeed;
    private final ParticleOptions secondaryParticle;
    private final int secondaryParticleCount;
    private final double secondaryParticleYOffset;
    private final double secondaryParticleSpreadX;
    private final double secondaryParticleSpreadY;
    private final double secondaryParticleSpreadZ;
    private final double secondaryParticleSpeed;
    private final SoundEvent rainSound;
    private final float rainSoundVolume;
    private final float rainSoundPitch;
    private final List<RainDrop> dropPool;

    private GoldenRainEvent(
            int dropCountMin,
            int dropCountMax,
            double radius,
            double minHeight,
            double maxHeight,
                double itemMotionXZ,
                double itemMotionYBase,
                double itemMotionYRandom,
                int itemPickupDelay,
                ParticleOptions primaryParticle,
                int primaryParticleCount,
                double primaryParticleYOffset,
                double primaryParticleSpreadX,
                double primaryParticleSpreadY,
                double primaryParticleSpreadZ,
                double primaryParticleSpeed,
                ParticleOptions secondaryParticle,
                int secondaryParticleCount,
                double secondaryParticleYOffset,
                double secondaryParticleSpreadX,
                double secondaryParticleSpreadY,
                double secondaryParticleSpreadZ,
                double secondaryParticleSpeed,
                SoundEvent rainSound,
                float rainSoundVolume,
                float rainSoundPitch,
            List<RainDrop> dropPool
    ) {
        this.dropCountMin = dropCountMin;
        this.dropCountMax = dropCountMax;
        this.radius = radius;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.itemMotionXZ = itemMotionXZ;
        this.itemMotionYBase = itemMotionYBase;
        this.itemMotionYRandom = itemMotionYRandom;
        this.itemPickupDelay = itemPickupDelay;
        this.primaryParticle = primaryParticle;
        this.primaryParticleCount = primaryParticleCount;
        this.primaryParticleYOffset = primaryParticleYOffset;
        this.primaryParticleSpreadX = primaryParticleSpreadX;
        this.primaryParticleSpreadY = primaryParticleSpreadY;
        this.primaryParticleSpreadZ = primaryParticleSpreadZ;
        this.primaryParticleSpeed = primaryParticleSpeed;
        this.secondaryParticle = secondaryParticle;
        this.secondaryParticleCount = secondaryParticleCount;
        this.secondaryParticleYOffset = secondaryParticleYOffset;
        this.secondaryParticleSpreadX = secondaryParticleSpreadX;
        this.secondaryParticleSpreadY = secondaryParticleSpreadY;
        this.secondaryParticleSpreadZ = secondaryParticleSpreadZ;
        this.secondaryParticleSpeed = secondaryParticleSpeed;
        this.rainSound = rainSound;
        this.rainSoundVolume = rainSoundVolume;
        this.rainSoundPitch = rainSoundPitch;
        this.dropPool = dropPool;
    }

    public static GoldenRainEvent fromJson(JsonObject obj) {
        int dropCountMin = obj.has("drop_count_min") ? obj.get("drop_count_min").getAsInt() : 14;
        int dropCountMax = obj.has("drop_count_max") ? obj.get("drop_count_max").getAsInt() : 24;
        double radius = obj.has("radius") ? obj.get("radius").getAsDouble() : 4.0;
        double minHeight = obj.has("min_height") ? obj.get("min_height").getAsDouble() : 7.0;
        double maxHeight = obj.has("max_height") ? obj.get("max_height").getAsDouble() : 10.0;

        if (dropCountMin < 1) dropCountMin = 1;
        if (dropCountMax < dropCountMin) dropCountMax = dropCountMin;
        if (radius < 0.5) radius = 0.5;
        if (minHeight < 2.0) minHeight = 2.0;
        if (maxHeight < minHeight) maxHeight = minHeight;

        double itemMotionXZ = obj.has("item_motion_xz") ? obj.get("item_motion_xz").getAsDouble() : 0.02;
        double itemMotionYBase = obj.has("item_motion_y_base") ? obj.get("item_motion_y_base").getAsDouble() : -0.20;
        double itemMotionYRandom = obj.has("item_motion_y_random") ? obj.get("item_motion_y_random").getAsDouble() : 0.06;
        int itemPickupDelay = obj.has("item_pickup_delay") ? obj.get("item_pickup_delay").getAsInt() : 10;
        ParticleOptions primaryParticle = parseParticle(obj, "primary_particle", ParticleTypes.GLOW);
        int primaryParticleCount = obj.has("primary_particle_count") ? obj.get("primary_particle_count").getAsInt() : 28;
        double primaryParticleYOffset = obj.has("primary_particle_y_offset") ? obj.get("primary_particle_y_offset").getAsDouble() : 1.4;
        double primaryParticleSpreadX = obj.has("primary_particle_spread_x") ? obj.get("primary_particle_spread_x").getAsDouble() : 1.3;
        double primaryParticleSpreadY = obj.has("primary_particle_spread_y") ? obj.get("primary_particle_spread_y").getAsDouble() : 0.9;
        double primaryParticleSpreadZ = obj.has("primary_particle_spread_z") ? obj.get("primary_particle_spread_z").getAsDouble() : 1.3;
        double primaryParticleSpeed = obj.has("primary_particle_speed") ? obj.get("primary_particle_speed").getAsDouble() : 0.02;
        ParticleOptions secondaryParticle = parseParticle(obj, "secondary_particle", ParticleTypes.END_ROD);
        int secondaryParticleCount = obj.has("secondary_particle_count") ? obj.get("secondary_particle_count").getAsInt() : 18;
        double secondaryParticleYOffset = obj.has("secondary_particle_y_offset") ? obj.get("secondary_particle_y_offset").getAsDouble() : 1.0;
        double secondaryParticleSpreadX = obj.has("secondary_particle_spread_x") ? obj.get("secondary_particle_spread_x").getAsDouble() : 1.1;
        double secondaryParticleSpreadY = obj.has("secondary_particle_spread_y") ? obj.get("secondary_particle_spread_y").getAsDouble() : 0.7;
        double secondaryParticleSpreadZ = obj.has("secondary_particle_spread_z") ? obj.get("secondary_particle_spread_z").getAsDouble() : 1.1;
        double secondaryParticleSpeed = obj.has("secondary_particle_speed") ? obj.get("secondary_particle_speed").getAsDouble() : 0.03;
        SoundEvent rainSound = parseSoundEvent(obj, "rain_sound", SoundEvents.BELL_RESONATE);
        float rainSoundVolume = obj.has("rain_sound_volume") ? obj.get("rain_sound_volume").getAsFloat() : 0.95f;
        float rainSoundPitch = obj.has("rain_sound_pitch") ? obj.get("rain_sound_pitch").getAsFloat() : 1.18f;

        if (itemMotionXZ < 0.0) itemMotionXZ = 0.0;
        if (itemMotionYRandom < 0.0) itemMotionYRandom = 0.0;
        if (itemPickupDelay < 0) itemPickupDelay = 0;
        if (primaryParticleCount < 0) primaryParticleCount = 0;
        if (secondaryParticleCount < 0) secondaryParticleCount = 0;

        List<RainDrop> pool = new ArrayList<>();
        if (obj.has("drops") && obj.get("drops").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("drops");
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject entry = el.getAsJsonObject();
                if (!entry.has("item")) continue;

                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.get("item").getAsString()));
                if (item == null || item == Items.AIR) continue;
                double weight = entry.has("weight") ? entry.get("weight").getAsDouble() : 1.0;
                int countMin = entry.has("count_min") ? entry.get("count_min").getAsInt() : 1;
                int countMax = entry.has("count_max") ? entry.get("count_max").getAsInt() : countMin;
                if (weight <= 0.0) continue;
                if (countMin < 1) countMin = 1;
                if (countMax < countMin) countMax = countMin;
                pool.add(new RainDrop(item, weight, countMin, countMax));
            }
        }

        if (pool.isEmpty()) {
            pool.add(new RainDrop(Items.GOLD_NUGGET, 6.0, 5, 14));
            pool.add(new RainDrop(Items.GOLD_INGOT, 4.0, 2, 5));
            pool.add(new RainDrop(Items.EMERALD, 2.2, 1, 3));
            pool.add(new RainDrop(Items.EXPERIENCE_BOTTLE, 2.8, 1, 3));
            pool.add(new RainDrop(Items.GOLDEN_APPLE, 1.2, 1, 1));
            pool.add(new RainDrop(Items.GLOW_BERRIES, 2.4, 2, 6));
        }

        return new GoldenRainEvent(
            dropCountMin,
            dropCountMax,
            radius,
            minHeight,
            maxHeight,
            itemMotionXZ,
            itemMotionYBase,
            itemMotionYRandom,
            itemPickupDelay,
            primaryParticle,
            primaryParticleCount,
            primaryParticleYOffset,
            primaryParticleSpreadX,
            primaryParticleSpreadY,
            primaryParticleSpreadZ,
            primaryParticleSpeed,
            secondaryParticle,
            secondaryParticleCount,
            secondaryParticleYOffset,
            secondaryParticleSpreadX,
            secondaryParticleSpreadY,
            secondaryParticleSpreadZ,
            secondaryParticleSpeed,
            rainSound,
            rainSoundVolume,
            rainSoundPitch,
            List.copyOf(pool)
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        RandomSource random = level.getRandom();
        int drops = dropCountMin + random.nextInt(Math.max(1, dropCountMax - dropCountMin + 1));

        double cx = player.getX();
        double cz = player.getZ();

        for (int i = 0; i < drops; i++) {
            RainDrop rainDrop = chooseWeighted(random);
            int count = rainDrop.countMin + random.nextInt(Math.max(1, rainDrop.countMax - rainDrop.countMin + 1));

            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = random.nextDouble() * radius;
            double x = cx + Math.cos(angle) * dist;
            double z = cz + Math.sin(angle) * dist;
            double y = player.getY() + minHeight + random.nextDouble() * Math.max(0.01, (maxHeight - minHeight));

            ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(rainDrop.item, count));
            entity.setDeltaMovement(
                    (random.nextDouble() - 0.5) * itemMotionXZ,
                    itemMotionYBase - random.nextDouble() * itemMotionYRandom,
                    (random.nextDouble() - 0.5) * itemMotionXZ
            );
            entity.setPickUpDelay(itemPickupDelay);
            level.addFreshEntity(entity);
        }

        if (primaryParticleCount > 0) {
            level.sendParticles(
                    primaryParticle,
                    player.getX(),
                    player.getY() + primaryParticleYOffset,
                    player.getZ(),
                    primaryParticleCount,
                    primaryParticleSpreadX,
                    primaryParticleSpreadY,
                    primaryParticleSpreadZ,
                    primaryParticleSpeed
            );
        }
        if (secondaryParticleCount > 0) {
            level.sendParticles(
                    secondaryParticle,
                    player.getX(),
                    player.getY() + secondaryParticleYOffset,
                    player.getZ(),
                    secondaryParticleCount,
                    secondaryParticleSpreadX,
                    secondaryParticleSpreadY,
                    secondaryParticleSpreadZ,
                    secondaryParticleSpeed
            );
        }
        level.playSound(null, player.blockPosition(), rainSound, SoundSource.PLAYERS, rainSoundVolume, rainSoundPitch);
    }

    private RainDrop chooseWeighted(RandomSource random) {
        double total = 0.0;
        for (RainDrop drop : dropPool) {
            total += drop.weight;
        }

        double pick = random.nextDouble() * Math.max(0.0001, total);
        double running = 0.0;
        for (RainDrop drop : dropPool) {
            running += drop.weight;
            if (pick <= running) {
                return drop;
            }
        }
        return dropPool.get(dropPool.size() - 1);
    }

    private static ParticleOptions parseParticle(JsonObject obj, String field, ParticleOptions fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            var particleType = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(obj.get(field).getAsString()));
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
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(obj.get(field).getAsString()));
            return sound != null ? sound : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
