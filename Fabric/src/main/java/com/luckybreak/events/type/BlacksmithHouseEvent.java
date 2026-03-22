package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spawns a blacksmith house structure, fills its chest with random loot,
 * and spawns a villager blacksmith inside.
 */
public class BlacksmithHouseEvent implements LuckyEvent {

    private record LootEntry(Item item, double weight, int countMin, int countMax) {}

    private final ResourceLocation structureId;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final BlockPos structureOrigin;
    private final int structureSizeX;
    private final int structureSizeY;
    private final int structureSizeZ;
    private final boolean spawnInFrontOfPlayer;
    private final int spawnDistance;
    private final Direction structureFront;
    private final BlockPos doorLocalPos;
    private final Direction doorFacing;
    private final int playerDistanceFromDoor;
    private final boolean clearAreaBeforePlace;
    private final boolean fillFoundationToGround;
    private final int maxFoundationDepth;
    private final Block foundationBlock;
    private final boolean ignoreAir;
    private final boolean replaceAirOnly;
    private final BlockPos chestLocalPos;
    private final int rollsMin;
    private final int rollsMax;
    private final List<LootEntry> loot;
    private final BlockPos villagerSpawnLocal;
    private final String villagerProfessionId;
    private final String villagerTypeId;
    private final int villagerLevel;

    private BlacksmithHouseEvent(
            ResourceLocation structureId,
            int offsetX,
            int offsetY,
            int offsetZ,
            BlockPos structureOrigin,
            int structureSizeX,
            int structureSizeY,
            int structureSizeZ,
            boolean spawnInFrontOfPlayer,
            int spawnDistance,
            Direction structureFront,
            BlockPos doorLocalPos,
            Direction doorFacing,
            int playerDistanceFromDoor,
            boolean clearAreaBeforePlace,
            boolean fillFoundationToGround,
            int maxFoundationDepth,
            Block foundationBlock,
            boolean ignoreAir,
            boolean replaceAirOnly,
            BlockPos chestLocalPos,
            int rollsMin,
            int rollsMax,
            List<LootEntry> loot,
            BlockPos villagerSpawnLocal,
                String villagerProfessionId,
                String villagerTypeId,
                int villagerLevel
    ) {
        this.structureId = structureId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.structureOrigin = structureOrigin;
        this.structureSizeX = structureSizeX;
        this.structureSizeY = structureSizeY;
        this.structureSizeZ = structureSizeZ;
        this.spawnInFrontOfPlayer = spawnInFrontOfPlayer;
        this.spawnDistance = spawnDistance;
        this.structureFront = structureFront;
        this.doorLocalPos = doorLocalPos;
        this.doorFacing = doorFacing;
        this.playerDistanceFromDoor = playerDistanceFromDoor;
        this.clearAreaBeforePlace = clearAreaBeforePlace;
        this.fillFoundationToGround = fillFoundationToGround;
        this.maxFoundationDepth = maxFoundationDepth;
        this.foundationBlock = foundationBlock;
        this.ignoreAir = ignoreAir;
        this.replaceAirOnly = replaceAirOnly;
        this.chestLocalPos = chestLocalPos;
        this.rollsMin = rollsMin;
        this.rollsMax = rollsMax;
        this.loot = loot;
        this.villagerSpawnLocal = villagerSpawnLocal;
        this.villagerProfessionId = villagerProfessionId;
        this.villagerTypeId = villagerTypeId;
        this.villagerLevel = villagerLevel;
    }

