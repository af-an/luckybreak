package com.luckybreak.api.event.lifecycle.v1;

import com.luckybreak.api.event.Event;
import com.luckybreak.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ServerLifecycleEvents {

    private ServerLifecycleEvents() {
    }

    public static final Event<ServerStarted> SERVER_STARTED = EventFactory.createArrayBacked(ServerStarted.class, callbacks -> server -> {
        for (ServerStarted callback : callbacks) {
            callback.onServerStarted(server);
        }
    });

    public static final Event<EndDataPackReload> END_DATA_PACK_RELOAD = EventFactory.createArrayBacked(EndDataPackReload.class, callbacks -> (server, resourceManager) -> {
        for (EndDataPackReload callback : callbacks) {
            callback.onEndDataPackReload(server, resourceManager);
        }
    });

    @FunctionalInterface
    public interface ServerStarted {
        void onServerStarted(MinecraftServer server);
    }

    @FunctionalInterface
    public interface EndDataPackReload {
        void onEndDataPackReload(MinecraftServer server, ResourceManager resourceManager);
    }
}