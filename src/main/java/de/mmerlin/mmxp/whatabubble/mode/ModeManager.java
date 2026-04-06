package de.mmerlin.mmxp.whatabubble.mode;

import de.mmerlin.mmxp.whatabubble.config.ModConfig;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;

public class ModeManager {

    private ModeStrategy currentStrategy;
    private ModConfig config;

    public ModeManager(ModConfig config) {
        this.config = config;
        this.currentStrategy = createStrategy(config.getSelectedMode());
    }

    public void setMode(BubbleMode mode) {
        this.currentStrategy = createStrategy(mode);
        config.setSelectedMode(mode);
        ModLogger.info("Mode changed to: {}", mode);
    }

    public void reloadFromConfig(ModConfig config) {
        this.config = config;
        this.currentStrategy = createStrategy(config.getSelectedMode());
        ModLogger.info("Mode reloaded from config: {}", config.getSelectedMode());
    }

    public ModeStrategy getCurrentStrategy() { return currentStrategy; }

    public BubbleMode getCurrentMode() { return currentStrategy.getMode(); }

    private ModeStrategy createStrategy(BubbleMode mode) {
        return switch (mode) {
            case BUBBLES_ONLY      -> new BubbleOnlyMode();
            case VOICE_AND_BUBBLES -> new VoiceAndBubbleMode();
            case FULL_MUTE         -> new FullMuteMode();
        };
    }
}


