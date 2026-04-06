package de.mmerlin.mmxp.whatabubble.mode;

import java.util.UUID;

public interface ModeStrategy {

    BubbleMode getMode();

    boolean shouldShowBubbles();

    boolean shouldAllowVoice();

    void applyToPlayer(UUID playerUuid);
}

