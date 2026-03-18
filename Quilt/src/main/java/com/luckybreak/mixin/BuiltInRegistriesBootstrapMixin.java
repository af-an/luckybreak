package com.luckybreak.mixin;

import com.luckybreak.LuckyBreak;
import com.luckybreak.ModEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltInRegistries.class)
public abstract class BuiltInRegistriesBootstrapMixin {

    // Inject BETWEEN createContents() and freeze() — registries are populated but not yet frozen,
    // so Registry.register() and Block.<init> createIntrusiveHolder() both work correctly.
    @Inject(method = "bootStrap", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/registries/BuiltInRegistries;freeze()V"
    ))
    private static void luckybreak$bootstrap(CallbackInfo ci) {
        LuckyBreak.initialize();
    }

    @Inject(method = "bootStrap", at = @At("RETURN"))
    private static void luckybreak$registerEntityAttributes(CallbackInfo ci) {
        ModEntities.ensureRuntimeSetup();
    }
}
