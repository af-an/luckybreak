package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.ModItems;
import com.luckybreak.events.LuckyEvent;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class GoldenArmorStandEvent implements LuckyEvent {

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final boolean clearSpaceIfNeeded;
    private final int clearRadius;
    private final int clearHeight;
    private final boolean placeSupportBlockIfNeeded;
    private final Block supportBlock;
    private final boolean invulnerable;
    private final boolean showArms;
    private final Item helmetItem;
    private final Item chestItem;
    private final Item legsItem;
    private final Item bootsItem;
    private final List<Item> mainHandItems;
    private final String message;
    private final int messageColor;
    private final ParticleOptions particleType;
    private final int particleCount;
    private final double particleYOffset;
    private final double particleSpreadX;
    private final double particleSpreadY;
    private final double particleSpreadZ;
    private final double particleSpeed;
    private final int fireworkBurstCount;
    private final int fireworkRocketCount;
    private final int fireworkFlight;
    private final IntList fireworkColors;
    private final IntList fireworkFadeColors;
    private final boolean fireworkFlicker;
    private final boolean fireworkTrail;

    private GoldenArmorStandEvent(
            int offsetX,
            int offsetY,
            int offsetZ,
            boolean clearSpaceIfNeeded,
            int clearRadius,
            int clearHeight,
            boolean placeSupportBlockIfNeeded,
            Block supportBlock,
            boolean invulnerable,
            boolean showArms,
            Item helmetItem,
            Item chestItem,
            Item legsItem,
            Item bootsItem,
            List<Item> mainHandItems,
            String message,
            int messageColor,
            ParticleOptions particleType,
            int particleCount,
            double particleYOffset,
            double particleSpreadX,
            double particleSpreadY,
            double particleSpreadZ,
            double particleSpeed,
            int fireworkBurstCount,
            int fireworkRocketCount,
            int fireworkFlight,
            IntList fireworkColors,
            IntList fireworkFadeColors,
            boolean fireworkFlicker,
            boolean fireworkTrail
    ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.clearSpaceIfNeeded = clearSpaceIfNeeded;
        this.clearRadius = clearRadius;
        this.clearHeight = clearHeight;
        this.placeSupportBlockIfNeeded = placeSupportBlockIfNeeded;
        this.supportBlock = supportBlock;
        this.invulnerable = invulnerable;
        this.showArms = showArms;
        this.helmetItem = helmetItem;
        this.chestItem = chestItem;
        this.legsItem = legsItem;
        this.bootsItem = bootsItem;
        this.mainHandItems = mainHandItems;
        this.message = message;
        this.messageColor = messageColor;
        this.particleType = particleType;
        this.particleCount = particleCount;
        this.particleYOffset = particleYOffset;
        this.particleSpreadX = particleSpreadX;
        this.particleSpreadY = particleSpreadY;
        this.particleSpreadZ = particleSpreadZ;
        this.particleSpeed = particleSpeed;
        this.fireworkBurstCount = fireworkBurstCount;
        this.fireworkRocketCount = fireworkRocketCount;
        this.fireworkFlight = fireworkFlight;
        this.fireworkColors = fireworkColors;
        this.fireworkFadeColors = fireworkFadeColors;
        this.fireworkFlicker = fireworkFlicker;
        this.fireworkTrail = fireworkTrail;
    }

    public static GoldenArmorStandEvent fromJson(JsonObject obj) {
        int offsetX = obj.has("offset_x") ? obj.get("offset_x").getAsInt() : 0;
        int offsetY = obj.has("offset_y") ? obj.get("offset_y").getAsInt() : 0;
        int offsetZ = obj.has("offset_z") ? obj.get("offset_z").getAsInt() : 0;

        boolean clearSpaceIfNeeded = !obj.has("clear_space_if_needed") || obj.get("clear_space_if_needed").getAsBoolean();
        int clearRadius = obj.has("clear_radius") ? obj.get("clear_radius").getAsInt() : 1;
        int clearHeight = obj.has("clear_height") ? obj.get("clear_height").getAsInt() : 3;

        boolean placeSupportBlockIfNeeded = !obj.has("place_support_block_if_needed") || obj.get("place_support_block_if_needed").getAsBoolean();
        Block supportBlock = parseBlock(obj, "support_block", Blocks.GOLD_BLOCK);

        boolean invulnerable = obj.has("invulnerable") && obj.get("invulnerable").getAsBoolean();
        boolean showArms = !obj.has("show_arms") || obj.get("show_arms").getAsBoolean();

        Item helmetItem = parseItem(obj, "helmet_item", Items.GOLDEN_HELMET);
        Item chestItem = parseItem(obj, "chest_item", Items.GOLDEN_CHESTPLATE);
        Item legsItem = parseItem(obj, "legs_item", Items.GOLDEN_LEGGINGS);
        Item bootsItem = parseItem(obj, "boots_item", Items.GOLDEN_BOOTS);
        List<Item> mainHandItems = parseMainHandItems(obj);

        String message = obj.has("message") ? obj.get("message").getAsString() : "Golden guardian arrived!";
        int messageColor = parseRgbColor(obj, "message_color", 0xFFD700);

        ParticleOptions particleType = parseParticle(obj, "particle_type", ParticleTypes.WAX_ON);
        int particleCount = obj.has("particle_count") ? obj.get("particle_count").getAsInt() : 30;
        double particleYOffset = obj.has("particle_y_offset") ? obj.get("particle_y_offset").getAsDouble() : 0.8;
        double particleSpreadX = obj.has("particle_spread_x") ? obj.get("particle_spread_x").getAsDouble() : 0.45;
        double particleSpreadY = obj.has("particle_spread_y") ? obj.get("particle_spread_y").getAsDouble() : 0.45;
        double particleSpreadZ = obj.has("particle_spread_z") ? obj.get("particle_spread_z").getAsDouble() : 0.45;
        double particleSpeed = obj.has("particle_speed") ? obj.get("particle_speed").getAsDouble() : 0.02;

        int fireworkBurstCount = obj.has("firework_burst_count") ? obj.get("firework_burst_count").getAsInt() : 24;
        int fireworkRocketCount = obj.has("firework_rocket_count") ? obj.get("firework_rocket_count").getAsInt() : 2;
        int fireworkFlight = obj.has("firework_flight") ? obj.get("firework_flight").getAsInt() : 1;
        IntList fireworkColors = parseColorList(obj, "firework_colors", List.of(0xFFD700, 0xFFAA00));
        IntList fireworkFadeColors = parseColorList(obj, "firework_fade_colors", List.of(0xFFF0A0));
        boolean fireworkFlicker = !obj.has("firework_flicker") || obj.get("firework_flicker").getAsBoolean();
        boolean fireworkTrail = !obj.has("firework_trail") || obj.get("firework_trail").getAsBoolean();

        clearRadius = Mth.clamp(clearRadius, 0, 5);
        clearHeight = Mth.clamp(clearHeight, 2, 8);
        particleCount = Mth.clamp(particleCount, 0, 200);
        particleSpreadX = Math.max(0.0, particleSpreadX);
        particleSpreadY = Math.max(0.0, particleSpreadY);
        particleSpreadZ = Math.max(0.0, particleSpreadZ);
        particleSpeed = Math.max(0.0, particleSpeed);
        fireworkBurstCount = Mth.clamp(fireworkBurstCount, 0, 200);
        fireworkRocketCount = Mth.clamp(fireworkRocketCount, 0, 16);
        fireworkFlight = Mth.clamp(fireworkFlight, 0, 3);

        return new GoldenArmorStandEvent(
                offsetX,
                offsetY,
                offsetZ,
                clearSpaceIfNeeded,
                clearRadius,
                clearHeight,
                placeSupportBlockIfNeeded,
                supportBlock,
                invulnerable,
                showArms,
                helmetItem,
                chestItem,
                legsItem,
                bootsItem,
                List.copyOf(mainHandItems),
                message,
                messageColor,
                particleType,
                particleCount,
                particleYOffset,
                particleSpreadX,
                particleSpreadY,
                particleSpreadZ,
                particleSpeed,
                fireworkBurstCount,
                fireworkRocketCount,
                fireworkFlight,
                new IntArrayList(fireworkColors),
                new IntArrayList(fireworkFadeColors),
                fireworkFlicker,
                fireworkTrail
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockPos spawnPos = pos.offset(offsetX, offsetY, offsetZ);

        if (clearSpaceIfNeeded && !canSpawnAt(level, spawnPos)) {
            clearSpace(level, spawnPos);
        }

        if (placeSupportBlockIfNeeded && !hasSolidFloor(level, spawnPos.below())) {
            level.setBlock(spawnPos.below(), supportBlock.defaultBlockState(), 3);
        }

        if (!canSpawnAt(level, spawnPos)) {
            return;
        }

        ArmorStand armorStand = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(level, EntitySpawnReason.EVENT);
        if (armorStand == null) {
            return;
        }

        double standX = spawnPos.getX() + 0.5;
        double standZ = spawnPos.getZ() + 0.5;
        double dx = player.getX() - standX;
        double dz = player.getZ() - standZ;
        float facingYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;

        armorStand.setPos(standX, spawnPos.getY(), standZ);
        armorStand.setYRot(facingYaw);
        armorStand.setYHeadRot(facingYaw);
        armorStand.setShowArms(showArms);
        armorStand.setInvulnerable(invulnerable);
        armorStand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmetItem));
        armorStand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chestItem));
        armorStand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legsItem));
        armorStand.setItemSlot(EquipmentSlot.FEET, new ItemStack(bootsItem));
        Item selectedMainHand = mainHandItems.get(level.getRandom().nextInt(mainHandItems.size()));
        armorStand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(selectedMainHand));

        level.addFreshEntity(armorStand);

        double fx = spawnPos.getX() + 0.5;
        double fy = spawnPos.getY() + particleYOffset;
        double fz = spawnPos.getZ() + 0.5;

        if (particleCount > 0) {
            level.sendParticles(particleType, fx, fy, fz, particleCount, particleSpreadX, particleSpreadY, particleSpreadZ, particleSpeed);
        }

        if (fireworkBurstCount > 0) {
            level.sendParticles(ParticleTypes.FIREWORK, fx, fy + 0.25, fz, fireworkBurstCount, 0.35, 0.35, 0.35, 0.06);
        }

        spawnColoredFireworks(level, spawnPos.above());

        if (!message.isBlank()) {
            player.displayClientMessage(
                    Component.literal(message).withStyle(style -> style.withColor(TextColor.fromRgb(messageColor))),
                    false
            );
        }
    }

    private boolean canSpawnAt(ServerLevel level, BlockPos spawnPos) {
        if (!level.getBlockState(spawnPos).isAir() || !level.getBlockState(spawnPos.above()).isAir()) {
            return false;
        }
        return hasSolidFloor(level, spawnPos.below());
    }

    private boolean hasSolidFloor(ServerLevel level, BlockPos floorPos) {
        BlockState floor = level.getBlockState(floorPos);
        return floor.isFaceSturdy(level, floorPos, Direction.UP);
    }

    private void clearSpace(ServerLevel level, BlockPos center) {
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        for (int dx = -clearRadius; dx <= clearRadius; dx++) {
            for (int dz = -clearRadius; dz <= clearRadius; dz++) {
                for (int dy = 0; dy < clearHeight; dy++) {
                    BlockPos target = center.offset(dx, dy, dz);
                    int y = target.getY();
                    if (y <= minY || y >= maxY) {
                        continue;
                    }
                    BlockState state = level.getBlockState(target);
                    if (state.isAir()) {
                        continue;
                    }
                    if (state.getDestroySpeed(level, target) < 0.0f) {
                        continue;
                    }
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
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

    private static Item parseItem(JsonObject obj, String field, Item fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(obj.get(field).getAsString()));
            if (item != null && item != Items.AIR) {
                return item;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static List<Item> parseMainHandItems(JsonObject obj) {
        List<Item> defaults = List.of(
                ModItems.LUCKY_SWORD,
                ModItems.LUCKY_PICKAXE,
                ModItems.LUCKY_SHOVEL,
                ModItems.LUCKY_HOE,
                ModItems.LUCKY_AXE,
                ModItems.LUCKY_BOW
        );

        List<Item> parsed = new ArrayList<>();
        if (obj.has("main_hand_items") && obj.get("main_hand_items").isJsonArray()) {
            JsonArray array = obj.getAsJsonArray("main_hand_items");
            for (JsonElement element : array) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                try {
                    Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(element.getAsString()));
                    if (item != null && item != Items.AIR) {
                        parsed.add(item);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (!parsed.isEmpty()) {
            return parsed;
        }

        if (obj.has("main_hand_item")) {
            return List.of(parseItem(obj, "main_hand_item", ModItems.LUCKY_SWORD));
        }

        return defaults;
    }

    private static Block parseBlock(JsonObject obj, String field, Block fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        try {
            Block block = BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse(obj.get(field).getAsString()));
            if (block != null && block != Blocks.AIR) {
                return block;
            }
        } catch (Exception ignored) {
        }
        return fallback;
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

    private static int parseRgbColor(JsonObject obj, String field, int fallback) {
        if (!obj.has(field)) {
            return fallback;
        }
        Integer color = parseColor(obj.get(field));
        if (color != null) {
            return color;
        }

        try {
            String named = obj.get(field).getAsString().trim();
            ChatFormatting formatting = ChatFormatting.valueOf(named.toUpperCase());
            Integer formattingColor = formatting.getColor();
            if (formattingColor != null) {
                return formattingColor & 0xFFFFFF;
            }
        } catch (Exception ignored) {
        }

        return fallback;
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