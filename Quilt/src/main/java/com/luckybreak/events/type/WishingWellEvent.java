package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import com.luckybreak.item.WellCoinMarker;
import com.luckybreak.world.WishingWellManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Places a wishing-well structure, gives the player a named coin,
 * then waits for the coin to be thrown into the well to spawn rising foods.
 */
public class WishingWellEvent implements LuckyEvent {

    private static final int DEFAULT_BOTTOM_OFFSET_Y = -3;

    private final Identifier structureId;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final int spawnDistance;
    private final int detectRadius;
    private final int bottomOffsetY;
    private final int activeTicks;
    private final int foodCountMin;
    private final int foodCountMax;
    private final int foodRainDurationTicks;
    private final int heartDurationTicks;
    private final int heartIntervalTicks;
    private final int heartCount;
    private final int bubblePopCount;
    private final ParticleOptions primaryParticle;
    private final ParticleOptions secondaryParticle;
    private final double primaryParticleYOffset;
    private final double secondaryParticleYOffset;
    private final String coinName;
    private final Item coinItem;
    private final ChatFormatting coinColor;
    private final String coinMessage;
    private final int coinMessageColor;
    private final String wishMessage;
    private final int wishMessageColor;
    private final Item guaranteedDropItem;
    private final int guaranteedDropCount;
    private final List<WishingWellManager.WeightedDrop> dropPool;
    private final int dropTrailDurationTicks;
    private final int dropTrailIntervalTicks;
    private final int dropTrailCount;
    private final double dropTrailSpread;
    private final SoundEvent activationSound;
    private final float activationSoundVolume;
    private final float activationSoundPitch;
    private final int fireworkBurstCount;
    private final int fireworkRocketCount;
    private final int fireworkFlight;
    private final IntList fireworkColors;
    private final IntList fireworkFadeColors;
    private final boolean fireworkFlicker;
    private final boolean fireworkTrail;
    private final double fireworkLaunchYOffset;
    private final BlockPos structureOrigin;
    private final boolean ignoreAir;
    private final boolean replaceAirOnly;
    private final Set<Block> replaceBlocksWithAir;

    private WishingWellEvent(
            Identifier structureId,
            int offsetX,
            int offsetY,
            int offsetZ,
            int spawnDistance,
            int detectRadius,
            int bottomOffsetY,
            int activeTicks,
            int foodCountMin,
            int foodCountMax,
            int foodRainDurationTicks,
            int heartDurationTicks,
            int heartIntervalTicks,
            int heartCount,
            int bubblePopCount,
            ParticleOptions primaryParticle,
            ParticleOptions secondaryParticle,
            double primaryParticleYOffset,
            double secondaryParticleYOffset,
            String coinName,
            Item coinItem,
            ChatFormatting coinColor,
            String coinMessage,
            int coinMessageColor,
            String wishMessage,
            int wishMessageColor,
            Item guaranteedDropItem,
            int guaranteedDropCount,
            List<WishingWellManager.WeightedDrop> dropPool,
            int dropTrailDurationTicks,
            int dropTrailIntervalTicks,
            int dropTrailCount,
            double dropTrailSpread,
            SoundEvent activationSound,
            float activationSoundVolume,
            float activationSoundPitch,
            int fireworkBurstCount,
            int fireworkRocketCount,
            int fireworkFlight,
            IntList fireworkColors,
            IntList fireworkFadeColors,
            boolean fireworkFlicker,
            boolean fireworkTrail,
            double fireworkLaunchYOffset,
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
        this.bottomOffsetY = bottomOffsetY;
        this.activeTicks = activeTicks;
        this.foodCountMin = foodCountMin;
        this.foodCountMax = foodCountMax;
        this.foodRainDurationTicks = foodRainDurationTicks;
        this.heartDurationTicks = heartDurationTicks;
        this.heartIntervalTicks = heartIntervalTicks;
        this.heartCount = heartCount;
        this.bubblePopCount = bubblePopCount;
        this.primaryParticle = primaryParticle;
        this.secondaryParticle = secondaryParticle;
        this.primaryParticleYOffset = primaryParticleYOffset;
        this.secondaryParticleYOffset = secondaryParticleYOffset;
        this.coinName = coinName;
        this.coinItem = coinItem;
        this.coinColor = coinColor;
        this.coinMessage = coinMessage;
        this.coinMessageColor = coinMessageColor;
        this.wishMessage = wishMessage;
        this.wishMessageColor = wishMessageColor;
        this.guaranteedDropItem = guaranteedDropItem;
        this.guaranteedDropCount = guaranteedDropCount;
        this.dropPool = dropPool;
        this.dropTrailDurationTicks = dropTrailDurationTicks;
        this.dropTrailIntervalTicks = dropTrailIntervalTicks;
        this.dropTrailCount = dropTrailCount;
        this.dropTrailSpread = dropTrailSpread;
        this.activationSound = activationSound;
        this.activationSoundVolume = activationSoundVolume;
        this.activationSoundPitch = activationSoundPitch;
        this.fireworkBurstCount = fireworkBurstCount;
        this.fireworkRocketCount = fireworkRocketCount;
        this.fireworkFlight = fireworkFlight;
        this.fireworkColors = fireworkColors;
        this.fireworkFadeColors = fireworkFadeColors;
        this.fireworkFlicker = fireworkFlicker;
        this.fireworkTrail = fireworkTrail;
        this.fireworkLaunchYOffset = fireworkLaunchYOffset;
        this.structureOrigin = structureOrigin;
        this.ignoreAir = ignoreAir;
        this.replaceAirOnly = replaceAirOnly;
        this.replaceBlocksWithAir = replaceBlocksWithAir;
    }

