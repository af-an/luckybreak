package com.luckybreak.mixin;

import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(BiomeGenerationSettings.class)
public interface BiomeGenerationSettingsInvoker {

    @Invoker("<init>")
    static BiomeGenerationSettings luckybreak$create(
            HolderSet<ConfiguredWorldCarver<?>> carvers,
            List<HolderSet<PlacedFeature>> features
    ) {
        throw new UnsupportedOperationException();
    }
}
