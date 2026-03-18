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

public class GemSprinkleEvent implements LuckyEvent {

    private record DropEntry(Item item, double weight, int countMin, int countMax) {}

    private final int dropCountMin;
    private final int dropCountMax;
    private final double radius;
    private final List<DropEntry> dropPool;
    private final ParticleOptions primaryParticle;
    private final int primaryParticleCount;
    private final ParticleOptions secondaryParticle;
    private final int secondaryParticleCount;
    private final SoundEvent sparkleSound;
    private final float sparkleSoundVolume;
    private final float sparkleSoundPitch;

    private GemSprinkleEvent(
            int dropCountMin,
            int dropCountMax,
            double radius,
            List<DropEntry> dropPool,
            ParticleOptions primaryParticle,
            int primaryParticleCount,
            ParticleOptions secondaryParticle,
            int secondaryParticleCount,
            SoundEvent sparkleSound,
            float sparkleSoundVolume,
            float sparkleSoundPitch
    ) {
        this.dropCountMin = dropCountMin;
        this.dropCountMax = dropCountMax;
        this.radius = radius;
        this.dropPool = dropPool;
        this.primaryParticle = primaryParticle;
        this.primaryParticleCount = primaryParticleCount;
        this.secondaryParticle = secondaryParticle;
        this.secondaryParticleCount = secondaryParticleCount;
        this.sparkleSound = sparkleSound;
        this.sparkleSoundVolume = sparkleSoundVolume;
        this.sparkleSoundPitch = sparkleSoundPitch;
    }

    public static GemSprinkleEvent fromJson(JsonObject obj) {
        int dropCountMin = obj.has("drop_count_min") ? obj.get("drop_count_min").getAsInt() : 8;
        int dropCountMax = obj.has("drop_count_max") ? obj.get("drop_count_max").getAsInt() : 14;
        double radius = obj.has("radius") ? obj.get("radius").getAsDouble() : 2.6;

        if (dropCountMin < 1) dropCountMin = 1;
        if (dropCountMax < dropCountMin) dropCountMax = dropCountMin;
        if (radius < 0.2) radius = 0.2;

        ParticleOptions primaryParticle = parseParticle(obj, "primary_particle", ParticleTypes.WAX_ON);
        int primaryParticleCount = obj.has("primary_particle_count") ? obj.get("primary_particle_count").getAsInt() : 28;
        ParticleOptions secondaryParticle = parseParticle(obj, "secondary_particle", ParticleTypes.HAPPY_VILLAGER);
        int secondaryParticleCount = obj.has("secondary_particle_count") ? obj.get("secondary_particle_count").getAsInt() : 18;
        SoundEvent sparkleSound = parseSoundEvent(obj, "sparkle_sound", SoundEvents.AMETHYST_BLOCK_CHIME);
        float sparkleSoundVolume = obj.has("sparkle_sound_volume") ? obj.get("sparkle_sound_volume").getAsFloat() : 0.95f;
        float sparkleSoundPitch = obj.has("sparkle_sound_pitch") ? obj.get("sparkle_sound_pitch").getAsFloat() : 1.15f;

        if (primaryParticleCount < 0) primaryParticleCount = 0;
        if (secondaryParticleCount < 0) secondaryParticleCount = 0;

        List<DropEntry> pool = new ArrayList<>();
        if (obj.has("drops") && obj.get("drops").isJsonArray()) {
            JsonArray drops = obj.getAsJsonArray("drops");
            for (JsonElement element : drops) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject drop = element.getAsJsonObject();
                if (!drop.has("item")) {
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(drop.get("item").getAsString()));
                if (item == null || item == Items.AIR) {
                    continue;
                }

                double weight = drop.has("weight") ? drop.get("weight").getAsDouble() : 1.0;
                int countMin = drop.has("count_min") ? drop.get("count_min").getAsInt() : 1;
                int countMax = drop.has("count_max") ? drop.get("count_max").getAsInt() : countMin;
                if (weight <= 0) {
                    continue;
                }
                if (countMin < 1) countMin = 1;
                if (countMax < countMin) countMax = countMin;

                pool.add(new DropEntry(item, weight, countMin, countMax));
            }
        }

        if (pool.isEmpty()) {
            pool.add(new DropEntry(Items.IRON_INGOT, 5.0, 2, 5));
            pool.add(new DropEntry(Items.GOLD_INGOT, 4.0, 2, 4));
            pool.add(new DropEntry(Items.REDSTONE, 4.0, 4, 10));
            pool.add(new DropEntry(Items.LAPIS_LAZULI, 4.0, 4, 10));
            pool.add(new DropEntry(Items.EMERALD, 2.5, 1, 3));
            pool.add(new DropEntry(Items.DIAMOND, 1.4, 1, 2));
            pool.add(new DropEntry(Items.AMETHYST_SHARD, 3.0, 3, 8));
        }

        return new GemSprinkleEvent(
            dropCountMin,
            dropCountMax,
            radius,
            List.copyOf(pool),
            primaryParticle,
            primaryParticleCount,
            secondaryParticle,
            secondaryParticleCount,
            sparkleSound,
            sparkleSoundVolume,
            sparkleSoundPitch
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        RandomSource random = level.getRandom();
        int drops = dropCountMin + random.nextInt(Math.max(1, dropCountMax - dropCountMin + 1));

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.1;
        double cz = pos.getZ() + 0.5;

        for (int i = 0; i < drops; i++) {
            DropEntry entry = chooseWeighted(random);
            int count = entry.countMin + random.nextInt(Math.max(1, entry.countMax - entry.countMin + 1));
            ItemStack stack = new ItemStack(entry.item, count);

            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 0.2 + random.nextDouble() * radius;
            double x = cx + Math.cos(angle) * distance;
            double z = cz + Math.sin(angle) * distance;

            ItemEntity entity = new ItemEntity(level, x, cy, z, stack);
            entity.setDeltaMovement(
                    (random.nextDouble() - 0.5) * 0.16,
                    0.15 + random.nextDouble() * 0.12,
                    (random.nextDouble() - 0.5) * 0.16
            );
            entity.setPickUpDelay(10);
            level.addFreshEntity(entity);
        }

        if (primaryParticleCount > 0) {
            level.sendParticles(primaryParticle, cx, cy + 0.2, cz, primaryParticleCount, 0.8, 0.5, 0.8, 0.01);
        }
        if (secondaryParticleCount > 0) {
            level.sendParticles(secondaryParticle, cx, cy + 0.3, cz, secondaryParticleCount, 0.8, 0.4, 0.8, 0.02);
        }
        level.playSound(null, pos, sparkleSound, SoundSource.PLAYERS, sparkleSoundVolume, sparkleSoundPitch);
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

    private DropEntry chooseWeighted(RandomSource random) {
        double total = 0.0;
        for (DropEntry entry : dropPool) {
            total += entry.weight;
        }

        double pick = random.nextDouble() * Math.max(0.0001, total);
        double running = 0.0;
        for (DropEntry entry : dropPool) {
            running += entry.weight;
            if (pick <= running) {
                return entry;
            }
        }
        return dropPool.get(dropPool.size() - 1);
    }
}
