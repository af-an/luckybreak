package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luckybreak.ModBlocks;
import com.luckybreak.events.LuckyEvent;
import com.luckybreak.events.LuckyScheduler;
import com.luckybreak.world.ForcedLuckyPairManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;

/**
 * Spawns a very lucky and very unlucky block with a sign in the middle.
 * Breaking one makes the other disappear.
 *
 * JSON fields:
 *   spacing: 3   (number of air blocks between lucky/unlucky blocks, default 3)
 *   sign_lines: ["One is Lucky,", "One is Not,", "Choose Wisely.", ""]
 *   clear_height: 3   (vertical air clearance for structure spawn)
 */
public class ChooseWiselyEvent implements LuckyEvent {

    private static final String[] DEFAULT_SIGN_LINES = {
            "One is Lucky,",
            "One is Not,",
            "Choose Wisely.",
            ""
    };

    private final int spacing;
    private final String[] signLines;
    private final int clearHeight;

    private ChooseWiselyEvent(int spacing, String[] signLines, int clearHeight) {
        this.spacing = spacing;
        this.signLines = signLines;
        this.clearHeight = clearHeight;
    }

    public static ChooseWiselyEvent fromJson(JsonObject obj) {
        int spacing = obj.has("spacing") ? obj.get("spacing").getAsInt() : 3;
        if (spacing < 1) {
            spacing = 1;
        }
        int clearHeight = obj.has("clear_height") ? obj.get("clear_height").getAsInt() : 3;
        if (clearHeight < 1) {
            clearHeight = 1;
        }

        String[] lines = DEFAULT_SIGN_LINES.clone();
        if (obj.has("sign_lines")) {
            JsonArray array = obj.getAsJsonArray("sign_lines");
            for (int i = 0; i < Math.min(4, array.size()); i++) {
                lines[i] = array.get(i).getAsString();
            }
        }

        return new ChooseWiselyEvent(spacing, lines, clearHeight);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Direction facing = player.getDirection();
        if (facing.getAxis().isVertical()) {
            facing = Direction.NORTH;
        }
        Direction line = facing.getClockWise();

        int offset = spacing / 2 + 1;
        BlockPos center = pos;
        BlockPos sideA = center.relative(line, offset);
        BlockPos sideB = center.relative(line.getOpposite(), offset);

        // Clear a compact area so sign and both blocks can always appear.
        clearSpawnArea(level, center, line, offset, clearHeight);

        RandomSource random = level.getRandom();
        boolean luckyOnSideA = random.nextBoolean();
        BlockPos luckyPos = luckyOnSideA ? sideA : sideB;
        BlockPos unluckyPos = luckyOnSideA ? sideB : sideA;

        level.setBlock(luckyPos, ModBlocks.VERY_LUCKY_BLOCK.defaultBlockState(), 3);
        level.setBlock(unluckyPos, ModBlocks.VERY_UNLUCKY_BLOCK.defaultBlockState(), 3);
        ForcedLuckyPairManager.registerPair(level, luckyPos, unluckyPos);

        Direction signFacing = getDirectionTowardPlayer(center, player, facing.getOpposite());
        int rotation = RotationSegment.convertToSegment(signFacing);
        level.setBlock(center, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation), 3);

        // Apply sign text one tick later to ensure block entity exists reliably in all trigger paths.
        LuckyScheduler.INSTANCE.schedule(1, () -> applySignText(level, center));
    }

    private void clearSpawnArea(ServerLevel level, BlockPos center, Direction line, int offset, int height) {
        Direction perp = line.getClockWise();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int longitudinal = -offset; longitudinal <= offset; longitudinal++) {
            for (int lateral = -1; lateral <= 1; lateral++) {
                BlockPos base = center.relative(line, longitudinal).relative(perp, lateral);
                for (int y = 0; y < height; y++) {
                    level.setBlock(base.above(y), air, 3);
                }
            }
        }
    }

    private void applySignText(ServerLevel level, BlockPos center) {
        if (!(level.getBlockEntity(center) instanceof SignBlockEntity sign)) {
            return;
        }

        SignText text = new SignText();
        for (int i = 0; i < 4; i++) {
            text = text.setMessage(i, Component.literal(signLines[i]));
        }

        sign.setText(text, true);
        sign.setChanged();
        BlockState state = level.getBlockState(center);
        level.sendBlockUpdated(center, state, state, 3);
    }

    private Direction getDirectionTowardPlayer(BlockPos center, ServerPlayer player, Direction fallback) {
        int dx = player.blockPosition().getX() - center.getX();
        int dz = player.blockPosition().getZ() - center.getZ();

        if (Math.abs(dx) > Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(dz) > 0) {
            return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return fallback;
    }
}
