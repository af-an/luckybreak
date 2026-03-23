package com.luckybreak.client.render;

import com.luckybreak.entity.GoldenHenConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.ResourceLocation;

public class GoldenHenGlintLayer extends EnergySwirlLayer<ChickenRenderState, ChickenModel> {

    private final ChickenModel glintModelAdult;
    private final ChickenModel glintModelBaby;
    private ChickenModel activeModel;

    public GoldenHenGlintLayer(RenderLayerParent<ChickenRenderState, ChickenModel> parent,
                                ChickenModel glintModelAdult, ChickenModel glintModelBaby) {
        super(parent);
        this.glintModelAdult = glintModelAdult;
        this.glintModelBaby = glintModelBaby;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       ChickenRenderState state, float f, float g) {
        if (!GoldenHenConfig.goldenHenGlintEnabled()) {
            return;
        }

        this.activeModel = state.isBaby ? this.glintModelBaby : this.glintModelAdult;
        super.render(poseStack, bufferSource, packedLight, state, f, g);
    }

    @Override protected boolean isPowered(ChickenRenderState state) { return GoldenHenConfig.goldenHenGlintEnabled(); }
    @Override protected float xOffset(float ageInTicks) { return ageInTicks * 0.01f; }
    @Override protected ResourceLocation getTextureLocation() { return GoldenHenConfig.goldenHenGlintTexture(); }
    @Override protected ChickenModel model() {
        return this.activeModel != null ? this.activeModel : getParentModel();
    }
}
