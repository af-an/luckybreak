package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Places a tier-1 beacon setup: a 3x3 iron block base with a beacon on top.
 *
 * JSON:
 * {
 *   "type": "luckybreak:iron_beacon_tier1",
 *   "weight": 8,
 *   "clear_above_beacon": true
 * }
 */
public class IronBeaconTier1Event implements LuckyEvent {

    private final boolean clearAboveBeacon;

    private IronBeaconTier1Event(boolean clearAboveBeacon) {
        this.clearAboveBeacon = clearAboveBeacon;
    }

    public static IronBeaconTier1Event fromJson(JsonObject obj) {
        boolean clearAboveBeacon = !obj.has("clear_above_beacon") || obj.get("clear_above_beacon").getAsBoolean();
        return new IronBeaconTier1Event(clearAboveBeacon);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        // Build a 3x3 iron base centered on the lucky block position.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos basePos = pos.offset(dx, 0, dz);
                level.setBlock(basePos, Blocks.IRON_BLOCK.defaultBlockState(), 3);
            }
        }

        BlockPos beaconPos = pos.above();
        if (clearAboveBeacon) {
            level.setBlock(beaconPos.above(), Blocks.AIR.defaultBlockState(), 3);
        }

        level.setBlock(beaconPos, Blocks.BEACON.defaultBlockState(), 3);
    }
}
