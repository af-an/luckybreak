package com.luckybreak.events.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luckybreak.LuckyBreak;
import com.luckybreak.events.LuckyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spawns tamed cats, wolves, and parrots for the triggering player.
 * Each companion gets a random name from the configured name list.
 */
public class TamedCompanionsEvent implements LuckyEvent {

    private static final int DEFAULT_MESSAGE_COLOR = 0xFF69B4;
    private static final String DEFAULT_MESSAGE_TEXT = "You made some friends!";

    private static final List<String> DEFAULT_NAMES = List.of(
            "Luna", "Buddy", "Mochi", "Nova", "Coco",
            "Pip", "Maple", "Sunny", "Rex", "Skye"
    );

    private final int catMin;
    private final int catMax;
    private final int wolfMin;
    private final int wolfMax;
    private final int parrotMin;
    private final int parrotMax;
    private final double radius;
    private final String message;
    private final int messageColor;
    private final boolean randomWolfCollarColors;
    private final List<DyeColor> wolfCollarColors;
    private final List<String> catNames;
    private final List<String> wolfNames;
    private final List<String> parrotNames;

    private TamedCompanionsEvent(
            int catMin,
            int catMax,
            int wolfMin,
            int wolfMax,
            int parrotMin,
            int parrotMax,
            double radius,
            String message,
            int messageColor,
            boolean randomWolfCollarColors,
                List<DyeColor> wolfCollarColors,
            List<String> catNames,
            List<String> wolfNames,
            List<String> parrotNames
    ) {
        this.catMin = catMin;
        this.catMax = catMax;
        this.wolfMin = wolfMin;
        this.wolfMax = wolfMax;
        this.parrotMin = parrotMin;
        this.parrotMax = parrotMax;
        this.radius = radius;
        this.message = message;
        this.messageColor = messageColor;
        this.randomWolfCollarColors = randomWolfCollarColors;
        this.wolfCollarColors = wolfCollarColors;
        this.catNames = catNames;
        this.wolfNames = wolfNames;
        this.parrotNames = parrotNames;
    }

    public static TamedCompanionsEvent fromJson(JsonObject obj) {
        int catMin = obj.has("cat_min") ? obj.get("cat_min").getAsInt() : 1;
        int catMax = obj.has("cat_max") ? obj.get("cat_max").getAsInt() : 3;
        int wolfMin = obj.has("wolf_min") ? obj.get("wolf_min").getAsInt() : 1;
        int wolfMax = obj.has("wolf_max") ? obj.get("wolf_max").getAsInt() : 3;
        int parrotMin = obj.has("parrot_min") ? obj.get("parrot_min").getAsInt() : 1;
        int parrotMax = obj.has("parrot_max") ? obj.get("parrot_max").getAsInt() : 2;
        double radius = obj.has("radius") ? obj.get("radius").getAsDouble() : 4.0;
        String message = obj.has("message") ? obj.get("message").getAsString() : DEFAULT_MESSAGE_TEXT;
        int messageColor = parseRgbColor(obj, "message_color", DEFAULT_MESSAGE_COLOR);
        boolean randomWolfCollarColors = !obj.has("random_wolf_collar_colors") || obj.get("random_wolf_collar_colors").getAsBoolean();
        List<DyeColor> wolfCollarColors = parseDyeColorList(obj, "wolf_collar_colors");
        if (wolfCollarColors.isEmpty()) {
            wolfCollarColors = List.of(DyeColor.values());
        }

        if (catMin < 0) catMin = 0;
        if (wolfMin < 0) wolfMin = 0;
        if (parrotMin < 0) parrotMin = 0;
        if (catMax < catMin) catMax = catMin;
        if (wolfMax < wolfMin) wolfMax = wolfMin;
        if (parrotMax < parrotMin) parrotMax = parrotMin;
        if (radius < 1.0) radius = 1.0;

        List<String> sharedNames = parseNameList(obj, "names");
        if (sharedNames.isEmpty()) {
            sharedNames = DEFAULT_NAMES;
        }

        List<String> catNames = parseNameList(obj, "cat_names");
        List<String> wolfNames = parseNameList(obj, "wolf_names");
        List<String> parrotNames = parseNameList(obj, "parrot_names");

        if (catNames.isEmpty()) {
            catNames = sharedNames;
        }
        if (wolfNames.isEmpty()) {
            wolfNames = sharedNames;
        }
        if (parrotNames.isEmpty()) {
            parrotNames = sharedNames;
        }

        return new TamedCompanionsEvent(
                catMin,
                catMax,
                wolfMin,
                wolfMax,
                parrotMin,
                parrotMax,
                radius,
                message,
                messageColor,
                randomWolfCollarColors,
                List.copyOf(wolfCollarColors),
                List.copyOf(catNames),
                List.copyOf(wolfNames),
                List.copyOf(parrotNames)
        );
    }

    private static List<DyeColor> parseDyeColorList(JsonObject obj, String field) {
        List<DyeColor> list = new ArrayList<>();
        if (!obj.has(field) || !obj.get(field).isJsonArray()) {
            return list;
        }

        JsonArray arr = obj.getAsJsonArray(field);
        for (JsonElement element : arr) {
            if (!element.isJsonPrimitive()) {
                continue;
            }

            try {
                if (element.getAsJsonPrimitive().isNumber()) {
                    int index = element.getAsInt();
                    DyeColor[] values = DyeColor.values();
                    if (index >= 0 && index < values.length) {
                        list.add(values[index]);
                    }
                    continue;
                }

                String raw = element.getAsString().trim();
                if (raw.isEmpty()) {
                    continue;
                }
                String normalized = raw.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
                list.add(DyeColor.valueOf(normalized));
            } catch (Exception ignored) {
            }
        }

        return list;
    }

