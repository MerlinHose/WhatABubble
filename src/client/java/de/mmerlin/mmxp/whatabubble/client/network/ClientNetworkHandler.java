package de.mmerlin.mmxp.whatabubble.client.network;

import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubbleManager;
import de.mmerlin.mmxp.whatabubble.client.WhatABubbleClient;
import de.mmerlin.mmxp.whatabubble.client.debug.DebugOverlay;
import de.mmerlin.mmxp.whatabubble.client.speech.BubbleTranslationService;
import de.mmerlin.mmxp.whatabubble.config.ModConfig;
import de.mmerlin.mmxp.whatabubble.mode.BubbleMode;
import de.mmerlin.mmxp.whatabubble.mode.ModeManager;
import de.mmerlin.mmxp.whatabubble.network.packet.ModeSyncPacket;
import de.mmerlin.mmxp.whatabubble.network.packet.SpeechPacket;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import de.mmerlin.mmxp.whatabubble.util.TextUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.UUID;

public class ClientNetworkHandler {

    private final SpeechBubbleManager bubbleManager;
    private final ModeManager modeManager;
    private final DebugOverlay debugOverlay;

    public ClientNetworkHandler(SpeechBubbleManager bubbleManager,
                                ModeManager modeManager,
                                DebugOverlay debugOverlay) {
        this.bubbleManager = bubbleManager;
        this.modeManager = modeManager;
        this.debugOverlay = debugOverlay;
    }

    public void registerReceivers() {
        ModLogger.info("[ClientNet] Registering SpeechPacket receiver...");
        // Receive bubble from server (forwarded speech of any player, including self)
        ClientPlayNetworking.registerGlobalReceiver(SpeechPacket.PACKET_ID, (payload, context) ->
                context.client().execute(() -> {
                    UUID localUuid = context.client().player != null
                            ? context.client().player.getUuid() : null;
                    // Skip local player — bubble was already added directly in startSpeechRecognition()
                    if (payload.playerUuid().equals(localUuid)) {
                        ModLogger.info("[ClientNet] Ignoring own SpeechPacket echo from server.");
                        return;
                    }
                    WhatABubbleClient wab = WhatABubbleClient.getInstance();
                    ModConfig config = wab != null ? wab.getConfigManager().getConfig() : null;
                    String targetLanguage = config != null ? config.getSelectedLanguage() : payload.languageCode();

                    ModLogger.info("[ClientNet] Received SpeechPacket from server: player={} lang={} text=\"{}\"",
                            payload.playerUuid(), payload.languageCode(), payload.text());

                    BubbleTranslationService.translateReceivedBubble(
                                    config,
                                    payload.text(),
                                    payload.languageCode(),
                                    targetLanguage
                            )
                            .thenAccept(translatedText -> context.client().execute(() -> {
                                int maxChars = wab != null
                                        ? wab.getConfigManager().getConfig().getMaxBubbleLineChars()
                                        : 40;
                                for (String line : TextUtils.wordWrap(translatedText, maxChars)) {
                                    bubbleManager.addBubble(payload.playerUuid(), line);
                                }
                                ModLogger.info("[ClientNet] Added bubble(s) for {} in local lang={}: \"{}\"",
                                        payload.playerUuid(), targetLanguage, translatedText);
                            }));
                })
        );

        ModLogger.info("[ClientNet] Registering ModeSyncPacket receiver...");
        // Receive mode sync from another player
        ClientPlayNetworking.registerGlobalReceiver(ModeSyncPacket.PACKET_ID, (payload, context) ->
                context.client().execute(() -> {
                    ModLogger.info("[ClientNet] Received ModeSyncPacket: player={} mode={}",
                            payload.playerUuid(), payload.mode());
                    debugOverlay.updateMode(payload.mode());
                    ModLogger.info("[ClientNet] Mode sync from {}: {}", payload.playerUuid(), payload.mode());
                })
        );
        ModLogger.info("[ClientNet] All receivers registered.");
    }

    /** Sends recognized speech to the server for broadcast to nearby players. */
    public void sendSpeechPacket(UUID playerUuid, String languageCode, String text) {
        ModLogger.info("[ClientNet] Sending SpeechPacket: player={} lang={} text=\"{}\"", playerUuid, languageCode, text);
        ClientPlayNetworking.send(new SpeechPacket(playerUuid, languageCode, text));
        ModLogger.debug("[ClientNet] SpeechPacket sent.");
    }

    /** Broadcasts the local player's current mode to the server. */
    public void sendModeSyncPacket(UUID playerUuid, BubbleMode mode) {
        ModLogger.info("[ClientNet] Sending ModeSyncPacket: player={} mode={}", playerUuid, mode);
        ClientPlayNetworking.send(new ModeSyncPacket(playerUuid, mode));
    }
}


