package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import com.luckybreak.item.WellCoinMarker;
import com.luckybreak.world.WishingWellManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Unlucky wishing well variant: coin toss triggers debuffs, TNT/fire hazards,
 * smoke particles, and converts water in the well to lava.
 */
public class WishingWellUnluckyEvent implements LuckyEvent {

    private final ResourceLocation structureId;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final int spawnDistance;
    private final int detectRadius;
    private final int activeTicks;
    private final String coinName;
    private final Item coinItem;
    private final ChatFormatting coinColor;
    private final String coinMessage;
    private final int coinMessageColor;
    private final String wishMessage;
    private final int wishMessageColor;
    private final int smokeDurationTicks;
    private final int smokeIntervalTicks;
    private final int smokeCount;
    private final int smokeBurstCount;
    private final ParticleOptions primaryParticle;
    private final ParticleOptions secondaryParticle;
    private final double primaryParticleYOffset;
    private final double secondaryParticleYOffset;
    private final int debuffDurationTicks;
    private final int tntCountMin;
    private final int tntCountMax;
    private final int tntSpawnRadius;
    private final int fireSpreadRadius;
    private final int fireSpreadAttempts;
    private final boolean convertWaterToLava;
    private final long nightTime;
    private final int mobCountMin;
    private final int mobCountMax;
    private final int mobSpawnRadius;
    private final boolean forceSkeletonBow;
    private final float skeletonBowChance;
    private final float zombieWeaponChance;
    private final float zombieArmorChance;
    private final SoundEvent activationSound;
    private final float activationSoundVolume;
    private final float activationSoundPitch;
    private final BlockPos structureOrigin;
    private final boolean ignoreAir;
    private final boolean replaceAirOnly;
    private final Set<Block> replaceBlocksWithAir;

    private WishingWellUnluckyEvent(
            ResourceLocation structureId,
            int offsetX,
            int offsetY,
            int offsetZ,
            int spawnDistance,
            int detectRadius,
            int activeTicks,
            String coinName,
            Item coinItem,
            ChatFormatting coinColor,
            String coinMessage,
            int coinMessageColor,
            String wishMessage,
            int wishMessageColor,
            int smokeDurationTicks,
            int smokeIntervalTicks,
            int smokeCount,
            int smokeBurstCount,
            ParticleOptions primaryParticle,
            ParticleOptions secondaryParticle,
            double primaryParticleYOffset,
            double secondaryParticleYOffset,
            int debuffDurationTicks,
            int tntCountMin,
            int tntCountMax,
            int tntSpawnRadius,
            int fireSpreadRadius,
            int fireSpreadAttempts,
            boolean convertWaterToLava,
            long nightTime,
            int mobCountMin,
            int mobCountMax,
            int mobSpawnRadius,
            boolean forceSkeletonBow,
            float skeletonBowChance,
            float zombieWeaponChance,
            float zombieArmorChance,
            SoundEvent activationSound,
            float activationSoundVolume,
            float activationSoundPitch,
            BlockPos structureOrigin,
            boolean ignoreAir,
            boolean replaceAirOnly,
            Set<Block> replaceBlocksWithAir
    ) {
        this.structureId = structureId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.spawnDistance = spawnDistance;
        this.detectRadius = detectRadius;
        this.activeTicks = activeTicks;
        this.coinName = coinName;
        this.coinItem = coinItem;
        this.coinColor = coinColor;
        this.coinMessage = coinMessage;
        this.coinMessageColor = coinMessageColor;
        this.wishMessage = wishMessage;
        this.wishMessageColor = wishMessageColor;
        this.smokeDurationTicks = smokeDurationTicks;
        this.smokeIntervalTicks = smokeIntervalTicks;
        this.smokeCount = smokeCount;
        this.smokeBurstCount = smokeBurstCount;
        this.primaryParticle = primaryParticle;
        this.secondaryParticle = secondaryParticle;
        this.primaryParticleYOffset = primaryParticleYOffset;
        this.secondaryParticleYOffset = secondaryParticleYOffset;
        this.debuffDurationTicks = debuffDurationTicks;
        this.tntCountMin = tntCountMin;
        this.tntCountMax = tntCountMax;
        this.tntSpawnRadius = tntSpawnRadius;
        this.fireSpreadRadius = fireSpreadRadius;
        this.fireSpreadAttempts = fireSpreadAttempts;
        this.convertWaterToLava = convertWaterToLava;
        this.nightTime = nightTime;
        this.mobCountMin = mobCountMin;
        this.mobCountMax = mobCountMax;
        this.mobSpawnRadius = mobSpawnRadius;
        this.forceSkeletonBow = forceSkeletonBow;
        this.skeletonBowChance = skeletonBowChance;
        this.zombieWeaponChance = zombieWeaponChance;
        this.zombieArmorChance = zombieArmorChance;
        this.activationSound = activationSound;
        this.activationSoundVolume = activationSoundVolume;
        this.activationSoundPitch = activationSoundPitch;
        this.structureOrigin = structureOrigin;
        this.ignoreAir = ignoreAir;
        this.replaceAirOnly = replaceAirOnly;
        this.replaceBlocksWithAir = replaceBlocksWithAir;
    }

