package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawns one random treasure block (diamond/gold/emerald by default) and
 * emits celebratory particles.
 *
 * JSON:
 * {
 *   "type": "luckybreak:spawn_treasure_block",
 *   "weight": 12,
 *   "blocks": [
 *     "minecraft:diamond_block",
 *     "minecraft:gold_block",
 *     "minecraft:emerald_block"
 *   ],
 *   "particle_count": 36
 * }
 */
public class SpawnTreasureBlockEvent implements LuckyEvent {

    private record WeightedBlock(Block block, int cumulativeWeight) {}

    private final List<WeightedBlock> treasureBlocks;
    private final int particleCount;

    private SpawnTreasureBlockEvent(List<WeightedBlock> treasureBlocks, int particleCount) {
        this.treasureBlocks = treasureBlocks;
        this.particleCount = particleCount;
    }

    public static SpawnTreasureBlockEvent fromJson(JsonObject obj) {
        List<Block> blocks = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        if (obj.has("blocks")) {
            JsonArray arr = obj.getAsJsonArray("blocks");
            for (JsonElement el : arr) {
                String id;
                int weight = 1;
                if (el.isJsonObject()) {
                    JsonObject entry = el.getAsJsonObject();
                    id = entry.get("block").getAsString();
                    if (entry.has("weight")) {
                        weight = entry.get("weight").getAsInt();
                    }
                } else {
                    id = el.getAsString();
                }

                if (weight <= 0) {
                    continue;
                }

                Block block = BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse(id));
                if (block != null && block != Blocks.AIR) {
                    blocks.add(block);
                    weights.add(weight);
                }
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(Blocks.DIAMOND_BLOCK);
            weights.add(1);
            blocks.add(Blocks.GOLD_BLOCK);
            weights.add(1);
            blocks.add(Blocks.EMERALD_BLOCK);
            weights.add(1);
        }

        List<WeightedBlock> weightedBlocks = new ArrayList<>(blocks.size());
        int running = 0;
        for (int i = 0; i < blocks.size(); i++) {
            running += weights.get(i);
            weightedBlocks.add(new WeightedBlock(blocks.get(i), running));
        }

        int particleCount = obj.has("particle_count") ? obj.get("particle_count").getAsInt() : 36;
        if (particleCount < 0) {
            particleCount = 0;
        }

        return new SpawnTreasureBlockEvent(weightedBlocks, particleCount);
    }

    private Block pickBlock(RandomSource random) {
        int totalWeight = treasureBlocks.get(treasureBlocks.size() - 1).cumulativeWeight();
        int roll = random.nextInt(totalWeight);
        for (WeightedBlock weightedBlock : treasureBlocks) {
            if (roll < weightedBlock.cumulativeWeight()) {
                return weightedBlock.block();
            }
        }
        return treasureBlocks.get(treasureBlocks.size() - 1).block();
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Block chosen = pickBlock(level.getRandom());
        level.setBlock(pos, chosen.defaultBlockState(), 3);

        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5,
                pos.getY() + 0.7,
                pos.getZ() + 0.5,
                particleCount,
                0.45,
                0.35,
                0.45,
                0.02
        );

            level.playSound(
                null,
                pos,
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS,
                0.5f,
                1.15f
            );
    }
}