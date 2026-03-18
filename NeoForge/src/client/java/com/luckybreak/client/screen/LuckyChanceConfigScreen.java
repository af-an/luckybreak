package com.luckybreak.client.screen;

import com.luckybreak.config.LuckyChanceSettings;
import com.luckybreak.events.LuckyEventRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LuckyChanceConfigScreen extends Screen {

    private static final Component TITLE = Component.literal("Lucky Break Settings");
    private static final Component SUBTITLE = Component.literal("Fine-tune how often each event tier appears when you break a Lucky Block.");
    private static final Component SECTION_TITLE = Component.literal("Event Tier Chances");
    private static final Component SMALL_WINDOW_WARNING = Component.literal("For best readability, use a larger window or lower GUI Scale.");
    private static final int SAVE_TOOLTIP_TICKS = 48;
    private static final int MIN_READABLE_WIDTH = 500;
    private static final int MIN_READABLE_HEIGHT = 320;

    private final Screen previousScreen;
    private ChanceSlider luckySlider;
    private ChanceSlider averageSlider;
    private ChanceSlider unluckySlider;
    private Button luckyMinusButton;
    private Button luckyPlusButton;
    private Button averageMinusButton;
    private Button averagePlusButton;
    private Button unluckyMinusButton;
    private Button unluckyPlusButton;
    private Button luckyInfoButton;
    private Button averageInfoButton;
    private Button unluckyInfoButton;
    private Button saveButton;
    private Component statusMessage = Component.empty();
    private int statusColor = 0xFFFFFFFF;
    private int savedTooltipTicks;
    private Component actionTooltipMessage = Component.empty();
    private int actionTooltipColor = 0xFF55FF55;
    private boolean applyingAutoBalance;

    public LuckyChanceConfigScreen(Screen previousScreen) {
        super(TITLE);
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        Layout layout = computeLayout();

        LuckyChanceSettings.Chances current = currentChances();
        this.luckySlider = this.addRenderableWidget(new ChanceSlider(layout.sliderX(), layout.rowStartY(), layout.sliderWidth(), layout.controlHeight(), current.lucky(), Component.literal("Lucky")));
        this.averageSlider = this.addRenderableWidget(new ChanceSlider(layout.sliderX(), layout.rowStartY() + layout.rowSpacing(), layout.sliderWidth(), layout.controlHeight(), current.average(), Component.literal("Average")));
        this.unluckySlider = this.addRenderableWidget(new ChanceSlider(layout.sliderX(), layout.rowStartY() + (layout.rowSpacing() * 2), layout.sliderWidth(), layout.controlHeight(), current.unlucky(), Component.literal("Unlucky")));

        this.luckyMinusButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> stepSlider(this.luckySlider, -1))
            .bounds(layout.minusX(), layout.rowStartY(), layout.controlHeight(), layout.controlHeight())
            .build());
        this.luckyPlusButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> stepSlider(this.luckySlider, 1))
            .bounds(layout.plusX(), layout.rowStartY(), layout.controlHeight(), layout.controlHeight())
            .build());
        this.luckyInfoButton = addRenderableWidget(Button.builder(Component.literal("i"), b -> {
            })
            .bounds(layout.infoX(), layout.rowStartY(), layout.controlHeight(), layout.controlHeight())
            .tooltip(Tooltip.create(Component.literal("Lucky events are the best outcomes: bonus loot, powerful rewards, and positive surprises.")))
            .build());

        this.averageMinusButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> stepSlider(this.averageSlider, -1))
            .bounds(layout.minusX(), layout.rowStartY() + layout.rowSpacing(), layout.controlHeight(), layout.controlHeight())
            .build());
        this.averagePlusButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> stepSlider(this.averageSlider, 1))
            .bounds(layout.plusX(), layout.rowStartY() + layout.rowSpacing(), layout.controlHeight(), layout.controlHeight())
            .build());
        this.averageInfoButton = addRenderableWidget(Button.builder(Component.literal("i"), b -> {
            })
            .bounds(layout.infoX(), layout.rowStartY() + layout.rowSpacing(), layout.controlHeight(), layout.controlHeight())
            .tooltip(Tooltip.create(Component.literal("Average events are balanced outcomes and represent your standard Lucky Block behavior.")))
            .build());

        this.unluckyMinusButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> stepSlider(this.unluckySlider, -1))
            .bounds(layout.minusX(), layout.rowStartY() + (layout.rowSpacing() * 2), layout.controlHeight(), layout.controlHeight())
            .build());
        this.unluckyPlusButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> stepSlider(this.unluckySlider, 1))
            .bounds(layout.plusX(), layout.rowStartY() + (layout.rowSpacing() * 2), layout.controlHeight(), layout.controlHeight())
            .build());
        this.unluckyInfoButton = addRenderableWidget(Button.builder(Component.literal("i"), b -> {
            })
            .bounds(layout.infoX(), layout.rowStartY() + (layout.rowSpacing() * 2), layout.controlHeight(), layout.controlHeight())
            .tooltip(Tooltip.create(Component.literal("Unlucky events are risky outcomes: traps, hazards, or setbacks.")))
            .build());

        this.saveButton = this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveChanges())
            .bounds(layout.saveX(), layout.buttonY(), layout.saveWidth(), layout.buttonHeight())
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Return to Defaults"), b -> returnToDefaults())
            .bounds(layout.defaultsX(), layout.buttonY(), layout.defaultsWidth(), layout.buttonHeight())
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(layout.backX(), layout.buttonY(), layout.backWidth(), layout.buttonHeight())
                .build());

        this.statusMessage = Component.empty();
        refreshSaveButtonState();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.savedTooltipTicks > 0) {
            this.savedTooltipTicks--;
        }
    }

    private LuckyChanceSettings.Chances currentChances() {
        LuckyChanceSettings.Chances persisted = LuckyChanceSettings.loadOverride();
        if (persisted != null && persisted.isValid()) {
            return persisted;
        }

        int lucky = LuckyEventRegistry.INSTANCE.luckyChance();
        int average = LuckyEventRegistry.INSTANCE.averageChance();
        int unlucky = LuckyEventRegistry.INSTANCE.unluckyChance();
        LuckyChanceSettings.Chances fromRegistry = new LuckyChanceSettings.Chances(lucky, average, unlucky);
        if (fromRegistry.isValid()) {
            return fromRegistry;
        }

        return LuckyChanceSettings.defaults();
    }

    private void returnToDefaults() {
        LuckyChanceSettings.Chances defaults = LuckyChanceSettings.defaults();
        this.luckySlider.setPercentDirect(defaults.lucky());
        this.averageSlider.setPercentDirect(defaults.average());
        this.unluckySlider.setPercentDirect(defaults.unlucky());
        refreshSaveButtonState();
        setStatus("Defaults loaded. Press Save to apply.", 0xFFFFFF55);
    }

    private void saveChanges() {
        int lucky = this.luckySlider.percent();
        int average = this.averageSlider.percent();
        int unlucky = this.unluckySlider.percent();

        LuckyChanceSettings.Chances chances = new LuckyChanceSettings.Chances(lucky, average, unlucky);
        if (!chances.isValid()) {
            showActionTooltip(Component.literal("Error: chances must total 100%"), 0xFFFF5555);
            refreshSaveButtonState();
            return;
        }

        boolean saved = LuckyChanceSettings.save(chances);
        if (!saved) {
            showActionTooltip(Component.literal("Error: could not save config file"), 0xFFFF5555);
            return;
        }

        LuckyEventRegistry.INSTANCE.applyLuckyBlockChances(chances.lucky(), chances.average(), chances.unlucky());
        this.statusMessage = Component.empty();
        showActionTooltip(Component.literal("Changes Saved"), 0xFF55FF55);

        refreshSaveButtonState();
    }

    private void showActionTooltip(Component message, int color) {
        this.actionTooltipMessage = message;
        this.actionTooltipColor = color;
        this.savedTooltipTicks = SAVE_TOOLTIP_TICKS;
    }

    private int totalPercent() {
        return this.luckySlider.percent() + this.averageSlider.percent() + this.unluckySlider.percent();
    }

    private void refreshSaveButtonState() {
        if (this.saveButton != null) {
            this.saveButton.active = totalPercent() == 100;
        }
        refreshStepButtonState(this.luckySlider, this.luckyMinusButton, this.luckyPlusButton);
        refreshStepButtonState(this.averageSlider, this.averageMinusButton, this.averagePlusButton);
        refreshStepButtonState(this.unluckySlider, this.unluckyMinusButton, this.unluckyPlusButton);
    }

    private void refreshStepButtonState(ChanceSlider slider, Button minusButton, Button plusButton) {
        if (slider == null || minusButton == null || plusButton == null) {
            return;
        }
        minusButton.active = slider.percent() > 0;
        plusButton.active = slider.percent() < 100;
    }

    private void setStatus(String message, int color) {
        this.statusMessage = Component.literal(message);
        this.statusColor = color;
    }

    private void stepSlider(ChanceSlider slider, int delta) {
        setSliderAndRebalance(slider, slider.percent() + delta);
    }

    private void setSliderAndRebalance(ChanceSlider changedSlider, int requestedPercent) {
        if (this.applyingAutoBalance || changedSlider == null) {
            return;
        }

        ChanceSlider firstOther;
        ChanceSlider secondOther;
        if (changedSlider == this.luckySlider) {
            firstOther = this.averageSlider;
            secondOther = this.unluckySlider;
        } else if (changedSlider == this.averageSlider) {
            firstOther = this.luckySlider;
            secondOther = this.unluckySlider;
        } else {
            firstOther = this.luckySlider;
            secondOther = this.averageSlider;
        }

        int newPrimary = Math.max(0, Math.min(100, requestedPercent));
        int remaining = 100 - newPrimary;
        int oldOtherTotal = firstOther.percent() + secondOther.percent();

        int firstNew;
        int secondNew;
        if (oldOtherTotal <= 0) {
            firstNew = remaining;
            secondNew = 0;
        } else {
            firstNew = (int) Math.round((double) remaining * (double) firstOther.percent() / (double) oldOtherTotal);
            firstNew = Math.max(0, Math.min(remaining, firstNew));
            secondNew = remaining - firstNew;
        }

        this.applyingAutoBalance = true;
        changedSlider.setPercentDirect(newPrimary);
        firstOther.setPercentDirect(firstNew);
        secondOther.setPercentDirect(secondNew);
        this.applyingAutoBalance = false;

        refreshSaveButtonState();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(previousScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = computeLayout();

        float titleScale = 1.5F;
        int titleBaseY = layout.panelTop() + 8;
        int titleX = Math.round(layout.centerX() / titleScale);
        int titleY = Math.round(titleBaseY / titleScale);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(titleScale, titleScale);
        guiGraphics.drawCenteredString(this.font, this.title, titleX, titleY, 0xFFFFD700);
        guiGraphics.pose().popMatrix();

        guiGraphics.drawCenteredString(this.font, SUBTITLE, layout.centerX(), layout.panelTop() + 29, 0xFFD0D0D0);
        guiGraphics.drawCenteredString(this.font, SECTION_TITLE, layout.centerX(), layout.panelTop() + 42, 0xFFFFFFAA);

        float tipScale = 0.9F;
        int tipX = 8;
        int tipY = this.height - 20;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(tipScale, tipScale);
        guiGraphics.drawString(this.font, Component.literal("TIP: PRESS CTRL+K TO OPEN THIS MENU IN-GAME."), Math.round(tipX / tipScale), Math.round(tipY / tipScale), 0xFFD8D8D8, true);
        guiGraphics.drawString(this.font, Component.literal("TIP: HOVER OVER (i) TO SEE WHAT EACH CHANCE TIER MEANS."), Math.round(tipX / tipScale), Math.round((tipY + 10) / tipScale), 0xFFD8D8D8, true);
        guiGraphics.pose().popMatrix();

        if (this.width < MIN_READABLE_WIDTH || this.height < MIN_READABLE_HEIGHT) {
            guiGraphics.drawCenteredString(this.font, SMALL_WINDOW_WARNING, layout.centerX(), layout.panelTop() + 56, 0xFFFFAA55);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (!this.statusMessage.getString().isEmpty()) {
            guiGraphics.drawCenteredString(this.font, this.statusMessage, layout.centerX(), layout.statusY(), this.statusColor);
        }

        if (this.savedTooltipTicks > 0 && !this.actionTooltipMessage.getString().isEmpty()) {
            renderSavedToast(guiGraphics, partialTick);
        }
    }

    private void renderSavedToast(GuiGraphics guiGraphics, float partialTick) {
        String text = this.actionTooltipMessage.getString();
        int textWidth = this.font.width(text);
        int scaledTextWidth = Math.round(textWidth * 1.5F);
        int scaledLineHeight = Math.round(this.font.lineHeight * 1.5F);
        float remaining = Math.max(0.0F, this.savedTooltipTicks - partialTick);
        float progress = 1.0F - (remaining / (float) SAVE_TOOLTIP_TICKS);
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        float eased = 1.0F - (float) Math.pow(1.0F - clamped, 3.0F);

        int startY = this.height - 10;
        int targetY = this.height - 56;
        int y = (int) Math.round(startY + (targetY - startY) * eased);
        int x = (this.width - scaledTextWidth) / 2;
        int left = x - 10;
        int top = y - 7;
        int right = x + scaledTextWidth + 10;
        int bottom = y + scaledLineHeight + 7;

        guiGraphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xD0100010);
        guiGraphics.fill(left, top, right, bottom, 0xE0202028);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(1.5F, 1.5F);
        guiGraphics.drawString(this.font, this.actionTooltipMessage, Math.round(x / 1.5F), Math.round(y / 1.5F), this.actionTooltipColor, false);
        guiGraphics.pose().popMatrix();
    }

    private final class ChanceSlider extends AbstractSliderButton {

        private final Component label;
        private int percent;

        private ChanceSlider(int x, int y, int width, int height, int initialPercent, Component label) {
            super(x, y, width, height, Component.empty(), clampToValue(initialPercent));
            this.label = label;
            this.percent = initialPercent;
            this.updateMessage();
        }

        private void setPercent(int newPercent) {
            this.percent = Math.max(0, Math.min(100, newPercent));
            this.value = clampToValue(this.percent);
            this.updateMessage();
            refreshSaveButtonState();
        }

        private void setPercentDirect(int newPercent) {
            this.percent = Math.max(0, Math.min(100, newPercent));
            this.value = clampToValue(this.percent);
            this.updateMessage();
        }

        private int percent() {
            return this.percent;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(this.label.getString() + ": " + this.percent + "%"));
        }

        @Override
        protected void applyValue() {
            int newPercent = (int) Math.round(this.value * 100.0);
            setSliderAndRebalance(this, newPercent);
        }

        private static double clampToValue(int percent) {
            return Math.max(0.0, Math.min(1.0, percent / 100.0));
        }
    }

    private Layout computeLayout() {
        int centerX = this.width / 2;
        int panelWidth = Math.max(380, Math.min(680, this.width - 16));
        int panelHeight = Math.max(280, Math.min(390, this.height - 16));
        panelWidth = Math.min(panelWidth, this.width - 4);
        panelHeight = Math.min(panelHeight, this.height - 4);

        int panelLeft = centerX - (panelWidth / 2);
        int panelRight = panelLeft + panelWidth;
        int panelTop = (this.height - panelHeight) / 2;
        int panelBottom = panelTop + panelHeight;

        int controlHeight = Math.max(24, Math.min(32, panelHeight / 11));
        int rowStartY = panelTop + 96;
        int rowSpacing = controlHeight + 8;

        int buttonHeight = Math.max(22, Math.min(28, controlHeight));
        int backWidth = Math.max(84, Math.min(110, panelWidth / 5));
        int defaultsWidth = Math.max(128, Math.min(178, panelWidth / 3));
        int buttonGap = 8;
        int saveWidth = panelWidth - 32 - backWidth - defaultsWidth - (buttonGap * 2);
        saveWidth = Math.max(136, Math.min(220, saveWidth));

        int totalButtonWidth = backWidth + saveWidth + defaultsWidth + (buttonGap * 2);
        int buttonRowX = panelLeft + ((panelWidth - totalButtonWidth) / 2);
        int backX = buttonRowX;
        int saveX = backX + backWidth + buttonGap;
        int defaultsX = saveX + saveWidth + buttonGap;
        int bottomSliderBottom = rowStartY + (rowSpacing * 2) + controlHeight;
        int buttonY = bottomSliderBottom + 12;
        int maxButtonY = panelBottom - buttonHeight - 16;
        buttonY = Math.min(buttonY, maxButtonY);

        int statusY = buttonY + buttonHeight + 6;

        int sideControlsWidth = (controlHeight * 3) + (4 * 2);
        int sliderWidth = Math.max(180, panelWidth - 64 - sideControlsWidth);
        int sliderX = centerX - (sliderWidth / 2);
        int minusX = sliderX - (controlHeight + 4);
        int plusX = sliderX + sliderWidth + 4;
        int infoX = plusX + controlHeight + 4;

        return new Layout(
            centerX,
            panelLeft,
            panelRight,
            panelTop,
            panelBottom,
            sliderX,
            sliderWidth,
            controlHeight,
            rowStartY,
            rowSpacing,
            minusX,
            plusX,
            infoX,
            buttonY,
            buttonHeight,
            backX,
            backWidth,
            saveX,
            saveWidth,
            defaultsX,
            defaultsWidth,
            statusY
        );
    }

    private record Layout(
        int centerX,
        int panelLeft,
        int panelRight,
        int panelTop,
        int panelBottom,
        int sliderX,
        int sliderWidth,
        int controlHeight,
        int rowStartY,
        int rowSpacing,
        int minusX,
        int plusX,
        int infoX,
        int buttonY,
        int buttonHeight,
        int backX,
        int backWidth,
        int saveX,
        int saveWidth,
        int defaultsX,
        int defaultsWidth,
        int statusY
    ) {
    }
}
