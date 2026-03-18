package com.luckybreak.world;

import com.luckybreak.events.LuckyScheduler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Runtime tracker for wishing well interactions.
 * A well is active for a short duration after spawn, and consumes one named coin.
 */
public final class WishingWellManager {

    private static final Map<ResourceKey<Level>, List<ActiveWell>> ACTIVE_WELLS = new HashMap<>();
    private static final int WELL_TOP_WATER_Y_OFFSET = 9;
    private static final double DEFAULT_WELL_FIREWORK_LAUNCH_Y_OFFSET = 16.4;
    private static final int[][] WELL_TOP_WATER_OFFSETS = new int[][]{
            {-2, -1}, {-2, 0}, {-2, 1},
            {-1, -2}, {-1, -1}, {-1, 0}, {-1, 1}, {-1, 2},
            {0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2},
            {1, -2}, {1, -1}, {1, 0}, {1, 1}, {1, 2},
            {2, -1}, {2, 0}, {2, 1}
    };

    private WishingWellManager() {
    }

    public static void register() {
    }

    @Mod.EventBusSubscriber(modid = com.luckybreak.LuckyBreak.MOD_ID)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
            tickServer(event.server());
        }
    }

    public static void registerWell(
            ServerLevel level,
            BlockPos center,
            int detectRadius,
            int bottomOffsetY,
            int activeTicks,
            int foodCountMin,
            int foodCountMax,
            int foodRainDurationTicks,
            int heartDurationTicks,
            int heartIntervalTicks,
            int heartCount,
            int bubblePopCount,
            ParticleOptions primaryParticle,
            ParticleOptions secondaryParticle,
            double primaryParticleYOffset,
            double secondaryParticleYOffset,
            String coinName,
            Item coinItem,
            String wishMessage,
            int wishMessageColor,
            Item guaranteedDropItem,
            int guaranteedDropCount,
            int dropTrailDurationTicks,
            int dropTrailIntervalTicks,
            int dropTrailCount,
            double dropTrailSpread,
            SoundEvent activationSound,
            float activationSoundVolume,
            float activationSoundPitch,
                int fireworkBurstCount,
                int fireworkRocketCount,
                int fireworkFlight,
                IntList fireworkColors,
                IntList fireworkFadeColors,
                boolean fireworkFlicker,
                boolean fireworkTrail,
            double fireworkLaunchYOffset,
            List<WeightedDrop> dropPool
    ) {
        List<ActiveWell> wells = ACTIVE_WELLS.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>());
        wells.add(new ActiveWell(
                center.immutable(),
                detectRadius,
                bottomOffsetY,
                activeTicks,
                foodCountMin,
                foodCountMax,
                foodRainDurationTicks,
                heartDurationTicks,
                heartIntervalTicks,
                heartCount,
                bubblePopCount,
                primaryParticle,
                secondaryParticle,
                primaryParticleYOffset,
                secondaryParticleYOffset,
                coinName,
                coinItem,
                wishMessage,
                wishMessageColor,
                guaranteedDropItem,
                guaranteedDropCount,
                dropTrailDurationTicks,
                dropTrailIntervalTicks,
                dropTrailCount,
                dropTrailSpread,
                activationSound,
                activationSoundVolume,
                activationSoundPitch,
                fireworkBurstCount,
                fireworkRocketCount,
                fireworkFlight,
                new IntArrayList(fireworkColors),
                new IntArrayList(fireworkFadeColors),
                fireworkFlicker,
                fireworkTrail,
                fireworkLaunchYOffset,
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                13000,
                0,
                0,
                0,
                false,
                0.0f,
                0.0f,
                0.0f,
                List.copyOf(dropPool)
        ));
    }

            public static void registerUnluckyWell(
                ServerLevel level,
                BlockPos center,
                int detectRadius,
                int bottomOffsetY,
                int activeTicks,
                int smokeDurationTicks,
                int smokeIntervalTicks,
                int smokeCount,
                int smokeBurstCount,
                ParticleOptions primaryParticle,
                ParticleOptions secondaryParticle,
                double primaryParticleYOffset,
                double secondaryParticleYOffset,
                String coinName,
                Item coinItem,
                String wishMessage,
                int wishMessageColor,
                SoundEvent activationSound,
                float activationSoundVolume,
                float activationSoundPitch,
                int debuffDurationTicks,
                int tntCountMin,
                int tntCountMax,
                int tntSpawnRadius,
                int fireSpreadRadius,
                int fireSpreadAttempts,
                    boolean convertWaterToLava,
                    long nightTime,
                    int mobCountMin,
                    int mobCountMax,
                        int mobSpawnRadius,
                        boolean forceSkeletonBow,
                        float skeletonBowChance,
                        float zombieWeaponChance,
                        float zombieArmorChance
            ) {
            List<ActiveWell> wells = ACTIVE_WELLS.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>());
            wells.add(new ActiveWell(
                center.immutable(),
                detectRadius,
                bottomOffsetY,
                activeTicks,
                0,
                0,
                0,
                smokeDurationTicks,
                smokeIntervalTicks,
                smokeCount,
                smokeBurstCount,
                primaryParticle,
                secondaryParticle,
                primaryParticleYOffset,
                secondaryParticleYOffset,
                coinName,
                coinItem,
                wishMessage,
                wishMessageColor,
                null,
                0,
                0,
                1,
                0,
                0.0,
                activationSound,
                activationSoundVolume,
                activationSoundPitch,
                0,
                0,
                0,
                new IntArrayList(),
                new IntArrayList(),
                false,
                false,
                DEFAULT_WELL_FIREWORK_LAUNCH_Y_OFFSET,
                true,
                debuffDurationTicks,
                tntCountMin,
                tntCountMax,
                tntSpawnRadius,
                fireSpreadRadius,
                fireSpreadAttempts,
                convertWaterToLava,
                nightTime,
                mobCountMin,
                mobCountMax,
                mobSpawnRadius,
                forceSkeletonBow,
                skeletonBowChance,
                zombieWeaponChance,
                zombieArmorChance,
                List.of()
            ));
            }

    private static void tickServer(MinecraftServer server) {
        Iterator<Map.Entry<ResourceKey<Level>, List<ActiveWell>>> dims = ACTIVE_WELLS.entrySet().iterator();
        while (dims.hasNext()) {
            Map.Entry<ResourceKey<Level>, List<ActiveWell>> entry = dims.next();
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                dims.remove();
                continue;
            }

            List<ActiveWell> wells = entry.getValue();
            Iterator<ActiveWell> wellIterator = wells.iterator();
            while (wellIterator.hasNext()) {
                ActiveWell well = wellIterator.next();
                well.ticksRemaining--;
                if (well.ticksRemaining <= 0) {
                    wellIterator.remove();
                    continue;
                }

                if (tryConsumeCoin(level, well)) {
                    wellIterator.remove();
                }
            }

            if (wells.isEmpty()) {
                dims.remove();
            }
        }
    }

    private static boolean tryConsumeCoin(ServerLevel level, ActiveWell well) {
        double radius = Math.max(2.0, well.detectRadius + 0.6);
        AABB box = new AABB(well.center).inflate(radius, 2.5, radius);
        List<ItemEntity> coins = level.getEntitiesOfClass(
                ItemEntity.class,
                box,
                entity -> isCoin(entity.getItem(), well.coinName, well.coinItem)
        );
        if (coins.isEmpty()) {
            return false;
        }

        ItemEntity coinEntity = null;
        for (ItemEntity candidate : coins) {
            if (level.getFluidState(candidate.blockPosition()).is(FluidTags.WATER)) {
                coinEntity = candidate;
                break;
            }
        }

        if (coinEntity == null) {
            return false;
        }

        Vec3 coinPos = coinEntity.position();
        ServerPlayer coinOwner = coinEntity.getOwner() instanceof ServerPlayer sp ? sp : null;
        coinEntity.discard();
        if (!well.unluckyMode) {
            spawnFallingFoodsOverTime(level, well);
        }
        if (!well.unluckyMode) {
            spawnWellFireworks(level, well);
        }
        emitRisingHearts(level, well, coinPos);
        if (!well.unluckyMode) {
            spawnGuaranteedDrop(level, well, coinOwner);
        } else {
            triggerUnluckyOutcome(level, well, coinOwner);
        }
        sendWishMessage(level, well, coinEntity);
        level.playSound(null, well.center, well.activationSound, SoundSource.BLOCKS, well.activationSoundVolume, well.activationSoundPitch);
        return true;
    }

    private static void triggerUnluckyOutcome(ServerLevel level, ActiveWell well, ServerPlayer owner) {
        ServerPlayer target = owner;
        if (target == null) {
            Player nearest = level.getNearestPlayer(
                    well.center.getX() + 0.5,
                    well.center.getY() + 0.5,
                    well.center.getZ() + 0.5,
                    well.detectRadius + 12.0,
                    false
            );
            if (nearest instanceof ServerPlayer sp) {
                target = sp;
            }
        }

        if (well.convertWaterToLava) {
            convertWellWaterToLava(level, well);
        }

        ensureNormalDifficultyIfPeaceful(level);
        level.setDayTime(well.nightTime);

        // Hazards should originate from the cursed well itself.
        spawnActivatedTntFromWell(level, well.center, target, well.tntCountMin, well.tntCountMax, well.tntSpawnRadius);
        spreadFire(level, well.center, well.fireSpreadRadius, well.fireSpreadAttempts);

        if (target != null) {
            applyUnluckyDebuffs(target, well.debuffDurationTicks);
                spawnHostileMobs(
                    level,
                    target.blockPosition(),
                    well.mobCountMin,
                    well.mobCountMax,
                    well.mobSpawnRadius,
                    well.forceSkeletonBow,
                    well.skeletonBowChance,
                    well.zombieWeaponChance,
                    well.zombieArmorChance
                );
        }
    }

    private static void ensureNormalDifficultyIfPeaceful(ServerLevel level) {
        if (level.getServer() == null) {
            return;
        }
        if (level.getServer().getWorldData().getDifficulty() == Difficulty.PEACEFUL) {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
        }
    }

    private static void applyUnluckyDebuffs(ServerPlayer player, int durationTicks) {
        int duration = Math.max(20, durationTicks);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
    }

    private static void spawnActivatedTntFromWell(ServerLevel level, BlockPos wellCenter, ServerPlayer owner, int minCount, int maxCount, int launchRadius) {
        RandomSource random = level.getRandom();
        int min = Math.max(0, minCount);
        int max = Math.max(min, maxCount);
        int count = min + random.nextInt(Math.max(1, max - min + 1));
        double baseX = wellCenter.getX() + 0.5;
        double baseY = wellCenter.getY() + WELL_TOP_WATER_Y_OFFSET + 1.2;
        double baseZ = wellCenter.getZ() + 0.5;
        double speed = Math.max(0.25, Math.min(1.2, launchRadius * 0.09));

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * (Math.PI * 2.0);
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);

            PrimedTnt tnt = new PrimedTnt(level, baseX + dirX * 0.9, baseY, baseZ + dirZ * 0.9, owner);
            tnt.setFuse(40 + random.nextInt(41));
            tnt.setDeltaMovement(dirX * speed, 0.34 + random.nextDouble() * 0.10, dirZ * speed);
            level.addFreshEntity(tnt);
        }
    }

        private static void spawnHostileMobs(
            ServerLevel level,
            BlockPos around,
            int minCount,
            int maxCount,
            int spawnRadius,
            boolean forceSkeletonBow,
                float skeletonBowChance,
                float zombieWeaponChance,
                float zombieArmorChance
        ) {
        RandomSource random = level.getRandom();
        int min = Math.max(0, minCount);
        int max = Math.max(min, maxCount);
        int count = min + random.nextInt(Math.max(1, max - min + 1));
        int radius = Math.max(2, spawnRadius);

        for (int i = 0; i < count; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            BlockPos base = around.offset(dx, 0, dz);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
            BlockPos spawnPos = new BlockPos(base.getX(), Math.max(level.getMinY() + 1, y), base.getZ());

            Mob mob = switch (random.nextInt(3)) {
                case 0 -> EntityType.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
                case 1 -> EntityType.SPIDER.create(level, EntitySpawnReason.TRIGGERED);
                default -> EntityType.SKELETON.create(level, EntitySpawnReason.TRIGGERED);
            };

            if (mob == null) {
                continue;
            }

            mob.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

            // Initialize the mob with local difficulty data so default gear/attributes are applied.
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, null);

            // Keep skeletons ranged based on unlucky well JSON settings.
            boolean shouldGiveSkeletonBow = forceSkeletonBow
                    || (mob.getType() == EntityType.SKELETON
                    && mob.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
                    && random.nextFloat() <= skeletonBowChance);
            if (mob.getType() == EntityType.SKELETON && shouldGiveSkeletonBow) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            }

            if (mob.getType() == EntityType.ZOMBIE) {
                if (mob.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && random.nextFloat() <= zombieWeaponChance) {
                    ItemStack weapon = switch (random.nextInt(4)) {
                        case 0 -> new ItemStack(Items.STONE_SWORD);
                        case 1 -> new ItemStack(Items.IRON_SWORD);
                        case 2 -> new ItemStack(Items.STONE_AXE);
                        default -> new ItemStack(Items.IRON_AXE);
                    };
                    mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
                }

                if (random.nextFloat() <= zombieArmorChance) {
                    ItemStack helmet = switch (random.nextInt(3)) {
                        case 0 -> new ItemStack(Items.LEATHER_HELMET);
                        case 1 -> new ItemStack(Items.CHAINMAIL_HELMET);
                        default -> new ItemStack(Items.IRON_HELMET);
                    };
                    ItemStack chest = switch (random.nextInt(3)) {
                        case 0 -> new ItemStack(Items.LEATHER_CHESTPLATE);
                        case 1 -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                        default -> new ItemStack(Items.IRON_CHESTPLATE);
                    };
                    ItemStack legs = switch (random.nextInt(3)) {
                        case 0 -> new ItemStack(Items.LEATHER_LEGGINGS);
                        case 1 -> new ItemStack(Items.CHAINMAIL_LEGGINGS);
                        default -> new ItemStack(Items.IRON_LEGGINGS);
                    };
                    ItemStack boots = switch (random.nextInt(3)) {
                        case 0 -> new ItemStack(Items.LEATHER_BOOTS);
                        case 1 -> new ItemStack(Items.CHAINMAIL_BOOTS);
                        default -> new ItemStack(Items.IRON_BOOTS);
                    };

                    if (mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                        mob.setItemSlot(EquipmentSlot.HEAD, helmet);
                    }
                    if (mob.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                        mob.setItemSlot(EquipmentSlot.CHEST, chest);
                    }
                    if (mob.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) {
                        mob.setItemSlot(EquipmentSlot.LEGS, legs);
                    }
                    if (mob.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
                        mob.setItemSlot(EquipmentSlot.FEET, boots);
                    }
                }
            }

            level.addFreshEntity(mob);
        }
    }

    private static void spreadFire(ServerLevel level, BlockPos around, int spreadRadius, int attempts) {
        RandomSource random = level.getRandom();
        int radius = Math.max(1, spreadRadius);
        int tryCount = Math.max(0, attempts);

        for (int i = 0; i < tryCount; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            BlockPos base = around.offset(dx, 0, dz);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
            BlockPos firePos = new BlockPos(base.getX(), Math.max(level.getMinY() + 1, y), base.getZ());
            BlockPos below = firePos.below();

            if (level.isEmptyBlock(firePos) && level.getBlockState(below).isSolidRender()) {
                level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }

    private static void convertWellWaterToLava(ServerLevel level, ActiveWell well) {
        int radius = Math.max(2, well.detectRadius + 2);
        int minY = well.center.getY() - 10;
        int maxY = well.center.getY() + 16;

        for (int x = well.center.getX() - radius; x <= well.center.getX() + radius; x++) {
            for (int z = well.center.getZ() - radius; z <= well.center.getZ() + radius; z++) {
                for (int y = maxY; y >= minY; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getFluidState(pos).is(FluidTags.WATER)) {
                        level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Keep the cursed well molten: replace common water-lava reaction blocks back to lava.
        for (int x = well.center.getX() - radius; x <= well.center.getX() + radius; x++) {
            for (int z = well.center.getZ() - radius; z <= well.center.getZ() + radius; z++) {
                for (int y = maxY; y >= minY; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    var state = level.getBlockState(pos);
                    if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE) || state.is(Blocks.BASALT)) {
                        level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void spawnGuaranteedDrop(ServerLevel level, ActiveWell well, ServerPlayer owner) {
        if (well.guaranteedDropItem == null || well.guaranteedDropCount <= 0) {
            return;
        }

        ItemStack stack = new ItemStack(well.guaranteedDropItem, well.guaranteedDropCount);

        if (owner != null) {
            Vec3 ownerPos = owner.position();
            double dx = ownerPos.x - (well.center.getX() + 0.5);
            double dz = ownerPos.z - (well.center.getZ() + 0.5);
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-4) {
                Vec3 look = owner.getLookAngle();
                dx = look.x;
                dz = look.z;
                len = Math.sqrt(dx * dx + dz * dz);
            }
            if (len < 1.0E-4) {
                dx = 0.0;
                dz = 1.0;
                len = 1.0;
            }

            double nx = dx / len;
            double nz = dz / len;
            double spawnX = ownerPos.x + nx * 0.85;
            double spawnY = owner.getY() + 0.35;
            double spawnZ = ownerPos.z + nz * 0.85;

            ItemEntity entity = new ItemEntity(level, spawnX, spawnY, spawnZ, stack);
            entity.setDeltaMovement(nx * 0.04, 0.06, nz * 0.04);
            level.addFreshEntity(entity);
            return;
        }

        double x = well.center.getX() + 0.5;
        double z = well.center.getZ() + 0.5;
        double y = well.center.getY() + WELL_TOP_WATER_Y_OFFSET + 1.6;

        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDeltaMovement(0.0, -0.08, 0.0);
        level.addFreshEntity(entity);
    }

    private static void sendWishMessage(ServerLevel level, ActiveWell well, ItemEntity coinEntity) {
        Component message = Component.literal(well.wishMessage)
                .withStyle(style -> style.withColor(TextColor.fromRgb(well.wishMessageColor)));
        if (coinEntity.getOwner() instanceof ServerPlayer owner) {
            owner.displayClientMessage(message, false);
            return;
        }

        Player nearestPlayer = level.getNearestPlayer(
                well.center.getX() + 0.5,
                well.center.getY() + 0.5,
                well.center.getZ() + 0.5,
                well.detectRadius + 6.0,
                false
        );
        if (nearestPlayer instanceof ServerPlayer nearest) {
            nearest.displayClientMessage(message, false);
        }
    }

    private static boolean isCoin(ItemStack stack, String coinName, Item coinItem) {
        var customName = stack.get(DataComponents.CUSTOM_NAME);
        return stack.getItem() == coinItem
                && customName != null
                && customName.getString().equalsIgnoreCase(coinName);
    }

    private static void spawnFallingFoodsOverTime(ServerLevel level, ActiveWell well) {
        RandomSource random = level.getRandom();
        int count = well.foodCountMin + random.nextInt(Math.max(1, well.foodCountMax - well.foodCountMin + 1));

        for (int i = 0; i < count; i++) {
            int delay = random.nextInt(Math.max(1, well.foodRainDurationTicks + 1));
            LuckyScheduler.INSTANCE.schedule(delay, () -> {
                WeightedDrop drop = chooseWeightedDrop(random, well.dropPool);
                int minCount = Math.max(1, drop.countMin());
                int maxCount = Math.max(minCount, drop.countMax());
                int stackCount = minCount + random.nextInt(Math.max(1, maxCount - minCount + 1));
                ItemStack stack = new ItemStack(drop.item(), stackCount);

                double angle = random.nextDouble() * (Math.PI * 2.0);
                double minRadius = Math.max(2.8, well.detectRadius + 1.8);
                double maxRadius = minRadius + 1.8;
                double radius2d = minRadius + random.nextDouble() * (maxRadius - minRadius);

                double x = well.center.getX() + 0.5 + Math.cos(angle) * radius2d + (random.nextDouble() - 0.5) * 0.20;
                double z = well.center.getZ() + 0.5 + Math.sin(angle) * radius2d + (random.nextDouble() - 0.5) * 0.20;
                double y = well.center.getY() + WELL_TOP_WATER_Y_OFFSET + 2.4 + random.nextDouble();

                ItemEntity entity = new ItemEntity(level, x, y, z, stack);
                entity.setDeltaMovement((random.nextDouble() - 0.5) * 0.01, -0.34 - random.nextDouble() * 0.08, (random.nextDouble() - 0.5) * 0.01);
                level.addFreshEntity(entity);
                scheduleDropTrail(level, well, entity);
            });
        }
    }

    private static void spawnWellFireworks(ServerLevel level, ActiveWell well) {
        if (well.fireworkBurstCount <= 0 && well.fireworkRocketCount <= 0) {
            return;
        }

        double x = well.center.getX() + 0.5;
        double y = well.center.getY() + well.fireworkLaunchYOffset;
        double z = well.center.getZ() + 0.5;

        if (well.fireworkBurstCount > 0) {
            level.sendParticles(ParticleTypes.FIREWORK, x, y, z, well.fireworkBurstCount, 0.35, 0.35, 0.35, 0.06);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        if (well.fireworkRocketCount <= 0) {
            return;
        }

        for (int i = 0; i < well.fireworkRocketCount; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET, 1);
            FireworkExplosion explosion = new FireworkExplosion(
                    FireworkExplosion.Shape.LARGE_BALL,
                    well.fireworkColors,
                    well.fireworkFadeColors,
                    well.fireworkTrail,
                    well.fireworkFlicker
            );
            Fireworks fireworks = new Fireworks(well.fireworkFlight, List.of(explosion));
            rocket.set(DataComponents.FIREWORKS, fireworks);

            double spawnX = x + (level.getRandom().nextDouble() - 0.5) * 0.4;
            double spawnY = y + 0.2;
            double spawnZ = z + (level.getRandom().nextDouble() - 0.5) * 0.4;
            FireworkRocketEntity entity = new FireworkRocketEntity(level, spawnX, spawnY, spawnZ, rocket);
            entity.setDeltaMovement(
                    (level.getRandom().nextDouble() - 0.5) * 0.08,
                    0.35 + level.getRandom().nextDouble() * 0.15,
                    (level.getRandom().nextDouble() - 0.5) * 0.08
            );
            level.addFreshEntity(entity);
        }
    }

    private static void scheduleDropTrail(ServerLevel level, ActiveWell well, ItemEntity entity) {
        if (well.dropTrailDurationTicks <= 0 || well.dropTrailCount <= 0) {
            return;
        }

        final int trailDurationTicks = well.dropTrailDurationTicks;
        final int intervalTicks = well.dropTrailIntervalTicks;

        for (int t = 0; t <= trailDurationTicks; t += intervalTicks) {
            LuckyScheduler.INSTANCE.schedule(t, () -> {
                if (!entity.isAlive() || entity.onGround()) {
                    return;
                }

                double x = entity.getX();
                double y = entity.getY() + 0.12;
                double z = entity.getZ();

                sendParticlesToNearbyPlayers(
                        level,
                        well.primaryParticle,
                        x,
                        y,
                        z,
                        well.dropTrailCount,
                        well.dropTrailSpread,
                        well.dropTrailSpread,
                        well.dropTrailSpread,
                        0.0,
                        well.center,
                        Math.max(8, well.detectRadius + 18)
                );
            });
        }
    }

    private static WeightedDrop chooseWeightedDrop(RandomSource random, List<WeightedDrop> drops) {
        if (drops.isEmpty()) {
            return new WeightedDrop(net.minecraft.world.item.Items.BREAD, 1, 1, 1);
        }

        int totalWeight = 0;
        for (WeightedDrop drop : drops) {
            totalWeight += Math.max(1, drop.weight());
        }

        int pick = random.nextInt(Math.max(1, totalWeight));
        int running = 0;
        for (WeightedDrop drop : drops) {
            running += Math.max(1, drop.weight());
            if (pick < running) {
                return drop;
            }
        }

        return drops.get(drops.size() - 1);
    }

    private static void emitRisingHearts(ServerLevel level, ActiveWell well, Vec3 sourcePos) {
        List<Vec3> surfaces = resolveKnownWellTopSurfaces(well);
        if (surfaces.isEmpty()) {
            surfaces = resolveWaterSurfaces(level, well);
        }
        if (surfaces.isEmpty()) {
            surfaces = List.of(resolveFallbackSurface(level, sourcePos));
        }

        for (int t = 0; t <= well.heartDurationTicks; t += well.heartIntervalTicks) {
            List<Vec3> sources = surfaces;
            LuckyScheduler.INSTANCE.schedule(t, () -> {
            int bursts = Math.min(3, Math.max(1, sources.size() / 8));
                for (int i = 0; i < bursts; i++) {
                    Vec3 base = sources.get(level.random.nextInt(sources.size()));
                    double x = base.x;
                    double y = base.y + well.primaryParticleYOffset + level.random.nextDouble() * 0.04;
                    double z = base.z;

                    sendParticlesToNearbyPlayers(
                            level,
                            well.primaryParticle,
                            x,
                            y,
                            z,
                            Math.max(1, well.heartCount),
                    0.12,
                    0.03,
                    0.12,
                            0.0,
                            well.center,
                            Math.max(8, well.detectRadius + 16)
                    );
                }

                if (well.bubblePopCount > 0) {
                    Vec3 bubbleBase = sources.get(level.random.nextInt(sources.size()));
                    sendParticlesToNearbyPlayers(
                            level,
                            well.secondaryParticle,
                            bubbleBase.x,
                            bubbleBase.y - 0.08 + well.secondaryParticleYOffset,
                            bubbleBase.z,
                            well.bubblePopCount,
                    0.14,
                    0.03,
                    0.14,
                            0.02,
                            well.center,
                            Math.max(8, well.detectRadius + 12)
                    );
                }
            });
        }
    }

    private static List<Vec3> resolveKnownWellTopSurfaces(ActiveWell well) {
        List<Vec3> points = new ArrayList<>(WELL_TOP_WATER_OFFSETS.length);
        double y = well.center.getY() + WELL_TOP_WATER_Y_OFFSET + 1.02;
        for (int[] offset : WELL_TOP_WATER_OFFSETS) {
            points.add(new Vec3(
                    well.center.getX() + offset[0] + 0.5,
                    y,
                    well.center.getZ() + offset[1] + 0.5
            ));
        }
        return points;
    }

    private static void sendParticlesToNearbyPlayers(
            ServerLevel level,
            ParticleOptions options,
            double x,
            double y,
            double z,
            int count,
            double spreadX,
            double spreadY,
            double spreadZ,
            double speed,
            BlockPos center,
            int radius
    ) {
        double radiusSq = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5) > radiusSq) {
                continue;
            }
            level.sendParticles(player, options, true, false, x, y, z, count, spreadX, spreadY, spreadZ, speed);
        }
    }

    private static List<Vec3> resolveWaterSurfaces(ServerLevel level, ActiveWell well) {
        int searchRadius = Math.max(3, well.detectRadius + 2);
        int minY = well.center.getY() - 32;
        int maxY = well.center.getY() + 32;

        List<Vec3> result = new ArrayList<>();
        for (int x = well.center.getX() - searchRadius; x <= well.center.getX() + searchRadius; x++) {
            for (int z = well.center.getZ() - searchRadius; z <= well.center.getZ() + searchRadius; z++) {
                for (int y = maxY; y >= minY; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    FluidState fluid = level.getFluidState(pos);
                    if (!fluid.is(FluidTags.WATER)) {
                        continue;
                    }
                    if (!level.getFluidState(pos.above()).isEmpty()) {
                        continue;
                    }
                    result.add(new Vec3(x + 0.5, y + 1.02, z + 0.5));
                    break;
                }
            }
        }

        return result;
    }

    private static Vec3 resolveFallbackSurface(ServerLevel level, Vec3 sourcePos) {
        BlockPos sourceBlock = BlockPos.containing(sourcePos.x, sourcePos.y, sourcePos.z);
        for (int y = sourceBlock.getY() + 8; y >= sourceBlock.getY() - 8; y--) {
            BlockPos pos = new BlockPos(sourceBlock.getX(), y, sourceBlock.getZ());
            if (level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.above()).isEmpty()) {
                return new Vec3(pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5);
            }
        }
        return new Vec3(sourcePos.x, sourcePos.y + 0.9, sourcePos.z);
    }

    private static class ActiveWell {
        private final BlockPos center;
        private final int detectRadius;
        private final int bottomOffsetY;
        private int ticksRemaining;
        private final int foodCountMin;
        private final int foodCountMax;
        private final int foodRainDurationTicks;
        private final int heartDurationTicks;
        private final int heartIntervalTicks;
        private final int heartCount;
        private final int bubblePopCount;
        private final ParticleOptions primaryParticle;
        private final ParticleOptions secondaryParticle;
        private final double primaryParticleYOffset;
        private final double secondaryParticleYOffset;
        private final String coinName;
        private final Item coinItem;
        private final String wishMessage;
        private final int wishMessageColor;
        private final Item guaranteedDropItem;
        private final int guaranteedDropCount;
        private final int dropTrailDurationTicks;
        private final int dropTrailIntervalTicks;
        private final int dropTrailCount;
        private final double dropTrailSpread;
        private final SoundEvent activationSound;
        private final float activationSoundVolume;
        private final float activationSoundPitch;
        private final int fireworkBurstCount;
        private final int fireworkRocketCount;
        private final int fireworkFlight;
        private final IntList fireworkColors;
        private final IntList fireworkFadeColors;
        private final boolean fireworkFlicker;
        private final boolean fireworkTrail;
        private final double fireworkLaunchYOffset;
        private final boolean unluckyMode;
        private final int debuffDurationTicks;
        private final int tntCountMin;
        private final int tntCountMax;
        private final int tntSpawnRadius;
        private final int fireSpreadRadius;
        private final int fireSpreadAttempts;
        private final boolean convertWaterToLava;
        private final long nightTime;
        private final int mobCountMin;
        private final int mobCountMax;
        private final int mobSpawnRadius;
        private final boolean forceSkeletonBow;
        private final float skeletonBowChance;
        private final float zombieWeaponChance;
        private final float zombieArmorChance;
        private final List<WeightedDrop> dropPool;

        private ActiveWell(
                BlockPos center,
                int detectRadius,
                int bottomOffsetY,
                int ticksRemaining,
                int foodCountMin,
                int foodCountMax,
                int foodRainDurationTicks,
                int heartDurationTicks,
                int heartIntervalTicks,
                int heartCount,
                int bubblePopCount,
                ParticleOptions primaryParticle,
                ParticleOptions secondaryParticle,
                double primaryParticleYOffset,
                double secondaryParticleYOffset,
                String coinName,
                Item coinItem,
                String wishMessage,
                int wishMessageColor,
                Item guaranteedDropItem,
                int guaranteedDropCount,
                int dropTrailDurationTicks,
                int dropTrailIntervalTicks,
                int dropTrailCount,
                double dropTrailSpread,
                SoundEvent activationSound,
                float activationSoundVolume,
                float activationSoundPitch,
                int fireworkBurstCount,
                int fireworkRocketCount,
                int fireworkFlight,
                IntList fireworkColors,
                IntList fireworkFadeColors,
                boolean fireworkFlicker,
                boolean fireworkTrail,
                double fireworkLaunchYOffset,
                boolean unluckyMode,
                int debuffDurationTicks,
                int tntCountMin,
                int tntCountMax,
                int tntSpawnRadius,
                int fireSpreadRadius,
                int fireSpreadAttempts,
                boolean convertWaterToLava,
                long nightTime,
                int mobCountMin,
                int mobCountMax,
                int mobSpawnRadius,
                boolean forceSkeletonBow,
                float skeletonBowChance,
                float zombieWeaponChance,
                float zombieArmorChance,
                List<WeightedDrop> dropPool
        ) {
            this.center = center;
            this.detectRadius = detectRadius;
            this.bottomOffsetY = bottomOffsetY;
            this.ticksRemaining = ticksRemaining;
            this.foodCountMin = foodCountMin;
            this.foodCountMax = foodCountMax;
            this.foodRainDurationTicks = foodRainDurationTicks;
            this.heartDurationTicks = heartDurationTicks;
            this.heartIntervalTicks = heartIntervalTicks;
            this.heartCount = heartCount;
            this.bubblePopCount = bubblePopCount;
            this.primaryParticle = primaryParticle;
            this.secondaryParticle = secondaryParticle;
            this.primaryParticleYOffset = primaryParticleYOffset;
            this.secondaryParticleYOffset = secondaryParticleYOffset;
            this.coinName = coinName;
            this.coinItem = coinItem;
            this.wishMessage = wishMessage;
            this.wishMessageColor = wishMessageColor;
            this.guaranteedDropItem = guaranteedDropItem;
            this.guaranteedDropCount = guaranteedDropCount;
            this.dropTrailDurationTicks = dropTrailDurationTicks;
            this.dropTrailIntervalTicks = dropTrailIntervalTicks;
            this.dropTrailCount = dropTrailCount;
            this.dropTrailSpread = dropTrailSpread;
            this.activationSound = activationSound;
            this.activationSoundVolume = activationSoundVolume;
            this.activationSoundPitch = activationSoundPitch;
            this.fireworkBurstCount = fireworkBurstCount;
            this.fireworkRocketCount = fireworkRocketCount;
            this.fireworkFlight = fireworkFlight;
            this.fireworkColors = fireworkColors;
            this.fireworkFadeColors = fireworkFadeColors;
            this.fireworkFlicker = fireworkFlicker;
            this.fireworkTrail = fireworkTrail;
            this.fireworkLaunchYOffset = fireworkLaunchYOffset;
            this.unluckyMode = unluckyMode;
            this.debuffDurationTicks = debuffDurationTicks;
            this.tntCountMin = tntCountMin;
            this.tntCountMax = tntCountMax;
            this.tntSpawnRadius = tntSpawnRadius;
            this.fireSpreadRadius = fireSpreadRadius;
            this.fireSpreadAttempts = fireSpreadAttempts;
            this.convertWaterToLava = convertWaterToLava;
            this.nightTime = nightTime;
            this.mobCountMin = mobCountMin;
            this.mobCountMax = mobCountMax;
            this.mobSpawnRadius = mobSpawnRadius;
            this.forceSkeletonBow = forceSkeletonBow;
            this.skeletonBowChance = skeletonBowChance;
            this.zombieWeaponChance = zombieWeaponChance;
            this.zombieArmorChance = zombieArmorChance;
            this.dropPool = dropPool;
        }
    }

    public record WeightedDrop(Item item, int weight, int countMin, int countMax) {
        public WeightedDrop {
            if (weight < 1) {
                weight = 1;
            }
            if (countMin < 1) {
                countMin = 1;
            }
            if (countMax < countMin) {
                countMax = countMin;
            }
        }

        public WeightedDrop(Item item, int weight) {
            this(item, weight, 1, 1);
        }
    }
}
