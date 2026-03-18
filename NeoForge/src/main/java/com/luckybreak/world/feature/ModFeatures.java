package com.luckybreak.world.feature;

import com.luckybreak.LuckyBreak;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class ModFeatures {

    public static final ResourceKey<Feature<?>> SIMPLE_JSON_STRUCTURE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "simple_json_structure"));
    private static final Identifier SIMPLE_JSON_STRUCTURE_ID = Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, "simple_json_structure");

    public static final Feature<SimpleJsonStructureFeatureConfig> SIMPLE_JSON_STRUCTURE =
            new SimpleJsonStructureFeature(SimpleJsonStructureFeatureConfig.CODEC);

    private ModFeatures() {
    }

    public static void register() {
    }

    @EventBusSubscriber(modid = LuckyBreak.MOD_ID)
    public static final class NeoForgeEvents {
        @SubscribeEvent
        public static void onRegisterFeatures(RegisterEvent event) {
            event.register(Registries.FEATURE, helper ->
                    helper.register(SIMPLE_JSON_STRUCTURE_ID, SIMPLE_JSON_STRUCTURE)
            );
        }
    }
}
