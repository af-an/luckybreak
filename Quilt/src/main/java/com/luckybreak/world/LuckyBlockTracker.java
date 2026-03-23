package com.luckybreak.world;

import com.luckybreak.LuckyBreak;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SavedData that persists all Lucky Block positions in a dimension.
 *
 * Uses 1.21.5's SavedDataType + Codec API. Positions are grouped in-memory by
 * ChunkPos so the spiral nearest-neighbor search can skip empty chunks instantly.
 */
public class LuckyBlockTracker extends SavedData {

    private static final String DATA_NAME = LuckyBreak.MOD_ID + "_lucky_blocks";

    /** In-memory store: chunk → set of Lucky Block positions within that chunk. */
    private final Map<ChunkPos, Set<BlockPos>> chunkToPositions = new HashMap<>();

    // -------------------------------------------------------------------------
    // Codec — flat list of packed BlockPos longs
    // -------------------------------------------------------------------------

    /**
     * Serializes/deserializes as a list of packed BlockPos longs.
     * Chunk grouping is an in-memory optimization; we rebuild it on load.
     */
    public static final Codec<LuckyBlockTracker> CODEC = Codec.LONG.listOf().xmap(
            longs -> {
                LuckyBlockTracker tracker = new LuckyBlockTracker();
                longs.forEach(l -> tracker.addInternal(BlockPos.of(l)));
                return tracker;
            },
            tracker -> tracker.chunkToPositions.values().stream()
                    .flatMap(Collection::stream)
                    .map(BlockPos::asLong)
                    .collect(Collectors.toList())
    );

    // -------------------------------------------------------------------------
    // SavedDataType — the 1.21.5 replacement for SavedData.Factory
    // -------------------------------------------------------------------------

    public static final SavedDataType<LuckyBlockTracker> TYPE = new SavedDataType<>(
            DATA_NAME,
            LuckyBlockTracker::new,
            CODEC,
            DataFixTypes.SAVED_DATA_FORCED_CHUNKS
    );

    // -------------------------------------------------------------------------
    // Retrieval
    // -------------------------------------------------------------------------

    /**
     * Retrieve (or lazily create) the tracker for this dimension.
     * Each dimension has its own DimensionDataStorage, so this is
     * automatically dimension-scoped — no GlobalPos needed.
     */
    public static LuckyBlockTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * Register a Lucky Block at {@code pos}.
     * Only marks dirty if this is a genuinely new position, so the chunk-load
     * scan doesn't trigger unnecessary disk writes for already-tracked blocks.
     */
    public void add(BlockPos pos) {
        if (addInternal(pos)) {
            setDirty();
        }
    }

    /**
     * Internal add; returns {@code true} if the position was not already tracked.
     * Used by both add() and the Codec deserializer.
     */
    private boolean addInternal(BlockPos pos) {
        return chunkToPositions
                .computeIfAbsent(new ChunkPos(pos), k -> new HashSet<>())
                .add(pos.immutable());
    }

    /**
     * Unregister a Lucky Block at {@code pos}.
     * Call from block removal hooks.
     */
    public void remove(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Set<BlockPos> set = chunkToPositions.get(chunkPos);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) {
                chunkToPositions.remove(chunkPos);
            }
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // Nearest-block search (spiral ring expansion)
    // -------------------------------------------------------------------------

    /**
     * Search outward from the chunk containing {@code from}, ring by ring,
     * and return the nearest Lucky Block within {@code maxChunkRadius} chunks.
     *
     * <p>Early-exit once the closest unvisited ring is provably farther than
     * the current best, so the search terminates as soon as possible.
     *
     * @param from           origin position (typically the player's feet)
     * @param maxChunkRadius maximum ring radius to search (~16 × radius blocks)
     * @return the nearest Lucky Block BlockPos, or {@code null} if none found
     */
    public BlockPos findNearest(BlockPos from, int maxChunkRadius) {
        ChunkPos centerChunk = new ChunkPos(from);
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int radius = 0; radius <= maxChunkRadius; radius++) {

            // Early-exit: if the inner edge of this ring is already further
            // than our best hit, no block in this or any later ring can win.
            if (nearest != null) {
                double minRingDist = Math.max(0.0, (radius - 1) * 16.0);
                if (minRingDist * minRingDist > nearestDistSq) {
                    break;
                }
            }

            for (ChunkPos chunkPos : getChunkRing(centerChunk, radius)) {
                Set<BlockPos> positions = chunkToPositions.get(chunkPos);
                if (positions == null) continue;

                for (BlockPos candidate : positions) {
                    double distSq = from.distSqr(candidate);
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearest = candidate;
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Returns the hollow square ring of ChunkPos values at exactly
     * {@code radius} chunks away from {@code center}.
     * radius=0 yields only the center chunk itself.
     */
    private static List<ChunkPos> getChunkRing(ChunkPos center, int radius) {
        if (radius == 0) {
            return List.of(center);
        }
        List<ChunkPos> ring = new ArrayList<>();
        int cx = center.x, cz = center.z;

        // Top row (z = cz-radius) and bottom row (z = cz+radius)
        for (int dx = -radius; dx <= radius; dx++) {
            ring.add(new ChunkPos(cx + dx, cz - radius));
            ring.add(new ChunkPos(cx + dx, cz + radius));
        }
        // Left and right columns, corners already covered above
        for (int dz = -radius + 1; dz <= radius - 1; dz++) {
            ring.add(new ChunkPos(cx - radius, cz + dz));
            ring.add(new ChunkPos(cx + radius, cz + dz));
        }
        return ring;
    }
}

