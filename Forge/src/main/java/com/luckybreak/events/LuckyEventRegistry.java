package com.luckybreak.events;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.config.LuckyChanceSettings;
import com.luckybreak.events.type.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.Reader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Loads lucky block events from data pack JSON files and dispatches them on block break.
 *
 * JSON files are read from: data/<namespace>/lucky_events/{unlucky,average,lucky}/*.json
 *
 * Each file has the form:
 * <pre>
 * {
 *   "events": [
 *     { "type": "luckybreak:spawn_mob", "weight": 20, "entity": "minecraft:zombie", "count": 5 },
 *     ...
 *   ]
 * }
 * </pre>
 */
@SuppressWarnings("deprecation")
public class LuckyEventRegistry implements PreparableReloadListener {

    public static final LuckyEventRegistry INSTANCE = new LuckyEventRegistry();

    private final Map<LuckyTier, EventPool> pools = new EnumMap<>(LuckyTier.class);
    private final Map<String, Function<JsonObject, LuckyEvent>> eventParsers = new HashMap<>();
    private final Map<String, LuckyEvent> namedEvents = new HashMap<>();
    private final Map<LuckyTier, Map<String, LuckyEvent>> namedEventsByTier = new EnumMap<>(LuckyTier.class);
    private final Gson gson = new Gson();

    // Tier chances (%), loaded from data/luckybreak/lucky_events/config.json
    private int luckyChance   = 10;
    private int averageChance = 60;
    private int mostlyLuckyLuckyChance = 70;
    private int mostlyLuckyAverageChance = 20;
    private int mostlyUnluckyLuckyChance = 10;
    private int mostlyUnluckyAverageChance = 20;
    private boolean triggerEventsInCreative = false;
    private boolean matchLuckyBlockNameColorForTierBlocks = true;
    private String luckyCompassBlockedNetherMessage = "The Lucky Compass doesn't work in The Nether";
    private String luckyCompassBlockedEndMessage = "The Lucky Compass doesn't work in The End";
    private int luckyCompassBlockedNetherMessageColor = 0xFFD700;
    private int luckyCompassBlockedEndMessageColor = 0xFFD700;
    private String luckyCompassFoundMessage = "Lucky Block found!";
    private String luckyCompassNotFoundMessage = "No Lucky Block found nearby.";
    private int luckyCompassFoundMessageColor = 0xFFD700;
    private int luckyCompassNotFoundMessageColor = 0xFFFFFF;

    private LuckyEventRegistry() {
        for (LuckyTier tier : LuckyTier.values()) {
            namedEventsByTier.put(tier, new HashMap<>());
        }
        registerDefaultParsers();
    }

