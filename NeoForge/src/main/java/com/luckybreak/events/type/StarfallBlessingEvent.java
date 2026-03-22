package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

import java.util.ArrayList;
import java.util.List;

public class StarfallBlessingEvent implements LuckyEvent {

    private record RewardEntry(Item item, double weight, int countMin, int countMax) {}

    private final int rewardDropsMin;
    private final int rewardDropsMax;
    private final int fireworkRocketCount;
    private final int fireworkFlight;
    private final IntList fireworkColors;
    private final IntList fireworkFadeColors;
    private final boolean fireworkFlicker;
    private final boolean fireworkTrail;
    private final FireworkExplosion.Shape fireworkShape;
    private final double fireworkSpawnSpread;
    private final double fireworkSpawnYOffset;
    private final double fireworkMotionXZ;
    private final double fireworkMotionYBase;
    private final double fireworkMotionYRandom;
    private final int blessingDurationTicks;
    private final int regenerationAmplifier;
    private final int absorptionAmplifier;
    private final int speedAmplifier;
    private final double rewardSpawnYOffset;
    private final double rewardMotionXZ;
    private final double rewardMotionYBase;
    private final double rewardMotionYRandom;
    private final int rewardPickupDelay;
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
    private final SoundEvent blessingSound;
    private final float blessingSoundVolume;
    private final float blessingSoundPitch;
    private final List<RewardEntry> rewards;

    private StarfallBlessingEvent(
            int rewardDropsMin,
            int rewardDropsMax,
            int fireworkRocketCount,
            int fireworkFlight,
            IntList fireworkColors,
            IntList fireworkFadeColors,
            boolean fireworkFlicker,
            boolean fireworkTrail,
                FireworkExplosion.Shape fireworkShape,
                double fireworkSpawnSpread,
                double fireworkSpawnYOffset,
                double fireworkMotionXZ,
                double fireworkMotionYBase,
                double fireworkMotionYRandom,
            int blessingDurationTicks,
                int regenerationAmplifier,
                int absorptionAmplifier,
                int speedAmplifier,
                double rewardSpawnYOffset,
                double rewardMotionXZ,
                double rewardMotionYBase,
                double rewardMotionYRandom,
                int rewardPickupDelay,
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
                SoundEvent blessingSound,
                float blessingSoundVolume,
                float blessingSoundPitch,
            List<RewardEntry> rewards
    ) {
        this.rewardDropsMin = rewardDropsMin;
        this.rewardDropsMax = rewardDropsMax;
        this.fireworkRocketCount = fireworkRocketCount;
        this.fireworkFlight = fireworkFlight;
        this.fireworkColors = fireworkColors;
        this.fireworkFadeColors = fireworkFadeColors;
        this.fireworkFlicker = fireworkFlicker;
        this.fireworkTrail = fireworkTrail;
        this.fireworkShape = fireworkShape;
        this.fireworkSpawnSpread = fireworkSpawnSpread;
        this.fireworkSpawnYOffset = fireworkSpawnYOffset;
        this.fireworkMotionXZ = fireworkMotionXZ;
        this.fireworkMotionYBase = fireworkMotionYBase;
        this.fireworkMotionYRandom = fireworkMotionYRandom;
        this.blessingDurationTicks = blessingDurationTicks;
        this.regenerationAmplifier = regenerationAmplifier;
        this.absorptionAmplifier = absorptionAmplifier;
        this.speedAmplifier = speedAmplifier;
        this.rewardSpawnYOffset = rewardSpawnYOffset;
        this.rewardMotionXZ = rewardMotionXZ;
        this.rewardMotionYBase = rewardMotionYBase;
        this.rewardMotionYRandom = rewardMotionYRandom;
        this.rewardPickupDelay = rewardPickupDelay;
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
        this.blessingSound = blessingSound;
        this.blessingSoundVolume = blessingSoundVolume;
        this.blessingSoundPitch = blessingSoundPitch;
        this.rewards = rewards;
    }

