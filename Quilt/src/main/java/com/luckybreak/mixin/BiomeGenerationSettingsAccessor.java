package com.luckybreak.mixin;

import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(BiomeGenerationSettings.class)
public interface BiomeGenerationSettingsAccessor {

    @Accessor("carvers")
    HolderSet<ConfiguredWorldCarver<?>> luckybreak$getCarvers();

    @Accessor("features")
    @Mutable
    @Final
    void luckybreak$setFeatures(List<HolderSet<PlacedFeature>> features);
}
