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
 * The Lucky Block class. This block will drop different items depending on the player's luck.
 */
public class LuckyBlock extends Block {
    private static final Random RANDOM = new Random();

    /**
     * Constructor for Lucky Block.
     */
    public LuckyBlock() {
        super(Settings.create()
                .mapColor(Blocks.STONE.getDefaultMapColor())
                .strength(0.5f)
                .requiresTool()
                .luminance(state -> 5)
                .sounds(BlockSoundGroup.AMETHYST_BLOCK));
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            // Get the player's luck, or a random value if luck attribute is not accessible
            float luckFactor = player.getLuck() + RANDOM.nextFloat();
            
            // Different tiers of drops based on luck factor
            if (luckFactor < 0.3) {
                // Very unlucky - bad outcomes
                dropUnluckyItems(world, pos);
            } else if (luckFactor < 0.7) {
                // Normal - average drops
                dropNormalItems(world, pos);
            } else if (luckFactor < 0.9) {
                // Lucky - good drops
                dropLuckyItems(world, pos);
            } else {
                // Very lucky - excellent drops
                dropVeryLuckyItems(world, pos);
            }
        }
        
        super.onBreak(world, pos, state, player);
    }
    
    /**
     * Drop unlucky items - these are generally negative outcomes.
     */
    private void dropUnluckyItems(World world, BlockPos pos) {
        int outcome = RANDOM.nextInt(5);
        
        switch (outcome) {
            case 0:
                // Spawn a small explosion
                world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 2.0f, false, ExplosionSourceType.BLOCK);
                break;
            case 1:
                // Drop some dirt
                dropStack(world, pos, new ItemStack(Items.DIRT, 5));
                break;
            case 2:
                // Drop a single piece of coal
                dropStack(world, pos, new ItemStack(Items.COAL, 1));
                break;
            case 3:
                // Drop a poisonous potato
                dropStack(world, pos, new ItemStack(Items.POISONOUS_POTATO, 1));
                break;
            case 4:
                // Drop a dead bush
                dropStack(world, pos, new ItemStack(Items.DEAD_BUSH, 2));
                break;
        }
    }
    
    /**
     * Drop normal items - these are average drops.
     */
    private void dropNormalItems(World world, BlockPos pos) {
        int outcome = RANDOM.nextInt(5);
        
        switch (outcome) {
            case 0:
                // Drop some iron ingots
                dropStack(world, pos, new ItemStack(Items.IRON_INGOT, 3));
                break;
            case 1:
                // Drop some wheat
                dropStack(world, pos, new ItemStack(Items.WHEAT, 8));
                break;
            case 2:
                // Drop a few coal
                dropStack(world, pos, new ItemStack(Items.COAL, 6));
                break;
            case 3:
                // Drop some planks
                dropStack(world, pos, new ItemStack(Items.OAK_PLANKS, 16));
                break;
            case 4:
                // Drop some stone
                dropStack(world, pos, new ItemStack(Items.STONE, 20));
                break;
        }
    }
    
    /**
     * Drop lucky items - these are good drops.
     */
    private void dropLuckyItems(World world, BlockPos pos) {
        int outcome = RANDOM.nextInt(5);
        
        switch (outcome) {
            case 0:
                // Drop some gold ingots
                dropStack(world, pos, new ItemStack(Items.GOLD_INGOT, 5));
                break;
            case 1:
                // Drop a diamond
                dropStack(world, pos, new ItemStack(Items.DIAMOND, 2));
                break;
            case 2:
                // Drop an iron chestplate
                dropStack(world, pos, new ItemStack(Items.IRON_CHESTPLATE, 1));
                break;
            case 3:
                // Drop some bread
                dropStack(world, pos, new ItemStack(Items.BREAD, 10));
                break;
            case 4:
                // Drop some emeralds
                dropStack(world, pos, new ItemStack(Items.EMERALD, 3));
                break;
        }
    }
    
    /**
     * Drop very lucky items - these are excellent drops.
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