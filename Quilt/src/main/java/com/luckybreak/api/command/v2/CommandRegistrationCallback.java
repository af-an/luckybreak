package com.luckybreak.api.command.v2;

import com.luckybreak.api.event.Event;
import com.luckybreak.api.event.EventFactory;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public interface CommandRegistrationCallback {

    Event<CommandRegistrationCallback> EVENT = EventFactory.createArrayBacked(CommandRegistrationCallback.class, callbacks -> dispatcher -> {
        for (CommandRegistrationCallback callback : callbacks) {
            callback.register(dispatcher);
        }
    });

    void register(CommandDispatcher<CommandSourceStack> dispatcher);
}