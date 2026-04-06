package de.mmerlin.mmxp.whatabubble.voicechat;

import java.util.UUID;

public interface VoiceChatIntegration {

    boolean isAvailable();

    void mutePlayer(UUID playerUuid);

    void unmutePlayer(UUID playerUuid);

    void applyMode(UUID playerUuid, boolean allowVoice);
}

