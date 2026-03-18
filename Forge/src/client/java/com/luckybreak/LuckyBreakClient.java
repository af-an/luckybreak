package com.luckybreak;

import com.luckybreak.client.screen.LuckyChanceConfigScreen;
import com.luckybreak.client.render.GoldenHenRenderer;
import com.luckybreak.tooltip.TooltipConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class LuckyBreakClient {
	private static boolean ctrlKWasDown;
	private static boolean configScreenRegistered;

	public static void registerConfigScreenFactory() {
		if (configScreenRegistered) {
			return;
		}

		ModLoadingContext.get().registerExtensionPoint(
				ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory(previousScreen -> new LuckyChanceConfigScreen(previousScreen))
		);

		configScreenRegistered = true;
	}

	public void onInitializeClient() {
	}

	@Mod.EventBusSubscriber(modid = LuckyBreak.MOD_ID, value = Dist.CLIENT)
	public static final class ForgeClientEvents {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
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

	@Mod.EventBusSubscriber(modid = LuckyBreak.MOD_ID, value = Dist.CLIENT)
	public static final class ForgeClientModBusEvents {
		@SubscribeEvent
		public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
			event.registerEntityRenderer(ModEntities.GOLDEN_HEN, GoldenHenRenderer::new);
		}
	}

	@Mod.EventBusSubscriber(modid = LuckyBreak.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static final class ForgeClientLifecycleEvents {
		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event) {
			registerConfigScreenFactory();
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