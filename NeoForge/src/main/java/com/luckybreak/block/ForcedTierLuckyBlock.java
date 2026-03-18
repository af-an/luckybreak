package com.luckybreak.block;

import com.luckybreak.ModBlocks;
import com.luckybreak.events.LuckyEventRegistry;
import com.luckybreak.events.LuckyTier;
import com.luckybreak.world.ForcedLuckyPairManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Collections;
import java.util.List;

public class ForcedTierLuckyBlock extends LuckyBlock {

    private final LuckyTier forcedTier;

    public ForcedTierLuckyBlock(ResourceKey<Block> key, LuckyTier forcedTier) {
        super(key);
        this.forcedTier = forcedTier;
    }

    @Override
    protected void triggerLuckyEvent(ServerLevel level, net.minecraft.core.BlockPos pos, ServerPlayer player) {
        LuckyEventRegistry.INSTANCE.triggerTier(level, pos, player, forcedTier);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos other = ForcedLuckyPairManager.consumeOther(serverLevel, pos);
            if (other == null) {
                other = findFallbackCounterpart(serverLevel, pos, state);
            }
            if (other != null) {
                serverLevel.setBlock(other, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level,
                                               BlockPos pos, boolean notify) {
        super.affectNeighborsAfterRemoval(state, level, pos, notify);
        ForcedLuckyPairManager.clear(level, pos);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && !tool.isEmpty() && hasSilkTouch(tool)) {
            return List.of(new ItemStack(this));
        }
        return Collections.emptyList();
    }

    private BlockPos findFallbackCounterpart(ServerLevel level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int distance = 2; distance <= 32; distance += 2) {
                BlockPos candidatePos = pos.relative(direction, distance);
                BlockState candidateState = level.getBlockState(candidatePos);
                if (!isOppositeVariant(state, candidateState)) {
                    continue;
                }

                BlockPos middle = pos.relative(direction, distance / 2);
                if (level.getBlockEntity(middle) instanceof SignBlockEntity) {
                    return candidatePos;
                }
            }
        }
        return null;
    }

    private boolean isOppositeVariant(BlockState brokenState, BlockState otherState) {
        Block broken = brokenState.getBlock();
        Block other = otherState.getBlock();
        if (broken == ModBlocks.VERY_LUCKY_BLOCK && other == ModBlocks.VERY_UNLUCKY_BLOCK) {
            return true;
        }
        return broken == ModBlocks.VERY_UNLUCKY_BLOCK && other == ModBlocks.VERY_LUCKY_BLOCK;
    }
}
