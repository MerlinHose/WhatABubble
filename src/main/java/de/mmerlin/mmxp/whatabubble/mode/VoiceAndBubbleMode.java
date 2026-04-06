package de.mmerlin.mmxp.whatabubble.mode;

import java.util.UUID;

public class VoiceAndBubbleMode implements ModeStrategy {

    @Override public BubbleMode getMode() { return BubbleMode.VOICE_AND_BUBBLES; }

    @Override public boolean shouldShowBubbles() { return true; }

    @Override public boolean shouldAllowVoice() { return true; }

    @Override public void applyToPlayer(UUID playerUuid) { }
}

