package com.luckybreak;

import com.luckybreak.client.screen.LuckyChanceConfigScreen;
import com.luckybreak.client.render.GoldenHenRenderer;
import com.luckybreak.mixin.client.LuckyCompassHeldTooltipMixin;
import com.luckybreak.tooltip.TooltipConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod(value = LuckyBreak.MOD_ID, dist = Dist.CLIENT)
public class LuckyBreakClient {
	private static boolean ctrlKWasDown;
	private static boolean configScreenRegistered;
	private static final int MIN_HELD_TOOLTIP_GAP_PX = 12;

	public LuckyBreakClient() {
		onInitializeClient();
	}

	public static void registerConfigScreenFactory() {
		if (configScreenRegistered) {
			return;
		}

		ModLoadingContext.get().registerExtensionPoint(
				IConfigScreenFactory.class,
				() -> (container, previousScreen) -> new LuckyChanceConfigScreen(previousScreen)
		);

		configScreenRegistered = true;
	}

	public void onInitializeClient() {
		registerConfigScreenFactory();
	}

	@EventBusSubscriber(modid = LuckyBreak.MOD_ID, value = Dist.CLIENT)
	public static final class NeoForgeClientModBusEvents {
		@SubscribeEvent
		public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
			event.registerEntityRenderer(ModEntities.GOLDEN_HEN, GoldenHenRenderer::new);
		}
	}

	@EventBusSubscriber(modid = LuckyBreak.MOD_ID, value = Dist.CLIENT)
	public static final class NeoForgeClientEvents {
		@SubscribeEvent
		public static void onRenderSelectedItemNameLayer(RenderGuiLayerEvent.Post event) {
			if (!VanillaGuiLayers.SELECTED_ITEM_NAME.equals(event.getName())) {
				return;
			}

			Minecraft client = Minecraft.getInstance();
			if (client.player == null) {
				return;
			}

			Gui gui = client.gui;
			LuckyCompassHeldTooltipMixin accessor = (LuckyCompassHeldTooltipMixin) (Object) gui;
			int toolHighlightTimer = accessor.luckybreak$getToolHighlightTimer();
			ItemStack lastToolHighlight = accessor.luckybreak$getLastToolHighlight();
			ItemStack activeStack = client.player.getMainHandItem();

			if (toolHighlightTimer <= 0 || activeStack.isEmpty() || lastToolHighlight == null || lastToolHighlight.isEmpty()) {
				return;
			}

			if (!ItemStack.isSameItemSameComponents(activeStack, lastToolHighlight)) {
				return;
			}

			Component hint = TooltipConfig.getHeldTooltipFor(activeStack);
			if (hint == null) {
				return;
			}

			Font font = client.font;
			int alpha = (int) ((float) toolHighlightTimer * 256.0F / 10.0F);
			if (alpha > 255) {
				alpha = 255;
			}

			if (alpha <= 0) {
				return;
			}

			var guiGraphics = event.getGuiGraphics();
			int x = (guiGraphics.guiWidth() - font.width(hint)) / 2;
			int vanillaNameY = guiGraphics.guiHeight() - 49;
			int configuredGap = Math.max(MIN_HELD_TOOLTIP_GAP_PX, TooltipConfig.getHeldYOffset());
			int y = Math.max(2, vanillaNameY - font.lineHeight - configuredGap);
			int rgb = TooltipConfig.getHeldColorFor(activeStack) & 0xFFFFFF;
			int color = rgb | (alpha << 24);
			guiGraphics.drawString(font, hint, x, y, color);
		}

		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null) {
				ctrlKWasDown = false;
				return;
			}

			var window = client.getWindow();
			boolean ctrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
					|| InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
			boolean kDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_K);
			boolean ctrlKDown = ctrlDown && kDown;

			if (ctrlKDown && !ctrlKWasDown && !(client.screen instanceof LuckyChanceConfigScreen)) {
				client.setScreen(new LuckyChanceConfigScreen(client.screen));
			}

			ctrlKWasDown = ctrlKDown;
		}

		@SubscribeEvent
		public static void onItemTooltip(ItemTooltipEvent event) {
			var stack = event.getItemStack();
			var lines = event.getToolTip();
			if (TooltipConfig.getHidePotionEffectsFor(stack)) {
				String noEffectsText = Component.translatable("effect.none").getString();
				lines.removeIf(line -> line != null && noEffectsText.equals(line.getString()));
			}

			Component configured = TooltipConfig.getMenuTooltipFor(stack);
			if (configured == null) {
				return;
			}

			// Avoid adding duplicate line when an item already appends the same tooltip.
			String configuredText = configured.getString();
			for (Component existing : lines) {
				if (existing.getString().equals(configuredText)) {
					return;
				}
			}

			List<Integer> letterColors = TooltipConfig.getMenuLetterColorsFor(stack);
			if (letterColors.isEmpty()) {
				int color = TooltipConfig.getMenuColorFor(stack);
				lines.add(configured.copy().withStyle(style -> style.withColor(TextColor.fromRgb(color))));
			} else {
				lines.add(buildRainbowText(configured.getString(), letterColors));
			}
		}
	}

	private static Component buildRainbowText(String text, List<Integer> colors) {
		if (text == null || text.isBlank() || colors == null || colors.isEmpty()) {
			return Component.empty();
		}

		MutableComponent line = Component.empty();
		int colorIndex = 0;
		for (int i = 0; i < text.length(); i++) {
			char character = text.charAt(i);
			if (Character.isWhitespace(character)) {
				line.append(Component.literal(String.valueOf(character)));
				continue;
			}

			int color = colors.get(colorIndex % colors.size());
			line.append(Component.literal(String.valueOf(character)).withStyle(style -> style.withColor(TextColor.fromRgb(color))));
			colorIndex++;
		}

		return line;
	}
}