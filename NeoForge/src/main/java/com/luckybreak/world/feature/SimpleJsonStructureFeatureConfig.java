package com.luckybreak.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public record SimpleJsonStructureFeatureConfig(
        Identifier structure,
        int originX,
        int originY,
        int originZ,
        boolean ignoreAir,
        boolean replaceAirOnly,
        List<Identifier> replaceBlocksWithAir,
        List<Identifier> requiredFloorBlocks,
        int requiredAirBlocksAbove,
        int minDistanceFromSameStructure,
        boolean generateSupportPillars,
        boolean clearOccupiedSpace,
        int searchDownDepth
) implements FeatureConfiguration {
    private static final Codec<Identifier> IDENTIFIER_CODEC = Codec.STRING.xmap(Identifier::parse, Identifier::toString);

    public static final Codec<SimpleJsonStructureFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IDENTIFIER_CODEC.fieldOf("structure").forGetter(SimpleJsonStructureFeatureConfig::structure),
            Codec.INT.optionalFieldOf("origin_x", 0).forGetter(SimpleJsonStructureFeatureConfig::originX),
            Codec.INT.optionalFieldOf("origin_y", 0).forGetter(SimpleJsonStructureFeatureConfig::originY),
            Codec.INT.optionalFieldOf("origin_z", 0).forGetter(SimpleJsonStructureFeatureConfig::originZ),
            Codec.BOOL.optionalFieldOf("ignore_air", true).forGetter(SimpleJsonStructureFeatureConfig::ignoreAir),
            Codec.BOOL.optionalFieldOf("replace_air_only", false).forGetter(SimpleJsonStructureFeatureConfig::replaceAirOnly),
            IDENTIFIER_CODEC.listOf().optionalFieldOf("replace_blocks_with_air", List.of()).forGetter(SimpleJsonStructureFeatureConfig::replaceBlocksWithAir),
            IDENTIFIER_CODEC.listOf().optionalFieldOf("required_floor_blocks", List.of()).forGetter(SimpleJsonStructureFeatureConfig::requiredFloorBlocks),
                        Codec.INT.optionalFieldOf("required_air_blocks_above", 2).forGetter(SimpleJsonStructureFeatureConfig::requiredAirBlocksAbove),
            Codec.INT.optionalFieldOf("min_distance_from_same_structure", 24).forGetter(SimpleJsonStructureFeatureConfig::minDistanceFromSameStructure),
                        Codec.BOOL.optionalFieldOf("generate_support_pillars", false).forGetter(SimpleJsonStructureFeatureConfig::generateSupportPillars),
            Codec.BOOL.optionalFieldOf("clear_occupied_space", true).forGetter(SimpleJsonStructureFeatureConfig::clearOccupiedSpace),
            Codec.INT.optionalFieldOf("search_down_depth", 80).forGetter(SimpleJsonStructureFeatureConfig::searchDownDepth)
    ).apply(instance, SimpleJsonStructureFeatureConfig::new));
}
