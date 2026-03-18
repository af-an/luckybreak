package com.luckybreak.item;

import com.luckybreak.ModEntities;
import com.luckybreak.entity.GoldenHenConfig;
import com.luckybreak.entity.GoldenHenEntity;
import com.luckybreak.tooltip.TooltipConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class GoldenHenSpawnEggItem extends Item {

    public GoldenHenSpawnEggItem(ResourceKey<Item> key) {
        super(new Item.Properties().setId(key));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Component configured = TooltipConfig.getMenuTooltipFor(stack);
        if (configured != null) {
            int color = TooltipConfig.getMenuColorFor(stack);
            tooltip.accept(configured.copy().withStyle(style -> style.withColor(TextColor.fromRgb(color))));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = clicked.relative(face);

        GoldenHenEntity mob = ModEntities.GOLDEN_HEN.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (mob == null) {
            return InteractionResult.FAIL;
        }

        mob.setPos(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
            spawnPos.getZ() + 0.5D
        );
        mob.setYRot(context.getRotation());

        if (!((ServerLevel) level).addFreshEntity(mob)) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof GoldenHenEntity parent)) {
            return InteractionResult.PASS;
        }

        if (!GoldenHenConfig.allowSpawnEggOnParent()) {
            return InteractionResult.PASS;
        }

        Level level = parent.level();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        GoldenHenEntity baby = ModEntities.GOLDEN_HEN.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (baby == null) {
            return InteractionResult.FAIL;
        }

        baby.setBaby(true);
        baby.setPos(parent.getX(), parent.getY(), parent.getZ());
        baby.setYRot(parent.getYRot());

        if (!((ServerLevel) level).addFreshEntity(baby)) {
            return InteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}
