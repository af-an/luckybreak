package com.luckybreak.client.render;

import com.luckybreak.LuckyBreak;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.ResourceLocation;

public class GoldenHenRenderer extends ChickenRenderer {

        private static final ResourceLocation GOLDEN_HEN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LuckyBreak.MOD_ID,
            "textures/entity/chicken/golden_hen.png"
    );

    public GoldenHenRenderer(EntityRendererProvider.Context context) {
        super(context);

        // Dedicated models for the glint overlay with legs permanently hidden,
        // so the glint never appears on the feet regardless of render deferral.
        ChickenModel glintModelAdult = new ChickenModel(context.bakeLayer(ModelLayers.CHICKEN));
        glintModelAdult.root().getChild("right_leg").visible = false;
        glintModelAdult.root().getChild("left_leg").visible = false;

        ChickenModel glintModelBaby = new ChickenModel(context.bakeLayer(ModelLayers.CHICKEN_BABY));
        glintModelBaby.root().getChild("right_leg").visible = false;
        glintModelBaby.root().getChild("left_leg").visible = false;

        this.addLayer(new GoldenHenGlintLayer(this, glintModelAdult, glintModelBaby));
    }

    @Override
    public ResourceLocation getTextureLocation(ChickenRenderState chickenRenderState) {
        return GOLDEN_HEN_TEXTURE;
    }
}
