package com.luckybreak;

import com.luckybreak.entity.GoldenHenEntity;
import com.luckybreak.mixin.DefaultAttributesAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.chicken.Chicken;

import java.util.HashMap;
import java.util.Map;

public final class ModEntities {

    public static final EntityType<GoldenHenEntity> GOLDEN_HEN = register(
        "golden_hen",
        EntityType.Builder.of(GoldenHenEntity::new, MobCategory.CREATURE)
            .sized(0.4F, 0.7F)
            .eyeHeight(0.644F)
            .clientTrackingRange(10)
    );
    private static boolean attributesRegistered;

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
        // Entity types are registered during bootstrap via static initialization.
    }

    public static void ensureRuntimeSetup() {
        if (attributesRegistered) {
            return;
        }
        registerDefaultAttributes(GOLDEN_HEN, Chicken.createAttributes().build());
        attributesRegistered = true;
    }

    @SuppressWarnings("unchecked")
    private static void registerDefaultAttributes(EntityType<?> entityType, AttributeSupplier attributes) {
        try {
            Map<EntityType<? extends net.minecraft.world.entity.LivingEntity>, AttributeSupplier> suppliers =
                DefaultAttributesAccessor.luckybreak$getSuppliers();

            EntityType<? extends net.minecraft.world.entity.LivingEntity> livingEntityType =
                (EntityType<? extends net.minecraft.world.entity.LivingEntity>) entityType;

            if (suppliers.containsKey(livingEntityType)) {
                return;
            }

            try {
                suppliers.put(livingEntityType, attributes);
            } catch (UnsupportedOperationException immutableMap) {
                Map<EntityType<? extends net.minecraft.world.entity.LivingEntity>, AttributeSupplier> mutable =
                    new HashMap<>(suppliers);
                mutable.put(livingEntityType, attributes);
                DefaultAttributesAccessor.luckybreak$setSuppliers(mutable);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to register default attributes for entity type: " + entityType, exception);
        }
    }
}
