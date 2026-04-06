package de.mmerlin.mmxp.whatabubble.client.event;

import de.mmerlin.mmxp.whatabubble.bubble.BubbleLifetimeController;
import de.mmerlin.mmxp.whatabubble.bubble.BubbleStack;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubbleManager;
import de.mmerlin.mmxp.whatabubble.client.speech.BubbleTranslationService;
import de.mmerlin.mmxp.whatabubble.config.ConfigManager;
import de.mmerlin.mmxp.whatabubble.config.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.UUID;

public class ClientTickHandler {
    private static final String DEBUG_BUBBLE_SOURCE_LANGUAGE = "en";
    private static final String DEBUG_BUBBLE_DEFAULT_TEXT = "I am a test message";

    private final BubbleLifetimeController lifetimeController;
    private final SpeechBubbleManager bubbleManager;
    private final ConfigManager configManager;
    private String cachedDebugBubbleLanguage = "";
    private String cachedDebugBubbleText = DEBUG_BUBBLE_DEFAULT_TEXT;
    private boolean debugTranslationPending;

    public ClientTickHandler(BubbleLifetimeController lifetimeController,
                              SpeechBubbleManager bubbleManager,
                              ConfigManager configManager) {
        this.lifetimeController = lifetimeController;
        this.bubbleManager = bubbleManager;
        this.configManager = configManager;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            lifetimeController.tick();

            // Debug-Modus: permanente "WhatABubble"-Bubble über dem lokalen Spieler anzeigen.
            // Bubble wird über den BubbleManager verwaltet → identischer Render-Pfad wie echte Bubbles.
            if (client.player != null && configManager.getConfig().isDebugMode()) {
                UUID playerUuid = client.player.getUuid();
                BubbleStack stack = bubbleManager.getStack(playerUuid);
                if (stack == null || stack.isEmpty()) {
                    bubbleManager.addBubble(playerUuid, getDebugBubbleText());
                }
            } else {
                debugTranslationPending = false;
            }
        });
    }

    private String getDebugBubbleText() {
        ModConfig config = configManager.getConfig();
        String targetLanguage = config.getSelectedLanguage();

        if (targetLanguage == null || targetLanguage.isBlank()) {
            cachedDebugBubbleLanguage = "";
            cachedDebugBubbleText = DEBUG_BUBBLE_DEFAULT_TEXT;
            return cachedDebugBubbleText;
        }

        if (!targetLanguage.equalsIgnoreCase(cachedDebugBubbleLanguage)) {
            cachedDebugBubbleLanguage = targetLanguage;
            cachedDebugBubbleText = DEBUG_BUBBLE_DEFAULT_TEXT;
            debugTranslationPending = false;
        }

        if (!debugTranslationPending) {
            debugTranslationPending = true;
            BubbleTranslationService.translateReceivedBubble(
                            config,
                            DEBUG_BUBBLE_DEFAULT_TEXT,
                            DEBUG_BUBBLE_SOURCE_LANGUAGE,
                            targetLanguage
                    )
                    .thenAccept(translated -> {
                        cachedDebugBubbleText = translated == null || translated.isBlank()
                                ? DEBUG_BUBBLE_DEFAULT_TEXT
                                : translated.trim();
                        debugTranslationPending = false;
                    });
        }

        return cachedDebugBubbleText;
    }
}


