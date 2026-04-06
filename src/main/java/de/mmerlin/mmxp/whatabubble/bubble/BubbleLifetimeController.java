package de.mmerlin.mmxp.whatabubble.bubble;

import de.mmerlin.mmxp.whatabubble.util.ModLogger;

public class BubbleLifetimeController {

    private final SpeechBubbleManager bubbleManager;

    public BubbleLifetimeController(SpeechBubbleManager bubbleManager) {
        this.bubbleManager = bubbleManager;
    }

    public void tick() {
        int before = bubbleManager.totalBubbleCount();
        bubbleManager.tickAll();
        int after = bubbleManager.totalBubbleCount();
        if (before != after) {
            ModLogger.debug("[BubbleLifetime] Expired bubbles removed: {} -> {} (removed {})",
                    before, after, before - after);
        }
    }
}

