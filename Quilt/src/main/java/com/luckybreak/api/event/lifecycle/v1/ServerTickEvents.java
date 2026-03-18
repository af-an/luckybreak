package com.luckybreak.api.event.lifecycle.v1;

import com.luckybreak.api.event.Event;
import com.luckybreak.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class ServerTickEvents {

    private ServerTickEvents() {
    }

    public static final Event<EndTick> END_SERVER_TICK = EventFactory.createArrayBacked(EndTick.class, callbacks -> server -> {
        for (EndTick callback : callbacks) {
            callback.onEndTick(server);
        }
    });

    public static final Event<EndWorldTick> END_WORLD_TICK = EventFactory.createArrayBacked(EndWorldTick.class, callbacks -> world -> {
        for (EndWorldTick callback : callbacks) {
            callback.onEndTick(world);
        }
    });

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface EndWorldTick {
        void onEndTick(ServerLevel world);
    }
}