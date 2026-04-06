package de.mmerlin.mmxp.whatabubble;
import de.mmerlin.mmxp.whatabubble.network.NetworkHandler;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.fabricmc.api.ModInitializer;
public class WhatABubble implements ModInitializer {
    public static final String MOD_ID = "whatabubble";
    private final NetworkHandler networkHandler = new NetworkHandler();
    @Override
    public void onInitialize() {
        ModLogger.info("=== WhatABubble server initializing ===");
        ModLogger.info("[Server] Registering payload types...");
        networkHandler.registerPayloadTypes();
        ModLogger.info("[Server] Registering server packet receivers...");
        networkHandler.registerReceivers();
        ModLogger.info("=== WhatABubble server initialized ===");
    }
}