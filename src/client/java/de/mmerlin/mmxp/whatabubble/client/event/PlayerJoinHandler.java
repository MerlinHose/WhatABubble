package de.mmerlin.mmxp.whatabubble.client.event;

import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubbleManager;
import de.mmerlin.mmxp.whatabubble.client.WhatABubbleClient;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class PlayerJoinHandler {

    private final SpeechBubbleManager bubbleManager;

    public PlayerJoinHandler(SpeechBubbleManager bubbleManager) {
        this.bubbleManager = bubbleManager;
    }

    public void register() {
        // Clear all bubbles when disconnecting from a server
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ModLogger.info("[PlayerJoinHandler] Client disconnected from server – clearing all bubbles.");
            bubbleManager.clear();
            WhatABubbleClient modClient = WhatABubbleClient.getInstance();
            if (modClient != null) {
                modClient.onLeftWorld();
            }
            ModLogger.info("[PlayerJoinHandler] All bubbles cleared.");
        });

        // Log when the client joins a server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String serverAddress = handler.getConnection().getAddress() != null
                    ? handler.getConnection().getAddress().toString()
                    : "unknown";
            ModLogger.info("[PlayerJoinHandler] Client joined server: {}", serverAddress);
            WhatABubbleClient modClient = WhatABubbleClient.getInstance();
            if (modClient != null) {
                modClient.onJoinedWorld();
            }
            ModLogger.info("[PlayerJoinHandler] WhatABubble is active. Ready to show speech bubbles.");
        });
    }
}