    public static WishingWellEvent fromJson(JsonObject obj) {
        String structure = obj.has("structure") ? obj.get("structure").getAsString() : "luckybreak:wishing_well";
        int offsetX = obj.has("offset_x") ? obj.get("offset_x").getAsInt() : 0;
        int offsetY = obj.has("offset_y") ? obj.get("offset_y").getAsInt() : 0;
        int offsetZ = obj.has("offset_z") ? obj.get("offset_z").getAsInt() : 0;
        int spawnDistance = obj.has("spawn_distance") ? obj.get("spawn_distance").getAsInt() : 8;
        int detectRadius = obj.has("detect_radius") ? obj.get("detect_radius").getAsInt() : 2;
        int activeTicks = obj.has("active_ticks") ? obj.get("active_ticks").getAsInt() : 20 * 45;
        int foodCountMin = obj.has("drop_count_min")
            ? obj.get("drop_count_min").getAsInt()
            : (obj.has("food_count_min") ? obj.get("food_count_min").getAsInt() : 4);
        int foodCountMax = obj.has("drop_count_max")
            ? obj.get("drop_count_max").getAsInt()
            : (obj.has("food_count_max") ? obj.get("food_count_max").getAsInt() : 7);
        int foodRainDurationTicks = obj.has("drop_rain_duration_ticks")
            ? obj.get("drop_rain_duration_ticks").getAsInt()
            : (obj.has("food_rain_duration_ticks") ? obj.get("food_rain_duration_ticks").getAsInt() : 120);
        int heartDurationTicks = obj.has("particle_duration_ticks")
            ? obj.get("particle_duration_ticks").getAsInt()
            : (obj.has("heart_duration_ticks") ? obj.get("heart_duration_ticks").getAsInt() : 100);
        int heartIntervalTicks = obj.has("particle_interval_ticks")
            ? obj.get("particle_interval_ticks").getAsInt()
            : (obj.has("heart_interval_ticks") ? obj.get("heart_interval_ticks").getAsInt() : 4);
        int heartCount = obj.has("particle_count")
            ? obj.get("particle_count").getAsInt()
            : (obj.has("heart_count") ? obj.get("heart_count").getAsInt() : 4);
        int bubblePopCount = obj.has("secondary_particle_count")
            ? obj.get("secondary_particle_count").getAsInt()
            : (obj.has("bubble_pop_count") ? obj.get("bubble_pop_count").getAsInt() : 10);
        ParticleOptions primaryParticle = parseParticle(obj, "primary_particle", ParticleTypes.HEART);
        ParticleOptions secondaryParticle = parseParticle(obj, "secondary_particle", ParticleTypes.BUBBLE_POP);
        double primaryParticleYOffset = obj.has("particle_y_offset")
            ? obj.get("particle_y_offset").getAsDouble()
            : (obj.has("heart_y_offset") ? obj.get("heart_y_offset").getAsDouble() : 0.0);
        double secondaryParticleYOffset = obj.has("secondary_particle_y_offset")
            ? obj.get("secondary_particle_y_offset").getAsDouble()
            : (obj.has("bubble_y_offset") ? obj.get("bubble_y_offset").getAsDouble() : 0.0);
        String coinName = obj.has("coin_name") ? obj.get("coin_name").getAsString() : "Coin";
        Item coinItem = parseCoinItem(obj);
        ChatFormatting coinColor = parseCoinColor(obj);
        String coinMessage = obj.has("coin_message") ? obj.get("coin_message").getAsString() : "You got a Coin. Make a wish!";
        int coinMessageColor = parseRgbColor(obj, "coin_message_color", 0xFFFF55);
        String wishMessage = obj.has("wish_message") ? obj.get("wish_message").getAsString() : "Yummy wish!";
        int wishMessageColor = parseRgbColor(obj, "wish_message_color", 0x5555FF);
        Item guaranteedDropItem = parseOptionalItem(obj, "guaranteed_drop_item");
        int guaranteedDropCount = obj.has("guaranteed_drop_count") ? Math.max(0, obj.get("guaranteed_drop_count").getAsInt()) : 0;
        int dropTrailDurationTicks = obj.has("drop_trail_duration_ticks") ? obj.get("drop_trail_duration_ticks").getAsInt() : 24;
        int dropTrailIntervalTicks = obj.has("drop_trail_interval_ticks") ? obj.get("drop_trail_interval_ticks").getAsInt() : 2;
        int dropTrailCount = obj.has("drop_trail_count") ? obj.get("drop_trail_count").getAsInt() : 1;
        double dropTrailSpread = obj.has("drop_trail_spread") ? obj.get("drop_trail_spread").getAsDouble() : 0.03;
        SoundEvent activationSound = parseSoundEvent(obj, "activation_sound", SoundEvents.AMETHYST_BLOCK_CHIME);
        float activationSoundVolume = parseFloat(obj, "activation_sound_volume", 0.8f);
        float activationSoundPitch = parseFloat(obj, "activation_sound_pitch", 1.2f);
        int fireworkBurstCount = obj.has("firework_burst_count") ? obj.get("firework_burst_count").getAsInt() : 24;
        int fireworkRocketCount = obj.has("firework_rocket_count") ? obj.get("firework_rocket_count").getAsInt() : 2;
        int fireworkFlight = obj.has("firework_flight") ? obj.get("firework_flight").getAsInt() : 1;
        IntList fireworkColors = parseColorList(obj, "firework_colors", List.of(0xFFD700, 0xFFAA00));
        IntList fireworkFadeColors = parseColorList(obj, "firework_fade_colors", List.of(0xFFFFFF));
        boolean fireworkFlicker = !obj.has("firework_flicker") || obj.get("firework_flicker").getAsBoolean();
        boolean fireworkTrail = !obj.has("firework_trail") || obj.get("firework_trail").getAsBoolean();
        double fireworkLaunchYOffset = obj.has("firework_launch_y_offset") ? obj.get("firework_launch_y_offset").getAsDouble() : 16.4;
        int originX = obj.has("origin_x") ? obj.get("origin_x").getAsInt() : 0;
        int originY = obj.has("origin_y") ? obj.get("origin_y").getAsInt() : 0;
        int originZ = obj.has("origin_z") ? obj.get("origin_z").getAsInt() : 0;
        boolean ignoreAir = !obj.has("ignore_air") || obj.get("ignore_air").getAsBoolean();
        boolean replaceAirOnly = obj.has("replace_air_only") && obj.get("replace_air_only").getAsBoolean();
        Set<Block> replaceBlocksWithAir = parseReplaceBlocksWithAir(obj);

        if (spawnDistance < 1) spawnDistance = 1;
        if (detectRadius < 1) detectRadius = 1;
        if (activeTicks < 20) activeTicks = 20;
        if (foodCountMin < 1) foodCountMin = 1;
        if (foodCountMax < foodCountMin) foodCountMax = foodCountMin;
        if (foodRainDurationTicks < 20) foodRainDurationTicks = 20;
        if (heartDurationTicks < 4) heartDurationTicks = 4;
        if (heartIntervalTicks < 1) heartIntervalTicks = 1;
        if (heartCount < 1) heartCount = 1;
        if (bubblePopCount < 0) bubblePopCount = 0;
        if (dropTrailDurationTicks < 0) dropTrailDurationTicks = 0;
        if (dropTrailIntervalTicks < 1) dropTrailIntervalTicks = 1;
        if (dropTrailCount < 0) dropTrailCount = 0;
        if (dropTrailSpread < 0.0) dropTrailSpread = 0.0;
        if (fireworkBurstCount < 0) fireworkBurstCount = 0;
        if (fireworkRocketCount < 0) fireworkRocketCount = 0;
        if (fireworkFlight < 0) fireworkFlight = 0;
        if (fireworkFlight > 3) fireworkFlight = 3;
        if (fireworkLaunchYOffset < 0.0) fireworkLaunchYOffset = 0.0;

        List<WishingWellManager.WeightedDrop> dropPool = new ArrayList<>();
        String lootField = obj.has("drops") ? "drops" : "foods";
        if (obj.has(lootField)) {
            JsonArray foods = obj.getAsJsonArray(lootField);
            for (JsonElement element : foods) {
                WishingWellManager.WeightedDrop drop = parseWeightedDrop(element);
                if (drop != null) {
                    dropPool.add(drop);
                }
            }
        }

        if (dropPool.isEmpty()) {
            dropPool.add(new WishingWellManager.WeightedDrop(Items.BREAD, 1));
            dropPool.add(new WishingWellManager.WeightedDrop(Items.COOKED_BEEF, 1));
            dropPool.add(new WishingWellManager.WeightedDrop(Items.COOKED_CHICKEN, 1));
            dropPool.add(new WishingWellManager.WeightedDrop(Items.GOLDEN_CARROT, 1));
            dropPool.add(new WishingWellManager.WeightedDrop(Items.BAKED_POTATO, 1));
            dropPool.add(new WishingWellManager.WeightedDrop(Items.PUMPKIN_PIE, 1));
        }

        return new WishingWellEvent(
                Identifier.parse(structure),
            offsetX,
            offsetY,
            offsetZ,
                spawnDistance,
                detectRadius,
            DEFAULT_BOTTOM_OFFSET_Y,
                activeTicks,
                foodCountMin,
                foodCountMax,
                foodRainDurationTicks,
                heartDurationTicks,
                heartIntervalTicks,
                heartCount,
                bubblePopCount,
                primaryParticle,
                secondaryParticle,
                primaryParticleYOffset,
                secondaryParticleYOffset,
                coinName,
                coinItem,
                coinColor,
                coinMessage,
                coinMessageColor,
                wishMessage,
                wishMessageColor,
                guaranteedDropItem,
                guaranteedDropCount,
                dropPool,
                dropTrailDurationTicks,
                dropTrailIntervalTicks,
                dropTrailCount,
                dropTrailSpread,
                activationSound,
                activationSoundVolume,
                activationSoundPitch,
                fireworkBurstCount,
                fireworkRocketCount,
                fireworkFlight,
                new IntArrayList(fireworkColors),
                new IntArrayList(fireworkFadeColors),
                fireworkFlicker,
                fireworkTrail,
                fireworkLaunchYOffset,
                new BlockPos(originX, originY, originZ),
                ignoreAir,
                replaceAirOnly,
                replaceBlocksWithAir
        );
    }

