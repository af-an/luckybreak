package com.luckybreak.world.feature;

import com.luckybreak.ModBlocks;
import com.luckybreak.events.type.SimpleJsonStructure;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.HashSet;
import java.util.Set;

public class SimpleJsonStructureFeature extends Feature<SimpleJsonStructureFeatureConfig> {

    public SimpleJsonStructureFeature(Codec<SimpleJsonStructureFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SimpleJsonStructureFeatureConfig> context) {
        WorldGenLevel level = context.level();
        MinecraftServer server = level.getLevel().getServer();
        if (server == null) {
            return false;
        }

        SimpleJsonStructureFeatureConfig cfg = context.config();
        SimpleJsonStructure structure = SimpleJsonStructure.load(server.getResourceManager(), cfg.structure());
        if (structure == null) {
            return false;
        }

        Set<Block> requiredFloorBlocks = new HashSet<>();
        for (var blockId : cfg.requiredFloorBlocks()) {
            Block block = BuiltInRegistries.BLOCK.getValue(blockId);
            if (block != null && block != Blocks.AIR) {
                requiredFloorBlocks.add(block);
            }
        }
        boolean enforceRequiredFloor = !requiredFloorBlocks.isEmpty();

        Set<Block> replaceBlocksWithAir = new HashSet<>();
        for (var blockId : cfg.replaceBlocksWithAir()) {
            Block block = BuiltInRegistries.BLOCK.getValue(blockId);
            if (block != null && block != Blocks.AIR) {
                replaceBlocksWithAir.add(block);
            }
        }

        BlockPos origin = context.origin();
        int searchDepth = Math.max(0, cfg.searchDownDepth());
        int requiredAirBlocksAbove = Math.max(1, cfg.requiredAirBlocksAbove());
        int minDistance = Math.max(0, cfg.minDistanceFromSameStructure());
        int minY = Math.max(level.getMinY() + 1, origin.getY() - searchDepth);

        for (int y = origin.getY(); y >= minY; y--) {
            BlockPos candidate = new BlockPos(origin.getX(), y, origin.getZ());
            BlockPos floor = candidate.below();

            if (!level.getBlockState(candidate).isAir()) {
                continue;
            }
            if (!level.getFluidState(candidate).isEmpty() || !level.getFluidState(floor).isEmpty()) {
                continue;
            }
            if (enforceRequiredFloor) {
                BlockState floorState = level.getBlockState(floor);
                boolean validFloor = false;
                for (Block floorBlock : requiredFloorBlocks) {
                    if (floorState.is(floorBlock)) {
                        validFloor = true;
                        break;
                    }
                }
                if (!validFloor) {
                    continue;
                }
            }

            boolean hasClearance = true;
            for (int i = 1; i <= requiredAirBlocksAbove; i++) {
                BlockPos above = candidate.above(i);
                if (!level.getBlockState(above).isAir() || !level.getFluidState(above).isEmpty()) {
                    hasClearance = false;
                    break;
                }
            }
            if (!hasClearance) {
                continue;
            }

            if (minDistance > 0 && hasNearbyLuckyBlock(level, candidate, minDistance)) {
                continue;
            }

            if (cfg.clearOccupiedSpace()) {
                clearStructureArea(level, candidate, cfg, structure.size());
            }

            boolean placed = structure.place(
                    level,
                    candidate,
                    new BlockPos(cfg.originX(), cfg.originY(), cfg.originZ()),
                    cfg.ignoreAir(),
                    cfg.replaceAirOnly(),
                    replaceBlocksWithAir
            );
            if (placed && cfg.generateSupportPillars()) {
                placeSupportPillars(level, candidate, cfg, structure.size());
            }
            return placed;
        }

        return false;
    }

    private static boolean hasNearbyLuckyBlock(WorldGenLevel level, BlockPos center, int radius) {
        int yMin = Math.max(level.getMinY(), center.getY() - 8);
        int yMax = Math.min(level.getMaxY(), center.getY() + 8);
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                for (int y = yMin; y <= yMax; y++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (state.is(ModBlocks.LUCKY_BLOCK)
                            || state.is(ModBlocks.VERY_LUCKY_BLOCK)
                            || state.is(ModBlocks.VERY_UNLUCKY_BLOCK)
                            || state.is(ModBlocks.MOSTLY_LUCKY_BLOCK)
                            || state.is(ModBlocks.MOSTLY_UNLUCKY_BLOCK)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void clearStructureArea(WorldGenLevel level, BlockPos origin, SimpleJsonStructureFeatureConfig cfg, BlockPos size) {
        BlockPos pivot = new BlockPos(cfg.originX(), cfg.originY(), cfg.originZ());
        for (int rx = 0; rx < size.getX(); rx++) {
            for (int ry = 0; ry < size.getY(); ry++) {
                for (int rz = 0; rz < size.getZ(); rz++) {
                    BlockPos worldPos = origin.offset(rx - pivot.getX(), ry - pivot.getY(), rz - pivot.getZ());
                    if (worldPos.getY() <= level.getMinY()) {
                        continue;
                    }
                    BlockState state = level.getBlockState(worldPos);
                    if (state.is(Blocks.BEDROCK)) {
                        continue;
                    }
                    level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void placeSupportPillars(WorldGenLevel level, BlockPos origin, SimpleJsonStructureFeatureConfig cfg, BlockPos size) {
        int baseY = origin.getY() - cfg.originY() - 1;
        int minY = level.getMinY() + 1;
        BlockPos centerBase = new BlockPos(origin.getX() - cfg.originX(), baseY, origin.getZ() - cfg.originZ());
        int topY = baseY + Math.max(1, size.getY());

        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                int x = centerBase.getX() + dx;
                int z = centerBase.getZ() + dz;
                int anchorY = -1;
                for (int y = baseY; y <= topY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty()) {
                        anchorY = y;
                        break;
                    }
                }
                if (anchorY < 0) {
                    anchorY = baseY;
                }

                for (int y = anchorY - 1; y >= minY; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty()) {
                        break;
                    }
                    level.setBlock(pos, Blocks.CRACKED_NETHER_BRICKS.defaultBlockState(), 3);
                }
            }
        }
    }
}
