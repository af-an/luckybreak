package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

/**
 * Places a simple JSON structure centered on the player and keeps the player inside it.
 *
 * JSON fields:
 *   structure: "luckybreak:slime_house"  (simple_structures/<path>.json)
 *   offset_x / offset_y / offset_z: placement offset from player block pos
 *   origin_x / origin_y / origin_z: pivot within structure JSON coordinates
 */
public class BounceHouseEvent implements LuckyEvent {

    private final Identifier structureId;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final BlockPos structureOrigin;
    private final boolean clearAreaBeforePlace;
    private final String message;
    private final int messageColor;

    private BounceHouseEvent(
            Identifier structureId,
            int offsetX,
            int offsetY,
            int offsetZ,
            BlockPos structureOrigin,
            boolean clearAreaBeforePlace,
            String message,
            int messageColor
    ) {
        this.structureId = structureId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.structureOrigin = structureOrigin;
        this.clearAreaBeforePlace = clearAreaBeforePlace;
        this.message = message;
        this.messageColor = messageColor;
    }

    public static BounceHouseEvent fromJson(JsonObject obj) {
        String structure = obj.has("structure") ? obj.get("structure").getAsString() : "luckybreak:slime_house";
        int offsetX = obj.has("offset_x") ? obj.get("offset_x").getAsInt() : 0;
        int offsetY = obj.has("offset_y") ? obj.get("offset_y").getAsInt() : -1;
        int offsetZ = obj.has("offset_z") ? obj.get("offset_z").getAsInt() : 0;
        int originX = obj.has("origin_x") ? obj.get("origin_x").getAsInt() : 2;
        int originY = obj.has("origin_y") ? obj.get("origin_y").getAsInt() : 0;
        int originZ = obj.has("origin_z") ? obj.get("origin_z").getAsInt() : 2;
        boolean clearAreaBeforePlace = !obj.has("clear_area_before_place") || obj.get("clear_area_before_place").getAsBoolean();
        String message = obj.has("message") ? obj.get("message").getAsString() : "How high can you jump?";
        int messageColor = parseColor(obj, "message_color", 0x55FF55);

        return new BounceHouseEvent(
                Identifier.parse(structure),
                offsetX,
                offsetY,
                offsetZ,
                new BlockPos(originX, originY, originZ),
            clearAreaBeforePlace,
                message,
                messageColor
        );
    }

    private static int parseColor(JsonObject obj, String field, int fallback) {
        if (!obj.has(field)) {
            return fallback;
        }

        try {
            var element = obj.get(field);
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
            LuckyBreak.LOGGER.warn("[LuckyBreak] Bounce house structure not found: {}", structureId);
            return;
        }

        BlockPos anchor = player.blockPosition().offset(offsetX, offsetY, offsetZ);
        if (clearAreaBeforePlace) {
            clearPlacementBox(level, structure, anchor);
        }
        boolean placed = structure.place(level, anchor, structureOrigin, true, false, new HashSet<>());
        if (!placed) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Bounce house placement failed at {}", anchor);
            return;
        }

        player.teleportTo(
                anchor.getX() + 0.5,
                anchor.getY() + 1.0,
                anchor.getZ() + 0.5
        );

        if (!message.isEmpty()) {
            player.displayClientMessage(
                Component.literal(message).withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(messageColor))),
                false
            );
        }
    }

    private void clearPlacementBox(ServerLevel level, SimpleJsonStructure structure, BlockPos anchor) {
        BlockPos size = structure.size();
        int minX = anchor.getX() - structureOrigin.getX();
        int minY = anchor.getY() - structureOrigin.getY();
        int minZ = anchor.getZ() - structureOrigin.getZ();
        int maxX = minX + size.getX() - 1;
        int maxY = minY + size.getY() - 1;
        int maxZ = minZ + size.getZ() - 1;

        int worldMinY = level.getMinY();
        int worldMaxY = level.getMaxY();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (y <= worldMinY || y >= worldMaxY) {
                    continue;
                }
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos target = new BlockPos(x, y, z);
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
}
