package de.mmerlin.mmxp.whatabubble.mode;

import java.util.UUID;

public class BubbleOnlyMode implements ModeStrategy {

    @Override public BubbleMode getMode() { return BubbleMode.BUBBLES_ONLY; }

    @Override public boolean shouldShowBubbles() { return true; }

    @Override public boolean shouldAllowVoice() { return false; }

    @Override public void applyToPlayer(UUID playerUuid) { }
}

