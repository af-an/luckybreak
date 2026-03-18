package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spawns a random tree near the trigger position.
 *
 * JSON fields:
 *   spawn_radius: 8
 *   attempts: 12
 *   clear_height: 20
 *   fallback_random_attempts: true
 *   clear_land_if_needed: true
 *   clear_land_radius: 2
 *   bonemeal_attempts: 16
 *   min_player_distance: 2
 *   forward_fallback_min_distance: 3
 *   forward_fallback_max_distance: 6
 *   pale_oak_creaking_heart_chance: 0.15
 *   tree_chances: {
 *     "dark_oak": 20,
 *     "jungle": 20,
 *     "spruce": 20,
 *     "oak": 10,
 *     "birch": 10,
 *     "acacia": 10,
 *     "cherry": 5,
 *     "mangrove": 5
 *   }
 *
 * Legacy fallback:
 *   tree_types: ["dark_oak", "jungle", "spruce"]
 */
public class RandomTreeEvent implements LuckyEvent {

    private static final List<TreeOption> DEFAULT_TREE_OPTIONS = List.of(
        new TreeOption(TreeType.DARK_OAK, 20),
        new TreeOption(TreeType.JUNGLE, 20),
        new TreeOption(TreeType.SPRUCE, 20),
        new TreeOption(TreeType.OAK, 10),
            new TreeOption(TreeType.PALE_OAK, 10),
        new TreeOption(TreeType.BIRCH, 10),
        new TreeOption(TreeType.ACACIA, 10),
        new TreeOption(TreeType.CHERRY, 5),
        new TreeOption(TreeType.MANGROVE, 5)
    );

    private final int spawnRadius;
    private final int attempts;
    private final int clearHeight;
    private final boolean fallbackRandomAttempts;
    private final boolean clearLandIfNeeded;
    private final int clearLandRadius;
    private final int bonemealAttempts;
    private final int minPlayerDistance;
    private final int forwardFallbackMinDistance;
    private final int forwardFallbackMaxDistance;
    private final double paleOakCreakingHeartChance;
    private final List<TreeOption> treeOptions;
    private final int totalWeight;

    private RandomTreeEvent(
            int spawnRadius,
            int attempts,
            int clearHeight,
            boolean fallbackRandomAttempts,
            boolean clearLandIfNeeded,
            int clearLandRadius,
            int bonemealAttempts,
            int minPlayerDistance,
            int forwardFallbackMinDistance,
            int forwardFallbackMaxDistance,
            double paleOakCreakingHeartChance,
            List<TreeOption> treeOptions
    ) {
        this.spawnRadius = spawnRadius;
        this.attempts = attempts;
        this.clearHeight = clearHeight;
        this.fallbackRandomAttempts = fallbackRandomAttempts;
        this.clearLandIfNeeded = clearLandIfNeeded;
        this.clearLandRadius = clearLandRadius;
        this.bonemealAttempts = bonemealAttempts;
        this.minPlayerDistance = minPlayerDistance;
        this.forwardFallbackMinDistance = forwardFallbackMinDistance;
        this.forwardFallbackMaxDistance = forwardFallbackMaxDistance;
        this.paleOakCreakingHeartChance = paleOakCreakingHeartChance;
        this.treeOptions = treeOptions;

        int sum = 0;
        for (TreeOption option : treeOptions) {
            sum += Math.max(1, option.weight);
        }
        this.totalWeight = Math.max(1, sum);
    }

