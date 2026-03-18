package com.luckybreak.item;

import com.luckybreak.tooltip.TooltipConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LuckyPotionItem extends PotionItem {

    private static final String ITEM_ID = "lucky_potion";

    public LuckyPotionItem(ResourceKey<Item> key) {
        super(LuckyItemVisuals.apply(ITEM_ID, new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .usingConvertsTo(Items.GLASS_BOTTLE)
                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                .component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)));
    }

    @Override
    public Component getName(ItemStack stack) {
        return LuckyItemVisuals.styleName(ITEM_ID, this.getName());
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> tooltip,
                                TooltipFlag flag) {
        TooltipDisplay effectiveDisplay = display.withHidden(
            DataComponents.POTION_CONTENTS,
            TooltipConfig.getHidePotionEffectsFor(stack)
        );
        super.appendHoverText(stack, context, effectiveDisplay, tooltip, flag);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);

        Component hudMessage = TooltipConfig.getHeldTooltipFor(this);
        if (hudMessage != null && level.isClientSide() && livingEntity instanceof Player player) {
            int hudColor = TooltipConfig.getHeldColorFor(this);
            player.displayClientMessage(
                hudMessage.copy().withStyle(style -> style.withColor(TextColor.fromRgb(hudColor))),
                true
            );
        }

        if (level.isClientSide()) {
            return result;
        }

        LuckyPotionConfig.PositiveEffectSettings settings = LuckyPotionConfig.positiveEffectSettings();
        if (!settings.enabled() || settings.entries().isEmpty()) {
            return result;
        }

        int maxEffects = Math.min(settings.entries().size(), Math.max(settings.minEffects(), settings.maxEffects()));
        int minEffects = Math.min(Math.max(1, settings.minEffects()), maxEffects);
        int count = minEffects;
        if (maxEffects > minEffects) {
            count += level.random.nextInt(maxEffects - minEffects + 1);
        }

        List<LuckyPotionConfig.PositiveEffectEntry> pool = new ArrayList<>(settings.entries());
        var effectRegistry = level.registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            LuckyPotionConfig.PositiveEffectEntry selected = pickWeightedEntry(level, pool);
            if (selected == null) {
                break;
            }

            MobEffect effect = effectRegistry.getValue(selected.id());
            if (effect != null) {
                Holder<MobEffect> holder = effectRegistry.wrapAsHolder(effect);
                livingEntity.addEffect(new MobEffectInstance(holder, selected.durationTicks(), selected.amplifier()));
            }

            pool.remove(selected);
        }

        return result;
    }

    private LuckyPotionConfig.PositiveEffectEntry pickWeightedEntry(Level level, List<LuckyPotionConfig.PositiveEffectEntry> entries) {
        int totalWeight = 0;
        for (LuckyPotionConfig.PositiveEffectEntry entry : entries) {
            totalWeight += Math.max(0, entry.weight());
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = level.random.nextInt(totalWeight);
        for (LuckyPotionConfig.PositiveEffectEntry entry : entries) {
            int weight = Math.max(0, entry.weight());
            if (weight <= 0) {
                continue;
            }
            if (roll < weight) {
                return entry;
            }
            roll -= weight;
        }

        return null;
    }
}