    public static BlacksmithHouseEvent fromJson(JsonObject obj) {
        ResourceLocation structureId = parseIdentifier(obj, "structure", "luckybreak:villager_blacksmith");

        int offsetX = obj.has("offset_x") ? obj.get("offset_x").getAsInt() : 0;
        int offsetY = obj.has("offset_y") ? obj.get("offset_y").getAsInt() : 0;
        int offsetZ = obj.has("offset_z") ? obj.get("offset_z").getAsInt() : 0;

        int originX = obj.has("origin_x") ? obj.get("origin_x").getAsInt() : 4;
        int originY = obj.has("origin_y") ? obj.get("origin_y").getAsInt() : 0;
        int originZ = obj.has("origin_z") ? obj.get("origin_z").getAsInt() : 5;
        BlockPos structureOrigin = new BlockPos(originX, originY, originZ);

        int structureSizeX = obj.has("structure_size_x") ? obj.get("structure_size_x").getAsInt() : 9;
        int structureSizeY = obj.has("structure_size_y") ? obj.get("structure_size_y").getAsInt() : 8;
        int structureSizeZ = obj.has("structure_size_z") ? obj.get("structure_size_z").getAsInt() : 11;
        if (structureSizeX < 1) structureSizeX = 1;
        if (structureSizeY < 1) structureSizeY = 1;
        if (structureSizeZ < 1) structureSizeZ = 1;
        boolean spawnInFrontOfPlayer = !obj.has("spawn_in_front_of_player") || obj.get("spawn_in_front_of_player").getAsBoolean();
        int spawnDistance = obj.has("spawn_distance") ? obj.get("spawn_distance").getAsInt() : 8;
        if (spawnDistance < 1) spawnDistance = 1;
        Direction structureFront = parseHorizontalDirection(obj, "structure_front", Direction.SOUTH);
        int doorX = obj.has("door_x") ? obj.get("door_x").getAsInt() : 6;
        int doorY = obj.has("door_y") ? obj.get("door_y").getAsInt() : 1;
        int doorZ = obj.has("door_z") ? obj.get("door_z").getAsInt() : 4;
        BlockPos doorLocalPos = new BlockPos(doorX, doorY, doorZ);
        Direction doorFacing = parseHorizontalDirection(obj, "door_facing", Direction.NORTH);
        int playerDistanceFromDoor = obj.has("player_distance_from_door") ? obj.get("player_distance_from_door").getAsInt() : 1;
        if (playerDistanceFromDoor < 1) {
            playerDistanceFromDoor = 1;
        }
        boolean clearAreaBeforePlace = !obj.has("clear_area_before_place") || obj.get("clear_area_before_place").getAsBoolean();
        boolean fillFoundationToGround = !obj.has("fill_foundation_to_ground") || obj.get("fill_foundation_to_ground").getAsBoolean();
        int maxFoundationDepth = obj.has("max_foundation_depth") ? obj.get("max_foundation_depth").getAsInt() : 20;
        if (maxFoundationDepth < 1) {
            maxFoundationDepth = 1;
        }
        ResourceLocation foundationBlockId = parseIdentifier(obj, "foundation_block", "minecraft:cobblestone");
        Block foundationBlock = BuiltInRegistries.BLOCK.getValue(foundationBlockId);
        if (foundationBlock == null || foundationBlock == Blocks.AIR) {
            foundationBlock = Blocks.COBBLESTONE;
        }

        boolean ignoreAir = !obj.has("ignore_air") || obj.get("ignore_air").getAsBoolean();
        boolean replaceAirOnly = !obj.has("replace_air_only") || obj.get("replace_air_only").getAsBoolean();

        int chestX = obj.has("chest_x") ? obj.get("chest_x").getAsInt() : 2;
        int chestY = obj.has("chest_y") ? obj.get("chest_y").getAsInt() : 1;
        int chestZ = obj.has("chest_z") ? obj.get("chest_z").getAsInt() : 6;
        BlockPos chestLocalPos = new BlockPos(chestX, chestY, chestZ);

        int rollsMin = obj.has("loot_rolls_min") ? obj.get("loot_rolls_min").getAsInt() : 4;
        int rollsMax = obj.has("loot_rolls_max") ? obj.get("loot_rolls_max").getAsInt() : 8;
        if (rollsMin < 1) rollsMin = 1;
        if (rollsMax < rollsMin) rollsMax = rollsMin;

        List<LootEntry> loot = parseLoot(obj);

        int villagerX = obj.has("villager_x") ? obj.get("villager_x").getAsInt() : 4;
        int villagerY = obj.has("villager_y") ? obj.get("villager_y").getAsInt() : 1;
        int villagerZ = obj.has("villager_z") ? obj.get("villager_z").getAsInt() : 5;
        BlockPos villagerSpawnLocal = new BlockPos(villagerX, villagerY, villagerZ);

        String villagerProfession = parseIdentifier(obj, "villager_profession", "minecraft:toolsmith").toString();
        String villagerType = parseIdentifier(obj, "villager_type", "minecraft:plains").toString();
        int villagerLevel = obj.has("villager_level") ? obj.get("villager_level").getAsInt() : 2;
        if (villagerLevel < 1) villagerLevel = 1;
        if (villagerLevel > 5) villagerLevel = 5;

        return new BlacksmithHouseEvent(
                structureId,
                offsetX,
                offsetY,
                offsetZ,
                structureOrigin,
                structureSizeX,
                structureSizeY,
                structureSizeZ,
                spawnInFrontOfPlayer,
                spawnDistance,
                structureFront,
                doorLocalPos,
                doorFacing,
                playerDistanceFromDoor,
                clearAreaBeforePlace,
                fillFoundationToGround,
                maxFoundationDepth,
                foundationBlock,
                ignoreAir,
                replaceAirOnly,
                chestLocalPos,
                rollsMin,
                rollsMax,
                List.copyOf(loot),
                villagerSpawnLocal,
                villagerProfession,
                villagerType,
                villagerLevel
        );
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        SimpleJsonStructure structure = SimpleJsonStructure.load(level, structureId);
        if (structure == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Blacksmith house structure not found: {}", structureId);
            return;
        }

        Rotation rotation = spawnInFrontOfPlayer ? rotationForDoorFacingPlayer(player) : Rotation.NONE;
        BlockPos anchor = spawnInFrontOfPlayer
            ? computeAnchorAroundPlayer(level, player, rotation)
            : pos.offset(offsetX, offsetY, offsetZ);
        if (clearAreaBeforePlace) {
            clearBuildArea(level, anchor, rotation);
        }

        boolean placed = structure.place(level, anchor, structureOrigin, ignoreAir, replaceAirOnly, new HashSet<>(), rotation);
        if (!placed) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Blacksmith house placement failed at {}", anchor);
            return;
        }

