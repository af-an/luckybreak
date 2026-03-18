package com.luckybreak.mixin;

import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeTabs.class)
public abstract class CreativeModeTabsMixin {

    // Cancel vanilla duplicate-position check; pagination is handled by the screen mixin
    @Inject(method = "validate", at = @At("HEAD"), cancellable = true)
    private static void luckybreak$skipValidate(CallbackInfo ci) {
        ci.cancel();
    }
}