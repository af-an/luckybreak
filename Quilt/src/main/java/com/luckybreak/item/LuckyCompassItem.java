package com.luckybreak.item;

import com.luckybreak.events.LuckyEventRegistry;
import com.luckybreak.tooltip.TooltipConfig;
import com.luckybreak.world.LuckyBlockTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Consumer;

public class LuckyCompassItem extends Item {

    /** Maximum chunk radius to search outward from the player. 512 chunks ≈ 8192 blocks. */
    private static final int MAX_SEARCH_RADIUS_CHUNKS = 512;

    /** Maximum durability damage per single use. */
    private static final int MAX_DAMAGE_PER_USE = 32;

    public LuckyCompassItem(ResourceKey<Item> key) {
        super(new Item.Properties().setId(key).stacksTo(1).durability(64).repairable(Items.GOLD_INGOT));
    }

    /** Tooltip shown when the item is hovered in inventory. */
    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Component configured = TooltipConfig.getMenuTooltipFor(stack);
        if (configured != null) {
            int color = TooltipConfig.getMenuColorFor(stack);
            tooltip.accept(configured.copy().withStyle(style -> style.withColor(TextColor.fromRgb(color))));
        }
    }

    /**
     * Right-click handler. Server-side only.
     *
     * Queries LuckyBlockTracker for the nearest Lucky Block in this dimension,
     * then writes a LodestoneTracker component so the needle points toward it.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }

        ResourceKey<Level> dimension = level.dimension();
        if (dimension == Level.NETHER) {
            player.displayClientMessage(
                Component.literal(LuckyEventRegistry.INSTANCE.luckyCompassBlockedNetherMessage())
                    .withStyle(style -> style.withColor(TextColor.fromRgb(
                        LuckyEventRegistry.INSTANCE.luckyCompassBlockedNetherMessageColor()
                    ))), false);
            return InteractionResult.FAIL;
        }
        if (dimension == Level.END) {
            player.displayClientMessage(
                Component.literal(LuckyEventRegistry.INSTANCE.luckyCompassBlockedEndMessage())
                    .withStyle(style -> style.withColor(TextColor.fromRgb(
                        LuckyEventRegistry.INSTANCE.luckyCompassBlockedEndMessageColor()
                    ))), false);
            return InteractionResult.FAIL;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        LuckyBlockTracker tracker = LuckyBlockTracker.get(serverLevel);
        BlockPos nearest = tracker.findNearest(player.blockPosition(), MAX_SEARCH_RADIUS_CHUNKS);

        if (nearest == null) {
            player.displayClientMessage(
                Component.literal(LuckyEventRegistry.INSTANCE.luckyCompassNotFoundMessage())
                    .withStyle(style -> style.withColor(TextColor.fromRgb(
                        LuckyEventRegistry.INSTANCE.luckyCompassNotFoundMessageColor()
                    ))), false);
            return InteractionResult.FAIL;
        }

        GlobalPos globalPos = GlobalPos.of(serverLevel.dimension(), nearest);
        ItemStack compass = player.getItemInHand(hand);
        // tracked=false: needle points perpetually; our tracker handles block removal.
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(globalPos), false));

        // Apply durability damage scaled by distance, capped at MAX_DAMAGE_PER_USE.
        // At least 1 durability is consumed whenever a Lucky Block is found.
        double distance = Math.sqrt(player.blockPosition().distSqr(nearest));
        int damage = (int) Math.max(5, Math.min(distance / 25.0, MAX_DAMAGE_PER_USE));
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        compass.hurtAndBreak(damage, player, slot);

        player.displayClientMessage(
            Component.literal(LuckyEventRegistry.INSTANCE.luckyCompassFoundMessage())
                .withStyle(style -> style.withColor(TextColor.fromRgb(
                    LuckyEventRegistry.INSTANCE.luckyCompassFoundMessageColor()
                ))), false);

        return InteractionResult.SUCCESS;
    }

    /**
     * Resets a compass stack so its needle spins freely (points to spawn).
     * Called when the tracked Lucky Block is broken.
     */
    public static void resetCompass(ItemStack stack) {
        stack.remove(DataComponents.LODESTONE_TRACKER);
    }
}
