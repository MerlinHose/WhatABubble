package de.mmerlin.mmxp.whatabubble.mode;

import java.util.UUID;

public class FullMuteMode implements ModeStrategy {

    @Override public BubbleMode getMode() { return BubbleMode.FULL_MUTE; }

    @Override public boolean shouldShowBubbles() { return false; }

    @Override public boolean shouldAllowVoice() { return false; }

    @Override public void applyToPlayer(UUID playerUuid) { }
}

