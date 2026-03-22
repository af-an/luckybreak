package com.luckybreak.mixin.client;

import com.luckybreak.api.itemgroup.v1.CreativeTabRegistry;
import com.luckybreak.mixin.CreativeModeTabAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Unique
    private static int luckybreak$currentPage = 0;

    /** Kept as instance fields so active state can be updated without a full rebuildWidgets. */
    @Unique
    private Button luckybreak$prevBtn;
    @Unique
    private Button luckybreak$nextBtn;

    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    protected abstract void selectTab(CreativeModeTab tab);

    // Use accessor mixins instead of @Shadow for inherited members
    @Unique
    private int luckybreak$leftPos() {
        return ((AbstractContainerScreenAccessorMixin)(Object) this).getLeftPos();
    }

    @Unique
    private int luckybreak$topPos() {
        return ((AbstractContainerScreenAccessorMixin)(Object) this).getTopPos();
    }

    // Use the ScreenInvokerMixin accessor instead of shadowing the Screen-level method
    @Unique
    private void luckybreak$addWidget(Button btn) {
        ((ScreenInvokerMixin)(Object) this).luckybreak$addRenderableWidget(btn);
    }

    /**
     * The three vanilla "utility" tabs that have no page-specific items — they must remain
     * visible and clickable on every page: Search Items, Saved Hotbars, Survival Inventory.
     */
    @Unique
    private static boolean luckybreak$isSpecialTab(ResourceKey<CreativeModeTab> key) {
        if (key == null) return false;
        if (!"minecraft".equals(key.location().getNamespace())) return false;
        String path = key.location().getPath();
        return "search".equals(path) || "hotbar".equals(path) || "inventory".equals(path);
    }

    // -------------------------------------------------------------------------
    // init() — assign row/column to modded tabs and add page navigation buttons
    // -------------------------------------------------------------------------
    @Inject(method = "init", at = @At("RETURN"))
    private void luckybreak$init(CallbackInfo ci) {
        List<ResourceKey<CreativeModeTab>> moddedKeys = CreativeTabRegistry.getModdedTabKeys();
        if (moddedKeys.isEmpty()) {
            return;
        }

        // Assign real row/column to each modded tab (page 1)
        CreativeModeTab.Row[] rows = {CreativeModeTab.Row.TOP, CreativeModeTab.Row.BOTTOM};
        int count = 0;
        for (ResourceKey<CreativeModeTab> key : moddedKeys) {
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(key.location());
            if (tab == null) {
                continue;
            }
            int rowIndex = count < 7 ? 0 : 1;
            int col = count < 7 ? count : count - 7;
            CreativeModeTabAccessor accessor = (CreativeModeTabAccessor) tab;
            accessor.setRow(rows[rowIndex]);
            accessor.setColumn(col);
            count++;
        }

        // Page navigation buttons (top-right corner of the creative screen header).
        // Active state is set immediately so the correct button is greyed out on each page.
        int bx = luckybreak$leftPos() + 170;
        int by = luckybreak$topPos() + 4;
        luckybreak$prevBtn = Button.builder(Component.literal("<"),
                btn -> luckybreak$switchPage(0)).bounds(bx, by, 11, 10).build();
        luckybreak$nextBtn = Button.builder(Component.literal(">"),
                btn -> luckybreak$switchPage(1)).bounds(bx + 11, by, 11, 10).build();
        luckybreak$prevBtn.active = (luckybreak$currentPage > 0);
        luckybreak$nextBtn.active = (luckybreak$currentPage < 1);
        luckybreak$addWidget(luckybreak$prevBtn);
        luckybreak$addWidget(luckybreak$nextBtn);

        // Ensure a proper content tab (not a utility tab) is selected for this page
        luckybreak$ensureSelectedTabVisible();
    }

    @Unique
    private void luckybreak$switchPage(int page) {
        if (luckybreak$currentPage == page) {
            return;
        }
        luckybreak$currentPage = page;
        // Update button active states immediately so the UI reflects the new page
        if (luckybreak$prevBtn != null) luckybreak$prevBtn.active = (luckybreak$currentPage > 0);
        if (luckybreak$nextBtn != null) luckybreak$nextBtn.active = (luckybreak$currentPage < 1);
        luckybreak$ensureSelectedTabVisible();
    }

    @Unique
    private void luckybreak$ensureSelectedTabVisible() {
        // Only re-select if the currently selected tab is either missing or a utility tab
        // (utility tabs have no page-specific content, so selecting one on a page change
        //  would leave the item grid empty).
        if (selectedTab != null) {
            ResourceKey<CreativeModeTab> selKey =
                    BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(selectedTab).orElse(null);
            if (!luckybreak$isSpecialTab(selKey) && luckybreak$isTabOnCurrentPage(selectedTab)) {
                return;
            }
        }
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            ResourceKey<CreativeModeTab> key =
                    BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElse(null);
            if (luckybreak$isSpecialTab(key)) continue; // skip utility tabs when auto-selecting
            if (luckybreak$isTabOnCurrentPage(tab) && tab.shouldDisplay()) {
                selectTab(tab);
                return;
            }
        }
    }

    @Unique
    private boolean luckybreak$isTabOnCurrentPage(CreativeModeTab tab) {
        ResourceKey<CreativeModeTab> key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElse(null);
        if (key == null) return true;
        // Search, Saved Hotbars, and Survival Inventory are always visible on every page
        if (luckybreak$isSpecialTab(key)) return true;
        boolean isModded = CreativeTabRegistry.isModdedTab(key);
        return isModded ? (luckybreak$currentPage == 1) : (luckybreak$currentPage == 0);
    }

    // -------------------------------------------------------------------------
    // Filter tab rendering and interaction to only the current page
    // -------------------------------------------------------------------------

    @Inject(method = "renderTabButton", at = @At("HEAD"), cancellable = true)
    private void luckybreak$renderTabButton(GuiGraphics guiGraphics,
                                            CreativeModeTab tab, CallbackInfo ci) {
        if (!luckybreak$isTabOnCurrentPage(tab)) {
            ci.cancel();
        }
    }

    @Inject(method = "checkTabClicked", at = @At("HEAD"), cancellable = true)
    private void luckybreak$checkTabClicked(CreativeModeTab tab, double x, double y,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!luckybreak$isTabOnCurrentPage(tab)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
    private void luckybreak$checkTabHovering(GuiGraphics guiGraphics, CreativeModeTab tab,
                                             int mouseX, int mouseY,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!luckybreak$isTabOnCurrentPage(tab)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "selectTab", at = @At("HEAD"), cancellable = true)
    private void luckybreak$selectTabGuard(CreativeModeTab tab, CallbackInfo ci) {
        if (!luckybreak$isTabOnCurrentPage(tab)) {
            ci.cancel();
        }
    }
}

