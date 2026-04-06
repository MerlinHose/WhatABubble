package de.mmerlin.mmxp.whatabubble.client.debug;

import de.mmerlin.mmxp.whatabubble.mode.BubbleMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class DebugOverlay {

    private boolean enabled = false;
    private String lastRecognizedText = "(none)";
    private BubbleMode currentMode = BubbleMode.BUBBLES_ONLY;

    /** Called from HudRenderCallback — renders debug info in the top-left corner. */
    public void render(DrawContext context) {
        if (!enabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null) return;

        context.drawTextWithShadow(client.textRenderer,
                "§e[WhatABubble] §rMode: §b" + currentMode.name(), 4, 4, 0xFFFFFF);
        context.drawTextWithShadow(client.textRenderer,
                "§e[WhatABubble] §rLast: §a" + lastRecognizedText, 4, 16, 0xFFFFFF);
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isEnabled() { return enabled; }

    public void toggle() { this.enabled = !this.enabled; }

    public void updateRecognizedText(String text) { this.lastRecognizedText = text; }

    public void updateMode(BubbleMode mode) { this.currentMode = mode; }
}


