package com.luckybreak.mixin;

import com.luckybreak.api.event.lifecycle.v1.CommonLifecycleEvents;
import com.luckybreak.api.event.lifecycle.v1.ServerLifecycleEvents;
import com.luckybreak.api.event.lifecycle.v1.ServerTickEvents;
import com.luckybreak.api.worldgen.v1.BiomeModifications;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerHooksMixin {

    private static boolean luckybreak$serverStartedFired;

    // Apply biome modifications before any ChunkGenerator is built.
    // createLevels() is called once during server startup, before any chunk can be generated.
    @Inject(method = "createLevels", at = @At("HEAD"))
    private void luckybreak$onCreateLevels(CallbackInfo ci) {
        BiomeModifications.applyAll(((MinecraftServer) (Object) this).registryAccess());
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void luckybreak$onTickServer(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (!luckybreak$serverStartedFired) {
            ServerLifecycleEvents.SERVER_STARTED.invoker().onServerStarted(server);
            CommonLifecycleEvents.TAGS_LOADED.invoker().onTagsLoaded(server, false);
            luckybreak$serverStartedFired = true;
        }
        ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server);
    }

    @Inject(method = "reloadResources", at = @At("RETURN"))
    private void luckybreak$onReloadResources(Collection<String> dataPacks, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        cir.getReturnValue().thenRun(() -> {
            ServerLifecycleEvents.END_DATA_PACK_RELOAD.invoker().onEndDataPackReload(server, server.getResourceManager());
            CommonLifecycleEvents.TAGS_LOADED.invoker().onTagsLoaded(server, false);
        });
    }
}
