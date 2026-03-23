package com.luckybreak.world.feature;

import com.luckybreak.LuckyBreak;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

public final class ModFeatures {

    public static final ResourceKey<Feature<?>> SIMPLE_JSON_STRUCTURE_KEY =
            ResourceKey.create(Registries.FEATURE, ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "simple_json_structure"));
    private static final ResourceLocation SIMPLE_JSON_STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "simple_json_structure");
    private static boolean featureRegistered;

    public static final Feature<SimpleJsonStructureFeatureConfig> SIMPLE_JSON_STRUCTURE =
            new SimpleJsonStructureFeature(SimpleJsonStructureFeatureConfig.CODEC);

    private ModFeatures() {
    }

    public static void register() {
    }

    @Mod.EventBusSubscriber(modid = LuckyBreak.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onRegisterFeatures(RegisterEvent event) {
            if (featureRegistered) {
                return;
            }
            event.register(Registries.FEATURE, helper ->
                    {
                        helper.register(SIMPLE_JSON_STRUCTURE_ID, SIMPLE_JSON_STRUCTURE);
                        featureRegistered = true;
                    }
            );
        }
    }
}