    private void registerDefaultParsers() {
        registerParser("luckybreak:drop_items", DropItemsEvent::fromJson);
        registerParser("luckybreak:spawn_mob", SpawnMobEvent::fromJson);
        registerParser("luckybreak:set_time", SetTimeEvent::fromJson);
        registerParser("luckybreak:drop_falling_block", DropFallingBlockEvent::fromJson);
        registerParser("luckybreak:explosion", ExplosionEvent::fromJson);
        registerParser("luckybreak:lightning", LightningEvent::fromJson);
        registerParser("luckybreak:potion_effect", PotionEffectEvent::fromJson);
        registerParser("luckybreak:spawn_structure", SpawnStructureEvent::fromJson);
        registerParser("luckybreak:rainbow_column", RainbowColumnEvent::fromJson);
        registerParser("luckybreak:spawn_treasure_block", SpawnTreasureBlockEvent::fromJson);
        registerParser("luckybreak:throw_tnt_up", ThrowTntUpEvent::fromJson);
        registerParser("luckybreak:throw_tnt_up_slowness", ThrowTntUpSlownessEvent::fromJson);
        registerParser("luckybreak:lava_pit", LavaPitEvent::fromJson);
        registerParser("luckybreak:night_blind_ambush", NightBlindAmbushEvent::fromJson);
        registerParser("luckybreak:choose_wisely", ChooseWiselyEvent::fromJson);
        registerParser("luckybreak:wishing_well", WishingWellEvent::fromJson);
        registerParser("luckybreak:wishing_well_unlucky", WishingWellUnluckyEvent::fromJson);
        registerParser("luckybreak:bounce_house", BounceHouseEvent::fromJson);
        registerParser("luckybreak:loot_chest", LootChestEvent::fromJson);
        registerParser("luckybreak:tamed_companions", TamedCompanionsEvent::fromJson);
        registerParser("luckybreak:rainbow_sheep", RainbowSheepEvent::fromJson);
        registerParser("luckybreak:mob_arena", MobArenaWaveEvent::fromJson);
        registerParser("luckybreak:mob_arena_wave", MobArenaWaveEvent::fromJson);
        registerParser("luckybreak:iron_bars_look_up_trap", IronBarsLookUpTrapEvent::fromJson);
        registerParser("luckybreak:random_tree", RandomTreeEvent::fromJson);
        registerParser("luckybreak:gold_pedestal_falling_lucky_block", GoldPedestalFallingLuckyBlockEvent::fromJson);
        registerParser("luckybreak:iron_beacon", IronBeaconTier1Event::fromJson);
        registerParser("luckybreak:puffer_tank_trap", PufferTankTrapEvent::fromJson);
        registerParser("luckybreak:blacksmith_house", BlacksmithHouseEvent::fromJson);
        registerParser("luckybreak:night_riders", NightRidersEvent::fromJson);
        registerParser("luckybreak:gem_sprinkle", GemSprinkleEvent::fromJson);
        registerParser("luckybreak:friendly_circle", FriendlyCircleEvent::fromJson);
        registerParser("luckybreak:starfall_blessing", StarfallBlessingEvent::fromJson);
        registerParser("luckybreak:golden_rain", GoldenRainEvent::fromJson);
        registerParser("luckybreak:cobweb_snare", CobwebSnareEvent::fromJson);
        registerParser("luckybreak:golden_hen_gift", GoldenHenGiftEvent::fromJson);
        registerParser("luckybreak:golden_armor_stand", GoldenArmorStandEvent::fromJson);
    }

    public void registerParser(String type, Function<JsonObject, LuckyEvent> parser) {
        Function<JsonObject, LuckyEvent> previous = eventParsers.put(type, parser);
        if (previous != null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Replacing existing parser for event type {}", type);
        }
    }

    /** Register this listener so events reload with /reload. */
    public static void register() {
    }

