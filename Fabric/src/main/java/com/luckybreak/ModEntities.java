package com.luckybreak;

import com.luckybreak.entity.GoldenHenEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.chicken.Chicken;

public final class ModEntities {

    public static final EntityType<GoldenHenEntity> GOLDEN_HEN = register(
            "golden_hen",
            EntityType.Builder.of(GoldenHenEntity::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.7F)
                    .eyeHeight(0.644F)
                    .clientTrackingRange(10)
    );

    private ModEntities() {
    }

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(
            String name,
            EntityType.Builder<T> builder
    ) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(LuckyBreak.MOD_ID, name)
        );
        EntityType<T> type = builder.build(key);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
        return type;
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(GOLDEN_HEN, Chicken.createAttributes());
    }
}
