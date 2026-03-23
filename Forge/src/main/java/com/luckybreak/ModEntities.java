package com.luckybreak;

import com.luckybreak.entity.GoldenHenEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

public final class ModEntities {

    public static EntityType<GoldenHenEntity> GOLDEN_HEN;
    private static boolean entitiesRegistered;
    private static boolean entityAttributesRegistered;

    private ModEntities() {
    }

    public static void register() {
    }

    @Mod.EventBusSubscriber(modid = LuckyBreak.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onRegisterEntities(RegisterEvent event) {
            if (entitiesRegistered) {
                return;
            }
            event.register(Registries.ENTITY_TYPE, helper -> {
                if (entitiesRegistered) {
                    return;
                }
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "golden_hen");
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
                GOLDEN_HEN = EntityType.Builder.of(GoldenHenEntity::new, MobCategory.CREATURE)
                        .sized(0.4F, 0.7F)
                        .eyeHeight(0.644F)
                        .clientTrackingRange(10)
                        .build(key);
                helper.register(id, GOLDEN_HEN);
                entitiesRegistered = true;
            });
        }

        @SubscribeEvent
        public static void onEntityAttributes(EntityAttributeCreationEvent event) {
            if (!entityAttributesRegistered && GOLDEN_HEN != null) {
                event.put(GOLDEN_HEN, Chicken.createAttributes().build());
                entityAttributesRegistered = true;
            }
        }
    }
}