    public static WishingWellUnluckyEvent fromJson(JsonObject obj) {
        String structure = obj.has("structure") ? obj.get("structure").getAsString() : "luckybreak:wishing_well";
        int offsetX = obj.has("offset_x") ? obj.get("offset_x").getAsInt() : 0;
        int offsetY = obj.has("offset_y") ? obj.get("offset_y").getAsInt() : 0;
        int offsetZ = obj.has("offset_z") ? obj.get("offset_z").getAsInt() : 0;
        int spawnDistance = obj.has("spawn_distance") ? obj.get("spawn_distance").getAsInt() : 8;
        int detectRadius = obj.has("detect_radius") ? obj.get("detect_radius").getAsInt() : 2;
        int activeTicks = obj.has("active_ticks") ? obj.get("active_ticks").getAsInt() : 20 * 45;

        String coinName = obj.has("coin_name") ? obj.get("coin_name").getAsString() : "Cursed Coin";
        Item coinItem = parseCoinItem(obj);
        ChatFormatting coinColor = parseCoinColor(obj);
        String coinMessage = obj.has("coin_message") ? obj.get("coin_message").getAsString() : "You got a Cursed Coin...";
        int coinMessageColor = parseRgbColor(obj, "coin_message_color", 0xAA0000);
        String wishMessage = obj.has("wish_message") ? obj.get("wish_message").getAsString() : "Your luck ran out...";
        int wishMessageColor = parseRgbColor(obj, "wish_message_color", 0xFF5555);

        int smokeDurationTicks = obj.has("smoke_duration_ticks") ? obj.get("smoke_duration_ticks").getAsInt() : 100;
        int smokeIntervalTicks = obj.has("smoke_interval_ticks") ? obj.get("smoke_interval_ticks").getAsInt() : 3;
        int smokeCount = obj.has("smoke_count") ? obj.get("smoke_count").getAsInt() : 8;
        int smokeBurstCount = obj.has("smoke_burst_count") ? obj.get("smoke_burst_count").getAsInt() : 16;
        ParticleOptions primaryParticle = parseParticle(obj, "primary_particle", ParticleTypes.SMOKE);
        ParticleOptions secondaryParticle = parseParticle(obj, "secondary_particle", ParticleTypes.LARGE_SMOKE);
        double primaryParticleYOffset = obj.has("particle_y_offset") ? obj.get("particle_y_offset").getAsDouble() : 0.0;
        double secondaryParticleYOffset = obj.has("secondary_particle_y_offset") ? obj.get("secondary_particle_y_offset").getAsDouble() : 0.0;

        int debuffDurationTicks = obj.has("debuff_duration_ticks") ? obj.get("debuff_duration_ticks").getAsInt() : 160;
        int tntCountMin = obj.has("tnt_count_min") ? obj.get("tnt_count_min").getAsInt() : 2;
        int tntCountMax = obj.has("tnt_count_max") ? obj.get("tnt_count_max").getAsInt() : 4;
        int tntSpawnRadius = obj.has("tnt_spawn_radius") ? obj.get("tnt_spawn_radius").getAsInt() : 4;
        int fireSpreadRadius = obj.has("fire_spread_radius") ? obj.get("fire_spread_radius").getAsInt() : 4;
        int fireSpreadAttempts = obj.has("fire_spread_attempts") ? obj.get("fire_spread_attempts").getAsInt() : 20;
        boolean convertWaterToLava = !obj.has("convert_water_to_lava") || obj.get("convert_water_to_lava").getAsBoolean();
        long nightTime = obj.has("night_time") ? obj.get("night_time").getAsLong() : 13000L;
        int mobCountMin = obj.has("mob_count_min") ? obj.get("mob_count_min").getAsInt() : 3;
        int mobCountMax = obj.has("mob_count_max") ? obj.get("mob_count_max").getAsInt() : 6;
        int mobSpawnRadius = obj.has("mob_spawn_radius") ? obj.get("mob_spawn_radius").getAsInt() : 7;
        boolean forceSkeletonBow = !obj.has("force_skeleton_bow") || obj.get("force_skeleton_bow").getAsBoolean();
        float skeletonBowChance = parseFloat(obj, "skeleton_bow_chance", 1.0f);
        float zombieWeaponChance = parseFloat(obj, "zombie_weapon_chance", 0.60f);
        float zombieArmorChance = parseFloat(obj, "zombie_armor_chance", 0.45f);

        SoundEvent activationSound = parseSoundEvent(obj, "activation_sound", SoundEvents.FIRECHARGE_USE);
        float activationSoundVolume = parseFloat(obj, "activation_sound_volume", 1.0f);
        float activationSoundPitch = parseFloat(obj, "activation_sound_pitch", 0.8f);

        int originX = obj.has("origin_x") ? obj.get("origin_x").getAsInt() : 0;
        int originY = obj.has("origin_y") ? obj.get("origin_y").getAsInt() : 0;
        int originZ = obj.has("origin_z") ? obj.get("origin_z").getAsInt() : 0;
        boolean ignoreAir = !obj.has("ignore_air") || obj.get("ignore_air").getAsBoolean();
        boolean replaceAirOnly = obj.has("replace_air_only") && obj.get("replace_air_only").getAsBoolean();
        Set<Block> replaceBlocksWithAir = parseReplaceBlocksWithAir(obj);

        if (spawnDistance < 1) spawnDistance = 1;
        if (detectRadius < 1) detectRadius = 1;
        if (activeTicks < 20) activeTicks = 20;
        if (smokeDurationTicks < 4) smokeDurationTicks = 4;
        if (smokeIntervalTicks < 1) smokeIntervalTicks = 1;
        if (smokeCount < 1) smokeCount = 1;
        if (smokeBurstCount < 0) smokeBurstCount = 0;
        if (debuffDurationTicks < 20) debuffDurationTicks = 20;
        if (tntCountMin < 0) tntCountMin = 0;
        if (tntCountMax < tntCountMin) tntCountMax = tntCountMin;
        if (tntSpawnRadius < 1) tntSpawnRadius = 1;
        if (fireSpreadRadius < 1) fireSpreadRadius = 1;
        if (fireSpreadAttempts < 0) fireSpreadAttempts = 0;
        if (mobCountMin < 0) mobCountMin = 0;
        if (mobCountMax < mobCountMin) mobCountMax = mobCountMin;
        if (mobSpawnRadius < 2) mobSpawnRadius = 2;
        skeletonBowChance = Mth.clamp(skeletonBowChance, 0.0f, 1.0f);
        zombieWeaponChance = Mth.clamp(zombieWeaponChance, 0.0f, 1.0f);
        zombieArmorChance = Mth.clamp(zombieArmorChance, 0.0f, 1.0f);

        return new WishingWellUnluckyEvent(
                ResourceLocation.parse(structure),
                offsetX,
                offsetY,
                offsetZ,
                spawnDistance,
                detectRadius,
                activeTicks,
                coinName,
                coinItem,
                coinColor,
                coinMessage,
                coinMessageColor,
                wishMessage,
                wishMessageColor,
                smokeDurationTicks,
                smokeIntervalTicks,
                smokeCount,
                smokeBurstCount,
                primaryParticle,
                secondaryParticle,
                primaryParticleYOffset,
                secondaryParticleYOffset,
                debuffDurationTicks,
                tntCountMin,
                tntCountMax,
                tntSpawnRadius,
                fireSpreadRadius,
                fireSpreadAttempts,
                convertWaterToLava,
                nightTime,
                mobCountMin,
                mobCountMax,
                mobSpawnRadius,
                forceSkeletonBow,
                skeletonBowChance,
                zombieWeaponChance,
                zombieArmorChance,
                activationSound,
                activationSoundVolume,
                activationSoundPitch,
                new BlockPos(originX, originY, originZ),
                ignoreAir,
                replaceAirOnly,
                replaceBlocksWithAir
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        SimpleJsonStructure structure = SimpleJsonStructure.load(level, structureId);
        if (structure == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] JSON structure not found for unlucky wishing well event: {}", structureId);
            return;
        }

        Vec3 look = player.getLookAngle();
        double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
        double dirX;
        double dirZ;
        if (horizontalLength > 1.0E-4) {
            dirX = look.x / horizontalLength;
            dirZ = look.z / horizontalLength;
        } else {
            dirX = player.getDirection().getStepX();
            dirZ = player.getDirection().getStepZ();
        }

        int anchorX = Mth.floor(player.getX() + dirX * spawnDistance) + offsetX;
        int anchorZ = Mth.floor(player.getZ() + dirZ * spawnDistance) + offsetZ;

        int placeY = player.blockPosition().getY() + offsetY;
        int minSafeY = level.getMinY() + 1;
        if (placeY < minSafeY) {
            placeY = minSafeY;
        }

        BlockPos wellCenter = new BlockPos(anchorX, placeY, anchorZ);
        boolean placed = structure.place(level, wellCenter, structureOrigin, ignoreAir, replaceAirOnly, replaceBlocksWithAir);
        if (!placed) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Unlucky wishing well structure placement returned false at {}", wellCenter);
        }