    private static List<String> parseNameList(JsonObject obj, String field) {
        List<String> list = new ArrayList<>();
        if (!obj.has(field) || !obj.get(field).isJsonArray()) {
            return list;
        }

        JsonArray arr = obj.getAsJsonArray(field);
        for (JsonElement element : arr) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            String name = element.getAsString().trim();
            if (!name.isEmpty()) {
                list.add(name);
            }
        }
        return list;
    }

    @Override
    public void execute(ServerLevel level, BlockPos pos, ServerPlayer player) {
        RandomSource random = level.getRandom();

        int catCount = catMin + random.nextInt(Math.max(1, catMax - catMin + 1));
        int wolfCount = wolfMin + random.nextInt(Math.max(1, wolfMax - wolfMin + 1));
        int parrotCount = parrotMin + random.nextInt(Math.max(1, parrotMax - parrotMin + 1));

        for (int i = 0; i < catCount; i++) {
            Cat cat = net.minecraft.world.entity.EntityType.CAT.create(level, EntitySpawnReason.TRIGGERED);
            if (cat != null) {
                spawnAndTame(level, player, cat, random, catNames);
            }
        }

        for (int i = 0; i < wolfCount; i++) {
            Wolf wolf = net.minecraft.world.entity.EntityType.WOLF.create(level, EntitySpawnReason.TRIGGERED);
            if (wolf != null) {
                spawnAndTame(level, player, wolf, random, wolfNames);
            }
        }

        for (int i = 0; i < parrotCount; i++) {
            Parrot parrot = net.minecraft.world.entity.EntityType.PARROT.create(level, EntitySpawnReason.TRIGGERED);
            if (parrot != null) {
                spawnAndTame(level, player, parrot, random, parrotNames);
            }
        }

        if (!message.isEmpty()) {
            player.displayClientMessage(
                Component.literal(message).withStyle(style -> style.withColor(TextColor.fromRgb(messageColor))),
                false
            );
        }
    }

    private void spawnAndTame(ServerLevel level, ServerPlayer player, TamableAnimal animal, RandomSource random, List<String> names) {
        BlockPos safePos = findSafeSpawnPos(level, player, animal, random);
        animal.setPos(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

        // Force random companion appearance before taming.
        if (animal instanceof Cat cat) {
            cat.finalizeSpawn(level, level.getCurrentDifficultyAt(cat.blockPosition()), EntitySpawnReason.NATURAL, null);
        } else if (animal instanceof Wolf) {
            // Keep wolf variant independent of local biome by avoiding biome-based spawn finalization.
        } else if (animal instanceof Parrot parrot) {
            parrot.finalizeSpawn(level, level.getCurrentDifficultyAt(parrot.blockPosition()), EntitySpawnReason.NATURAL, null);
        }

        animal.tame(player);
        animal.setOrderedToSit(false);
        animal.setPersistenceRequired();

        if (animal instanceof Wolf wolf && randomWolfCollarColors) {
            DyeColor chosen = wolfCollarColors.get(random.nextInt(wolfCollarColors.size()));
            applyRandomWolfCollarColor(wolf, chosen);
        }

        String randomName = names.get(random.nextInt(names.size()));
        animal.setCustomName(Component.literal(randomName));

        level.addFreshEntity(animal);
    }

    private BlockPos findSafeSpawnPos(ServerLevel level, ServerPlayer player, TamableAnimal animal, RandomSource random) {
        BlockPos fallback = player.blockPosition();
        int minY = level.getMinY() + 1;

        for (int attempt = 0; attempt < 24; attempt++) {
            double ox = (random.nextDouble() - 0.5) * 2.0 * radius;
            double oz = (random.nextDouble() - 0.5) * 2.0 * radius;
            int x = (int) Math.floor(player.getX() + ox);
            int z = (int) Math.floor(player.getZ() + oz);
            int baseY = Math.max(minY, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));

            for (int dy = -2; dy <= 2; dy++) {
                BlockPos candidate = new BlockPos(x, Math.max(minY, baseY + dy), z);
                if (isSafeSpawnAt(level, animal, candidate)) {
                    return candidate;
                }
            }
        }

        for (int dy = 0; dy <= 3; dy++) {
            BlockPos candidate = fallback.above(dy);
            if (isSafeSpawnAt(level, animal, candidate)) {
                return candidate;
            }
        }

        return fallback;
    }

    private boolean isSafeSpawnAt(ServerLevel level, TamableAnimal animal, BlockPos feetPos) {
        BlockPos headPos = feetPos.above();
        BlockPos belowPos = feetPos.below();

        if (!level.getBlockState(feetPos).isAir() || !level.getBlockState(headPos).isAir()) {
            return false;
        }
        if (!level.getBlockState(belowPos).isSolidRender()) {
            return false;
        }

        animal.setPos(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5);
        return level.noCollision(animal);
    }

    private static int parseRgbColor(JsonObject obj, String field, int fallback) {
        if (!obj.has(field)) {
            return fallback;
        }

        JsonElement element = obj.get(field);
        try {
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

    private void applyRandomWolfCollarColor(Wolf wolf, DyeColor color) {
        try {
            Method setter = Wolf.class.getDeclaredMethod("setCollarColor", DyeColor.class);
            setter.setAccessible(true);
            setter.invoke(wolf, color);
        } catch (Exception e) {
            LuckyBreak.LOGGER.warn("[LuckyBreak] Failed to set random wolf collar color: {}", e.getMessage());
        }
    }
}
