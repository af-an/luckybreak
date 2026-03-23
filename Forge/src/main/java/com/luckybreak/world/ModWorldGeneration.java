package com.luckybreak.world;

import com.luckybreak.LuckyBreak;
import com.luckybreak.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
    }

        // Scan every chunk when it loads to register any Lucky Blocks that were
        // placed during worldgen. Worldgen uses WorldGenRegion (not ServerLevel),
        // so Block.onPlace() is never called in that context — this scan is the
        // authoritative source of truth for generated blocks.
        //
        // Strategy: per column, check the surface heightmap ± 3 blocks. Lucky
        // blocks are always placed on the surface, so this window catches them
        // cheaply (~1280 checks per chunk) without scanning the whole chunk.
    @Mod.EventBusSubscriber(modid = LuckyBreak.MOD_ID)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onChunkLoad(ChunkEvent.Load event) {
            if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                return;
            }
            if (!(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) {
                return;
            }
            LuckyBlockTracker tracker = LuckyBlockTracker.get(serverLevel);
            ChunkPos chunkPos = chunk.getPos();

            int minY = chunk.getMinY();
            int maxY = chunk.getMaxY();

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int worldX = chunkPos.getMinBlockX() + lx;
                    int worldZ = chunkPos.getMinBlockZ() + lz;

                    for (int y = minY; y < maxY; y++) {
                        BlockPos pos = new BlockPos(worldX, y, worldZ);
                        if (isTrackedLuckyBlock(chunk.getBlockState(pos))) {
                            tracker.add(pos);
                        }
                    }
                }
            }
        }

        private static boolean isTrackedLuckyBlock(net.minecraft.world.level.block.state.BlockState state) {
            return state.is(ModBlocks.LUCKY_BLOCK)
                    || state.is(ModBlocks.VERY_LUCKY_BLOCK)
                    || state.is(ModBlocks.VERY_UNLUCKY_BLOCK)
                    || state.is(ModBlocks.MOSTLY_LUCKY_BLOCK)
                    || state.is(ModBlocks.MOSTLY_UNLUCKY_BLOCK);
        }
    }
}

