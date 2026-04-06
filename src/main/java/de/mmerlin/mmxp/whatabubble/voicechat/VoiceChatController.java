package de.mmerlin.mmxp.whatabubble.voicechat;

import de.mmerlin.mmxp.whatabubble.mode.ModeStrategy;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;

import java.util.UUID;

public class VoiceChatController {

    private final VoiceChatIntegration integration;

    public VoiceChatController(VoiceChatIntegration integration) {
        this.integration = integration;
        if (!integration.isAvailable()) {
            ModLogger.info("Simple Voice Chat not found — voice control disabled.");
        }
    }

    public void applyStrategy(UUID playerUuid, ModeStrategy strategy) {
        if (!integration.isAvailable()) return;
        integration.applyMode(playerUuid, strategy.shouldAllowVoice());
    }

    public void onPlayerJoin(UUID playerUuid) {
        if (!integration.isAvailable()) return;
        ModLogger.debug("VoiceChatController: player joined {}", playerUuid);
    }

    public void onPlayerLeave(UUID playerUuid) {
        if (!integration.isAvailable()) return;
        integration.unmutePlayer(playerUuid);
    }
}


