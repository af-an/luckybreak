package com.luckybreak.mixin;

import com.luckybreak.item.LuckyBowEntityHitEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void luckybreak$onHitEntity(EntityHitResult hitResult, CallbackInfo ci) {
        if (!(hitResult.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        LuckyBowEntityHitEffects.handleArrowHitEntity((AbstractArrow) (Object) this, livingEntity);
    }
}
