package com.luckybreak.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime manager for temporary forced-lucky block pairs.
 *
 * When one paired block is broken, the counterpart should be removed.
 */
public final class ForcedLuckyPairManager {

    private static final Map<ResourceKey<Level>, Map<BlockPos, BlockPos>> PAIRS = new HashMap<>();

    private ForcedLuckyPairManager() {
    }

    public static void registerPair(ServerLevel level, BlockPos first, BlockPos second) {
        Map<BlockPos, BlockPos> byPos = PAIRS.computeIfAbsent(level.dimension(), ignored -> new HashMap<>());
        BlockPos a = first.immutable();
        BlockPos b = second.immutable();
        byPos.put(a, b);
        byPos.put(b, a);
    }

    public static BlockPos consumeOther(ServerLevel level, BlockPos brokenPos) {
        Map<BlockPos, BlockPos> byPos = PAIRS.get(level.dimension());
        if (byPos == null) {
            return null;
        }

        BlockPos broken = brokenPos.immutable();
        BlockPos other = byPos.remove(broken);
        if (other != null) {
            byPos.remove(other);
        }

        if (byPos.isEmpty()) {
            PAIRS.remove(level.dimension());
        }

        return other;
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        consumeOther(level, pos);
    }
}
