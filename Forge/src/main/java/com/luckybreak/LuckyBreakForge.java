package com.luckybreak;

import com.luckybreak.world.feature.ModFeatures;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LuckyBreak.MOD_ID)
public final class LuckyBreakForge {

    public LuckyBreakForge() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(ModBlocks.ForgeEvents::onRegisterBlocks);
        modEventBus.addListener(ModBlocks.ForgeEvents::onRegisterItems);
        modEventBus.addListener(ModItems.ForgeEvents::onRegisterItems);
        modEventBus.addListener(ModEntities.ForgeEvents::onRegisterEntities);
        modEventBus.addListener(ModEntities.ForgeEvents::onEntityAttributes);
        modEventBus.addListener(ModFeatures.ForgeEvents::onRegisterFeatures);
        modEventBus.addListener(LuckyBreak.ForgeEvents::onRegisterCreativeTabs);
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
