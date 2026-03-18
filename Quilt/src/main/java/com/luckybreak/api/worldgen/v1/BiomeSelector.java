package com.luckybreak.api.worldgen.v1;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

@FunctionalInterface
public interface BiomeSelector {
    boolean isBiomeIncluded(Holder<Biome> biome);
}
