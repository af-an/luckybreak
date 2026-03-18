package com.luckybreak;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.luckybreak.api.itemgroup.v1.CreativeTabRegistry;
import com.luckybreak.api.itemgroup.v1.ItemGroupEvents;
import com.luckybreak.events.LuckyEventRegistry;
import com.luckybreak.events.NightRiderXpHandler;
import com.luckybreak.events.LuckyScheduler;
import com.luckybreak.commands.LuckyBreakCommands;
import com.luckybreak.item.LuckyBowEntityHitEffects;
import com.luckybreak.api.event.lifecycle.v1.ServerLifecycleEvents;
import com.luckybreak.world.ModWorldGeneration;
import com.luckybreak.world.WishingWellManager;
import com.luckybreak.world.feature.ModFeatures;
import org.quiltmc.loader.api.entrypoint.GameEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuckyBreak implements GameEntrypoint {

	public static final String MOD_ID = "luckybreak";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static boolean initialized = false;
	private static boolean runtimeInitialized = false;
	private static boolean creativeMenuRegistered = false;

	public static final ResourceKey<CreativeModeTab> LUCKY_BREAK_TAB = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(MOD_ID, "lucky_break")
	);

	public LuckyBreak() {
		initialize();
	}

	public static void initialize() {
		if (initialized) {
			LOGGER.warn("Lucky Break initialization skipped because it already ran once.");
			return;
		}
		initialized = true;

		ModEntities.register();
		ModFeatures.register();
		ModBlocks.register();
		ModItems.register();
		registerCreativeMenu();
		ModWorldGeneration.register();
		LuckyEventRegistry.register();
		NightRiderXpHandler.register();
		LuckyScheduler.register();
		WishingWellManager.register();
		LuckyBowEntityHitEffects.register();
		LuckyBreakCommands.register();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> initializeRuntime());

		LOGGER.info("Lucky Break initialized!");
	}

	public static void registerCreativeMenu() {
		if (creativeMenuRegistered) {
			return;
		}
		creativeMenuRegistered = true;

		CreativeTabRegistry.registerTab(
				Identifier.fromNamespaceAndPath(MOD_ID, "lucky_break"),
				Component.translatable("itemGroup.luckybreak.lucky_break"),
				() -> luckybreak$stackOfItem("lucky_block")
		);

		ItemGroupEvents.modifyEntriesEvent(LUCKY_BREAK_TAB).register(entries -> {
			entries.add(luckybreak$stackOfItem("lucky_block"));

			entries.add(luckybreak$stackOfItem("lucky_compass"));
			entries.add(luckybreak$stackOfItem("lucky_bow"));
			entries.add(luckybreak$stackOfItem("lucky_potion"));
			entries.add(luckybreak$stackOfItem("lucky_sword"));
			entries.add(luckybreak$stackOfItem("lucky_pickaxe"));
			entries.add(luckybreak$stackOfItem("lucky_axe"));
			entries.add(luckybreak$stackOfItem("lucky_shovel"));
			entries.add(luckybreak$stackOfItem("lucky_hoe"));
			entries.add(luckybreak$stackOfItem("golden_hen_spawn_egg"));
		});

		LOGGER.info("Lucky Break custom creative tab registered.");
	}

	public static void initializeRuntime() {
		if (runtimeInitialized) {
			return;
		}
		runtimeInitialized = true;
		ModEntities.ensureRuntimeSetup();
	}

	private static ItemStack luckybreak$stackOfItem(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		Item item = BuiltInRegistries.ITEM.getValue(id);
		if (item == null) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item);
	}

}