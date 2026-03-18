package com.luckybreak;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(LuckyBreak.MOD_ID)
public final class LuckyBreakForge {

    public LuckyBreakForge() {
        new LuckyBreak().onInitialize();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> LuckyBreakClientBootstrap::new);
    }

    private static final class LuckyBreakClientBootstrap implements DistExecutor.SafeRunnable {
        @Override
        public void run() {
            new LuckyBreakClient().onInitializeClient();
        }
    }
}