        ItemStack coin = new ItemStack(coinItem);
        coin.set(DataComponents.CUSTOM_NAME, Component.literal(coinName).withStyle(coinColor));
        WellCoinMarker.mark(coin);
        Inventory inventory = player.getInventory();
        if (!inventory.add(coin)) {
            player.drop(coin, false);
        }

        player.displayClientMessage(
                Component.literal(coinMessage).withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(coinMessageColor))),
                false
        );

        WishingWellManager.registerUnluckyWell(
                level,
                wellCenter,
                detectRadius,
                -3,
                activeTicks,
                smokeDurationTicks,
                smokeIntervalTicks,
                smokeCount,
                smokeBurstCount,
                primaryParticle,
                secondaryParticle,
                primaryParticleYOffset,
                secondaryParticleYOffset,
                coinName,
                coinItem,
                wishMessage,
                wishMessageColor,
                activationSound,
                activationSoundVolume,
                activationSoundPitch,
                debuffDurationTicks,
                tntCountMin,
                tntCountMax,
                tntSpawnRadius,
                fireSpreadRadius,
                fireSpreadAttempts,
                convertWaterToLava,
                nightTime,
                mobCountMin,
                mobCountMax,
                mobSpawnRadius,
                forceSkeletonBow,
                skeletonBowChance,
                zombieWeaponChance,
                zombieArmorChance
        );
    }

    private static Item parseCoinItem(JsonObject obj) {
        if (!obj.has("coin_item")) {
            return Items.GOLD_NUGGET;
        }
        try {
            Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(obj.get("coin_item").getAsString()));
            if (item != null && item != Items.AIR) {
                return item;
            }
        } catch (Exception ignored) {
        }
        return Items.GOLD_NUGGET;
    }

    private static ChatFormatting parseCoinColor(JsonObject obj) {
        if (!obj.has("coin_color")) {
            return ChatFormatting.DARK_RED;
        }
        try {
            return ChatFormatting.valueOf(obj.get("coin_color").getAsString().trim().toUpperCase());
        } catch (Exception ignored) {
            return ChatFormatting.DARK_RED;
        }
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

    private static float parseFloat(JsonObject obj, String field, float fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            return obj.get(field).getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseRgbColor(JsonObject obj, String field, int fallback) {
        if (!obj.has(field)) {
            return fallback;
        }

        try {
            JsonElement element = obj.get(field);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt() & 0xFFFFFF;
            }

            String value = element.getAsString().trim();
            try {
                ChatFormatting formatting = ChatFormatting.valueOf(value.toUpperCase());
                Integer named = formatting.getColor();
                if (named != null) {
                    return named & 0xFFFFFF;
                }
            } catch (Exception ignored) {
            }

            if (value.startsWith("#")) {
                value = value.substring(1);
            } else if (value.startsWith("0x") || value.startsWith("0X")) {
                value = value.substring(2);
            }
            if (value.length() == 6) {
                return Integer.parseInt(value, 16) & 0xFFFFFF;
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private static Set<Block> parseReplaceBlocksWithAir(JsonObject obj) {
        Set<Block> blocks = new HashSet<>();

        if (obj.has("replace_block_with_air")) {
            Block block = BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse(obj.get("replace_block_with_air").getAsString()));
            if (block != null && block != Blocks.AIR) {
                blocks.add(block);
            }
        }

        if (obj.has("replace_blocks_with_air")) {
            JsonArray arr = obj.getAsJsonArray("replace_blocks_with_air");
            for (JsonElement element : arr) {
                Block block = BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse(element.getAsString()));
                if (block != null && block != Blocks.AIR) {
                    blocks.add(block);
                }
            }
        }

        return blocks;
    }
}
