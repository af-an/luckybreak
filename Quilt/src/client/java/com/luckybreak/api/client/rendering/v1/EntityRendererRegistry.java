package com.luckybreak.api.client.rendering.v1;

import com.luckybreak.mixin.client.EntityRenderersAccessor;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry for entity renderers.
 * Modeled after Fabric/QFAPI's EntityRendererRegistry; no external dependency.
 *
 * Call {@code register()} any time before EntityRenderers.<clinit> fires (i.e. during mod init).
 * {@code bootstrap()} is invoked by EntityRenderersBootstrapMixin at the tail of that static block.
 */
public final class EntityRendererRegistry {

    private EntityRendererRegistry() {}

    private record Entry<T extends Entity>(
            EntityType<? extends T> type,
            EntityRendererProvider<T> provider
    ) {}

    private static final List<Entry<?>> PENDING = new ArrayList<>();

    public static <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        PENDING.add(new Entry<>(type, provider));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void bootstrap() {
        for (Entry<?> entry : PENDING) {
            EntityRenderersAccessor.luckybreak$register(
                    (EntityType) entry.type(),
                    (EntityRendererProvider) entry.provider()
            );
        }
    }
}