    private static Set<Block> parseReplaceBlocksWithAir(JsonObject obj) {
        Set<Block> blocks = new HashSet<>();

        if (obj.has("replace_block_with_air")) {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(obj.get("replace_block_with_air").getAsString()));
            if (block != null && block != Blocks.AIR) {
                blocks.add(block);
            }
        }

        if (obj.has("replace_blocks_with_air")) {
            JsonArray arr = obj.getAsJsonArray("replace_blocks_with_air");
            for (JsonElement element : arr) {
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(element.getAsString()));
                if (block != null && block != Blocks.AIR) {
                    blocks.add(block);
                }
            }
        }

        return blocks;
    }

    private static Item parseCoinItem(JsonObject obj) {
        if (!obj.has("coin_item")) {
            return Items.GOLD_NUGGET;
        }

        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(obj.get("coin_item").getAsString()));
            if (item != null && item != Items.AIR) {
                return item;
            }
        } catch (Exception ignored) {
        }

        return Items.GOLD_NUGGET;
    }

    private static Item parseOptionalItem(JsonObject obj, String field) {
        if (!obj.has(field)) {
            return null;
        }
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(obj.get(field).getAsString()));
            if (item != null && item != Items.AIR) {
                return item;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static WishingWellManager.WeightedDrop parseWeightedDrop(JsonElement element) {
        try {
            if (element.isJsonPrimitive()) {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(element.getAsString()));
                if (item != null && item != Items.AIR) {
                    return new WishingWellManager.WeightedDrop(item, 1);
                }
                return null;
            }

            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("item")) {
                    return null;
                }
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(obj.get("item").getAsString()));
                if (item == null || item == Items.AIR) {
                    return null;
                }
                int weight = obj.has("weight") ? Math.max(1, obj.get("weight").getAsInt()) : 1;
                int countMin = obj.has("count_min")
                        ? Math.max(1, obj.get("count_min").getAsInt())
                        : (obj.has("min_count") ? Math.max(1, obj.get("min_count").getAsInt()) : 1);
                int countMax = obj.has("count_max")
                        ? Math.max(countMin, obj.get("count_max").getAsInt())
                        : (obj.has("max_count") ? Math.max(countMin, obj.get("max_count").getAsInt()) : countMin);
                return new WishingWellManager.WeightedDrop(item, weight, countMin, countMax);
            }
        } catch (Exception ignored) {
        }
        return null;
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

    private static ChatFormatting parseCoinColor(JsonObject obj) {
        if (!obj.has("coin_color")) {
            return ChatFormatting.GOLD;
        }

        try {
            return ChatFormatting.valueOf(obj.get("coin_color").getAsString().trim().toUpperCase());
        } catch (Exception ignored) {
            return ChatFormatting.GOLD;
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

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        SimpleJsonStructure structure = SimpleJsonStructure.load(level, structureId);
        if (structure == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] JSON structure not found for wishing well event: {}", structureId);
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

        int playerY = player.blockPosition().getY();
        int placeY = playerY + offsetY;
        int minSafeY = level.getMinY() + 1;
        if (placeY < minSafeY) {
            placeY = minSafeY;
        }

        BlockPos wellCenter = new BlockPos(anchorX, placeY, anchorZ);
        boolean placed = structure.place(level, wellCenter, structureOrigin, ignoreAir, replaceAirOnly, replaceBlocksWithAir);
        if (!placed) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Wishing well JSON structure placement returned false at {}", wellCenter);
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

        WishingWellManager.registerWell(
                level,
                wellCenter,
                detectRadius,
                bottomOffsetY,
                activeTicks,
                foodCountMin,
                foodCountMax,
                foodRainDurationTicks,
                heartDurationTicks,
                heartIntervalTicks,
                heartCount,
                bubblePopCount,
                primaryParticle,
                secondaryParticle,
                primaryParticleYOffset,
                secondaryParticleYOffset,
                coinName,
                coinItem,
                wishMessage,
                wishMessageColor,
                guaranteedDropItem,
                guaranteedDropCount,
                dropTrailDurationTicks,
                dropTrailIntervalTicks,
                dropTrailCount,
                dropTrailSpread,
                activationSound,
                activationSoundVolume,
                activationSoundPitch,
                fireworkBurstCount,
                fireworkRocketCount,
                fireworkFlight,
                fireworkColors,
                fireworkFadeColors,
                fireworkFlicker,
                fireworkTrail,
                fireworkLaunchYOffset,
                dropPool
        );
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