package com.luckybreak.entity;

import com.luckybreak.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GoldenHenEntity extends Chicken {

    private int goldLayTime;

    public GoldenHenEntity(EntityType<? extends Chicken> entityType, Level level) {
        super(entityType, level);
        this.goldLayTime = GoldenHenConfig.nextLayIntervalTicks(this.random);
        this.eggTime = Integer.MAX_VALUE;
    }

    @Override
    public void aiStep() {
        this.eggTime = Integer.MAX_VALUE;
        super.aiStep();

        if (this.level().isClientSide() || this.isBaby() || this.isChickenJockey() || !this.isAlive()) {
            return;
        }

        this.goldLayTime--;
        if (this.goldLayTime <= 0) {
            var dropItem = GoldenHenConfig.nextDropItem(this.random);
            ItemStack drop = new ItemStack(
                dropItem,
                GoldenHenConfig.nextDropCount(this.random, dropItem)
            );
                this.spawnAtLocation((ServerLevel) this.level(), drop);

            if (GoldenHenConfig.playLaySound()) {
                this.level().playSound(
                        (Player) null,
                        this,
                        GoldenHenConfig.laySound(),
                        SoundSource.BLOCKS,
                        1.0F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F
                );
            }

            this.goldLayTime = GoldenHenConfig.nextLayIntervalTicks(this.random);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource damageSource, boolean recentlyHit) {
        int count = GoldenHenConfig.deathDropCount();
        if (count > 0) {
            this.spawnAtLocation(serverLevel, new ItemStack(GoldenHenConfig.deathDropItem(), count));
        }
    }

    @Override
    public Chicken getBreedOffspring(ServerLevel serverLevel, net.minecraft.world.entity.AgeableMob ageableMob) {
        return ModEntities.GOLDEN_HEN.create(serverLevel, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GoldenHenConfig.ambientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return GoldenHenConfig.hurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GoldenHenConfig.deathSound();
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Override
    public boolean canMate(Animal animal) {
        return false;
    }
}
