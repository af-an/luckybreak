package com.luckybreak.api.worldgen.v1;

import net.minecraft.tags.BiomeTags;

public final class BiomeSelectors {

    private BiomeSelectors() {}

    public static BiomeSelector foundInOverworld() {
        return biome -> !biome.is(BiomeTags.IS_NETHER) && !biome.is(BiomeTags.IS_END);
    }

    public static BiomeSelector foundInTheNether() {
        return biome -> biome.is(BiomeTags.IS_NETHER);
    }

    public static BiomeSelector foundInTheEnd() {
        return biome -> biome.is(BiomeTags.IS_END);
    }
}
