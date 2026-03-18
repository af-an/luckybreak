package com.luckybreak.mixin;

import com.luckybreak.api.command.v2.CommandRegistrationCallback;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsHooksMixin {

    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;
    @Unique private boolean luckybreak$commandsRegistered;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void luckybreak$onConstructed(CallbackInfo ci) {
        if (luckybreak$commandsRegistered) {
            return;
        }
        luckybreak$commandsRegistered = true;
        CommandRegistrationCallback.EVENT.invoker().register(dispatcher);
    }
}
