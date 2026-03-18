package com.luckybreak.mixin.client;

import com.luckybreak.LuckyBreakClient;
import com.luckybreak.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientHooksMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void luckybreak$ensureClientInit(CallbackInfo ci) {
        // Quilt Loader does not automatically trigger client_init entrypoints without QSL/QFAPI.
        // Calling initializeClient() here guarantees all client-side registrations run on the
        // first tick. The initialized guard inside makes this a no-op on every subsequent call.
        LuckyBreakClient.initializeClient();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void luckybreak$onTick(CallbackInfo ci) {
        ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick((Minecraft) (Object) this);
    }
}
