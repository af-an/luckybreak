package com.luckybreak.world.feature;

import com.luckybreak.LuckyBreak;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class ModFeatures {

    public static final Feature<SimpleJsonStructureFeatureConfig> SIMPLE_JSON_STRUCTURE =
            new SimpleJsonStructureFeature(SimpleJsonStructureFeatureConfig.CODEC);

    private ModFeatures() {
    }

    public static void register() {
        Registry.register(
                BuiltInRegistries.FEATURE,
                Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "simple_json_structure"),
                SIMPLE_JSON_STRUCTURE
        );
    }
}
