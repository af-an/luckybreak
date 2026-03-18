package com.luckybreak.mixin.client;

import com.luckybreak.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderers.class)
public abstract class EntityRenderersBootstrapMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void luckybreak$bootstrapEntityRenderers(CallbackInfo ci) {
        EntityRendererRegistry.bootstrap();
    }
}
