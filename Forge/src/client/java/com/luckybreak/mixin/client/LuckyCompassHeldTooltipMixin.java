package com.luckybreak.mixin.client;

import com.luckybreak.tooltip.TooltipConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class LuckyCompassHeldTooltipMixin {

    private static final int MIN_HELD_TOOLTIP_GAP_PX = 12;

    @Shadow
    private int toolHighlightTimer;

    @Shadow
    private ItemStack lastToolHighlight;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderSelectedItemName", at = @At("TAIL"))
    private void luckybreak$renderCompassHint(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (this.toolHighlightTimer <= 0 || this.lastToolHighlight.isEmpty()) {
            return;
        }

        Font font = this.minecraft.font;
        int alpha = (int) ((float) this.toolHighlightTimer * 256.0F / 10.0F);
        if (alpha > 255) {
            alpha = 255;
        }

        if (alpha <= 0) {
            return;
        }

        Component hint = TooltipConfig.getHeldTooltipFor(this.lastToolHighlight);
        if (hint == null) {
            return;
        }

        int x = (guiGraphics.guiWidth() - font.width(hint)) / 2;
        int vanillaNameY = guiGraphics.guiHeight() - 49;
        int configuredGap = Math.max(MIN_HELD_TOOLTIP_GAP_PX, TooltipConfig.getHeldYOffset());
        int y = Math.max(2, vanillaNameY - font.lineHeight - configuredGap);
        int rgb = TooltipConfig.getHeldColorFor(this.lastToolHighlight) & 0xFFFFFF;
        int color = rgb | (alpha << 24);
        guiGraphics.drawString(font, hint, x, y, color);
    }
}
