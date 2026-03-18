package com.luckybreak.mixin;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Biome.class)
public interface BiomeAccessor {

    @Accessor("generationSettings")
    @Mutable
    @Final
    void luckybreak$setGenerationSettings(BiomeGenerationSettings generationSettings);
}