    public static RandomTreeEvent fromJson(JsonObject obj) {
        int spawnRadius = obj.has("spawn_radius") ? obj.get("spawn_radius").getAsInt() : 8;
        int attempts = obj.has("attempts") ? obj.get("attempts").getAsInt() : 12;
        int clearHeight = obj.has("clear_height") ? obj.get("clear_height").getAsInt() : 20;
        boolean fallbackRandomAttempts = !obj.has("fallback_random_attempts") || obj.get("fallback_random_attempts").getAsBoolean();
        boolean clearLandIfNeeded = !obj.has("clear_land_if_needed") || obj.get("clear_land_if_needed").getAsBoolean();
        int clearLandRadius = obj.has("clear_land_radius") ? obj.get("clear_land_radius").getAsInt() : 2;
        int bonemealAttempts = obj.has("bonemeal_attempts") ? obj.get("bonemeal_attempts").getAsInt() : 16;
        int minPlayerDistance = obj.has("min_player_distance") ? obj.get("min_player_distance").getAsInt() : 2;
        int forwardFallbackMinDistance = obj.has("forward_fallback_min_distance") ? obj.get("forward_fallback_min_distance").getAsInt() : 3;
        int forwardFallbackMaxDistance = obj.has("forward_fallback_max_distance") ? obj.get("forward_fallback_max_distance").getAsInt() : 6;
        double paleOakCreakingHeartChance = obj.has("pale_oak_creaking_heart_chance") ? obj.get("pale_oak_creaking_heart_chance").getAsDouble() : 0.15;

        if (spawnRadius < 1) spawnRadius = 1;
        if (attempts < 1) attempts = 1;
        if (clearHeight < 6) clearHeight = 6;
        if (clearLandRadius < 0) clearLandRadius = 0;
        if (bonemealAttempts < 1) bonemealAttempts = 1;
        if (minPlayerDistance < 0) minPlayerDistance = 0;
        if (forwardFallbackMinDistance < 1) forwardFallbackMinDistance = 1;
        if (forwardFallbackMaxDistance < forwardFallbackMinDistance) {
            forwardFallbackMaxDistance = forwardFallbackMinDistance;
        }
        if (paleOakCreakingHeartChance < 0.0) paleOakCreakingHeartChance = 0.0;
        if (paleOakCreakingHeartChance > 1.0) paleOakCreakingHeartChance = 1.0;

        List<TreeOption> options = parseTreeOptions(obj);
        if (options.isEmpty()) {
            options = DEFAULT_TREE_OPTIONS;
        }

        return new RandomTreeEvent(
            spawnRadius,
            attempts,
            clearHeight,
            fallbackRandomAttempts,
            clearLandIfNeeded,
            clearLandRadius,
            bonemealAttempts,
            minPlayerDistance,
            forwardFallbackMinDistance,
            forwardFallbackMaxDistance,
            paleOakCreakingHeartChance,
            List.copyOf(options)
        );
    }

