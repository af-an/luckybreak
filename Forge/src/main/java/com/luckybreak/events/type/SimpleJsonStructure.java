package com.luckybreak.events.type;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lightweight JSON structure format for event-driven placement.
 */
public class SimpleJsonStructure {

    private static final Gson GSON = new Gson();

    private final List<BlockPlacement> placements;
    private final BlockPos size;

    private SimpleJsonStructure(List<BlockPlacement> placements, BlockPos size) {
        this.placements = placements;
        this.size = size;
    }

    public static SimpleJsonStructure load(ServerLevel level, Identifier structureId) {
        return load(level.getServer().getResourceManager(), structureId);
    }

    public static SimpleJsonStructure load(ResourceManager resourceManager, Identifier structureId) {
        Identifier resourceId = Identifier.fromNamespaceAndPath(
                structureId.getNamespace(),
                "simple_structures/" + structureId.getPath() + ".json"
        );

        Optional<Resource> resource = resourceManager.getResource(resourceId);
        if (resource.isEmpty()) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Simple structure JSON not found: {}", resourceId);
            return null;
        }

        try (Reader reader = resource.get().openAsReader()) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonArray blocks = root.getAsJsonArray("blocks");
            List<BlockPlacement> placements = new ArrayList<>();
            int maxX = 0;
            int maxY = 0;
            int maxZ = 0;

            for (JsonElement element : blocks) {
                JsonObject entry = element.getAsJsonObject();
                JsonArray pos = entry.getAsJsonArray("pos");
                int x = pos.get(0).getAsInt();
                int y = pos.get(1).getAsInt();
                int z = pos.get(2).getAsInt();
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
                if (z > maxZ) maxZ = z;

                String blockId = entry.get("block").getAsString();
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId));
                if (block == null) {
                    continue;
                }
                BlockState state = block.defaultBlockState();
                if (entry.has("properties")) {
                    state = applyProperties(state, entry.getAsJsonObject("properties"));
                }

                placements.add(new BlockPlacement(new BlockPos(x, y, z), block, state));
            }

            BlockPos size = new BlockPos(maxX + 1, maxY + 1, maxZ + 1);
            if (root.has("size") && root.get("size").isJsonArray()) {
                JsonArray sizeArr = root.getAsJsonArray("size");
                if (sizeArr.size() >= 3) {
                    size = new BlockPos(sizeArr.get(0).getAsInt(), sizeArr.get(1).getAsInt(), sizeArr.get(2).getAsInt());
                }
            }

            return new SimpleJsonStructure(placements, size);
        } catch (Exception e) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load simple structure {}: {}", resourceId, e.getMessage());
            return null;
        }
    }

    public BlockPos size() {
        return size;
    }

    public boolean place(
            LevelAccessor level,
            BlockPos origin,
            BlockPos originPivot,
            boolean ignoreAir,
            boolean replaceAirOnly,
            Set<Block> replaceBlocksWithAir
    ) {
        return place(level, origin, originPivot, ignoreAir, replaceAirOnly, replaceBlocksWithAir, Rotation.NONE);
        }

        public boolean place(
            LevelAccessor level,
            BlockPos origin,
            BlockPos originPivot,
            boolean ignoreAir,
            boolean replaceAirOnly,
            Set<Block> replaceBlocksWithAir,
            Rotation rotation
        ) {
        if (placements.isEmpty()) {
            return false;
        }

        int placedCount = 0;
        for (BlockPlacement placement : placements) {
            BlockPos relative = placement.offset().subtract(originPivot);
            BlockPos rotated = rotateRelative(relative, rotation);
            BlockPos worldPos = origin.offset(rotated);
            boolean forceAir = replaceBlocksWithAir.contains(placement.sourceBlock());
            BlockState state = forceAir ? Blocks.AIR.defaultBlockState() : placement.state().rotate(rotation);

            if (!forceAir && replaceAirOnly && !level.getBlockState(worldPos).isAir()) {
                continue;
            }

            if (!forceAir && ignoreAir && state.isAir()) {
                continue;
            }

            level.setBlock(worldPos, state, 3);
            placedCount++;
        }
        return placedCount > 0;
    }

    private static BlockPos rotateRelative(BlockPos pos, Rotation rotation) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(-z, y, x);
            case CLOCKWISE_180 -> new BlockPos(-x, y, -z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, -x);
            case NONE -> pos;
        };
    }

    private static BlockState applyProperties(BlockState state, JsonObject properties) {
        BlockState result = state;
        for (String key : properties.keySet()) {
            Property<?> property = result.getBlock().getStateDefinition().getProperty(key);
            if (property == null) {
                continue;
            }
            result = applyProperty(result, property, properties.get(key).getAsString());
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState applyProperty(BlockState state, Property property, String value) {
        Optional parsed = property.getValue(value);
        if (parsed.isEmpty()) {
            return state;
        }
        return state.setValue(property, (Comparable) parsed.get());
    }

    private record BlockPlacement(BlockPos offset, Block sourceBlock, BlockState state) {
    }
}