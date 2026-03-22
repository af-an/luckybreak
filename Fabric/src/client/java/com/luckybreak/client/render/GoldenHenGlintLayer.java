package com.luckybreak.client.render;

import com.luckybreak.entity.GoldenHenConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class GoldenHenGlintLayer extends EnergySwirlLayer<ChickenRenderState, ChickenModel> {

    // Dedicated models with legs permanently invisible — avoids deferred-render race condition
    private final ChickenModel glintModelAdult;
    private final ChickenModel glintModelBaby;

    public GoldenHenGlintLayer(RenderLayerParent<ChickenRenderState, ChickenModel> parent,
                                ChickenModel glintModelAdult, ChickenModel glintModelBaby) {
        super(parent);
        this.glintModelAdult = glintModelAdult;
        this.glintModelBaby  = glintModelBaby;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       ChickenRenderState state, float f, float g) {
        if (!GoldenHenConfig.goldenHenGlintEnabled()) return;

        float ageInTicks = state.ageInTicks;
        float speed = GoldenHenConfig.goldenHenGlintPulseSpeed();
        float wave = (float) ((Math.sin(ageInTicks * speed) + 1.0) * 0.5);
        float alpha = GoldenHenConfig.goldenHenGlintMinAlpha()
                + (GoldenHenConfig.goldenHenGlintMaxAlpha() - GoldenHenConfig.goldenHenGlintMinAlpha()) * wave;
        int alphaByte = Mth.clamp((int) (alpha * 255f), 0, 255);
        if (alphaByte <= 0) return;

        int argb = (alphaByte << 24) | (GoldenHenConfig.goldenHenGlintColor() & 0xFFFFFF);
        float xOff = (ageInTicks * 0.01f) % 1.0f;
        float yOff = (ageInTicks * 0.006f) % 1.0f;

        // Sync the correct glint model's animation pose to match the hen's current state
        ChickenModel glintModel = state.isBaby ? glintModelBaby : glintModelAdult;
        glintModel.setupAnim(state);

        collector.order(1).submitModel(
                glintModel, state, poseStack,
                RenderType.energySwirl(getTextureLocation(), xOff, yOff),
                packedLight, OverlayTexture.NO_OVERLAY, argb, null);
    }

    // Abstract method implementations (not called — submit() is fully overridden above)
    @Override protected boolean isPowered(ChickenRenderState state) { return true; }
    @Override protected float xOffset(float ageInTicks) { return ageInTicks * 0.01f; }
    @Override protected ResourceLocation getTextureLocation() { return GoldenHenConfig.goldenHenGlintTexture(); }
    @Override protected ChickenModel model() { return getParentModel(); }
}
