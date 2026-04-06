package de.mmerlin.mmxp.whatabubble.client.hud;

import de.mmerlin.mmxp.whatabubble.bubble.BubbleStack;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubble;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubbleManager;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * Renders the LOCAL player's own speech bubbles as a HUD overlay
 * (visible in first-person). Other players' bubbles are rendered
 * in 3D world space by BubbleRenderer.
 */
public class LocalBubbleHud {

    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int LINE_HEIGHT = 12;
    private static final int BG_COLOR = 0xB0000000;   // semi-transparent black
    private static final int TEXT_COLOR = 0xFFFFFFFF; // white

    private final SpeechBubbleManager bubbleManager;

    public LocalBubbleHud(SpeechBubbleManager bubbleManager) {
        this.bubbleManager = bubbleManager;
    }

    public void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) return;

        BubbleStack stack = bubbleManager.getStack(client.player.getUuid());
        if (stack == null || stack.isEmpty()) return;

        List<SpeechBubble> bubbles = stack.getAll();
        int screenWidth = client.getWindow().getScaledWidth();

        // Calculate the widest line
        int maxTextWidth = 0;
        for (SpeechBubble bubble : bubbles) {
            int w = client.textRenderer.getWidth(bubble.getText());
            if (w > maxTextWidth) maxTextWidth = w;
        }

        int boxWidth  = maxTextWidth + PADDING_X * 2;
        int boxHeight = bubbles.size() * LINE_HEIGHT + PADDING_Y * 2;
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = 8; // top-center of screen

        // Draw background
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_COLOR);

        // Draw each bubble text (newest last = bottom)
        for (int i = 0; i < bubbles.size(); i++) {
            SpeechBubble bubble = bubbles.get(i);
            String text = bubble.getText();

            // Fade alpha near end of lifetime
            float ratio = bubble.getAgeRatio();
            float alpha = ratio < 0.75f ? 1.0f : 1.0f - (ratio - 0.75f) / 0.25f;
            int a = Math.max(16, (int)(alpha * 255));
            int color = (a << 24) | (TEXT_COLOR & 0x00FFFFFF);

            int textX = boxX + PADDING_X;
            int textY = boxY + PADDING_Y + i * LINE_HEIGHT;
            context.drawText(client.textRenderer, text, textX, textY, color, true);
        }

        ModLogger.debug("[LocalBubbleHud] Rendered {} bubble(s) for local player {}",
                bubbles.size(), client.player.getName().getString());
    }
}

