package com.luckybreak.world;

import com.luckybreak.LuckyBreak;
import com.luckybreak.ModBlocks;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

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

    public static void register() {
        BiomeModifications.addFeature(
                BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                LUCKY_BLOCK_PLACED
        );

        BiomeModifications.addFeature(
            BiomeSelectors.tag(BiomeTags.IS_NETHER),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            NETHER_LUCKY_BLOCK_PLACED
        );

        BiomeModifications.addFeature(
            BiomeSelectors.tag(BiomeTags.IS_END),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            END_LUCKY_CAGE_PLACED
        );

        // Scan every chunk when it loads to register any Lucky Blocks that were
        // placed during worldgen. Worldgen uses WorldGenRegion (not ServerLevel),
        // so Block.onPlace() is never called in that context — this scan is the
        // authoritative source of truth for generated blocks.
        //
        // Strategy: per column, check the surface heightmap ± 3 blocks. Lucky
        // blocks are always placed on the surface, so this window catches them
        // cheaply (~1280 checks per chunk) without scanning the whole chunk.
        ServerChunkEvents.CHUNK_LOAD.register((serverLevel, chunk) -> {
            LuckyBlockTracker tracker = LuckyBlockTracker.get(serverLevel);
            ChunkPos chunkPos = chunk.getPos();

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int worldX = chunkPos.getMinBlockX() + lx;
                    int worldZ = chunkPos.getMinBlockZ() + lz;
                    int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);

                    // Check a small window around the surface — lucky blocks sit
                    // on top of terrain so the range is intentionally tight.
                    for (int y = surface - 2; y <= surface + 1; y++) {
                        BlockPos pos = new BlockPos(worldX, y, worldZ);
                        if (chunk.getBlockState(pos).is(ModBlocks.LUCKY_BLOCK)) {
                            tracker.add(pos);
                        }
                    }
                }
            }
        });
    }
}

