package com.luckybreak;

import com.luckybreak.api.client.event.lifecycle.v1.ClientTickEvents;
import com.luckybreak.api.client.item.v1.ItemTooltipCallback;
import com.luckybreak.api.client.rendering.v1.EntityRendererRegistry;
import com.luckybreak.client.render.GoldenHenRenderer;
import com.luckybreak.client.screen.LuckyChanceConfigScreen;
import com.luckybreak.tooltip.TooltipConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import org.quiltmc.loader.api.entrypoint.GameEntrypoint;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class LuckyBreakClient implements GameEntrypoint {
	private static boolean ctrlKWasDown;
	private static boolean initialized;

	public LuckyBreakClient() {
		initializeClient();
	}

	public static void initializeClient() {
		if (initialized) {
			return;
		}
		initialized = true;

		EntityRendererRegistry.register(ModEntities.GOLDEN_HEN, GoldenHenRenderer::new);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				ctrlKWasDown = false;
				return;
			}

			long window = client.getWindow().getWindow();
			boolean ctrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
					|| InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
			boolean kDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_K);
			boolean ctrlKDown = ctrlDown && kDown;

			if (ctrlKDown && !ctrlKWasDown && !(client.screen instanceof LuckyChanceConfigScreen)) {
				client.setScreen(new LuckyChanceConfigScreen(client.screen));
			}

			ctrlKWasDown = ctrlKDown;
		});

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (TooltipConfig.getHidePotionEffectsFor(stack)) {
				String noEffectsText = Component.translatable("effect.none").getString();
				lines.removeIf(line -> line != null && noEffectsText.equals(line.getString()));
			}

			Component configured = TooltipConfig.getMenuTooltipFor(stack);
			if (configured == null) {
				return;
			}

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
		});
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
