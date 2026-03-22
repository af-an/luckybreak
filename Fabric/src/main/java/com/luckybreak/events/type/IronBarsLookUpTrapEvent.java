package com.luckybreak.events.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import com.luckybreak.events.LuckyScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Traps the player inside iron bars, warns them, then drops an anvil or starts lava flow from above.
 *
 * JSON fields:
 *   bar_block: "minecraft:iron_bars"
 *   cage_radius: 1
 *   cage_height: 3
 *   center_player: true
 *   message: "uh-oh, look up"
 *   message_color: "#FF0000"
 *   hazard_mode: "random"         ("random", "anvil", "lava")
 *   anvil_chance: 0.5              (used when hazard_mode = "random")
 *   hazard_delay_ticks: 25
 *   anvil_hazard_height: 16
 *   lava_hazard_height: 7
 *   hazard_height: 12              (legacy fallback if specific heights are not set)
 *   clear_cage_space: true
 *   clear_drop_column: true
 *   anvil_block: "minecraft:damaged_anvil"
 *   lava_block: "minecraft:lava"
 */
public class IronBarsLookUpTrapEvent implements LuckyEvent {

    private final ResourceLocation barBlockId;
    private final int cageRadius;
    private final int cageHeight;
    private final boolean centerPlayer;
    private final String message;
    private final int messageColor;
    private final String hazardMode;
    private final double anvilChance;
    private final int hazardDelayTicks;
    private final int anvilHazardHeight;
    private final int lavaHazardHeight;
    private final boolean clearCageSpace;
    private final boolean clearDropColumn;
    private final ResourceLocation anvilBlockId;
    private final ResourceLocation lavaBlockId;

    private IronBarsLookUpTrapEvent(
            ResourceLocation barBlockId,
            int cageRadius,
            int cageHeight,
            boolean centerPlayer,
            String message,
            int messageColor,
            String hazardMode,
            double anvilChance,
            int hazardDelayTicks,
            int anvilHazardHeight,
            int lavaHazardHeight,
            boolean clearCageSpace,
            boolean clearDropColumn,
            ResourceLocation anvilBlockId,
            ResourceLocation lavaBlockId
    ) {
        this.barBlockId = barBlockId;
        this.cageRadius = cageRadius;
        this.cageHeight = cageHeight;
        this.centerPlayer = centerPlayer;
        this.message = message;
        this.messageColor = messageColor;
        this.hazardMode = hazardMode;
        this.anvilChance = anvilChance;
        this.hazardDelayTicks = hazardDelayTicks;
        this.anvilHazardHeight = anvilHazardHeight;
        this.lavaHazardHeight = lavaHazardHeight;
        this.clearCageSpace = clearCageSpace;
        this.clearDropColumn = clearDropColumn;
        this.anvilBlockId = anvilBlockId;
        this.lavaBlockId = lavaBlockId;
    }

