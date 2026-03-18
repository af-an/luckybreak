package com.luckybreak.block;

import com.luckybreak.events.LuckyEventRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

/**
 * Lucky block variant that triggers a configured weighted tier profile.
 */
public class WeightedTierLuckyBlock extends LuckyBlock {

    public enum Profile {
        MOSTLY_LUCKY,
        MOSTLY_UNLUCKY
    }

    private final Profile profile;

    public WeightedTierLuckyBlock(ResourceKey<Block> key, Profile profile) {
        super(key);
        this.profile = profile;
    }

    @Override
    protected void triggerLuckyEvent(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (profile == Profile.MOSTLY_LUCKY) {
            LuckyEventRegistry.INSTANCE.triggerMostlyLucky(level, pos, player);
            return;
        }
        LuckyEventRegistry.INSTANCE.triggerMostlyUnlucky(level, pos, player);
    }
}
