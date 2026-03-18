package com.luckybreak.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * A single lucky block event that can be executed when the block is broken.
 */
public interface LuckyEvent {
    void execute(ServerLevel level, BlockPos pos, ServerPlayer player);
}
