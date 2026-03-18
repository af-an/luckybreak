package com.luckybreak.block;

import com.luckybreak.events.LuckyEventRegistry;
import com.luckybreak.events.LuckyScheduler;
import com.luckybreak.world.LuckyBlockTracker;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Collections;
import java.util.List;

public class LuckyBlock extends Block {

    public LuckyBlock(ResourceKey<Block> key) {
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).setId(key));
    }

    // -------------------------------------------------------------------------
    // Tracker hooks
    // -------------------------------------------------------------------------

    /**
     * Called when this block is placed in the world (by a player, dispenser,
     * or world generation). Registers the position with LuckyBlockTracker so
     * the Lucky Compass can find it.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            LuckyBlockTracker.get(serverLevel).add(pos);
        }
    }

    /**
     * Called server-side after this block has been removed from the level.
     * Unregisters the position from LuckyBlockTracker.
     * This is the 1.21.11 replacement for the removed onRemove() hook.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level,
                                               BlockPos pos, boolean notify) {
        super.affectNeighborsAfterRemoval(state, level, pos, notify);
        LuckyBlockTracker.get(level).remove(pos);

        // Reset any Lucky Compass held by a player that was locked onto this block.
        GlobalPos removedPos = GlobalPos.of(level.dimension(), pos);
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            resetCompassIfTracking(player, removedPos);
        }
    }

    /** Clears the LodestoneTracker from any Lucky Compass in this player's hands
     *  or inventory that was pointing at {@code removedPos}. */
    private static void resetCompassIfTracking(
            net.minecraft.server.level.ServerPlayer player, GlobalPos removedPos) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof com.luckybreak.item.LuckyCompassItem) {
                LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                if (tracker != null
                        && tracker.target().isPresent()
                        && tracker.target().get().equals(removedPos)) {
                    com.luckybreak.item.LuckyCompassItem.resetCompass(stack);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Event trigger
    // -------------------------------------------------------------------------

    /**
     * Called after the player has broken the block. Triggers a random lucky event
     * unless the tool had Silk Touch.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && level instanceof ServerLevel serverLevel
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && serverPlayer.isCreative()
                && LuckyEventRegistry.INSTANCE.triggerEventsInCreative()) {
            ItemStack tool = serverPlayer.getMainHandItem();
            if (!hasSilkTouch(tool)) {
                BlockPos triggerPos = pos.immutable();
                LuckyScheduler.INSTANCE.schedule(1, () -> {
                    if (serverPlayer.isRemoved() || serverPlayer.level() != serverLevel) {
                        return;
                    }
                    triggerLuckyEvent(serverLevel, triggerPos, serverPlayer);
                });
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Survival/adventure trigger path. Creative is handled in playerWillDestroy().
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (serverPlayer.isCreative()) {
                return;
            }
            if (!hasSilkTouch(tool)) {
                triggerLuckyEvent(serverLevel, pos, serverPlayer);
            }
        }
    }

    protected void triggerLuckyEvent(ServerLevel level, BlockPos pos,
                                     net.minecraft.server.level.ServerPlayer player) {
        LuckyEventRegistry.INSTANCE.trigger(level, pos, player);
    }

    protected boolean hasSilkTouch(ItemStack tool) {
        ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(Enchantments.SILK_TOUCH)) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Drops
    // -------------------------------------------------------------------------

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && !tool.isEmpty()) {
            ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (entry.getKey().is(Enchantments.SILK_TOUCH)) {
                    return List.of(new ItemStack(this));
                }
            }
        }
        return Collections.emptyList();
    }
}
