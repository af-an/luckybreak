package com.luckybreak;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.ExplosionSourceType;

import java.util.Random;

/**
 * The Unlucky Block class. This block will primarily drop negative outcomes.
 */
public class UnluckyBlock extends Block {
    private static final Random RANDOM = new Random();

    /**
     * Constructor for Unlucky Block.
     */
    public UnluckyBlock() {
        super(Settings.create()
                .mapColor(Blocks.STONE.getDefaultMapColor())
                .strength(0.5f)
                .requiresTool()
                .luminance(state -> 3)
                .sounds(BlockSoundGroup.STONE));
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            // Random chance for different outcomes
            float chance = RANDOM.nextFloat();
            
            if (chance < 0.6f) {
                // 60% chance to explode
                createExplosion(world, pos);
            } else if (chance < 0.98f) {
                // 38% chance for unlucky drops
                dropUnluckyItems(world, pos);
            } else {
                // 2% chance for very lucky drops (rare good outcome)
                dropVeryLuckyItems(world, pos);
            }
        }
        
        super.onBreak(world, pos, state, player);
    }
    
    /**
     * Create an explosion at the block's position
     */
    private void createExplosion(World world, BlockPos pos) {
        world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 3.0f, true, ExplosionSourceType.BLOCK);
    }
    
    /**
     * Drop unlucky items - these are generally negative outcomes.
     */
    private void dropUnluckyItems(World world, BlockPos pos) {
        int outcome = RANDOM.nextInt(6);
        
        switch (outcome) {
            case 0:
                // Drop some dirt
                dropStack(world, pos, new ItemStack(Items.DIRT, 5));
                break;
            case 1:
                // Drop a single piece of coal
                dropStack(world, pos, new ItemStack(Items.COAL, 1));
                break;
            case 2:
                // Drop a poisonous potato
                dropStack(world, pos, new ItemStack(Items.POISONOUS_POTATO, 3));
                break;
            case 3:
                // Drop some rotten flesh
                dropStack(world, pos, new ItemStack(Items.ROTTEN_FLESH, 8));
                break;
            case 4:
                // Drop a dead bush
                dropStack(world, pos, new ItemStack(Items.DEAD_BUSH, 4));
                break;
            case 5:
                // Spawn some silverfish
                for (int i = 0; i < 3; i++) {
                    world.spawnEntity(net.minecraft.entity.EntityType.SILVERFISH.create(world));
                }
                break;
        }
    }
    
    /**
     * Drop very lucky items - these are excellent drops (rare chance).
     */
    private void dropVeryLuckyItems(World world, BlockPos pos) {
        int outcome = RANDOM.nextInt(5);
        
        switch (outcome) {
            case 0:
                // Drop a few diamonds
                dropStack(world, pos, new ItemStack(Items.DIAMOND, 5));
                break;
            case 1:
                // Drop netherite ingot
                dropStack(world, pos, new ItemStack(Items.NETHERITE_INGOT, 1));
                break;
            case 2:
                // Drop enchanted diamond armor
                ItemStack diamondChestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
                diamondChestplate.addEnchantment(net.minecraft.enchantment.Enchantments.PROTECTION, 4);
                dropStack(world, pos, diamondChestplate);
                break;
            case 3:
                // Drop a few enchanted golden apples
                dropStack(world, pos, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 2));
                break;
            case 4:
                // Drop emerald blocks
                dropStack(world, pos, new ItemStack(Items.EMERALD_BLOCK, 3));
                break;
        }
    }
} 