    private static List<TreeOption> parseTreeOptions(JsonObject obj) {
        List<TreeOption> result = new ArrayList<>();

        if (obj.has("tree_chances") && obj.get("tree_chances").isJsonObject()) {
            JsonObject chances = obj.getAsJsonObject("tree_chances");
            for (String key : chances.keySet()) {
                JsonElement weightEl = chances.get(key);
                if (!weightEl.isJsonPrimitive() || !weightEl.getAsJsonPrimitive().isNumber()) {
                    continue;
                }

                TreeType type = TreeType.fromId(key);
                if (type == null) {
                    continue;
                }

                int weight = Math.max(1, weightEl.getAsInt());
                result.add(new TreeOption(type, weight));
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        // Legacy fallback: equal weights from tree_types array.
        if (!obj.has("tree_types") || !obj.get("tree_types").isJsonArray()) {
            return result;
        }
        JsonArray arr = obj.getAsJsonArray("tree_types");
        for (JsonElement el : arr) {
            if (!el.isJsonPrimitive()) {
                continue;
            }
            TreeType type = TreeType.fromId(el.getAsString());
            if (type != null) {
                result.add(new TreeOption(type, 1));
            }
        }
        return result;
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        RandomSource random = level.getRandom();
        BlockPos center = enforceMinDistanceFromPlayer(pos, player);

        // First try the exact trigger position (broken block pos or command target pos).
        int centerY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
        BlockPos centerOrigin = new BlockPos(center.getX(), Math.max(level.getMinY() + 1, centerY), center.getZ());
        TreeType centerType = pickTreeType(random);
        if (wouldClipPlayer(player, centerOrigin, centerType.supportsBig2x2 ? 2 : 1)) {
            if (trySpawnFurtherInFrontOfPlayer(level, player, centerType, random)) {
                return;
            }
            if (!fallbackRandomAttempts) {
                LuckyBreak.LOGGER.warn("[LuckyBreak] random_tree exact-position spawn would clip the player and fallback_random_attempts is disabled.");
                dropSaplingCompensation(level, pos, centerType);
                return;
            }
        } else if (trySpawnTree(level, centerOrigin, centerType, random)) {
            maybePlacePaleOakCreakingHeart(level, centerOrigin, centerType, random);
            return;
        }

        if (!fallbackRandomAttempts) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] random_tree exact-position spawn failed and fallback_random_attempts is disabled.");
            dropSaplingCompensation(level, pos, centerType);
            return;
        }

        for (int i = 0; i < attempts; i++) {
            int dx = random.nextInt(spawnRadius * 2 + 1) - spawnRadius;
            int dz = random.nextInt(spawnRadius * 2 + 1) - spawnRadius;
            if (dx == 0 && dz == 0) {
                continue;
            }
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            if (!isFarEnoughFromPlayer(x, z, player)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos origin = new BlockPos(x, Math.max(level.getMinY() + 1, y), z);

            TreeType type = pickTreeType(random);
            if (wouldClipPlayer(player, origin, type.supportsBig2x2 ? 2 : 1)) {
                continue;
            }
            if (trySpawnTree(level, origin, type, random)) {
                maybePlacePaleOakCreakingHeart(level, origin, type, random);
                return;
            }
        }

        LuckyBreak.LOGGER.warn("[LuckyBreak] random_tree failed to find a valid spawn location; dropping saplings instead.");
        dropSaplingCompensation(level, pos, centerType);
    }

    private void dropSaplingCompensation(ServerLevel level, BlockPos pos, TreeType type) {
        int count = type.supportsBig2x2 ? 4 : 1;
        ItemStack drop = new ItemStack(type.sapling.asItem(), count);
        ItemEntity entity = new ItemEntity(
            level,
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            drop
        );
        level.addFreshEntity(entity);
    }

    private boolean trySpawnFurtherInFrontOfPlayer(ServerLevel level, ServerPlayer player, TreeType type, RandomSource random) {
        int stepX = player.getDirection().getStepX();
        int stepZ = player.getDirection().getStepZ();
        if (stepX == 0 && stepZ == 0) {
            Vec3 look = player.getLookAngle();
            if (Math.abs(look.x) >= Math.abs(look.z)) {
                stepX = look.x >= 0.0 ? 1 : -1;
                stepZ = 0;
            } else {
                stepX = 0;
                stepZ = look.z >= 0.0 ? 1 : -1;
            }
        }

        int trunkWidth = type.supportsBig2x2 ? 2 : 1;
        int minDistance = Math.max(minPlayerDistance + 1, forwardFallbackMinDistance);
        int maxDistance = Math.max(minDistance, forwardFallbackMaxDistance);
        for (int distance = minDistance; distance <= maxDistance; distance++) {
            int x = (int) Math.floor(player.getX()) + stepX * distance;
            int z = (int) Math.floor(player.getZ()) + stepZ * distance;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos origin = new BlockPos(x, Math.max(level.getMinY() + 1, y), z);

            if (wouldClipPlayer(player, origin, trunkWidth)) {
                continue;
            }

            if (trySpawnTree(level, origin, type, random)) {
                maybePlacePaleOakCreakingHeart(level, origin, type, random);
                return true;
            }
        }

        return false;
    }

    private BlockPos enforceMinDistanceFromPlayer(BlockPos target, ServerPlayer player) {
        if (minPlayerDistance <= 0 || isFarEnoughFromPlayer(target.getX(), target.getZ(), player)) {
            return target;
        }

        int stepX = player.getDirection().getStepX();
        int stepZ = player.getDirection().getStepZ();
        if (stepX == 0 && stepZ == 0) {
            Vec3 look = player.getLookAngle();
            if (Math.abs(look.x) >= Math.abs(look.z)) {
                stepX = look.x >= 0.0 ? 1 : -1;
                stepZ = 0;
            } else {
                stepX = 0;
                stepZ = look.z >= 0.0 ? 1 : -1;
            }
        }

        int x = (int) Math.floor(player.getX()) + stepX * minPlayerDistance;
        int z = (int) Math.floor(player.getZ()) + stepZ * minPlayerDistance;
        return new BlockPos(x, target.getY(), z);
    }

    private boolean isFarEnoughFromPlayer(int x, int z, ServerPlayer player) {
        if (minPlayerDistance <= 0) {
            return true;
        }
        double dx = (x + 0.5) - player.getX();
        double dz = (z + 0.5) - player.getZ();
        return (dx * dx + dz * dz) >= (double) (minPlayerDistance * minPlayerDistance);
    }

    private TreeType pickTreeType(RandomSource random) {
        int roll = random.nextInt(totalWeight);
        int running = 0;
        for (TreeOption option : treeOptions) {
            running += Math.max(1, option.weight);
            if (roll < running) {
                return option.type;
            }
        }
        return treeOptions.get(treeOptions.size() - 1).type;
    }

    private boolean trySpawnTree(ServerLevel level, BlockPos origin, TreeType type, RandomSource random) {
        if (clearLandIfNeeded) {
            clearLand(level, origin, type);
        }
        return type.supportsBig2x2
                ? trySpawnBigTree(level, origin, type, random)
                : trySpawnSingleTree(level, origin, type, random);
    }

    private void clearLand(ServerLevel level, BlockPos origin, TreeType type) {
        int trunkWidth = type.supportsBig2x2 ? 2 : 1;
        int spreadRadius = type == TreeType.SPRUCE ? clearLandRadius : 0;

        int minX = -spreadRadius;
        int maxX = (trunkWidth - 1) + spreadRadius;
        int minZ = -spreadRadius;
        int maxZ = (trunkWidth - 1) + spreadRadius;

        for (int dx = minX; dx <= maxX; dx++) {
            for (int dz = minZ; dz <= maxZ; dz++) {
                BlockPos ground = origin.offset(dx, -1, dz);
                level.setBlock(ground, Blocks.DIRT.defaultBlockState(), 3);

                // Clear low obstructions around the trunk area so growth has room.
                level.setBlock(origin.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(origin.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private boolean trySpawnBigTree(ServerLevel level, BlockPos origin, TreeType type, RandomSource random) {
        for (int ox = 0; ox <= 1; ox++) {
            for (int oz = 0; oz <= 1; oz++) {
                BlockPos below = origin.offset(ox, -1, oz);
                if (!level.getBlockState(below).isSolidRender()) {
                    level.setBlock(below, Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }

        for (int y = 0; y <= clearHeight; y++) {
            for (int ox = 0; ox <= 1; ox++) {
                for (int oz = 0; oz <= 1; oz++) {
                    BlockPos clearPos = origin.offset(ox, y, oz);
                    level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        Block sapling = type.sapling;
        BlockState saplingState = sapling.defaultBlockState();
        for (int ox = 0; ox <= 1; ox++) {
            for (int oz = 0; oz <= 1; oz++) {
                level.setBlock(origin.offset(ox, 0, oz), saplingState, 3);
            }
        }

        if (!(sapling instanceof SaplingBlock saplingBlock)) {
            return false;
        }
        if (!(saplingBlock instanceof BonemealableBlock bonemealable)) {
            return false;
        }

        BlockPos triggerPos = origin;
        for (int tries = 0; tries < bonemealAttempts; tries++) {
            BlockState state = level.getBlockState(triggerPos);
            if (state.getBlock() instanceof BonemealableBlock && bonemealable.isValidBonemealTarget(level, triggerPos, state)) {
                bonemealable.performBonemeal(level, random, triggerPos, state);
            }

            if (treeGenerated(level, origin, sapling)) {
                return true;
            }
        }
        return treeGenerated(level, origin, sapling);
    }

    private boolean trySpawnSingleTree(ServerLevel level, BlockPos origin, TreeType type, RandomSource random) {
        BlockPos below = origin.below();
        if (!level.getBlockState(below).isSolidRender()) {
            level.setBlock(below, Blocks.DIRT.defaultBlockState(), 3);
        }

        for (int y = 0; y <= clearHeight; y++) {
            level.setBlock(origin.above(y), Blocks.AIR.defaultBlockState(), 3);
        }

        Block sapling = type.sapling;
        BlockState saplingState = sapling.defaultBlockState();
        level.setBlock(origin, saplingState, 3);

        if (!(sapling instanceof BonemealableBlock bonemealable)) {
            return false;
        }

        for (int tries = 0; tries < bonemealAttempts; tries++) {
            BlockState state = level.getBlockState(origin);
            if (state.getBlock() instanceof BonemealableBlock && bonemealable.isValidBonemealTarget(level, origin, state)) {
                bonemealable.performBonemeal(level, random, origin, state);
            }

            if (!level.getBlockState(origin).is(sapling)) {
                return true;
            }
        }
        return !level.getBlockState(origin).is(sapling);
    }

    private boolean treeGenerated(ServerLevel level, BlockPos origin, Block sapling) {
        for (int ox = 0; ox <= 1; ox++) {
            for (int oz = 0; oz <= 1; oz++) {
                if (level.getBlockState(origin.offset(ox, 0, oz)).is(sapling)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean wouldClipPlayer(ServerPlayer player, BlockPos origin, int trunkWidth) {
        // Reserve the lower trunk area (up to 2 blocks high) so we never spawn a tree into the player.
        AABB playerBox = player.getBoundingBox().deflate(0.001);
        AABB trunkBox = new AABB(
            origin.getX(),
            origin.getY(),
            origin.getZ(),
            origin.getX() + trunkWidth,
            origin.getY() + 2,
            origin.getZ() + trunkWidth
        );
        return playerBox.intersects(trunkBox);
    }

    private void maybePlacePaleOakCreakingHeart(ServerLevel level, BlockPos origin, TreeType type, RandomSource random) {
        if (type != TreeType.PALE_OAK || paleOakCreakingHeartChance <= 0.0) {
            return;
        }
        if (random.nextDouble() > paleOakCreakingHeartChance) {
            return;
        }

        List<BlockPos> trunkLogs = new ArrayList<>();
        int trunkWidth = type.supportsBig2x2 ? 2 : 1;
        for (int y = 0; y <= clearHeight + 8; y++) {
            for (int ox = 0; ox < trunkWidth; ox++) {
                for (int oz = 0; oz < trunkWidth; oz++) {
                    BlockPos p = origin.offset(ox, y, oz);
                    if (level.getBlockState(p).is(Blocks.PALE_OAK_LOG)) {
                        trunkLogs.add(p);
                    }
                }
            }
        }

        if (trunkLogs.isEmpty()) {
            return;
        }

        BlockPos chosen = trunkLogs.get(random.nextInt(trunkLogs.size()));
        level.setBlock(chosen, Blocks.CREAKING_HEART.defaultBlockState(), 3);
    }

    private enum TreeType {
        DARK_OAK("dark_oak", Blocks.DARK_OAK_SAPLING, true),
        JUNGLE("jungle", Blocks.JUNGLE_SAPLING, true),
        SPRUCE("spruce", Blocks.SPRUCE_SAPLING, true),
        OAK("oak", Blocks.OAK_SAPLING, false),
        PALE_OAK("pale_oak", Blocks.PALE_OAK_SAPLING, true),
        BIRCH("birch", Blocks.BIRCH_SAPLING, false),
        ACACIA("acacia", Blocks.ACACIA_SAPLING, false),
        CHERRY("cherry", Blocks.CHERRY_SAPLING, false),
        MANGROVE("mangrove", Blocks.MANGROVE_PROPAGULE, false);

        private final String id;
        private final Block sapling;
        private final boolean supportsBig2x2;

        TreeType(String id, Block sapling, boolean supportsBig2x2) {
            this.id = id;
            this.sapling = sapling;
            this.supportsBig2x2 = supportsBig2x2;
        }

        private static TreeType fromId(String raw) {
            String id = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            for (TreeType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return null;
        }
    }

    private static class TreeOption {
        private final TreeType type;
        private final int weight;

        private TreeOption(TreeType type, int weight) {
            this.type = type;
            this.weight = weight;
        }
    }
}