    public static StarfallBlessingEvent fromJson(JsonObject obj) {
        int rewardDropsMin = obj.has("reward_drops_min") ? obj.get("reward_drops_min").getAsInt() : 4;
        int rewardDropsMax = obj.has("reward_drops_max") ? obj.get("reward_drops_max").getAsInt() : 7;
        int fireworkRocketCount = obj.has("firework_rocket_count") ? obj.get("firework_rocket_count").getAsInt() : 3;
        int fireworkFlight = obj.has("firework_flight") ? obj.get("firework_flight").getAsInt() : 1;
        IntList fireworkColors = parseColorList(obj, "firework_colors", List.of(0xFFD700, 0xFFFFFF, 0x55FFFF));
        IntList fireworkFadeColors = parseColorList(obj, "firework_fade_colors", List.of(0xFFFFFF));
        boolean fireworkFlicker = !obj.has("firework_flicker") || obj.get("firework_flicker").getAsBoolean();
        boolean fireworkTrail = !obj.has("firework_trail") || obj.get("firework_trail").getAsBoolean();
        FireworkExplosion.Shape fireworkShape = parseFireworkShape(obj, "firework_shape", FireworkExplosion.Shape.STAR);
        double fireworkSpawnSpread = obj.has("firework_spawn_spread") ? obj.get("firework_spawn_spread").getAsDouble() : 1.4;
        double fireworkSpawnYOffset = obj.has("firework_spawn_y_offset") ? obj.get("firework_spawn_y_offset").getAsDouble() : 0.25;
        double fireworkMotionXZ = obj.has("firework_motion_xz") ? obj.get("firework_motion_xz").getAsDouble() : 0.10;
        double fireworkMotionYBase = obj.has("firework_motion_y_base") ? obj.get("firework_motion_y_base").getAsDouble() : 0.42;
        double fireworkMotionYRandom = obj.has("firework_motion_y_random") ? obj.get("firework_motion_y_random").getAsDouble() : 0.14;
        int blessingDurationTicks = obj.has("blessing_duration_ticks") ? obj.get("blessing_duration_ticks").getAsInt() : 20 * 25;
        int regenerationAmplifier = obj.has("regeneration_amplifier") ? obj.get("regeneration_amplifier").getAsInt() : 1;
        int absorptionAmplifier = obj.has("absorption_amplifier") ? obj.get("absorption_amplifier").getAsInt() : 0;
        int speedAmplifier = obj.has("speed_amplifier") ? obj.get("speed_amplifier").getAsInt() : 0;
        double rewardSpawnYOffset = obj.has("reward_spawn_y_offset") ? obj.get("reward_spawn_y_offset").getAsDouble() : 0.2;
        double rewardMotionXZ = obj.has("reward_motion_xz") ? obj.get("reward_motion_xz").getAsDouble() : 0.26;
        double rewardMotionYBase = obj.has("reward_motion_y_base") ? obj.get("reward_motion_y_base").getAsDouble() : 0.28;
        double rewardMotionYRandom = obj.has("reward_motion_y_random") ? obj.get("reward_motion_y_random").getAsDouble() : 0.15;
        int rewardPickupDelay = obj.has("reward_pickup_delay") ? obj.get("reward_pickup_delay").getAsInt() : 8;
        ParticleOptions primaryParticle = parseParticle(obj, "primary_particle", ParticleTypes.TOTEM_OF_UNDYING);
        int primaryParticleCount = obj.has("primary_particle_count") ? obj.get("primary_particle_count").getAsInt() : 16;
        double primaryParticleYOffset = obj.has("primary_particle_y_offset") ? obj.get("primary_particle_y_offset").getAsDouble() : 0.25;
        double primaryParticleSpreadX = obj.has("primary_particle_spread_x") ? obj.get("primary_particle_spread_x").getAsDouble() : 0.8;
        double primaryParticleSpreadY = obj.has("primary_particle_spread_y") ? obj.get("primary_particle_spread_y").getAsDouble() : 0.5;
        double primaryParticleSpreadZ = obj.has("primary_particle_spread_z") ? obj.get("primary_particle_spread_z").getAsDouble() : 0.8;
        double primaryParticleSpeed = obj.has("primary_particle_speed") ? obj.get("primary_particle_speed").getAsDouble() : 0.02;
        ParticleOptions secondaryParticle = parseParticle(obj, "secondary_particle", ParticleTypes.END_ROD);
        int secondaryParticleCount = obj.has("secondary_particle_count") ? obj.get("secondary_particle_count").getAsInt() : 20;
        double secondaryParticleYOffset = obj.has("secondary_particle_y_offset") ? obj.get("secondary_particle_y_offset").getAsDouble() : 0.55;
        double secondaryParticleSpreadX = obj.has("secondary_particle_spread_x") ? obj.get("secondary_particle_spread_x").getAsDouble() : 0.9;
        double secondaryParticleSpreadY = obj.has("secondary_particle_spread_y") ? obj.get("secondary_particle_spread_y").getAsDouble() : 0.6;
        double secondaryParticleSpreadZ = obj.has("secondary_particle_spread_z") ? obj.get("secondary_particle_spread_z").getAsDouble() : 0.9;
        double secondaryParticleSpeed = obj.has("secondary_particle_speed") ? obj.get("secondary_particle_speed").getAsDouble() : 0.03;
        SoundEvent blessingSound = parseSoundEvent(obj, "blessing_sound", SoundEvents.BEACON_ACTIVATE);
        float blessingSoundVolume = obj.has("blessing_sound_volume") ? obj.get("blessing_sound_volume").getAsFloat() : 1.0f;
        float blessingSoundPitch = obj.has("blessing_sound_pitch") ? obj.get("blessing_sound_pitch").getAsFloat() : 1.12f;

        if (rewardDropsMin < 1) rewardDropsMin = 1;
        if (rewardDropsMax < rewardDropsMin) rewardDropsMax = rewardDropsMin;
        if (fireworkRocketCount < 0) fireworkRocketCount = 0;
        if (fireworkFlight < 0) fireworkFlight = 0;
        if (fireworkFlight > 3) fireworkFlight = 3;
        if (fireworkSpawnSpread < 0.0) fireworkSpawnSpread = 0.0;
        if (fireworkMotionXZ < 0.0) fireworkMotionXZ = 0.0;
        if (fireworkMotionYRandom < 0.0) fireworkMotionYRandom = 0.0;
        if (blessingDurationTicks < 20) blessingDurationTicks = 20;
        if (regenerationAmplifier < 0) regenerationAmplifier = 0;
        if (absorptionAmplifier < 0) absorptionAmplifier = 0;
        if (speedAmplifier < 0) speedAmplifier = 0;
        if (rewardMotionXZ < 0.0) rewardMotionXZ = 0.0;
        if (rewardMotionYRandom < 0.0) rewardMotionYRandom = 0.0;
        if (rewardPickupDelay < 0) rewardPickupDelay = 0;
        if (primaryParticleCount < 0) primaryParticleCount = 0;
        if (secondaryParticleCount < 0) secondaryParticleCount = 0;

        List<RewardEntry> rewards = new ArrayList<>();
        if (obj.has("rewards") && obj.get("rewards").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("rewards");
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject entry = el.getAsJsonObject();
                if (!entry.has("item")) continue;
                Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(entry.get("item").getAsString()));
                if (item == null || item == Items.AIR) continue;
                double weight = entry.has("weight") ? entry.get("weight").getAsDouble() : 1.0;
                int countMin = entry.has("count_min") ? entry.get("count_min").getAsInt() : 1;
                int countMax = entry.has("count_max") ? entry.get("count_max").getAsInt() : countMin;
                if (weight <= 0.0) continue;
                if (countMin < 1) countMin = 1;
                if (countMax < countMin) countMax = countMin;
                rewards.add(new RewardEntry(item, weight, countMin, countMax));
            }
        }

        if (rewards.isEmpty()) {
            rewards.add(new RewardEntry(Items.EMERALD, 3.0, 1, 3));
            rewards.add(new RewardEntry(Items.DIAMOND, 2.0, 1, 2));
            rewards.add(new RewardEntry(Items.EXPERIENCE_BOTTLE, 3.2, 2, 5));
            rewards.add(new RewardEntry(Items.GOLDEN_APPLE, 1.4, 1, 1));
            rewards.add(new RewardEntry(Items.ENDER_PEARL, 2.0, 1, 3));
            rewards.add(new RewardEntry(Items.AMETHYST_SHARD, 2.6, 3, 8));
        }

        return new StarfallBlessingEvent(
                rewardDropsMin,
                rewardDropsMax,
                fireworkRocketCount,
                fireworkFlight,
                new IntArrayList(fireworkColors),
                new IntArrayList(fireworkFadeColors),
                fireworkFlicker,
                fireworkTrail,
                fireworkShape,
                fireworkSpawnSpread,
                fireworkSpawnYOffset,
                fireworkMotionXZ,
                fireworkMotionYBase,
                fireworkMotionYRandom,
                blessingDurationTicks,
                regenerationAmplifier,
                absorptionAmplifier,
                speedAmplifier,
                rewardSpawnYOffset,
                rewardMotionXZ,
                rewardMotionYBase,
                rewardMotionYRandom,
                rewardPickupDelay,
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
                blessingSound,
                blessingSoundVolume,
                blessingSoundPitch,
                List.copyOf(rewards)
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        RandomSource random = level.getRandom();
        int drops = rewardDropsMin + random.nextInt(Math.max(1, rewardDropsMax - rewardDropsMin + 1));

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, blessingDurationTicks, regenerationAmplifier));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, blessingDurationTicks, absorptionAmplifier));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, blessingDurationTicks, speedAmplifier));

        double cx = player.getX();
        double cy = player.getY() + 1.0;
        double cz = player.getZ();

        for (int i = 0; i < fireworkRocketCount; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET, 1);
            FireworkExplosion explosion = new FireworkExplosion(
                    fireworkShape,
                    fireworkColors,
                    fireworkFadeColors,
                    fireworkTrail,
                    fireworkFlicker
            );
            rocket.set(DataComponents.FIREWORKS, new Fireworks(fireworkFlight, List.of(explosion)));

                double x = cx + (random.nextDouble() - 0.5) * fireworkSpawnSpread;
                double y = cy + fireworkSpawnYOffset;
                double z = cz + (random.nextDouble() - 0.5) * fireworkSpawnSpread;
            FireworkRocketEntity entity = new FireworkRocketEntity(level, x, y, z, rocket);
            entity.setDeltaMovement(
                    (random.nextDouble() - 0.5) * fireworkMotionXZ,
                    fireworkMotionYBase + random.nextDouble() * fireworkMotionYRandom,
                    (random.nextDouble() - 0.5) * fireworkMotionXZ
            );
            level.addFreshEntity(entity);
        }

        for (int i = 0; i < drops; i++) {
            RewardEntry reward = chooseReward(random);
            int count = reward.countMin + random.nextInt(Math.max(1, reward.countMax - reward.countMin + 1));
            ItemEntity itemEntity = new ItemEntity(level, cx, cy + rewardSpawnYOffset, cz, new ItemStack(reward.item, count));
            itemEntity.setDeltaMovement(
                    (random.nextDouble() - 0.5) * rewardMotionXZ,
                    rewardMotionYBase + random.nextDouble() * rewardMotionYRandom,
                    (random.nextDouble() - 0.5) * rewardMotionXZ
            );
            itemEntity.setPickUpDelay(rewardPickupDelay);
            level.addFreshEntity(itemEntity);
        }

        if (primaryParticleCount > 0) {
            level.sendParticles(
                    primaryParticle,
                    cx,
                    cy + primaryParticleYOffset,
                    cz,
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
                    cx,
                    cy + secondaryParticleYOffset,
                    cz,
                    secondaryParticleCount,
                    secondaryParticleSpreadX,
                    secondaryParticleSpreadY,
                    secondaryParticleSpreadZ,
                    secondaryParticleSpeed
            );
        }
        level.playSound(null, player.blockPosition(), blessingSound, SoundSource.PLAYERS, blessingSoundVolume, blessingSoundPitch);
    }

    private RewardEntry chooseReward(RandomSource random) {
        double totalWeight = 0.0;
        for (RewardEntry reward : rewards) {
            totalWeight += reward.weight;
        }

        double pick = random.nextDouble() * Math.max(0.0001, totalWeight);
        double running = 0.0;
        for (RewardEntry reward : rewards) {
            running += reward.weight;
            if (pick <= running) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
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

    private static FireworkExplosion.Shape parseFireworkShape(JsonObject obj, String field, FireworkExplosion.Shape fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            return FireworkExplosion.Shape.valueOf(obj.get(field).getAsString().trim().toUpperCase());
        } catch (Exception ignored) {
            return fallback;
        }
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
