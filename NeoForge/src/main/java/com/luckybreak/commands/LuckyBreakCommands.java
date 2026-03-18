package com.luckybreak.commands;

import com.luckybreak.ModItems;
import com.luckybreak.events.LuckyEvent;
import com.luckybreak.events.LuckyEventRegistry;
import com.luckybreak.events.LuckyTier;
import com.luckybreak.events.type.MobArenaWaveEvent;
import com.luckybreak.item.LuckyAxeItem;
import com.luckybreak.item.LuckyBowItem;
import com.luckybreak.item.LuckyHoeItem;
import com.luckybreak.item.LuckyPickaxeItem;
import com.luckybreak.item.LuckyShovelItem;
import com.luckybreak.item.LuckySwordItem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

public final class LuckyBreakCommands {

    private static final List<String> LUCKY_ITEM_NAMES = List.of(
        "lucky_sword",
        "lucky_pickaxe",
        "lucky_axe",
        "lucky_shovel",
        "lucky_hoe",
        "lucky_bow",
        "lucky_compass",
        "lucky_potion"
    );

    private static final List<String> LUCKY_SWORD_EVENT_NAMES = List.of(
        "lightning",
        "knockback_fire_trail",
        "temporary_enchantments",
        "ender_swap",
        "sky_launch",
        "echo_slash",
        "chain_lightning",
        "shadow_step",
        "frost_prison",
        "starfall_strike"
    );

        private static final List<String> LUCKY_PICKAXE_EVENT_NAMES = List.of(
            "tnt_transform",
            "bedrock_transform",
            "lucky_block_transform",
            "block_transform",
            "golden_hen_spawn",
            "bonus_drops",
            "xp_burst",
            "hostile_spawn",
            "friendly_spawn",
            "ore_vein_burst",
            "seismic_burst",
            "temporary_enchantments"
        );

        private static final List<String> LUCKY_AXE_EVENT_NAMES = List.of(
            "block_break_lightning",
            "mob_hit_lightning",
            "lucky_block_transform",
            "bonus_drops",
            "leaf_storm",
            "bee_swarm",
            "timber_launch",
            "loot_pinata",
            "temporary_enchantments"
        );

        private static final List<String> LUCKY_SHOVEL_EVENT_NAMES = List.of(
            "lucky_block_transform",
            "bonus_drops",
            "sandstorm",
            "treasure_burst",
            "temporary_enchantments"
        );

        private static final List<String> LUCKY_HOE_EVENT_NAMES = List.of(
            "mob_hit_food_bonus_drops",
            "till_bonus_drops",
            "villager_spawn",
            "crop_bloom",
            "chicken_parade",
            "pinata_hit"
        );

            private static final List<String> LUCKY_BOW_EVENT_NAMES = List.of(
                "entity_hit_lightning",
                "block_transform",
                "fire_spread",
                "entity_hit_block_transform",
                "entity_hit_potion",
                "tnt_trail",
                "chicken_rain",
                "party_pop",
                "entity_bounce",
                "temporary_enchantments"
            );

                private static final List<String> LUCKY_POTION_EVENT_NAMES = List.of(
                    "none"
                );

    private LuckyBreakCommands() {
    }

    public static void register() {
    }

