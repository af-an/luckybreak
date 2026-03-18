package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

/**
 * Places a puffer tank structure and teleports the player into tank water safely.
 */
@SuppressWarnings("deprecation")
public class PufferTankTrapEvent implements LuckyEvent {

    private final Identifier structureId;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final BlockPos structureOrigin;
    private final BlockPos preferredSpawnLocal;
    private final int fallbackSearchRadius;
    private final int pufferfishCount;
    private final String message;
    private final int messageColor;

    private PufferTankTrapEvent(
            Identifier structureId,
            int offsetX,
            int offsetY,
            int offsetZ,
            BlockPos structureOrigin,
            BlockPos preferredSpawnLocal,
            int fallbackSearchRadius,
                int pufferfishCount,
            String message,
            int messageColor
    ) {
        this.structureId = structureId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.structureOrigin = structureOrigin;
        this.preferredSpawnLocal = preferredSpawnLocal;
        this.fallbackSearchRadius = fallbackSearchRadius;
        this.pufferfishCount = pufferfishCount;
        this.message = message;
        this.messageColor = messageColor;
    }

    public static PufferTankTrapEvent fromJson(JsonObject obj) {
        String structure = obj.has("structure") ? obj.get("structure").getAsString() : "luckybreak:puffer_tank";
        int offsetX = obj.has("offset_x") ? obj.get("offset_x").getAsInt() : 0;
        int offsetY = obj.has("offset_y") ? obj.get("offset_y").getAsInt() : 0;
        int offsetZ = obj.has("offset_z") ? obj.get("offset_z").getAsInt() : 0;

        int originX = obj.has("origin_x") ? obj.get("origin_x").getAsInt() : 1;
        int originY = obj.has("origin_y") ? obj.get("origin_y").getAsInt() : 0;
        int originZ = obj.has("origin_z") ? obj.get("origin_z").getAsInt() : 1;

        int spawnX = obj.has("spawn_x") ? obj.get("spawn_x").getAsInt() : 1;
        int spawnY = obj.has("spawn_y") ? obj.get("spawn_y").getAsInt() : 2;
        int spawnZ = obj.has("spawn_z") ? obj.get("spawn_z").getAsInt() : 1;

        int fallbackSearchRadius = obj.has("fallback_search_radius") ? obj.get("fallback_search_radius").getAsInt() : 2;
        if (fallbackSearchRadius < 0) {
            fallbackSearchRadius = 0;
        }
        int pufferfishCount = obj.has("pufferfish_count") ? obj.get("pufferfish_count").getAsInt() : 1;
        if (pufferfishCount < 0) {
            pufferfishCount = 0;
        }

        String message = obj.has("message") ? obj.get("message").getAsString() : "Glub glub...";
        int messageColor = parseRgbColor(obj, "message_color", 0x55CCFF);

        return new PufferTankTrapEvent(
                Identifier.parse(structure),
                offsetX,
                offsetY,
                offsetZ,
                new BlockPos(originX, originY, originZ),
                new BlockPos(spawnX, spawnY, spawnZ),
                fallbackSearchRadius,
                pufferfishCount,
                message,
                messageColor
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        SimpleJsonStructure structure = SimpleJsonStructure.load(level, structureId);
        if (structure == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Puffer tank structure not found: {}", structureId);
            return;
        }

        BlockPos anchor = pos.offset(offsetX, offsetY, offsetZ);
        boolean placed = structure.place(level, anchor, structureOrigin, true, false, new HashSet<>());
        if (!placed) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Puffer tank placement failed at {}", anchor);
            return;
        }

        BlockPos safeSpawn = findSafeWaterSpawn(level, player, anchor);
        if (safeSpawn != null) {
            player.teleportTo(safeSpawn.getX() + 0.5, safeSpawn.getY(), safeSpawn.getZ() + 0.5);
            player.setDeltaMovement(0.0, Math.min(player.getDeltaMovement().y, 0.0), 0.0);
            spawnPufferfish(level, safeSpawn);
        } else {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Puffer tank could not find a safe water spawn inside structure at {}", anchor);
        }

        if (!message.isEmpty()) {
            player.displayClientMessage(
                    Component.literal(message).withStyle(style -> style.withColor(TextColor.fromRgb(messageColor))),
                    false
            );
        }
    }

    private BlockPos findSafeWaterSpawn(ServerLevel level, ServerPlayer player, BlockPos anchor) {
        BlockPos preferredWorld = anchor.offset(preferredSpawnLocal).subtract(structureOrigin);
        if (isSafeWaterSpawn(level, player, preferredWorld)) {
            return preferredWorld;
        }

        for (int radius = 1; radius <= fallbackSearchRadius; radius++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                            continue;
                        }
                        BlockPos candidate = preferredWorld.offset(dx, dy, dz);
                        if (isSafeWaterSpawn(level, player, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isSafeWaterSpawn(ServerLevel level, ServerPlayer player, BlockPos feetPos) {
        BlockPos headPos = feetPos.above();
        BlockState feet = level.getBlockState(feetPos);
        BlockState head = level.getBlockState(headPos);

        boolean feetWater = feet.getFluidState().is(FluidTags.WATER);
        boolean headWaterOrAir = head.getFluidState().is(FluidTags.WATER) || head.isAir();
        if (!feetWater || !headWaterOrAir) {
            return false;
        }
        if (feet.blocksMotion() || head.blocksMotion()) {
            return false;
        }

        Vec3 originalPos = player.position();
        player.setPos(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5);
        boolean canFit = level.noCollision(player);
        player.setPos(originalPos.x, originalPos.y, originalPos.z);
        return canFit;
    }

    private void spawnPufferfish(ServerLevel level, BlockPos waterPos) {
        if (pufferfishCount <= 0) {
            return;
        }

        for (int i = 0; i < pufferfishCount; i++) {
            Entity fish = EntityType.PUFFERFISH.create(level, EntitySpawnReason.EVENT);
            if (fish == null) {
                continue;
            }

            double x = waterPos.getX() + 0.35 + level.getRandom().nextDouble() * 0.3;
            double y = waterPos.getY() + 0.1 + level.getRandom().nextDouble() * 0.5;
            double z = waterPos.getZ() + 0.35 + level.getRandom().nextDouble() * 0.3;
            fish.setPos(x, y, z);

            if (fish instanceof LivingEntity living) {
                living.setHealth(living.getMaxHealth() * 0.5f);
            }

            if (level.noCollision(fish) && level.getFluidState(fish.blockPosition()).is(FluidTags.WATER)) {
                level.addFreshEntity(fish);
            }
        }
    }

    private static int parseRgbColor(JsonObject obj, String field, int fallback) {
        if (!obj.has(field)) {
            return fallback;
        }

        try {
            var element = obj.get(field);
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

        return fallback;
    }
}
