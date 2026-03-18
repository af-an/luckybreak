package com.luckybreak.api.worldgen.v1;

import com.luckybreak.LuckyBreak;
import com.luckybreak.mixin.BiomeAccessor;
import com.luckybreak.mixin.BiomeGenerationSettingsAccessor;
import com.luckybreak.mixin.BiomeGenerationSettingsInvoker;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.List;

/**
 * API for modifying biome generation settings before world generation begins.
 * Modeled after Quilted Fabric API's BiomeModifications; no external dependency.
 *
 * Callbacks are applied once during MinecraftServer.createLevels() — before any
 * ChunkGenerator is built and before any chunk decoration can run.
 */
public final class BiomeModifications {

    private record Entry(BiomeSelector selector, GenerationStep.Decoration step, ResourceKey<PlacedFeature> featureKey) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private BiomeModifications() {}

    public static void addFeature(BiomeSelector selector, GenerationStep.Decoration step, ResourceKey<PlacedFeature> featureKey) {
        ENTRIES.add(new Entry(selector, step, featureKey));
    }

    /**
     * Apply all registered modifications into the biome registry.
     * Called by MinecraftServerHooksMixin at the HEAD of createLevels().
     */
    public static void applyAll(RegistryAccess registryAccess) {
        Registry<Biome> biomeRegistry = registryAccess.lookupOrThrow(Registries.BIOME);
        Registry<PlacedFeature> placedFeatureRegistry = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);

        int modified = 0;
        for (ResourceKey<Biome> biomeKey : biomeRegistry.registryKeySet()) {
            Holder.Reference<Biome> biomeHolder = biomeRegistry.get(biomeKey).orElseThrow();
            Biome biome = biomeHolder.value();
            BiomeGenerationSettings gs = biome.getGenerationSettings();
            List<HolderSet<PlacedFeature>> existingSteps = gs.features();

            List<HolderSet<PlacedFeature>> mutableSteps = null;

            for (Entry entry : ENTRIES) {
                if (!entry.selector().isBiomeIncluded(biomeHolder)) continue;

                Holder.Reference<PlacedFeature> featureHolder = placedFeatureRegistry.get(entry.featureKey()).orElse(null);
                if (featureHolder == null) continue;

                int stepIndex = entry.step().ordinal();
                if (mutableSteps == null) {
                    mutableSteps = new ArrayList<>(existingSteps);
                }
                // Pad list so all steps up to stepIndex exist
                while (mutableSteps.size() <= stepIndex) {
                    mutableSteps.add(HolderSet.direct(List.of()));
                }
                // Skip if already present
                if (mutableSteps.get(stepIndex).contains(featureHolder)) continue;

                List<Holder<PlacedFeature>> merged = new ArrayList<>();
                for (Holder<PlacedFeature> f : mutableSteps.get(stepIndex)) merged.add(f);
                merged.add(featureHolder);
                mutableSteps.set(stepIndex, HolderSet.direct(merged));
            }

            if (mutableSteps != null && mutableSteps != existingSteps) {
                var carvers = ((BiomeGenerationSettingsAccessor) (Object) gs).luckybreak$getCarvers();
                BiomeGenerationSettings newGs = BiomeGenerationSettingsInvoker.luckybreak$create(carvers, mutableSteps);
                ((BiomeAccessor) (Object) biome).luckybreak$setGenerationSettings(newGs);
                modified++;
            }
        }

        LuckyBreak.LOGGER.info("[LuckyBreak] BiomeModifications applied to {} biomes.", modified);
    }
}
