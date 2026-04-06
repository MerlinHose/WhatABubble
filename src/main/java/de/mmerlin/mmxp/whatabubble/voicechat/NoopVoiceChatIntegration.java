package de.mmerlin.mmxp.whatabubble.voicechat;

import java.util.UUID;

public class NoopVoiceChatIntegration implements VoiceChatIntegration {

    @Override public boolean isAvailable() { return false; }

    @Override public void mutePlayer(UUID playerUuid) { }

    @Override public void unmutePlayer(UUID playerUuid) { }

    @Override public void applyMode(UUID playerUuid, boolean allowVoice) { }
}

