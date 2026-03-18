package com.luckybreak.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LuckyChanceSettings {

    public static final int DEFAULT_LUCKY = 20;
    public static final int DEFAULT_AVERAGE = 60;
    public static final int DEFAULT_UNLUCKY = 20;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "lucky_chances.json";

    private LuckyChanceSettings() {
    }

    public static Path filePath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve("luckybreak")
                .resolve(FILE_NAME);
    }

    public static Chances defaults() {
        return new Chances(DEFAULT_LUCKY, DEFAULT_AVERAGE, DEFAULT_UNLUCKY);
    }

    public static Chances loadOrDefaults() {
        Path path = filePath();
        if (!Files.exists(path)) {
            return defaults();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return defaults();
            }

            JsonObject luckyBlock = root.has("lucky_block") && root.get("lucky_block").isJsonObject()
                    ? root.getAsJsonObject("lucky_block")
                    : root;

            int lucky = luckyBlock.has("lucky_chance") ? luckyBlock.get("lucky_chance").getAsInt() : DEFAULT_LUCKY;
            int average = luckyBlock.has("average_chance") ? luckyBlock.get("average_chance").getAsInt() : DEFAULT_AVERAGE;
            int unlucky = luckyBlock.has("unlucky_chance") ? luckyBlock.get("unlucky_chance").getAsInt() : DEFAULT_UNLUCKY;

            Chances parsed = new Chances(lucky, average, unlucky);
            return parsed.isValid() ? parsed : defaults();
        } catch (Exception ignored) {
            return defaults();
        }
    }

    public static Chances loadOverride() {
        Path path = filePath();
        if (!Files.exists(path)) {
            return null;
        }
        return loadOrDefaults();
    }

    public static boolean save(Chances chances) {
        if (chances == null || !chances.isValid()) {
            return false;
        }

        Path path = filePath();
        try {
            Files.createDirectories(path.getParent());

            JsonObject root = new JsonObject();
            root.addProperty("_comment", "Overrides Lucky Block tier chances. Values must sum to 100.");

            JsonObject luckyBlock = new JsonObject();
            luckyBlock.addProperty("lucky_chance", chances.lucky());
            luckyBlock.addProperty("average_chance", chances.average());
            luckyBlock.addProperty("unlucky_chance", chances.unlucky());
            root.add("lucky_block", luckyBlock);

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public record Chances(int lucky, int average, int unlucky) {
        public boolean isValid() {
            if (lucky < 0 || average < 0 || unlucky < 0) {
                return false;
            }
            return lucky + average + unlucky == 100;
        }
    }
}