        closePlacedDoor(level, anchor, rotation);

        if (fillFoundationToGround) {
            fillFoundationToGround(level, anchor, rotation);
        }

        ensureFenceSupports(level, anchor, rotation);

        BlockPos chestPos = toWorldPos(anchor, chestLocalPos, rotation);
        fillChestLoot(level, chestPos);

        BlockPos villagerPos = toWorldPos(anchor, villagerSpawnLocal, rotation);
        spawnBlacksmithVillager(level, findNearestSafeStandPos(level, villagerPos));

        if (spawnInFrontOfPlayer) {
            placePlayerInFrontOfDoor(level, player, anchor, rotation);
        }
    }

    private BlockPos computeAnchorAroundPlayer(ServerLevel level, ServerPlayer player, Rotation rotation) {
        Direction look = player.getDirection();
        int playerX = (int) Math.floor(player.getX());
        int playerZ = (int) Math.floor(player.getZ());
        int playerY = (int) Math.floor(player.getY());

        int doorX = playerX + look.getStepX() * playerDistanceFromDoor;
        int doorZ = playerZ + look.getStepZ() * playerDistanceFromDoor;
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, doorX, doorZ);
        int doorY = Math.max(Math.max(playerY, groundY), level.getMinY() + 1) + offsetY;

        BlockPos desiredDoorPos = new BlockPos(doorX, doorY, doorZ);
        BlockPos relDoor = rotateRelative(doorLocalPos.subtract(structureOrigin), rotation);
        return desiredDoorPos.offset(-relDoor.getX(), -relDoor.getY(), -relDoor.getZ());
    }

    private Rotation rotationForDoorFacingPlayer(ServerPlayer player) {
        Direction desiredDoorFacing = player.getDirection().getOpposite();
        return rotationFromTo(doorFacing, desiredDoorFacing);
    }

    private void placePlayerInFrontOfDoor(ServerLevel level, ServerPlayer player, BlockPos anchor, Rotation rotation) {
        BlockPos worldDoor = toWorldPos(anchor, doorLocalPos, rotation);
        Direction worldDoorFacing = rotateDirection(doorFacing, rotation);
        BlockPos targetFeet = worldDoor.relative(worldDoorFacing.getOpposite(), playerDistanceFromDoor);
        BlockPos safeFeet = findNearestSafeStandPos(level, targetFeet);

        float yaw = yawForFacing(worldDoorFacing);
        player.teleportTo(
            level,
            safeFeet.getX() + 0.5,
            safeFeet.getY(),
            safeFeet.getZ() + 0.5,
            Set.of(),
            yaw,
            player.getXRot(),
            true
        );
    }

    private void closePlacedDoor(ServerLevel level, BlockPos anchor, Rotation rotation) {
        BlockPos approxDoorPos = toWorldPos(anchor, doorLocalPos, rotation);
        BlockPos lowerDoorPos = resolveDoorLowerPos(level, approxDoorPos);
        if (lowerDoorPos == null) {
            return;
        }

        BlockState lower = level.getBlockState(lowerDoorPos);
        if (lower.hasProperty(DoorBlock.OPEN)) {
            level.setBlock(lowerDoorPos, lower.setValue(DoorBlock.OPEN, false), 3);
        }

        BlockPos upperDoorPos = lowerDoorPos.above();
        BlockState upper = level.getBlockState(upperDoorPos);
        if (upper.hasProperty(DoorBlock.OPEN)) {
            level.setBlock(upperDoorPos, upper.setValue(DoorBlock.OPEN, false), 3);
        }
    }

    private void ensureFenceSupports(ServerLevel level, BlockPos anchor, Rotation rotation) {
        BlockPos[] fenceLocals = new BlockPos[] {
                new BlockPos(3, 1, 3),
                new BlockPos(7, 1, 6),
                new BlockPos(7, 1, 10)
        };

        for (BlockPos localFence : fenceLocals) {
            BlockPos worldFence = toWorldPos(anchor, localFence, rotation);
            BlockPos below = worldFence.below();
            if (level.getBlockState(below).isAir()) {
                level.setBlock(below, Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }
    }

    private BlockPos resolveDoorLowerPos(ServerLevel level, BlockPos approxDoorPos) {
        BlockPos[] candidates = new BlockPos[]{approxDoorPos, approxDoorPos.below(), approxDoorPos.above()};
        for (BlockPos candidate : candidates) {
            BlockState state = level.getBlockState(candidate);
            if (!(state.getBlock() instanceof DoorBlock) || !state.hasProperty(DoorBlock.HALF)) {
                continue;
            }

            DoubleBlockHalf half = state.getValue(DoorBlock.HALF);
            return half == DoubleBlockHalf.UPPER ? candidate.below() : candidate;
        }
        return null;
    }

    private static Rotation rotationFromTo(Direction from, Direction to) {
        int fromStep = horizontalSteps(from);
        int toStep = horizontalSteps(to);
        int delta = Math.floorMod(toStep - fromStep, 4);
        return switch (delta) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static int horizontalSteps(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private static Direction rotateDirection(Direction direction, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> direction.getClockWise();
            case CLOCKWISE_180 -> direction.getOpposite();
            case COUNTERCLOCKWISE_90 -> direction.getCounterClockWise();
            case NONE -> direction;
        };
    }

    private static float yawForFacing(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case NORTH -> 180.0f;
            case EAST -> -90.0f;
            default -> 0.0f;
        };
    }

    private BlockPos findNearestSafeStandPos(ServerLevel level, BlockPos preferredFeet) {
        if (isSafeStandPos(level, preferredFeet)) {
            return preferredFeet;
        }

        for (int radius = 1; radius <= 2; radius++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockPos up = preferredFeet.offset(dx, dy, dz);
                        if (isSafeStandPos(level, up)) {
                            return up;
                        }

                        BlockPos down = preferredFeet.offset(dx, -dy, dz);
                        if (isSafeStandPos(level, down)) {
                            return down;
                        }
                    }
                }
            }
        }

        return preferredFeet;
    }

    private boolean isSafeStandPos(ServerLevel level, BlockPos feetPos) {
        if (feetPos.getY() <= level.getMinY()) {
            return false;
        }

        BlockPos below = feetPos.below();
        BlockPos head = feetPos.above();
        boolean solidBelow = level.getBlockState(below).isSolidRender();
        boolean feetClear = level.getBlockState(feetPos).canBeReplaced();
        boolean headClear = level.getBlockState(head).canBeReplaced();
        return solidBelow && feetClear && headClear;
    }

    private BlockPos toWorldPos(BlockPos anchor, BlockPos localPos, Rotation rotation) {
        BlockPos relative = localPos.subtract(structureOrigin);
        BlockPos rotated = rotateRelative(relative, rotation);
        return anchor.offset(rotated);
    }

    private void fillChestLoot(ServerLevel level, BlockPos chestPos) {
        if (!level.getBlockState(chestPos).is(Blocks.CHEST)) {
            return;
        }

        var be = level.getBlockEntity(chestPos);
        if (!(be instanceof Container container)) {
            return;
        }

        RandomSource random = level.getRandom();
        int rolls = rollsMin + random.nextInt(rollsMax - rollsMin + 1);

        for (int i = 0; i < rolls; i++) {
            LootEntry entry = chooseWeightedLoot(random);
            if (entry == null) {
                break;
            }

            int count = entry.countMin + random.nextInt(entry.countMax - entry.countMin + 1);
            ItemStack stack = new ItemStack(entry.item, count);
            if (!placeItemInRandomEmptySlot(container, random, stack)) {
                break;
            }
        }
    }

    private boolean placeItemInRandomEmptySlot(Container container, RandomSource random, ItemStack stack) {
        int size = container.getContainerSize();
        int slot = random.nextInt(size);
        int tries = 0;
        while (!container.getItem(slot).isEmpty() && tries < size) {
            slot = (slot + 1) % size;
            tries++;
        }
        if (!container.getItem(slot).isEmpty()) {
            return false;
        }
        container.setItem(slot, stack);
        return true;
    }

    private LootEntry chooseWeightedLoot(RandomSource random) {
        double total = 0.0;
        for (LootEntry entry : loot) {
            total += entry.weight;
        }
        if (total <= 0.0) {
            return null;
        }

        double roll = random.nextDouble() * total;
        for (LootEntry entry : loot) {
            roll -= entry.weight;
            if (roll <= 0.0) {
                return entry;
            }
        }
        return loot.get(loot.size() - 1);
    }

    private void spawnBlacksmithVillager(ServerLevel level, BlockPos villagerPos) {
        String command = String.format(
                "summon minecraft:villager %.2f %.2f %.2f {NoAI:0b,PersistenceRequired:1b,VillagerData:{type:\"%s\",profession:\"%s\",level:%d}}",
                villagerPos.getX() + 0.5,
                (double) villagerPos.getY(),
                villagerPos.getZ() + 0.5,
                villagerTypeId,
                villagerProfessionId,
                villagerLevel
        );
        level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level),
                command
        );
    }

    private void fillFoundationToGround(ServerLevel level, BlockPos anchor, Rotation rotation) {
        int footprintY = anchor.getY() - structureOrigin.getY();
        int[] bounds = footprintBoundsXZ(anchor, rotation);
        int minX = bounds[0];
        int maxX = bounds[1];
        int minZ = bounds[2];
        int maxZ = bounds[3];

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos below = new BlockPos(x, footprintY - 1, z);
                for (int depth = 0; depth < maxFoundationDepth; depth++) {
                    if (below.getY() <= level.getMinY()) {
                        break;
                    }

                    if (level.getBlockState(below).isSolidRender()) {
                        break;
                    }

                    level.setBlock(below, foundationBlock.defaultBlockState(), 3);
                    below = below.below();
                }
            }
        }
    }

    private void clearBuildArea(ServerLevel level, BlockPos anchor, Rotation rotation) {
        int[] bounds = footprintBoundsXZ(anchor, rotation);
        int minX = bounds[0] + 1;
        int maxX = bounds[1] - 1;
        int minZ = bounds[2] + 1;
        int maxZ = bounds[3] - 1;
        int minY = anchor.getY() - structureOrigin.getY();

        // Keep a 1-block perimeter ring untouched to avoid carving a moat-like air border.
        if (minX > maxX || minZ > maxZ) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = 0; y < structureSizeY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    level.setBlock(new BlockPos(x, minY + y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private int[] footprintBoundsXZ(BlockPos anchor, Rotation rotation) {
        BlockPos c1 = toWorldPos(anchor, new BlockPos(0, 0, 0), rotation);
        BlockPos c2 = toWorldPos(anchor, new BlockPos(structureSizeX - 1, 0, 0), rotation);
        BlockPos c3 = toWorldPos(anchor, new BlockPos(0, 0, structureSizeZ - 1), rotation);
        BlockPos c4 = toWorldPos(anchor, new BlockPos(structureSizeX - 1, 0, structureSizeZ - 1), rotation);

        int minX = Math.min(Math.min(c1.getX(), c2.getX()), Math.min(c3.getX(), c4.getX()));
        int maxX = Math.max(Math.max(c1.getX(), c2.getX()), Math.max(c3.getX(), c4.getX()));
        int minZ = Math.min(Math.min(c1.getZ(), c2.getZ()), Math.min(c3.getZ(), c4.getZ()));
        int maxZ = Math.max(Math.max(c1.getZ(), c2.getZ()), Math.max(c3.getZ(), c4.getZ()));
        return new int[]{minX, maxX, minZ, maxZ};
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

    private static Direction parseHorizontalDirection(JsonObject obj, String field, Direction fallback) {
        if (!obj.has(field)) {
            return fallback;
        }

        try {
            Direction direction = Direction.byName(obj.get(field).getAsString().toLowerCase());
            if (direction != null && direction.getAxis().isHorizontal()) {
                return direction;
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private static List<LootEntry> parseLoot(JsonObject obj) {
        List<LootEntry> list = new ArrayList<>();
        if (obj.has("loot") && obj.get("loot").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("loot");
            for (JsonElement element : arr) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject e = element.getAsJsonObject();
                if (!e.has("item")) {
                    continue;
                }

                ResourceLocation itemId;
                try {
                    itemId = ResourceLocation.parse(e.get("item").getAsString());
                } catch (Exception ignored) {
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.getValue(itemId);
                if (item == null || item == Items.AIR) {
                    continue;
                }

                double weight = e.has("weight") ? e.get("weight").getAsDouble() : 1.0;
                int countMin = e.has("count_min") ? e.get("count_min").getAsInt() : 1;
                int countMax = e.has("count_max") ? e.get("count_max").getAsInt() : countMin;

                if (weight <= 0.0) {
                    continue;
                }
                if (countMin < 1) countMin = 1;
                if (countMax < countMin) countMax = countMin;

                list.add(new LootEntry(item, weight, countMin, countMax));
            }
        }

        if (list.isEmpty()) {
            list.add(new LootEntry(Items.IRON_INGOT, 10.0, 3, 10));
            list.add(new LootEntry(Items.COAL, 8.0, 4, 16));
            list.add(new LootEntry(Items.BREAD, 6.0, 1, 4));
            list.add(new LootEntry(Items.IRON_PICKAXE, 3.0, 1, 1));
            list.add(new LootEntry(Items.IRON_SWORD, 2.0, 1, 1));
            list.add(new LootEntry(Items.EMERALD, 2.0, 1, 3));
        }

        return list;
    }

    private static ResourceLocation parseIdentifier(JsonObject obj, String field, String fallback) {
        try {
            if (!obj.has(field)) {
                return ResourceLocation.parse(fallback);
            }
            return ResourceLocation.parse(obj.get(field).getAsString());
        } catch (Exception ignored) {
            return ResourceLocation.parse(fallback);
        }
    }

}
