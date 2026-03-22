package com.luckybreak;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import com.luckybreak.events.LuckyEventRegistry;
import com.luckybreak.events.NightRiderXpHandler;
import com.luckybreak.events.LuckyScheduler;
import com.luckybreak.commands.LuckyBreakCommands;
import com.luckybreak.item.LuckyBowEntityHitEffects;
import com.luckybreak.world.ModWorldGeneration;
import com.luckybreak.world.WishingWellManager;
import com.luckybreak.world.feature.ModFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

public class LuckyBreak {

	public static final String MOD_ID = "luckybreak";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceKey<CreativeModeTab> LUCKY_BREAK_TAB = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "lucky_break")
	);
	private static final ResourceLocation LUCKY_BREAK_TAB_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "lucky_break");

	public void onInitialize() {
		ModFeatures.register();
		ModEntities.register();
		ModBlocks.register();
		ModItems.register();
		ModWorldGeneration.register();
		LuckyEventRegistry.register();
		NightRiderXpHandler.register();
		LuckyScheduler.register();
		WishingWellManager.register();
		LuckyBowEntityHitEffects.register();
		LuckyBreakCommands.register();

		LOGGER.info("Lucky Break initialized!");
	}

	@EventBusSubscriber(modid = MOD_ID)
	public static final class NeoForgeEvents {
		@SubscribeEvent
		public static void onRegisterCreativeTabs(RegisterEvent event) {
			event.register(Registries.CREATIVE_MODE_TAB, helper ->
				helper.register(LUCKY_BREAK_TAB_ID,
						CreativeModeTab.builder()
								.title(Component.translatable("itemGroup.luckybreak.lucky_break"))
								.icon(() -> new ItemStack(ModBlocks.LUCKY_BLOCK.asItem()))
								.displayItems((parameters, output) -> {
									output.accept(ModBlocks.LUCKY_BLOCK);
									output.accept(ModItems.LUCKY_COMPASS);
									output.accept(ModItems.LUCKY_BOW);
									output.accept(ModItems.LUCKY_POTION);
									output.accept(ModItems.LUCKY_SWORD);
									output.accept(ModItems.LUCKY_PICKAXE);
									output.accept(ModItems.LUCKY_AXE);
									output.accept(ModItems.LUCKY_SHOVEL);
									output.accept(ModItems.LUCKY_HOE);
									output.accept(ModItems.GOLDEN_HEN_SPAWN_EGG);
								})
								.build()
				)
			);
		}
	}
}