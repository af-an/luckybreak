package com.luckybreak.api.entity.event.v1;

import com.luckybreak.api.event.Event;
import com.luckybreak.api.event.EventFactory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class ServerLivingEntityEvents {

    private ServerLivingEntityEvents() {
    }

    public static final Event<AfterDeath> AFTER_DEATH = EventFactory.createArrayBacked(AfterDeath.class, callbacks -> (entity, source) -> {
        for (AfterDeath callback : callbacks) {
            callback.afterDeath(entity, source);
        }
    });

    /**
     * Fired after a living entity takes damage (mirrors Fabric's ServerLivingEntityEvents.AFTER_DAMAGE).
     * baseDamageTaken and damageTaken are both the reduced damage amount passed to actuallyHurt;
     * blocked is always false in this implementation (not tracked at mixin level).
     */
    public static final Event<AfterDamage> AFTER_DAMAGE = EventFactory.createArrayBacked(AfterDamage.class, callbacks -> (entity, source, baseDamageTaken, damageTaken, blocked) -> {
        for (AfterDamage callback : callbacks) {
            callback.afterDamage(entity, source, baseDamageTaken, damageTaken, blocked);
        }
    });

    @FunctionalInterface
    public interface AfterDeath {
        void afterDeath(LivingEntity entity, DamageSource source);
    }

    @FunctionalInterface
    public interface AfterDamage {
        void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked);
    }
}