package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creates a deep circular lava pit around the center block, with a surrounding stone ring.
 *
 * Defaults match the requested design:
 * - Lava radius: 10
 * - Stone wall thickness: 1 block (outside the lava)
 * - Depth: 30 blocks down
 * - Center block remains stone so the player can stand initially
 *
 * JSON fields:
 *   radius: 10
 *   depth: 30
 *   wall_thickness: 1
 *   clear_height: 12
 */
public class LavaPitEvent implements LuckyEvent {

    private final int radius;
    private final int depth;
    private final int wallThickness;
    private final int clearHeight;

    private LavaPitEvent(int radius, int depth, int wallThickness, int clearHeight) {
        this.radius = radius;
        this.depth = depth;
        this.wallThickness = wallThickness;
        this.clearHeight = clearHeight;
    }

    public static LavaPitEvent fromJson(JsonObject obj) {
        int radius = obj.has("radius") ? obj.get("radius").getAsInt() : 10;
        int depth = obj.has("depth") ? obj.get("depth").getAsInt() : 30;
        int wallThickness = obj.has("wall_thickness") ? obj.get("wall_thickness").getAsInt() : 1;
        int clearHeight = obj.has("clear_height") ? obj.get("clear_height").getAsInt() : 12;

        if (radius < 1) radius = 1;
        if (depth < 1) depth = 1;
        if (wallThickness < 1) wallThickness = 1;
        if (clearHeight < 0) clearHeight = 0;

        return new LavaPitEvent(radius, depth, wallThickness, clearHeight);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        // Use the block under the player's feet as the center platform.
        BlockPos center = player.blockPosition().below();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();

        int lavaR2 = radius * radius;
        int outerRadius = radius + wallThickness;
        int outerR2 = outerRadius * outerRadius;

        // Clear a vertical air space above the pit so overhead terrain does not remain.
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dy = 1; dy <= clearHeight; dy++) {
            int y = center.getY() + dy;
            for (int dx = -outerRadius; dx <= outerRadius; dx++) {
                for (int dz = -outerRadius; dz <= outerRadius; dz++) {
                    int dist2 = dx * dx + dz * dz;
                    if (dist2 <= outerR2) {
                        BlockPos target = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        level.setBlock(target, air, 3);
                    }
                }
            }
        }

        for (int dy = 0; dy < depth; dy++) {
            int y = center.getY() - dy;
            if (y < level.getMinY()) {
                break;
            }

            for (int dx = -outerRadius; dx <= outerRadius; dx++) {
                for (int dz = -outerRadius; dz <= outerRadius; dz++) {
                    int dist2 = dx * dx + dz * dz;
                    if (dist2 > outerR2) {
                        continue;
                    }

                    BlockPos target = new BlockPos(center.getX() + dx, y, center.getZ() + dz);

                    // Keep the center as stone so the player starts on a safe single block.
                    if (dx == 0 && dz == 0) {
                        level.setBlock(target, stone, 3);
                        continue;
                    }

                    if (dist2 <= lavaR2) {
                        level.setBlock(target, lava, 3);
                    } else {
                        level.setBlock(target, stone, 3);
                    }
                }
            }
        }
    }
}
