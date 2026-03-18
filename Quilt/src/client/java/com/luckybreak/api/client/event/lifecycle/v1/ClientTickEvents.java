package com.luckybreak.api.client.event.lifecycle.v1;

import com.luckybreak.api.event.Event;
import com.luckybreak.api.event.EventFactory;
import net.minecraft.client.Minecraft;

public final class ClientTickEvents {

    private ClientTickEvents() {
    }

    public static final Event<EndTick> END_CLIENT_TICK = EventFactory.createArrayBacked(EndTick.class, callbacks -> client -> {
        for (EndTick callback : callbacks) {
            callback.onEndTick(client);
        }
    });

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(Minecraft client);
    }
}