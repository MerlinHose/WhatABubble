package de.mmerlin.mmxp.whatabubble.client.render;

import de.mmerlin.mmxp.whatabubble.bubble.BubbleStack;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubble;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubbleManager;
import de.mmerlin.mmxp.whatabubble.client.WhatABubbleClient;
import de.mmerlin.mmxp.whatabubble.config.BubbleVisibility;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.List;
import java.util.UUID;

/**
 * Renders speech bubbles above player heads using billboard quads.
 * Registered via {@link net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents#END_MAIN}.
 *
 * <p>Each bubble is rendered as a 9-slice textured background + SEE_THROUGH text label.
 * Slot 0 = newest bubble = lowest position; higher slots stack upward.
 */
public class BubbleRenderer {
    private static long nextDiagnosticsLogAtMs = 0L;

    /** How far above the player's head the lowest bubble renders (world units before scale). */
    private static final float HEAD_HEIGHT_OFFSET = 0.5f;
    /** Vertical spacing between consecutive bubbles (text-pixel units, pre-scale). */
    private static final float BUBBLE_Y_SPACING = 13.0f;
    /** World-space scale — matches vanilla name-tag scale. */
    private static final float RENDER_SCALE = 0.025f;
    /** Ratio of lifetime at which fade-out begins. */
    private static final float FADE_START = 0.75f;

    private final SpeechBubbleManager bubbleManager;

    public BubbleRenderer(SpeechBubbleManager bubbleManager) {
        this.bubbleManager = bubbleManager;
    }

    // ── Public render entry point ─────────────────────────────────────────────

    public void renderAll(MatrixStack matrices,
                          VertexConsumerProvider consumers,
                          Vec3d cameraPos,
                          Quaternionf cameraRotation,
                          float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        WhatABubbleClient modClient = WhatABubbleClient.getInstance();
        if (modClient == null) return;

        BubbleVisibility vis = modClient.getConfigManager().getConfig().getBubbleVisibility();
        if (vis == BubbleVisibility.NONE) return;
        int bubbleRgb = modClient.getConfigManager().getConfig().getBubbleColorRgb();
        int textRgb = modClient.getConfigManager().getConfig().getTextColorRgb();
        int padLeft = modClient.getConfigManager().getConfig().getPaddingLeft();
        int padTop = modClient.getConfigManager().getConfig().getPaddingTop();
        int padRight = modClient.getConfigManager().getConfig().getPaddingRight();
        int padBottom = modClient.getConfigManager().getConfig().getPaddingBottom();
        int sliceLeft = modClient.getConfigManager().getConfig().getSliceLeft();
        int sliceTop = modClient.getConfigManager().getConfig().getSliceTop();
        int sliceRight = modClient.getConfigManager().getConfig().getSliceRight();
        int sliceBottom = modClient.getConfigManager().getConfig().getSliceBottom();

        UUID localUuid = client.player != null ? client.player.getUuid() : null;
        boolean debugMode = modClient.getConfigManager().getConfig().isDebugMode();

        List<AbstractClientPlayerEntity> players = client.world.getPlayers();
        for (AbstractClientPlayerEntity player : players) {
            if (player.isSneaking()) continue;

            boolean isLocal = player.getUuid().equals(localUuid);
            if (isLocal && client.options.getPerspective().isFirstPerson()) continue;
            if (vis == BubbleVisibility.OTHERS_ONLY && isLocal) continue;

            BubbleStack stack = bubbleManager.getStack(player.getUuid());
            boolean hasStack = stack != null && !stack.isEmpty();

            if (!hasStack) continue;

            renderPlayerBubbles(matrices, consumers, player, stack,
                    cameraPos, cameraRotation, tickDelta, client.textRenderer,
                    bubbleRgb, textRgb,
                    padLeft, padTop, padRight, padBottom,
                    sliceLeft, sliceTop, sliceRight, sliceBottom);

            if (debugMode && isLocal) {
                long now = System.currentTimeMillis();
                if (now >= nextDiagnosticsLogAtMs) {
                    nextDiagnosticsLogAtMs = now + 3000L;
                    Vec3d headPos = player.getLerpedPos(tickDelta)
                            .add(0, player.getHeight() + HEAD_HEIGHT_OFFSET, 0);
                    ModLogger.info("[BubbleRenderer] Rendering local bubble(s): count={} headPos={} cameraPos={}",
                            stack.getAll().size(), headPos, cameraPos);
                }
            }
        }
    }

