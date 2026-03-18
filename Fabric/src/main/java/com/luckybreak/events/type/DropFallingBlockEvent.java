package com.luckybreak.events.type;

import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drops falling block entities above the break position.
 *
 * JSON fields:
 *   block:  "minecraft:gold_block"
 *   count:  3         (default 1)
 *   spread: 2         (random XZ spread for multiple blocks, default 0)
 */
public class DropFallingBlockEvent implements LuckyEvent {

    private final String blockId;
    private final int count;
    private final int spread;

    private DropFallingBlockEvent(String blockId, int count, int spread) {
        this.blockId = blockId;
        this.count = count;
        this.spread = spread;
    }

    public static DropFallingBlockEvent fromJson(JsonObject obj) {
        String block  = obj.has("block")  ? obj.get("block").getAsString()  : "minecraft:sand";
        int count     = obj.has("count")  ? obj.get("count").getAsInt()     : 1;
        int spread    = obj.has("spread") ? obj.get("spread").getAsInt()    : 0;
        return new DropFallingBlockEvent(block, count, spread);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId));
        if (block == null || block == Blocks.AIR) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Unknown block for falling: {}", blockId);
            return;
        }
        BlockState state = block.defaultBlockState();

        for (int i = 0; i < count; i++) {
            int ox = spread > 0 ? (level.getRandom().nextInt(spread * 2 + 1) - spread) : 0;
            int oz = spread > 0 ? (level.getRandom().nextInt(spread * 2 + 1) - spread) : 0;
            // Spawn 3 blocks above the break point so they visibly fall
            BlockPos spawnPos = pos.offset(ox, 3, oz);
            FallingBlockEntity.fall(level, spawnPos, state);
        }
    }
}
