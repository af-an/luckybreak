package com.luckybreak.mixin;

import com.luckybreak.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelHooksMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void luckybreak$onTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerTickEvents.END_WORLD_TICK.invoker().onEndTick((ServerLevel) (Object) this);
    }
}
