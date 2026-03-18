package com.luckybreak.mixin;

import com.luckybreak.LuckyBreak;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Injects the mod's classpath resources (lang, models, textures, data) into every
 * PackRepository.openAllSelected() call.
 *
 * <p>Without Fabric API / QFAPI, classpath loading can diverge from the resource/data
 * pack pipeline. This bridge uses Quilt Loader's mod source roots directly so assets
 * and data are visible in both development and packaged runtime.</p>
 */
@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackRepositoryMixin.class);
    private static final String PACK_ID_PREFIX = "mod/luckybreak/bridge";

    @Inject(method = "openAllSelected", at = @At("RETURN"), cancellable = true)
    private void luckybreak$injectModResources(CallbackInfoReturnable<List<PackResources>> cir) {
        List<PackResources> current = cir.getReturnValue();
        if (current == null || current.isEmpty()) {
            return;
        }

        if (current.stream().anyMatch(pack -> pack.packId().startsWith(PACK_ID_PREFIX))) {
            return;
        }

        Optional<ModContainer> container = QuiltLoader.getModContainer(LuckyBreak.MOD_ID);
        if (container.isEmpty()) {
            return;
        }

        Set<Path> candidateRoots = new LinkedHashSet<>();
        ModContainer mod = container.get();
        candidateRoots.add(mod.rootPath());
        for (List<Path> sourceGroup : mod.getSourcePaths()) {
            candidateRoots.addAll(sourceGroup);
        }

        List<PackResources> modified = new ArrayList<>(current);
        int injected = 0;

        for (Path root : candidateRoots) {
            if (root == null) {
                continue;
            }

            Path assetsRoot = root.resolve("assets").resolve(LuckyBreak.MOD_ID);
            Path dataRoot = root.resolve("data").resolve(LuckyBreak.MOD_ID);
            if (!Files.exists(assetsRoot) && !Files.exists(dataRoot)) {
                continue;
            }

            PackLocationInfo location = new PackLocationInfo(
                    PACK_ID_PREFIX + "/" + injected,
                    Component.literal("Lucky Break"),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );
            modified.add(new PathPackResources(location, root));
            injected++;
        }

        if (injected > 0) {
            cir.setReturnValue(modified);
            LOGGER.debug("Injected {} Lucky Break resource root(s) into PackRepository", injected);
        }
    }
}
