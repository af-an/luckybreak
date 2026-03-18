package com.luckybreak;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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

public class LuckyBreak implements ModInitializer {

	public static final String MOD_ID = "luckybreak";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceKey<CreativeModeTab> LUCKY_BREAK_TAB = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(MOD_ID, "lucky_break")
	);

	@Override
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

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, LUCKY_BREAK_TAB,
				FabricItemGroup.builder()
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
		);

		LOGGER.info("Lucky Break initialized!");
	}
}