    // ── Per-player rendering ──────────────────────────────────────────────────

    private void renderPlayerBubbles(MatrixStack matrices,
                                     VertexConsumerProvider consumers,
                                     AbstractClientPlayerEntity player,
                                     BubbleStack stack,
                                     Vec3d cameraPos,
                                     Quaternionf cameraRotation,
                                     float tickDelta,
                                     TextRenderer textRenderer,
                                     int bubbleRgb,
                                     int textRgb,
                                     int padLeft,
                                     int padTop,
                                     int padRight,
                                     int padBottom,
                                     int sliceLeft,
                                     int sliceTop,
                                     int sliceRight,
                                     int sliceBottom) {
        Vec3d headPos = player.getLerpedPos(tickDelta)
                .add(0, player.getHeight() + HEAD_HEIGHT_OFFSET, 0);

        // Full brightness so bubbles are readable in any lighting condition
        int packedLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        matrices.push();
        BillboardHelper.applyBillboard(matrices, headPos, cameraPos, cameraRotation, RENDER_SCALE);

        List<SpeechBubble> bubbles = (stack != null) ? stack.getAll() : List.of();  // oldest = index 0, newest = last

        for (int i = 0; i < bubbles.size(); i++) {
            // Newest bubble (last in list) → slot 0 (lowest, nearest head)
            int bubbleSlot = bubbles.size() - 1 - i;
            renderSingleBubble(matrices, consumers, bubbles.get(i), bubbleSlot, textRenderer, packedLight,
                    bubbleRgb, textRgb,
                    padLeft, padTop, padRight, padBottom,
                    sliceLeft, sliceTop, sliceRight, sliceBottom);
        }

        matrices.pop();
    }

    // ── Per-bubble rendering ──────────────────────────────────────────────────


    private void renderSingleBubble(MatrixStack matrices,
                                    VertexConsumerProvider consumers,
                                    SpeechBubble bubble,
                                    int slot,
                                    TextRenderer textRenderer,
                                    int packedLight,
                                    int bubbleRgb,
                                    int textRgb,
                                    int padLeft,
                                    int padTop,
                                    int padRight,
                                    int padBottom,
                                    int sliceLeft,
                                    int sliceTop,
                                    int sliceRight,
                                    int sliceBottom) {
        String text = bubble.getText();
        float halfWidth = textRenderer.getWidth(text) / 2.0f;
        float alpha = calculateAlpha(bubble);
        int alphaInt = Math.max(8, (int) (alpha * 255));
        int textColor = RenderUtils.withAlpha(textRgb, alphaInt);

        matrices.push();
        // Slot 0 = no upward offset (lowest / newest);
        // Each higher slot moves further up in world space.
        // Because BillboardHelper negates Y-scale, subtracting in pixel space = moving UP.
        matrices.translate(0.0f, slot * -BUBBLE_Y_SPACING, 0.0f);

        // ── 9-slice background ────────────────────────────────────────────────
        // text is rendered at x=-halfWidth, y=0 (top of text).
        NineSliceRenderer.draw(
                matrices, consumers,
                -halfWidth - padLeft,                  // x0
                -padTop,                               // y0
                halfWidth + padRight,                  // x1
                textRenderer.fontHeight + padBottom,   // y1
                sliceLeft,
                sliceTop,
                sliceRight,
                sliceBottom,
                bubbleRgb, alphaInt, packedLight
        );

        // ── Text label ────────────────────────────────────────────────────────
        textRenderer.draw(
                text,
                -halfWidth,
                0,
                textColor,
                false,
                matrices.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0,   // no extra background from textRenderer (we have the 9-slice)
                packedLight
        );

        matrices.pop();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** 0→1: full opacity for most of lifetime, fades near expiry. */
    private static float calculateAlpha(SpeechBubble bubble) {
        float ratio = bubble.getAgeRatio();
        if (ratio < FADE_START) return 1.0f;
        return 1.0f - (ratio - FADE_START) / (1.0f - FADE_START);
    }
}