    public static IronBarsLookUpTrapEvent fromJson(JsonObject obj) {
        ResourceLocation barBlockId = parseIdentifier(obj, "bar_block", "minecraft:iron_bars");
        int cageRadius = obj.has("cage_radius") ? obj.get("cage_radius").getAsInt() : 1;
        int cageHeight = obj.has("cage_height") ? obj.get("cage_height").getAsInt() : 3;
        boolean centerPlayer = !obj.has("center_player") || obj.get("center_player").getAsBoolean();

        String message = obj.has("message") ? obj.get("message").getAsString() : "uh-oh, look up";
        int messageColor = parseRgbColor(obj, "message_color", 0xFF0000);

        String hazardMode = obj.has("hazard_mode") ? obj.get("hazard_mode").getAsString().trim().toLowerCase() : "random";
        double anvilChance = obj.has("anvil_chance") ? obj.get("anvil_chance").getAsDouble() : 0.5;
        int hazardDelayTicks = obj.has("hazard_delay_ticks") ? obj.get("hazard_delay_ticks").getAsInt() : 25;
        int legacyHazardHeight = obj.has("hazard_height") ? obj.get("hazard_height").getAsInt() : 12;
        int anvilHazardHeight = obj.has("anvil_hazard_height") ? obj.get("anvil_hazard_height").getAsInt() : legacyHazardHeight;
        int lavaHazardHeight = obj.has("lava_hazard_height") ? obj.get("lava_hazard_height").getAsInt() : Math.max(1, legacyHazardHeight - 5);
        boolean clearCageSpace = !obj.has("clear_cage_space") || obj.get("clear_cage_space").getAsBoolean();
        boolean clearDropColumn = !obj.has("clear_drop_column") || obj.get("clear_drop_column").getAsBoolean();

        ResourceLocation anvilBlockId = parseIdentifier(obj, "anvil_block", "minecraft:damaged_anvil");
        ResourceLocation lavaBlockId = parseIdentifier(obj, "lava_block", "minecraft:lava");

        if (cageRadius < 1) cageRadius = 1;
        if (cageHeight < 2) cageHeight = 2;
        if (anvilChance < 0.0) anvilChance = 0.0;
        if (anvilChance > 1.0) anvilChance = 1.0;
        if (hazardDelayTicks < 1) hazardDelayTicks = 1;
        if (anvilHazardHeight < 4) anvilHazardHeight = 4;
        if (lavaHazardHeight < 1) lavaHazardHeight = 1;

        if (!hazardMode.equals("random") && !hazardMode.equals("anvil") && !hazardMode.equals("lava")) {
            hazardMode = "random";
        }

        return new IronBarsLookUpTrapEvent(
                barBlockId,
                cageRadius,
                cageHeight,
                centerPlayer,
                message,
                messageColor,
                hazardMode,
                anvilChance,
                hazardDelayTicks,
                anvilHazardHeight,
                lavaHazardHeight,
                clearCageSpace,
                clearDropColumn,
                anvilBlockId,
                lavaBlockId
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockPos trapCenter = player.blockPosition();

        if (centerPlayer) {
            player.teleportTo(trapCenter.getX() + 0.5, player.getY(), trapCenter.getZ() + 0.5);
            player.setDeltaMovement(0.0, Math.min(player.getDeltaMovement().y, 0.0), 0.0);
        }

        Block barBlock = resolveBlock(barBlockId, Blocks.IRON_BARS);
        BlockState barState = barBlock.defaultBlockState();

        if (clearCageSpace) {
            for (int dy = 1; dy < cageHeight; dy++) {
                for (int dx = -cageRadius; dx <= cageRadius; dx++) {
                    for (int dz = -cageRadius; dz <= cageRadius; dz++) {
                        BlockPos clearPos = trapCenter.offset(dx, dy, dz);
                        level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        for (int dy = 0; dy < cageHeight; dy++) {
            for (int dx = -cageRadius; dx <= cageRadius; dx++) {
                for (int dz = -cageRadius; dz <= cageRadius; dz++) {
                    if (Math.abs(dx) != cageRadius && Math.abs(dz) != cageRadius) {
                        continue;
                    }
                    BlockPos barPos = trapCenter.offset(dx, dy, dz);
                    level.setBlock(barPos, barState, 3);
                }
            }
        }

        if (!message.isEmpty()) {
            player.displayClientMessage(
                    Component.literal(message).withStyle(style -> style.withColor(TextColor.fromRgb(messageColor))),
                    false
            );
        }

        LuckyScheduler.INSTANCE.schedule(hazardDelayTicks, () -> {
            if (player.isRemoved() || player.level() != level) {
                return;
            }

            boolean useAnvil = hazardMode.equals("anvil")
                    || (hazardMode.equals("random") && level.getRandom().nextDouble() < anvilChance);

            int chosenHeight = useAnvil ? anvilHazardHeight : lavaHazardHeight;
            int sourceY = trapCenter.getY() + chosenHeight;
            int maxY = level.getMaxY() - 1;
            int minY = level.getMinY() + 1;
            sourceY = Math.max(minY, Math.min(maxY, sourceY));
            BlockPos sourcePos = new BlockPos(trapCenter.getX(), sourceY, trapCenter.getZ());

            if (clearDropColumn) {
                for (int y = trapCenter.getY() + 1; y < sourceY; y++) {
                    level.setBlock(new BlockPos(trapCenter.getX(), y, trapCenter.getZ()), Blocks.AIR.defaultBlockState(), 3);
                }
            }

            if (useAnvil) {
                Block anvilBlock = resolveBlock(anvilBlockId, Blocks.DAMAGED_ANVIL);
                FallingBlockEntity.fall(level, sourcePos, anvilBlock.defaultBlockState());
            } else {
                Block lavaBlock = resolveBlock(lavaBlockId, Blocks.LAVA);
                level.setBlock(sourcePos, lavaBlock.defaultBlockState(), 3);
            }
        });
    }

    private static Block resolveBlock(ResourceLocation id, Block fallback) {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || block == Blocks.AIR) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Invalid block id '{}', using fallback {}", id, fallback);
            return fallback;
        }
        return block;
    }

    private static ResourceLocation parseIdentifier(JsonObject obj, String field, String fallback) {
        try {
            if (!obj.has(field)) {
                return ResourceLocation.parse(fallback);
            }
            return ResourceLocation.parse(obj.get(field).getAsString());
        } catch (Exception ignored) {
            return ResourceLocation.parse(fallback);
        }
    }

    private static int parseRgbColor(JsonObject obj, String field, int fallback) {
        if (!obj.has(field)) {
            return fallback;
        }

        JsonElement element = obj.get(field);
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

        return fallback;
    }
}
