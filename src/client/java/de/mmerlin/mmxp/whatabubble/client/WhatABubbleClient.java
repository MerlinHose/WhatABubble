package de.mmerlin.mmxp.whatabubble.client;
import de.mmerlin.mmxp.whatabubble.bubble.BubbleLifetimeController;
import de.mmerlin.mmxp.whatabubble.bubble.BubbleStack;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubble;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubbleManager;
import de.mmerlin.mmxp.whatabubble.client.audio.MicrophoneCapture;
import de.mmerlin.mmxp.whatabubble.client.debug.DebugOverlay;
import de.mmerlin.mmxp.whatabubble.client.event.ClientTickHandler;
import de.mmerlin.mmxp.whatabubble.client.event.PlayerJoinHandler;
import de.mmerlin.mmxp.whatabubble.client.event.RenderEventHandler;
import de.mmerlin.mmxp.whatabubble.client.render.CameraStateHolder;
import de.mmerlin.mmxp.whatabubble.client.gui.WhatABubbleSettingsScreen;
import de.mmerlin.mmxp.whatabubble.client.network.ClientNetworkHandler;
import de.mmerlin.mmxp.whatabubble.client.speech.BubbleTranslationService;
import de.mmerlin.mmxp.whatabubble.client.render.BubbleRenderer;
import de.mmerlin.mmxp.whatabubble.client.render.BubbleTextureManager;
import de.mmerlin.mmxp.whatabubble.config.ConfigManager;
import de.mmerlin.mmxp.whatabubble.config.ModConfig;
import de.mmerlin.mmxp.whatabubble.mode.ModeManager;
import de.mmerlin.mmxp.whatabubble.speech.SpeechService;
import de.mmerlin.mmxp.whatabubble.speech.SpeechServiceFactory;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import de.mmerlin.mmxp.whatabubble.util.TextUtils;
import de.mmerlin.mmxp.whatabubble.voicechat.NoopVoiceChatIntegration;
import de.mmerlin.mmxp.whatabubble.voicechat.VoiceChatController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WhatABubbleClient implements ClientModInitializer {
    private static WhatABubbleClient INSTANCE;
    private static final Text VOSK_READY_MESSAGE = Text.literal("[WhatABubble] Vosk-Modell geladen. Spracherkennung ist bereit.");
    private ConfigManager configManager;
    private ModeManager modeManager;
    private SpeechBubbleManager bubbleManager;
    private BubbleLifetimeController lifetimeController;
    private BubbleRenderer bubbleRenderer;
    private BubbleTextureManager bubbleTextureManager;
    private SpeechService speechService;
    private MicrophoneCapture microphoneCapture;
    private DebugOverlay debugOverlay;
    private ClientNetworkHandler clientNetworkHandler;
    private VoiceChatController voiceChatController;
    private boolean joinedWorld;
    private boolean speechServiceNeedsReload = true;
    private boolean speechServiceReadyMessagePending;
    private final List<Text> pendingSpeechStatusMessages = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        ModLogger.info("=== WhatABubble client initializing ===");

        ModLogger.info("[Init] Loading config...");
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir());
        configManager.load();
        BubbleTranslationService.reloadLocalServiceConfig(configManager.getConfig());
        ModLogger.info("[Init] Config loaded.");

        ModLogger.info("[Init] Setting up ModeManager. Mode={}", configManager.getConfig().getSelectedMode());
        modeManager = new ModeManager(configManager.getConfig());

        ModLogger.info("[Init] Creating SpeechBubbleManager and BubbleLifetimeController...");
        bubbleManager = new SpeechBubbleManager();
        lifetimeController = new BubbleLifetimeController(bubbleManager);
        bubbleTextureManager = new BubbleTextureManager();
        bubbleTextureManager.reloadFromConfig(configManager.getConfig());
        bubbleRenderer = new BubbleRenderer(bubbleManager);

        ModLogger.info("[Init] Setting up VoiceChatController...");
        voiceChatController = new VoiceChatController(new NoopVoiceChatIntegration());

        ModLogger.info("[Init] Setting up DebugOverlay...");
        debugOverlay = new DebugOverlay();
        debugOverlay.updateMode(modeManager.getCurrentMode());

        ModLogger.info("[Init] Registering client network receivers...");
        clientNetworkHandler = new ClientNetworkHandler(bubbleManager, modeManager, debugOverlay);
        clientNetworkHandler.registerReceivers();
        ModLogger.info("[Init] Client network receivers registered.");

        ModLogger.info("[Init] Creating MicrophoneCapture (bufferSize=4096)...");
        microphoneCapture = new MicrophoneCapture(4096);

        ModLogger.info("[Init] Registering key bindings...");
        KeyBindings.register();
        ModLogger.info("[Init] Key bindings registered.");

        ModLogger.info("[Init] Registering tick, join and render handlers...");
        new ClientTickHandler(lifetimeController, bubbleManager, configManager).register();
        new PlayerJoinHandler(bubbleManager).register();
        new RenderEventHandler().register();

        // Open settings screen when the configured key is pressed
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.OPEN_SETTINGS.wasPressed()) {
                client.setScreen(new WhatABubbleSettingsScreen(client.currentScreen));
            }
            tickSpeechLifecycle(client);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BubbleTranslationService.shutdownManagedLocalService());

        // ── HUD overlay: debug overlay only ──────────────────────────────────
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> debugOverlay.render(drawContext));

        // ── Local player bubble rendering (BEFORE_ENTITIES) ─────────────────
        // BEFORE_ENTITIES fires before the entity label queue is flushed, so
        // submitLabel() calls here ARE included in the current frame.
        // AFTER_ENTITIES fires after the queue has already been rendered – too late.
        //
        // submitLabel() with squaredDistanceToCamera=0 is silently skipped for the
        // local player in the entity renderer, so we render it here instead.
        // Skipped when NineSlice mode is active (BubbleRenderer handles rendering).
        WorldRenderEvents.BEFORE_ENTITIES.register(this::renderLocalPlayerBubbles);

        // ── NineSlice (BubbleRenderer) – END_MAIN ─────────────────────────────
        // Custom 9-slice billboard renderer. Registered after translucent world
        // geometry so water and clouds do not overdraw the bubbles.
        WorldRenderEvents.END_MAIN.register(this::renderNineSliceBubbles);

        ModLogger.info("[Init] All handlers registered.");

        boolean shouldListen = modeManager.getCurrentStrategy().shouldShowBubbles();
        ModLogger.info("[Init] shouldShowBubbles={} -> {}",
                shouldListen, shouldListen ? "Speech recognition will start after world join" : "Speech recognition NOT started (mode disabled)");

        ModLogger.info("=== WhatABubble client initialized. Mode={} ===", modeManager.getCurrentMode());
    }

    private static final float BUBBLE_FADE_START  = 0.75f;
    private static final float BUBBLE_HEAD_OFFSET = 0.3f;
    private static final float BUBBLE_SPACING     = 0.28f;

    /**
     * Renders speech bubbles for the LOCAL player via
     * {@link WorldRenderEvents#BEFORE_ENTITIES}.
     *
     * <p>Must run in BEFORE_ENTITIES so labels are submitted before the
     * {@link OrderedRenderCommandQueue} is flushed at the end of entity rendering.
     * AFTER_ENTITIES fires too late – the queue has already been drawn.
     *
     * <p>All OTHER players (and self in F5 / third-person) are handled by
     * {@code EntityRendererMixin} which injects into {@code renderLabelIfPresent}.
     *
     * <p>In first-person the local player entity is never rendered, so
     * {@code renderLabelIfPresent} is never called – hence the need for this
     * fallback using {@code ctx.commandQueue()}.
     */
    private void renderLocalPlayerBubbles(WorldRenderContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (client.options.getPerspective().isFirstPerson()) return;

        // NineSlice mode active → BubbleRenderer handles all rendering via END_MAIN.
        if (configManager.getConfig().isUseNineSlice()) return;

        boolean debugMode = configManager.getConfig().isDebugMode();
        de.mmerlin.mmxp.whatabubble.config.BubbleVisibility vis = configManager.getConfig().getBubbleVisibility();

        // Visibility filter: Debug-Modus umgeht alle Sichtbarkeits-Einschränkungen,
        // damit die Test-Bubble immer sichtbar ist.
        if (!debugMode) {
            if (vis == de.mmerlin.mmxp.whatabubble.config.BubbleVisibility.NONE
                    || vis == de.mmerlin.mmxp.whatabubble.config.BubbleVisibility.OTHERS_ONLY) {
                return;
            }
        }

        // Debug-Bubble wird vom ClientTickHandler in den BubbleManager injiziert.
        // Kein separater submitLabel-Aufruf nötig – sie erscheint in der normalen Bubble-Liste.
        BubbleStack stack = bubbleManager.getStack(client.player.getUuid());
        if (stack == null || stack.isEmpty()) return;

        // Get the command queue used for label / entity rendering.
        OrderedRenderCommandQueue commandQueue = ctx.commandQueue();
        if (commandQueue == null) {
            ModLogger.warn("[Render] renderLocalPlayerBubbles: commandQueue is null – skipping.");
            return;
        }

        // Camera state – prefer ctx.worldState(), fall back to CameraStateHolder
        // (populated by RenderEventHandler via BEFORE_ENTITIES capture).
        CameraRenderState camState = null;
        if (ctx.worldState() != null) {
            camState = ctx.worldState().cameraRenderState;
        }
        if (camState == null) {
            camState = CameraStateHolder.get();
        }
        if (camState == null) {
            ModLogger.warn("[Render] renderLocalPlayerBubbles: CameraRenderState not available – skipping.");
            return;
        }

        // World-space position just above the player's head.
        Vec3d playerPos = client.player.getLerpedPos(1.0f);
        Vec3d labelBase = playerPos.add(0.0, client.player.getHeight() + 0.5, 0.0);

        // Actual distance from camera so submitLabel does not discard the entry.
        double distSq = labelBase.squaredDistanceTo(camState.pos);

        MatrixStack matrices = new MatrixStack();

        List<SpeechBubble> bubbles = stack.getAll();
        ModLogger.debug("[Render] BEFORE_ENTITIES: submitting {} bubble(s) for local player (distSq={})", bubbles.size(), distSq);
        for (int i = 0; i < bubbles.size(); i++) {
            SpeechBubble bubble = bubbles.get(i);
            float ratio = bubble.getAgeRatio();
            float alpha = ratio < BUBBLE_FADE_START
                    ? 1.0f
                    : 1.0f - (ratio - BUBBLE_FADE_START) / (1.0f - BUBBLE_FADE_START);
            int bgAlpha = Math.max(4, (int) (alpha * 0x60));
            int bgColor = bgAlpha << 24;

            Vec3d bubblePos = labelBase.add(0.0, BUBBLE_HEAD_OFFSET + i * BUBBLE_SPACING, 0.0);

            commandQueue.submitLabel(
                    matrices,
                    bubblePos,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    Text.literal(bubble.getText()),
                    client.player.isSneaking(),
                    bgColor,
                    distSq,
                    camState
            );
        }
    }

    /**
     * Renders speech bubbles for ALL players using the custom 9-slice billboard
     * renderer ({@link BubbleRenderer}).
     *
     * <p>Only active when {@code config.useNineSlice = true}.
     * Uses {@link WorldRenderEvents#END_MAIN} so translucent world
     * surfaces like water or clouds do not render on top of the bubbles.
     * It also needs
     * {@link VertexConsumerProvider} which is available there.
     */
    private void renderNineSliceBubbles(WorldRenderContext ctx) {
        if (!configManager.getConfig().isUseNineSlice()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        VertexConsumerProvider consumers = ctx.consumers();
        if (consumers == null) {
            ModLogger.warn("[Render] renderNineSliceBubbles: consumers is null – skipping.");
            return;
        }

        CameraRenderState camState = ctx.worldState().cameraRenderState;
        Vec3d cameraPos = camState.pos;
        Quaternionf cameraRotation = camState.orientation;

        float tickDelta = 1.0f; // fully interpolated; bubbles on static entities

        bubbleRenderer.renderAll(new MatrixStack(), consumers, cameraPos, cameraRotation, tickDelta);
    }

    private void tickSpeechLifecycle(MinecraftClient client) {
        flushPendingSpeechStatusMessages(client);

        if (speechServiceReadyMessagePending && client.player != null) {
            client.player.sendMessage(VOSK_READY_MESSAGE, false);
            speechServiceReadyMessagePending = false;
        }

        boolean inWorld = joinedWorld && client.world != null && client.player != null;
        boolean shouldListen = modeManager.getCurrentStrategy().shouldShowBubbles();

        if (!inWorld || !shouldListen) {
            if (speechService != null && speechService.isRunning()) {
                stopSpeechRecognition();
            }
            return;
        }

        if (speechService == null || speechServiceNeedsReload) {
            initializeSpeechService();
        }

        if (speechService != null && !speechService.isRunning()) {
            startSpeechRecognition();
        }
    }

    private void initializeSpeechService() {
        if (!joinedWorld) return;

        if (speechService != null && speechService.isRunning()) {
            stopSpeechRecognition();
        }

        queueSpeechStatusMessage(Text.literal("[WhatABubble] Sprachmodell wird gestartet..."));

        var modelPath = SpeechServiceFactory.findModelPath(
                configManager.getConfig().getSelectedLanguage(),
                FabricLoader.getInstance().getGameDir()
        );
        if (modelPath != null) {
            queueSpeechStatusMessage(Text.literal("[WhatABubble] Modell gefunden: " + modelPath.getFileName()));
        } else {
            queueSpeechStatusMessage(Text.literal("[WhatABubble] Kein Modell gefunden. DummySpeechService wird verwendet."));
        }

        ModLogger.info("[Speech] Initializing speech service (language={}, mic='{}')...",
                configManager.getConfig().getSelectedLanguage(),
                configManager.getConfig().getSelectedMicrophone());

        speechService = SpeechServiceFactory.createBestAvailable(
                configManager.getConfig().getSelectedLanguage(),
                FabricLoader.getInstance().getGameDir(),
                configManager.getConfig().getSelectedMicrophone(),
                configManager.getConfig().getVoskHints()
        );
        speechService.setReadyCallback(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> speechServiceReadyMessagePending = true);
        });
        speechServiceNeedsReload = false;
        ModLogger.info("[Speech] SpeechService type: {}", speechService.getClass().getSimpleName());

        if (!speechService.getClass().getSimpleName().contains("Vosk")) {
            queueSpeechStatusMessage(Text.literal("[WhatABubble] Vosk ist nicht verfügbar. Fallback-Modus aktiv."));
        }
    }

    private void queueSpeechStatusMessage(Text message) {
        pendingSpeechStatusMessages.add(message);
    }

    private void flushPendingSpeechStatusMessages(MinecraftClient client) {
        if (client.player == null || pendingSpeechStatusMessages.isEmpty()) return;

        for (Text message : List.copyOf(pendingSpeechStatusMessages)) {
            client.player.sendMessage(message, false);
        }
        pendingSpeechStatusMessages.clear();
    }

    public void startSpeechRecognition() {
        if (speechService == null) {
            ModLogger.info("[Speech] startSpeechRecognition skipped – speech service not initialized yet.");
            return;
        }
        ModLogger.info("[Speech] Starting speech recognition...");
        speechService.start(result -> {
            if (!result.isFinal() || result.getText().isBlank()) {
                ModLogger.debug("[Speech] Ignoring non-final or blank result: final={} text=\"{}\"",
                        result.isFinal(), result.getText());
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                ModLogger.warn("[Speech] Got result but client.player is null – cannot add bubble.");
                return;
            }
            String text = result.getText();
            ModLogger.info("[Speech] Final result recognized: \"{}\" for player {} ({})",
                    text, client.player.getName().getString(), client.player.getUuid());
            debugOverlay.updateRecognizedText(text);

            int maxChars = configManager.getConfig().getMaxBubbleLineChars();
            for (String line : TextUtils.wordWrap(text, maxChars)) {
                bubbleManager.addBubble(client.player.getUuid(), line);
            }
            ModLogger.info("[Speech] Bubble(s) added locally (maxChars={}). Sending SpeechPacket to server...", maxChars);
            clientNetworkHandler.sendSpeechPacket(
                    client.player.getUuid(),
                    configManager.getConfig().getSelectedLanguage(),
                    text
            );
            ModLogger.info("[Speech] SpeechPacket sent.");
        });
        ModLogger.info("[Speech] Speech recognition started.");
    }

    public void stopSpeechRecognition() {
        if (speechService == null) return;
        ModLogger.info("[Speech] Stopping speech recognition...");
        speechService.stop();
        ModLogger.info("[Speech] Speech recognition stopped.");
    }

    /** Stops the current service, recreates it with the current config (mic + language), and restarts. */
    public void reloadSpeechService() {
        ModLogger.info("[Speech] Reload requested (mic='{}').",
                configManager.getConfig().getSelectedMicrophone());
        speechServiceNeedsReload = true;

        if (!joinedWorld) {
            ModLogger.info("[Speech] Reload deferred until the player joins a world.");
            return;
        }

        initializeSpeechService();
        if (modeManager.getCurrentStrategy().shouldShowBubbles()) {
            startSpeechRecognition();
        }
        ModLogger.info("[Speech] Speech service reloaded.");
    }

    public void requestSpeechServiceReloadForMicrophoneChange() {
        reloadSpeechService();
    }

    public void applyCurrentConfig(String previousMicrophone,
                                   String previousLanguage,
                                   List<String> previousHints,
                                   boolean notifyUser) {
        applyConfig(configManager.getConfig(), previousMicrophone, previousLanguage, previousHints, notifyUser);
    }

    public void reloadConfigFromDisk() {
        reloadConfigFromDisk(true);
    }

    private void reloadConfigFromDisk(boolean notifyUser) {
        ModConfig previousConfig = configManager.getConfig();
        String previousMicrophone = previousConfig.getSelectedMicrophone();
        String previousLanguage = previousConfig.getSelectedLanguage();
        List<String> previousHints = previousConfig.getVoskHints();

        configManager.load();
        applyConfig(configManager.getConfig(), previousMicrophone, previousLanguage, previousHints, notifyUser);
        ModLogger.info("[Config] JSON configuration reloaded from disk.");
    }

    private void applyConfig(ModConfig currentConfig,
                             String previousMicrophone,
                             String previousLanguage,
                             List<String> previousHints,
                             boolean notifyUser) {
        modeManager.reloadFromConfig(currentConfig);
        debugOverlay.updateMode(modeManager.getCurrentMode());
        BubbleTranslationService.reloadLocalServiceConfig(currentConfig);
        if (bubbleTextureManager != null) {
            bubbleTextureManager.reloadFromConfig(currentConfig);
        }
        if (joinedWorld) {
            BubbleTranslationService.prepareTranslationBackend(currentConfig);
        }

        boolean speechSettingsChanged = !Objects.equals(previousMicrophone, currentConfig.getSelectedMicrophone())
                || !Objects.equals(previousLanguage, currentConfig.getSelectedLanguage())
                || !Objects.equals(previousHints, currentConfig.getVoskHints());

        if (speechSettingsChanged) {
            reloadSpeechService();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (notifyUser && client != null && client.player != null) {
            client.player.sendMessage(Text.literal("[WhatABubble] JSON-Konfiguration neu geladen."), false);
        }
    }

    public void onJoinedWorld() {
        joinedWorld = true;
        speechServiceNeedsReload = true;
        reloadConfigFromDisk(false);
        pendingSpeechStatusMessages.clear();
        queueSpeechStatusMessage(Text.literal("[WhatABubble] Welt betreten. Spracherkennung wird vorbereitet..."));
        ModLogger.info("[Speech] Player joined a world – speech service may now initialize.");
    }

    public void onLeftWorld() {
        joinedWorld = false;
        speechServiceReadyMessagePending = false;
        pendingSpeechStatusMessages.clear();
        if (speechService != null && speechService.isRunning()) {
            stopSpeechRecognition();
        }
        BubbleTranslationService.shutdownManagedLocalService();
        speechService = null;
        speechServiceNeedsReload = true;
        ModLogger.info("[Speech] Player left the world – speech service unloaded.");
    }

    public static WhatABubbleClient getInstance() { return INSTANCE; }
    public ConfigManager getConfigManager() { return configManager; }
    public ModeManager getModeManager() { return modeManager; }
    public SpeechBubbleManager getBubbleManager() { return bubbleManager; }
    public BubbleTextureManager getBubbleTextureManager() { return bubbleTextureManager; }
    public DebugOverlay getDebugOverlay() { return debugOverlay; }
    public SpeechService getSpeechService() { return speechService; }
}



