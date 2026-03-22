package com.luckybreak.world;

import com.luckybreak.LuckyBreak;
import com.luckybreak.ModBlocks;
import com.luckybreak.api.event.lifecycle.v1.ServerTickEvents;
import com.luckybreak.api.worldgen.v1.BiomeModifications;
import com.luckybreak.api.worldgen.v1.BiomeSelectors;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ModWorldGeneration {

    private static final ResourceKey<PlacedFeature> LUCKY_BLOCK_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_block"));
    private static final ResourceKey<PlacedFeature> NETHER_LUCKY_BLOCK_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "nether_lucky_block"));
    private static final ResourceKey<PlacedFeature> END_LUCKY_CAGE_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "end_lucky_cage"));

    private static final Map<ResourceKey<Level>, LongSet> SCANNED_CHUNKS = new HashMap<>();

    public static void register() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                LUCKY_BLOCK_PLACED
        );
        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheNether(),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                NETHER_LUCKY_BLOCK_PLACED
        );
        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheEnd(),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                END_LUCKY_CAGE_PLACED
        );

        ServerTickEvents.END_WORLD_TICK.register(ModWorldGeneration::scanLoadedChunks);
        ServerTickEvents.END_SERVER_TICK.register(ModWorldGeneration::pruneUnloadedDimensions);
    }

    private static void pruneUnloadedDimensions(MinecraftServer server) {
        Iterator<ResourceKey<Level>> keyIterator = SCANNED_CHUNKS.keySet().iterator();
        while (keyIterator.hasNext()) {
            ResourceKey<Level> key = keyIterator.next();
            if (server.getLevel(key) == null) {
                keyIterator.remove();
            }
        }
    }

    private static void scanLoadedChunks(ServerLevel level) {
        LongSet scanned = SCANNED_CHUNKS.computeIfAbsent(level.dimension(), ignored -> new LongOpenHashSet());
        ServerChunkCache chunkSource = (ServerChunkCache) level.getChunkSource();

        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            int radius = Math.max(2, level.getServer().getPlayerList().getViewDistance() + 2);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int chunkX = center.x + dx;
                    int chunkZ = center.z + dz;

                    long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
                    if (!scanned.add(chunkKey)) continue;

                    ChunkAccess ca = chunkSource.getChunkNow(chunkX, chunkZ);
                    if (!(ca instanceof LevelChunk chunk)) {
                        scanned.remove(chunkKey);
                        continue;
                    }

                    trackLuckyBlocksInChunk(level, chunk);
                }
            }
        }
    }

    private static void trackLuckyBlocksInChunk(ServerLevel serverLevel, LevelChunk chunk) {
        LuckyBlockTracker tracker = LuckyBlockTracker.get(serverLevel);
        ChunkPos chunkPos = chunk.getPos();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int worldX = chunkPos.getMinBlockX() + lx;
                int worldZ = chunkPos.getMinBlockZ() + lz;
                int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);

                for (int y = surface - 2; y <= surface + 1; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (chunk.getBlockState(pos).is(ModBlocks.LUCKY_BLOCK)) {
                        tracker.add(pos);
                    }
                }
            }
        }
    }
}