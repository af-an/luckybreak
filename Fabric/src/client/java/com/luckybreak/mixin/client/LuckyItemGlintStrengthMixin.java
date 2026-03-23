package com.luckybreak.mixin.client;

import com.luckybreak.LuckyBreak;
import com.luckybreak.item.LuckyItemVisualConfig;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class LuckyItemGlintStrengthMixin {

    @Inject(method = "appendItemLayers", at = @At("TAIL"))
    private void luckybreak$useSpecialFoilForLuckyItems(ItemStackRenderState renderState,
                                                         ItemStack stack,
                                                         ItemDisplayContext displayContext,
                                                         Level level,
                                                         LivingEntity itemOwner,
                                                         int seed,
                                                         CallbackInfo ci) {
        if (!stack.hasFoil()) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!itemId.getNamespace().equals(LuckyBreak.MOD_ID)) {
            return;
        }

        if (LuckyItemVisualConfig.forItem(itemId.getPath()).glintStrength() <= 1.0F) {
            return;
        }

        if (!(renderState instanceof ItemStackRenderStateAccessor accessor)) {
            return;
        }

        ItemStackRenderState.LayerRenderState layer = accessor.luckybreak$firstLayer();
        if (layer != null) {
            layer.setFoilType(ItemStackRenderState.FoilType.SPECIAL);
        }
    }
}