    @EventBusSubscriber(modid = com.luckybreak.LuckyBreak.MOD_ID)
    public static final class NeoForgeEvents {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            registerCommands(event.getDispatcher());
        }
    }

    private static void registerCommands(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("luckybreak")
                        .requires(source -> true)
                        .then(Commands.literal("item")
                                .then(Commands.argument("itemname", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(LUCKY_ITEM_NAMES, builder))
                                        .then(Commands.argument("eventname", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    String itemName = StringArgumentType.getString(context, "itemname");
                                                    if ("lucky_sword".equals(itemName)) {
                                                        return SharedSuggestionProvider.suggest(LUCKY_SWORD_EVENT_NAMES, builder);
                                                    }
                                                    if ("lucky_pickaxe".equals(itemName)) {
                                                        return SharedSuggestionProvider.suggest(LUCKY_PICKAXE_EVENT_NAMES, builder);
                                                    }
                                                    if ("lucky_axe".equals(itemName)) {
                                                        return SharedSuggestionProvider.suggest(LUCKY_AXE_EVENT_NAMES, builder);
                                                    }
                                                    if ("lucky_shovel".equals(itemName)) {
                                                        return SharedSuggestionProvider.suggest(LUCKY_SHOVEL_EVENT_NAMES, builder);
                                                    }
                                                    if ("lucky_hoe".equals(itemName)) {
                                                        return SharedSuggestionProvider.suggest(LUCKY_HOE_EVENT_NAMES, builder);
                                                    }
                                                    if ("lucky_bow".equals(itemName)) {
                                                        return SharedSuggestionProvider.suggest(LUCKY_BOW_EVENT_NAMES, builder);
                                                    }
                                                    if ("lucky_potion".equals(itemName)) {
                                                        return SharedSuggestionProvider.suggest(LUCKY_POTION_EVENT_NAMES, builder);
                                                    }
                                                    return SharedSuggestionProvider.suggest(List.of(), builder);
                                                })
                                                .executes(LuckyBreakCommands::runItemEvent))))
                        .then(Commands.literal("events")
                                .then(createTierBranch(LuckyTier.AVERAGE, true))
                                .then(createTierBranch(LuckyTier.LUCKY, false))
                                .then(createTierBranch(LuckyTier.UNLUCKY, false)))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createTierBranch(LuckyTier tier, boolean includeMobArenaWaveCommand) {
        LiteralArgumentBuilder<CommandSourceStack> tierBranch = Commands.literal(tier.getId())
                .then(Commands.argument("eventname", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(LuckyEventRegistry.INSTANCE.getNamedEventNames(tier), builder))
                        .executes(context -> runNamedEvent(context, tier, null)));

        if (includeMobArenaWaveCommand) {
            tierBranch.then(Commands.literal("mob_arena")
                    .executes(context -> runNamedEvent(context, tier, "mob_arena"))
                    .then(Commands.literal("wave")
                            .then(Commands.argument("waves", IntegerArgumentType.integer(1, 5))
                                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"1", "2", "3", "4", "5"}, builder))
                                    .executes(context -> runNamedEventWithWaves(context, tier, "mob_arena")))));
        }

        return tierBranch;
    }

    private static int runNamedEventWithWaves(CommandContext<CommandSourceStack> context, LuckyTier tier, String forcedEventName) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String eventName = forcedEventName != null ? forcedEventName : StringArgumentType.getString(context, "eventname");
        int waves = IntegerArgumentType.getInteger(context, "waves");
        BlockPos targetPos = player.blockPosition().relative(player.getDirection());

        LuckyEvent event = LuckyEventRegistry.INSTANCE.getNamedEvent(tier, eventName);
        if (event == null) {
            source.sendFailure(Component.literal("Unknown " + tier.getId() + " event name: " + eventName));
            return 0;
        }

        if (!(event instanceof MobArenaWaveEvent mobArenaWaveEvent)) {
            source.sendFailure(Component.literal("Wave override only works for mob_arena."));
            return 0;
        }

        mobArenaWaveEvent.executeWithForcedWaveCount(source.getLevel(), targetPos, player, waves);
        source.sendSuccess(() -> Component.literal("Triggered " + tier.getId() + " event: " + eventName + " with " + waves + " wave(s)"), false);
        return 1;
    }

    private static int runNamedEvent(CommandContext<CommandSourceStack> context, LuckyTier tier, String forcedEventName) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String eventName = forcedEventName != null ? forcedEventName : StringArgumentType.getString(context, "eventname");
        BlockPos targetPos = player.blockPosition().relative(player.getDirection());

        boolean triggered = LuckyEventRegistry.INSTANCE.triggerNamed(source.getLevel(), targetPos, player, tier, eventName);
        if (!triggered) {
            source.sendFailure(Component.literal("Unknown " + tier.getId() + " event name: " + eventName));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Triggered " + tier.getId() + " event: " + eventName), false);
        return 1;
    }

    private static int runItemEvent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String itemName = StringArgumentType.getString(context, "itemname");
        String eventName = StringArgumentType.getString(context, "eventname");
        var lookTarget = player.blockPosition().relative(player.getDirection());
        LivingEntity closestMob = findClosestMob(player, 48.0D);

        ItemStack held = player.getMainHandItem();

        if ("lucky_sword".equals(itemName)) {
            if (!(held.getItem() instanceof LuckySwordItem swordItem) || held.getItem() != ModItems.LUCKY_SWORD) {
                source.sendFailure(Component.literal("Hold lucky_sword in your main hand to test sword events."));
                return 0;
            }

            if (!LUCKY_SWORD_EVENT_NAMES.contains(eventName)) {
                source.sendFailure(Component.literal("Unknown lucky_sword event name: " + eventName));
                return 0;
            }

            boolean triggered = swordItem.triggerCommandEvent(held, player, eventName);
            if (!triggered) {
                source.sendFailure(Component.literal("Could not run event. For mob events, make sure a nearby mob exists."));
                return 0;
            }

            source.sendSuccess(() -> Component.literal("Triggered lucky_sword event: " + eventName), false);
            return 1;
        }

        if ("lucky_pickaxe".equals(itemName)) {
            if (!(held.getItem() instanceof LuckyPickaxeItem pickaxeItem) || held.getItem() != ModItems.LUCKY_PICKAXE) {
                source.sendFailure(Component.literal("Hold lucky_pickaxe in your main hand to test pickaxe events."));
                return 0;
            }
            if (!LUCKY_PICKAXE_EVENT_NAMES.contains(eventName)) {
                source.sendFailure(Component.literal("Unknown lucky_pickaxe event name: " + eventName));
                return 0;
            }
            if (!pickaxeItem.triggerCommandEvent(held, player, eventName, lookTarget, closestMob)) {
                source.sendFailure(Component.literal("Could not run pickaxe event. Make sure a valid target exists."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("Triggered lucky_pickaxe event: " + eventName), false);
            return 1;
        }

        if ("lucky_axe".equals(itemName)) {
            if (!(held.getItem() instanceof LuckyAxeItem axeItem) || held.getItem() != ModItems.LUCKY_AXE) {
                source.sendFailure(Component.literal("Hold lucky_axe in your main hand to test axe events."));
                return 0;
            }
            if (!LUCKY_AXE_EVENT_NAMES.contains(eventName)) {
                source.sendFailure(Component.literal("Unknown lucky_axe event name: " + eventName));
                return 0;
            }
            if (!axeItem.triggerCommandEvent(held, player, eventName, lookTarget, closestMob)) {
                source.sendFailure(Component.literal("Could not run axe event. Make sure a valid target exists."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("Triggered lucky_axe event: " + eventName), false);
            return 1;
        }

        if ("lucky_shovel".equals(itemName)) {
            if (!(held.getItem() instanceof LuckyShovelItem shovelItem) || held.getItem() != ModItems.LUCKY_SHOVEL) {
                source.sendFailure(Component.literal("Hold lucky_shovel in your main hand to test shovel events."));
                return 0;
            }
            if (!LUCKY_SHOVEL_EVENT_NAMES.contains(eventName)) {
                source.sendFailure(Component.literal("Unknown lucky_shovel event name: " + eventName));
                return 0;
            }
            if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                source.sendFailure(Component.literal("Could not run shovel event on this level."));
                return 0;
            }
            if (!shovelItem.triggerCommandEvent(held, serverLevel, player, eventName, lookTarget)) {
                source.sendFailure(Component.literal("Could not run shovel event. Make sure a valid target exists."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("Triggered lucky_shovel event: " + eventName), false);
            return 1;
        }

        if ("lucky_hoe".equals(itemName)) {
            if (!(held.getItem() instanceof LuckyHoeItem hoeItem) || held.getItem() != ModItems.LUCKY_HOE) {
                source.sendFailure(Component.literal("Hold lucky_hoe in your main hand to test hoe events."));
                return 0;
            }
            if (!LUCKY_HOE_EVENT_NAMES.contains(eventName)) {
                source.sendFailure(Component.literal("Unknown lucky_hoe event name: " + eventName));
                return 0;
            }
            if (!hoeItem.triggerCommandEvent(held, player, eventName, lookTarget, closestMob)) {
                source.sendFailure(Component.literal("Could not run hoe event. Make sure a valid target exists."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("Triggered lucky_hoe event: " + eventName), false);
            return 1;
        }

        if ("lucky_bow".equals(itemName)) {
            if (!(held.getItem() instanceof LuckyBowItem bowItem) || held.getItem() != ModItems.LUCKY_BOW) {
                source.sendFailure(Component.literal("Hold lucky_bow in your main hand to test bow events."));
                return 0;
            }
            if (!LUCKY_BOW_EVENT_NAMES.contains(eventName)) {
                source.sendFailure(Component.literal("Unknown lucky_bow event name: " + eventName));
                return 0;
            }
            if (!bowItem.triggerCommandEvent(held, player, eventName, lookTarget, closestMob)) {
                source.sendFailure(Component.literal("Could not run bow event. Make sure a valid target exists."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("Triggered lucky_bow event: " + eventName), false);
            return 1;
        }

        if ("lucky_potion".equals(itemName)) {
            if (held.getItem() != ModItems.LUCKY_POTION) {
                source.sendFailure(Component.literal("Hold lucky_potion in your main hand to test potion events."));
                return 0;
            }
            if (!LUCKY_POTION_EVENT_NAMES.contains(eventName)) {
                source.sendFailure(Component.literal("Unknown lucky_potion event name: " + eventName));
                return 0;
            }

            source.sendSuccess(() -> Component.literal("Lucky potion has no custom testable events yet (event: none)."), false);
            return 1;
        }

        source.sendFailure(Component.literal("Item event testing is currently supported for lucky_sword, lucky_pickaxe, lucky_axe, lucky_shovel, lucky_hoe, lucky_bow, and lucky_potion."));
        return 0;
    }

    private static LivingEntity findClosestMob(ServerPlayer player, double radius) {
        var level = player.level();
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != null && entity.isAlive() && entity != player);

        LivingEntity closest = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (LivingEntity entity : nearby) {
            double distSq = entity.distanceToSqr(player);
            if (distSq < bestDistanceSq) {
                bestDistanceSq = distSq;
                closest = entity;
            }
        }
        return closest;
    }
}
