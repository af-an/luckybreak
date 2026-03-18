package com.luckybreak.api.event.lifecycle.v1;

import com.luckybreak.api.event.Event;
import com.luckybreak.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

public final class CommonLifecycleEvents {

    private CommonLifecycleEvents() {
    }

    public static final Event<TagsLoaded> TAGS_LOADED = EventFactory.createArrayBacked(TagsLoaded.class, callbacks -> (server, client) -> {
        for (TagsLoaded callback : callbacks) {
            callback.onTagsLoaded(server, client);
        }
    });

    @FunctionalInterface
    public interface TagsLoaded {
        void onTagsLoaded(MinecraftServer server, boolean client);
    }
}