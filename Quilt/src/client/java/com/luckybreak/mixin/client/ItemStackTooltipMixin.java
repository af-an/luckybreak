package com.luckybreak.mixin.client;

import com.luckybreak.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void luckybreak$fireTooltipCallback(Item.TooltipContext context, @Nullable Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> updated = new ArrayList<>(cir.getReturnValue());
        ItemTooltipCallback.EVENT.invoker().getTooltip((ItemStack) (Object) this, context, flag, updated);
        cir.setReturnValue(updated);
    }
}
