package com.luckybreak.mixin;

import com.luckybreak.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHooksMixin {

    @Inject(method = "die", at = @At("TAIL"))
    private void luckybreak$onDie(DamageSource source, CallbackInfo ci) {
        ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath((LivingEntity) (Object) this, source);
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void luckybreak$onActuallyHurt(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(
                (LivingEntity) (Object) this, source, amount, amount, false
        );
    }
}
