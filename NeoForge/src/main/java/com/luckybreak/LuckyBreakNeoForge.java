package com.luckybreak;

import net.neoforged.fml.common.Mod;

@Mod(LuckyBreak.MOD_ID)
public final class LuckyBreakNeoForge {

    public LuckyBreakNeoForge() {
        new LuckyBreak().onInitialize();
    }
}
