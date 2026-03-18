package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import com.luckybreak.events.LuckyScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Drops a rainbow terracotta column (red → orange → yellow → lime → blue → purple)
 * as falling block entities from above, stacking on landing, then drops a random
 * precious block on top and strikes lightning.
 *
 * JSON:
 * {
 *   "type": "luckybreak:rainbow_column", "weight": 10,
 *   "top_blocks": [
 *     { "block": "minecraft:diamond_block", "weight": 10 },
 *     { "block": "minecraft:gold_block",    "weight": 40 },
 *     { "block": "minecraft:iron_block",    "weight": 40 },
 *     { "block": "minecraft:emerald_block", "weight": 10 }
 *   ],
 *   "clear_height": 20
 * }
 */
public class RainbowColumnEvent implements LuckyEvent {

    // Order they fall: red first (lands at bottom), purple last (lands on top)
    private static final Block[] TERRACOTTA = {
        Blocks.RED_TERRACOTTA,
        Blocks.ORANGE_TERRACOTTA,
        Blocks.YELLOW_TERRACOTTA,
        Blocks.LIME_TERRACOTTA,
        Blocks.BLUE_TERRACOTTA,
        Blocks.PURPLE_TERRACOTTA
    };

    // Default top-block pool (used when "top_blocks" is absent from JSON)
    private static final Block[] DEFAULT_PRECIOUS = {
        Blocks.DIAMOND_BLOCK,
        Blocks.GOLD_BLOCK,
        Blocks.IRON_BLOCK,
        Blocks.EMERALD_BLOCK
    };

    private static final int SPAWN_HEIGHT    = 15;
    private static final int BLOCK_INTERVAL  = 6;
    private static final int FALL_TICKS      = 30;
    private static final int LIGHTNING_EXTRA = 5;
    private static final int PARTICLE_INTERVAL = 4;

    private final List<Block> topBlocks;
    private final int[]       cumWeights;
    private final int         fallTicks;
    private final int         lightningExtra;
    private final int         clearHeight;

    private RainbowColumnEvent(List<Block> topBlocks, int[] cumWeights, int fallTicks, int lightningExtra, int clearHeight) {
        this.topBlocks      = topBlocks;
        this.cumWeights     = cumWeights;
        this.fallTicks      = fallTicks;
        this.lightningExtra = lightningExtra;
        this.clearHeight    = clearHeight;
    }

    public static RainbowColumnEvent fromJson(JsonObject obj) {
        List<Block>   blocks  = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        if (obj.has("top_blocks")) {
            JsonArray arr = obj.getAsJsonArray("top_blocks");
            for (var el : arr) {
                JsonObject entry = el.getAsJsonObject();
                String id = entry.get("block").getAsString();
                int    w  = entry.has("weight") ? entry.get("weight").getAsInt() : 10;
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
                if (block == Blocks.AIR) {
                    LuckyBreak.LOGGER.warn("[LuckyBreak] rainbow_column: unknown block '{}', skipping.", id);
                    continue;
                }
                blocks.add(block);
                weights.add(w);
            }
        }

        if (blocks.isEmpty()) {
            for (Block b : DEFAULT_PRECIOUS) { blocks.add(b); weights.add(25); }
        }

        int[] cum = new int[weights.size()];
        int running = 0;
        for (int i = 0; i < weights.size(); i++) { running += weights.get(i); cum[i] = running; }

        int fallTicks      = obj.has("fall_ticks")           ? obj.get("fall_ticks").getAsInt()           : FALL_TICKS;
        int lightningExtra = obj.has("lightning_delay_ticks") ? obj.get("lightning_delay_ticks").getAsInt() : LIGHTNING_EXTRA;
        int clearHeight    = obj.has("clear_height") ? obj.get("clear_height").getAsInt() : SPAWN_HEIGHT + 5;
        if (clearHeight < 1) {
            clearHeight = 1;
        }

        return new RainbowColumnEvent(blocks, cum, fallTicks, lightningExtra, clearHeight);
    }

    private Block pickTopBlock(RandomSource rng) {
        int roll = rng.nextInt(cumWeights[cumWeights.length - 1]);
        for (int i = 0; i < cumWeights.length; i++) {
            if (roll < cumWeights[i]) return topBlocks.get(i);
        }
        return topBlocks.get(topBlocks.size() - 1);
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Block precious = pickTopBlock(level.getRandom());
        BlockPos spawnPos = pos.above(SPAWN_HEIGHT);

        // Clear the vertical drop lane so falling blocks are not obstructed.
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = 1; y <= clearHeight; y++) {
            level.setBlock(pos.above(y), air, 3);
        }

        // Drop each terracotta block from above with staggered timing
        for (int i = 0; i < TERRACOTTA.length; i++) {
            final Block block = TERRACOTTA[i];
            LuckyScheduler.INSTANCE.schedule(i * BLOCK_INTERVAL + 1, () ->
                FallingBlockEntity.fall(level, spawnPos, block.defaultBlockState())
            );
        }

        // Precious block falls after all terracotta
        int preciousDelay = TERRACOTTA.length * BLOCK_INTERVAL + 1;
        LuckyScheduler.INSTANCE.schedule(preciousDelay, () ->
            FallingBlockEntity.fall(level, spawnPos, precious.defaultBlockState())
        );

        // Lightning strikes the top of the stack after everything has landed
        int strikeDelay = preciousDelay + fallTicks + lightningExtra;
        BlockPos topPos = pos.above(TERRACOTTA.length + 1);

        // Add celebratory villager particles while blocks are falling.
        for (int t = 0; t <= strikeDelay; t += PARTICLE_INTERVAL) {
            final int tick = t;
            LuckyScheduler.INSTANCE.schedule(tick, () -> {
                double progress = strikeDelay <= 0 ? 1.0 : Math.min(1.0, (double) tick / (double) strikeDelay);
                double y = pos.getY() + 1.0 + progress * (TERRACOTTA.length + 1);
                level.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5,
                        y,
                        pos.getZ() + 0.5,
                        12,
                        0.45,
                        0.55,
                        0.45,
                        0.02
                );
            });
        }

        LuckyScheduler.INSTANCE.schedule(strikeDelay, () -> {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.setPos(topPos.getX() + 0.5, (double) topPos.getY(), topPos.getZ() + 0.5);
                level.addFreshEntity(bolt);
            }
        });
    }
}