    @Mod.EventBusSubscriber(modid = LuckyBreak.MOD_ID)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(INSTANCE);
        }
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager manager, Executor prepExecutor, Executor applyExecutor) {
        return CompletableFuture
                .runAsync(() -> onResourceManagerReload(manager), prepExecutor)
                .thenCompose(stage::wait)
                .thenRunAsync(() -> {
                }, applyExecutor);
    }

    @Override
    public String getName() {
        return ResourceLocation.fromNamespaceAndPath(LuckyBreak.MOD_ID, "lucky_events").toString();
    }

    public void onResourceManagerReload(ResourceManager manager) {
        pools.clear();
        namedEvents.clear();
        for (Map<String, LuckyEvent> tierMap : namedEventsByTier.values()) {
            tierMap.clear();
        }

        // ---- load tier percentages from config.json -------------------------
        luckyChance   = 10;
        averageChance = 60;
        mostlyLuckyLuckyChance = 70;
        mostlyLuckyAverageChance = 20;
        mostlyUnluckyLuckyChance = 10;
        mostlyUnluckyAverageChance = 20;
        triggerEventsInCreative = false;
        matchLuckyBlockNameColorForTierBlocks = true;
        luckyCompassBlockedNetherMessage = "The Lucky Compass doesn't work in The Nether";
        luckyCompassBlockedEndMessage = "The Lucky Compass doesn't work in The End";
        luckyCompassBlockedNetherMessageColor = 0xFFD700;
        luckyCompassBlockedEndMessageColor = 0xFFD700;
        luckyCompassFoundMessage = "Lucky Block found!";
        luckyCompassNotFoundMessage = "No Lucky Block found nearby.";
        luckyCompassFoundMessageColor = 0xFFD700;
        luckyCompassNotFoundMessageColor = 0xFFFFFF;
        Map<ResourceLocation, Resource> configs = manager.listResources(
                "lucky_events",
                id -> id.getPath().endsWith("config.json")
        );
        for (Map.Entry<ResourceLocation, Resource> cfg : configs.entrySet()) {
            try (Reader reader = cfg.getValue().openAsReader()) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                JsonObject luckyBlockObj = root.has("lucky_block") && root.get("lucky_block").isJsonObject()
                        ? root.getAsJsonObject("lucky_block")
                        : root;

                if (luckyBlockObj.has("lucky_chance"))   luckyChance   = luckyBlockObj.get("lucky_chance").getAsInt();
                if (luckyBlockObj.has("average_chance")) averageChance = luckyBlockObj.get("average_chance").getAsInt();
                if (luckyBlockObj.has("unlucky_chance")) {
                    int unluckyChance = luckyBlockObj.get("unlucky_chance").getAsInt();
                    averageChance = 100 - luckyChance - unluckyChance;
                }

                JsonObject mostlyLuckyObj = root.has("mostly_lucky_block") && root.get("mostly_lucky_block").isJsonObject()
                        ? root.getAsJsonObject("mostly_lucky_block")
                        : null;
                if (mostlyLuckyObj != null) {
                    if (mostlyLuckyObj.has("lucky_chance")) {
                        mostlyLuckyLuckyChance = mostlyLuckyObj.get("lucky_chance").getAsInt();
                    }
                    if (mostlyLuckyObj.has("average_chance")) {
                        mostlyLuckyAverageChance = mostlyLuckyObj.get("average_chance").getAsInt();
                    }
                    if (mostlyLuckyObj.has("unlucky_chance")) {
                        int mostlyLuckyUnluckyChance = mostlyLuckyObj.get("unlucky_chance").getAsInt();
                        mostlyLuckyAverageChance = 100 - mostlyLuckyLuckyChance - mostlyLuckyUnluckyChance;
                    }
                }

                JsonObject mostlyUnluckyObj = root.has("mostly_unlucky_block") && root.get("mostly_unlucky_block").isJsonObject()
                        ? root.getAsJsonObject("mostly_unlucky_block")
                        : null;
                if (mostlyUnluckyObj != null) {
                    if (mostlyUnluckyObj.has("lucky_chance")) {
                        mostlyUnluckyLuckyChance = mostlyUnluckyObj.get("lucky_chance").getAsInt();
                    }
                    if (mostlyUnluckyObj.has("average_chance")) {
                        mostlyUnluckyAverageChance = mostlyUnluckyObj.get("average_chance").getAsInt();
                    }
                    if (mostlyUnluckyObj.has("unlucky_chance")) {
                        int mostlyUnluckyUnluckyChance = mostlyUnluckyObj.get("unlucky_chance").getAsInt();
                        mostlyUnluckyAverageChance = 100 - mostlyUnluckyLuckyChance - mostlyUnluckyUnluckyChance;
                    }
                }
                if (root.has("trigger_events_in_creative")) {
                    triggerEventsInCreative = root.get("trigger_events_in_creative").getAsBoolean();
                }

                JsonObject blockNameColorsObj = root.has("block_name_colors") && root.get("block_name_colors").isJsonObject()
                    ? root.getAsJsonObject("block_name_colors")
                    : null;
                if (blockNameColorsObj != null && blockNameColorsObj.has("match_lucky_block_for_tier_blocks")) {
                    matchLuckyBlockNameColorForTierBlocks = blockNameColorsObj
                        .get("match_lucky_block_for_tier_blocks")
                        .getAsBoolean();
                }

                JsonObject luckyCompassObj = root.has("lucky_compass") && root.get("lucky_compass").isJsonObject()
                        ? root.getAsJsonObject("lucky_compass")
                        : null;
                if (luckyCompassObj != null) {
                    if (luckyCompassObj.has("blocked_nether_message")) {
                        String value = luckyCompassObj.get("blocked_nether_message").getAsString();
                        if (!value.isBlank()) {
                            luckyCompassBlockedNetherMessage = value;
                        }
                    }
                    if (luckyCompassObj.has("blocked_end_message")) {
                        String value = luckyCompassObj.get("blocked_end_message").getAsString();
                        if (!value.isBlank()) {
                            luckyCompassBlockedEndMessage = value;
                        }
                    }
                    luckyCompassBlockedNetherMessageColor = parseColor(
                            luckyCompassObj,
                            "blocked_nether_message_color",
                            luckyCompassBlockedNetherMessageColor
                    );
                    luckyCompassBlockedEndMessageColor = parseColor(
                            luckyCompassObj,
                            "blocked_end_message_color",
                            luckyCompassBlockedEndMessageColor
                    );
                    if (luckyCompassObj.has("found_message")) {
                        String value = luckyCompassObj.get("found_message").getAsString();
                        if (!value.isBlank()) {
                            luckyCompassFoundMessage = value;
                        }
                    }
                    if (luckyCompassObj.has("not_found_message")) {
                        String value = luckyCompassObj.get("not_found_message").getAsString();
                        if (!value.isBlank()) {
                            luckyCompassNotFoundMessage = value;
                        }
                    }
                    luckyCompassFoundMessageColor = parseColor(
                            luckyCompassObj,
                            "found_message_color",
                            luckyCompassFoundMessageColor
                    );
                    luckyCompassNotFoundMessageColor = parseColor(
                            luckyCompassObj,
                            "not_found_message_color",
                            luckyCompassNotFoundMessageColor
                    );
                }
                int unlucky = 100 - luckyChance - averageChance;
                if (unlucky < 0) {
                    LuckyBreak.LOGGER.warn("[LuckyBreak] Tier chances exceed 100%! Resetting to defaults.");
                    luckyChance = 10; averageChance = 60;
                } else {
                    LuckyBreak.LOGGER.info("[LuckyBreak] Tier chances — LUCKY: {}%, AVERAGE: {}%, UNLUCKY: {}%",
                            luckyChance, averageChance, unlucky);
                }
                int mostlyLuckyUnlucky = 100 - mostlyLuckyLuckyChance - mostlyLuckyAverageChance;
                if (mostlyLuckyUnlucky < 0) {
                    LuckyBreak.LOGGER.warn("[LuckyBreak] mostly_lucky_block chances exceed 100! Resetting to defaults.");
                    mostlyLuckyLuckyChance = 70;
                    mostlyLuckyAverageChance = 20;
                    mostlyLuckyUnlucky = 10;
                }
                LuckyBreak.LOGGER.info("[LuckyBreak] Mostly Lucky Block chances — LUCKY: {}%, AVERAGE: {}%, UNLUCKY: {}%",
                        mostlyLuckyLuckyChance, mostlyLuckyAverageChance, mostlyLuckyUnlucky);

                int mostlyUnluckyUnlucky = 100 - mostlyUnluckyLuckyChance - mostlyUnluckyAverageChance;
                if (mostlyUnluckyUnlucky < 0) {
                    LuckyBreak.LOGGER.warn("[LuckyBreak] mostly_unlucky_block chances exceed 100! Resetting to defaults.");
                    mostlyUnluckyLuckyChance = 10;
                    mostlyUnluckyAverageChance = 20;
                    mostlyUnluckyUnlucky = 70;
                }
                LuckyBreak.LOGGER.info("[LuckyBreak] Mostly Unlucky Block chances — LUCKY: {}%, AVERAGE: {}%, UNLUCKY: {}%",
                        mostlyUnluckyLuckyChance, mostlyUnluckyAverageChance, mostlyUnluckyUnlucky);
                LuckyBreak.LOGGER.info("[LuckyBreak] Creative trigger enabled: {}", triggerEventsInCreative);
            } catch (Exception e) {
                LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load config.json: {}", e.getMessage());
            }
            break; // first config.json wins
        }

        LuckyChanceSettings.Chances override = LuckyChanceSettings.loadOverride();
        if (override != null && override.isValid()) {
            luckyChance = override.lucky();
            averageChance = override.average();
            LuckyBreak.LOGGER.info(
                    "[LuckyBreak] Applied persistent Lucky Block chance override — LUCKY: {}%, AVERAGE: {}%, UNLUCKY: {}%",
                    override.lucky(),
                    override.average(),
                    override.unlucky()
            );
        }
        // ---------------------------------------------------------------------

        for (LuckyTier tier : LuckyTier.values()) {
            EventPool pool = new EventPool();
            final String tierBase = "lucky_events/" + tier.getId();
            final String tierPrefix = tierBase + "/";

            Map<ResourceLocation, Resource> resources = manager.listResources(
                tierBase,
                id -> id.getPath().startsWith(tierPrefix) && id.getPath().endsWith(".json")
            );

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonObject root = gson.fromJson(reader, JsonObject.class);

                    if (root.has("events") && root.get("events").isJsonArray()) {
                        JsonArray events = root.getAsJsonArray("events");
                        for (JsonElement el : events) {
                            if (el.isJsonObject()) {
                                registerEventObject(pool, tier, el.getAsJsonObject());
                            }
                        }
                    } else if (root.has("type")) {
                        registerEventObject(pool, tier, root);
                    } else {
                        LuckyBreak.LOGGER.warn("[LuckyBreak] Event file {} has no 'type' or 'events' array.", entry.getKey());
                    }
                } catch (Exception e) {
                    LuckyBreak.LOGGER.error("[LuckyBreak] Failed to load events from {}: {}",
                            entry.getKey(), e.getMessage());
                }
            }

            pools.put(tier, pool);
            LuckyBreak.LOGGER.info("[LuckyBreak] Loaded {} {} events.", pool.size(), tier.getId());
        }
    }

    private void registerEventObject(EventPool pool, LuckyTier tier, JsonObject obj) {
        LuckyEvent event = parseEvent(obj);
        int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 10;
        if (event != null) {
            pool.add(event, weight);
            registerNamedEvent(obj, tier, event);
        }
    }

    private void registerNamedEvent(JsonObject obj, LuckyTier tier, LuckyEvent event) {
        if (!obj.has("name")) {
            return;
        }

        String rawName = obj.get("name").getAsString().trim();
        if (rawName.isEmpty()) {
            return;
        }

        String name = rawName.toLowerCase(Locale.ROOT);
        LuckyEvent previous = namedEvents.put(name, event);
        Map<String, LuckyEvent> tierMap = namedEventsByTier.get(tier);
        LuckyEvent tierPrevious = tierMap.put(name, event);
        if (previous != null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Duplicate event name '{}' detected; replacing previous event.", name);
        }
        if (tierPrevious != null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Duplicate {} event name '{}' detected; replacing previous event.", tier.getId(), name);
        }
        LuckyBreak.LOGGER.info("[LuckyBreak] Registered named {} event: {}", tier.getId(), name);
    }

    private LuckyEvent parseEvent(JsonObject obj) {
        String type = obj.get("type").getAsString();
        Function<JsonObject, LuckyEvent> parser = eventParsers.get(type);
        if (parser == null) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Unknown event type: {}", type);
            return null;
        }

        try {
            return parser.apply(obj);
        } catch (Exception exception) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Failed to parse event type {}: {}", type, exception.getMessage());
            return null;
        }
    }

    /**
     * Roll a tier, pick a random event, and execute it.
     * Call this from LuckyBlock.playerDestroy() when broken without silk touch.
     */
    public void trigger(ServerLevel level, BlockPos pos, ServerPlayer player) {
        LuckyTier tier = LuckyTier.roll(level.getRandom(), luckyChance, averageChance);
        triggerTier(level, pos, player, tier);
    }

    public void triggerTier(ServerLevel level, BlockPos pos, ServerPlayer player, LuckyTier tier) {
        EventPool pool = pools.get(tier);
        if (pool == null || pool.isEmpty()) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] No events registered for tier {}.", tier.getId());
            return;
        }
        LuckyEvent event = pool.pick(level.getRandom());
        if (event != null) {
            LuckyBreak.LOGGER.info("[LuckyBreak] Triggering {} event: {}",
                    tier.getId(), event.getClass().getSimpleName());
            try {
                event.execute(level, pos, player);
            } catch (Exception e) {
                LuckyBreak.LOGGER.error("[LuckyBreak] Event {} threw an exception: {}",
                        event.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    public void triggerMostlyLucky(ServerLevel level, BlockPos pos, ServerPlayer player) {
        LuckyTier tier = LuckyTier.roll(level.getRandom(), mostlyLuckyLuckyChance, mostlyLuckyAverageChance);
        triggerTier(level, pos, player, tier);
    }

    public void triggerMostlyUnlucky(ServerLevel level, BlockPos pos, ServerPlayer player) {
        LuckyTier tier = LuckyTier.roll(level.getRandom(), mostlyUnluckyLuckyChance, mostlyUnluckyAverageChance);
        triggerTier(level, pos, player, tier);
    }

    public boolean triggerNamed(ServerLevel level, BlockPos pos, ServerPlayer player, String name) {
        LuckyEvent event = namedEvents.get(name.toLowerCase(Locale.ROOT));
        if (event == null) {
            return false;
        }

        try {
            event.execute(level, pos, player);
            return true;
        } catch (Exception e) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Named event {} threw an exception: {}", name, e.getMessage(), e);
            return false;
        }
    }

    public boolean triggerNamed(ServerLevel level, BlockPos pos, ServerPlayer player, LuckyTier tier, String name) {
        Map<String, LuckyEvent> tierMap = namedEventsByTier.get(tier);
        if (tierMap == null) {
            return false;
        }

        LuckyEvent event = tierMap.get(name.toLowerCase(Locale.ROOT));
        if (event == null) {
            return false;
        }

        try {
            event.execute(level, pos, player);
            return true;
        } catch (Exception e) {
            LuckyBreak.LOGGER.error("[LuckyBreak] Named {} event {} threw an exception: {}", tier.getId(), name, e.getMessage(), e);
            return false;
        }
    }

    public LuckyEvent getNamedEvent(LuckyTier tier, String name) {
        Map<String, LuckyEvent> tierMap = namedEventsByTier.get(tier);
        if (tierMap == null || name == null) {
            return null;
        }
        return tierMap.get(name.toLowerCase(Locale.ROOT));
    }

    public Set<String> getNamedEventNames() {
        return Set.copyOf(namedEvents.keySet());
    }

    public Set<String> getNamedEventNames(LuckyTier tier) {
        Map<String, LuckyEvent> tierMap = namedEventsByTier.get(tier);
        if (tierMap == null) {
            return Set.of();
        }
        return Set.copyOf(tierMap.keySet());
    }

    public boolean triggerEventsInCreative() {
        return triggerEventsInCreative;
    }

    public int luckyChance() {
        return luckyChance;
    }

    public int averageChance() {
        return averageChance;
    }

    public int unluckyChance() {
        return Math.max(0, 100 - luckyChance - averageChance);
    }

    public void applyLuckyBlockChances(int lucky, int average, int unlucky) {
        if (lucky < 0 || average < 0 || unlucky < 0) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Ignored invalid Lucky Block chances (negative value): {}/{}/{}", lucky, average, unlucky);
            return;
        }
        if (lucky + average + unlucky != 100) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Ignored invalid Lucky Block chances (sum != 100): {}/{}/{}", lucky, average, unlucky);
            return;
        }
        this.luckyChance = lucky;
        this.averageChance = average;
        LuckyBreak.LOGGER.info("[LuckyBreak] Applied Lucky Block chances instantly — LUCKY: {}%, AVERAGE: {}%, UNLUCKY: {}%", lucky, average, unlucky);
    }

    public boolean matchLuckyBlockNameColorForTierBlocks() {
        return matchLuckyBlockNameColorForTierBlocks;
    }

    public String luckyCompassBlockedNetherMessage() {
        return luckyCompassBlockedNetherMessage;
    }

    public String luckyCompassBlockedEndMessage() {
        return luckyCompassBlockedEndMessage;
    }

    public int luckyCompassBlockedNetherMessageColor() {
        return luckyCompassBlockedNetherMessageColor;
    }

    public int luckyCompassBlockedEndMessageColor() {
        return luckyCompassBlockedEndMessageColor;
    }

    public String luckyCompassFoundMessage() {
        return luckyCompassFoundMessage;
    }

    public String luckyCompassNotFoundMessage() {
        return luckyCompassNotFoundMessage;
    }

    public int luckyCompassFoundMessageColor() {
        return luckyCompassFoundMessageColor;
    }

    public int luckyCompassNotFoundMessageColor() {
        return luckyCompassNotFoundMessageColor;
    }

    private static int parseColor(JsonObject obj, String field, int fallback) {
        if (obj == null || !obj.has(field)) {
            return fallback;
        }

        try {
            JsonElement element = obj.get(field);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt() & 0xFFFFFF;
            }

            String value = element.getAsString().trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            if (value.length() == 6) {
                return Integer.parseInt(value, 16) & 0xFFFFFF;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
