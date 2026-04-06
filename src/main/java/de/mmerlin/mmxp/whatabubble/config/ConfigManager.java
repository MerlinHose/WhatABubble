package de.mmerlin.mmxp.whatabubble.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final String CONFIG_FILE = "whatabubble.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir;
    private ModConfig config = new ModConfig();

    public ConfigManager(Path configDir) {
        this.configDir = configDir;
    }

    public void load() {
        Path file = configDir.resolve(CONFIG_FILE);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) config = loaded;
            } catch (IOException e) {
                ModLogger.error("Failed to load config, using defaults.", e);
                config = new ModConfig();
            }
        } else {
            config = new ModConfig();
            save();
        }
        config.setUseNineSlice(true);
        config.setMaxBubbleLineChars(config.getMaxBubbleLineChars());
        config.setBubbleRed(config.getBubbleRed());
        config.setBubbleGreen(config.getBubbleGreen());
        config.setBubbleBlue(config.getBubbleBlue());
        config.setTextRed(config.getTextRed());
        config.setTextGreen(config.getTextGreen());
        config.setTextBlue(config.getTextBlue());
        config.setBubbleTexture(config.getBubbleTexture());
        config.setPadding(config.getPadding());
        config.setSliceBorders(config.getSliceBorders());
        config.setTranslateReceivedBubbles(config.isTranslateReceivedBubbles());
        config.setTranslationApiUrl(config.getTranslationApiUrl());
        config.setTranslationApiKey(config.getTranslationApiKey());
        config.setTranslationLocalDir(config.getTranslationLocalDir());
        config.setTranslationLocalStartScript(config.getTranslationLocalStartScript());
        config.setTranslationAutoStartLocalService(config.isTranslationAutoStartLocalService());
        config.setAdditionalVoskHints(config.getAdditionalVoskHints());
        ModLogger.info("Config loaded: mode={}, lang={}", config.getSelectedMode(), config.getSelectedLanguage());
    }

    public void save() {
        Path file = configDir.resolve(CONFIG_FILE);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            ModLogger.error("Failed to save config.", e);
        }
    }

    public ModConfig getConfig() { return config; }
}


