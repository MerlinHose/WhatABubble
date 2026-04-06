package de.mmerlin.mmxp.whatabubble.bubble;

import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.UUID;

public class SpeechBubble {

    private final UUID id;
    private final String text;
    private final long createdAtMs;
    private final long lifetimeMs;

    /** Cached per-bubble – text is final, so this is always valid once set. */
    private OrderedText cachedOrderedText;

    public SpeechBubble(String text, long lifetimeMs) {
        this.id = UUID.randomUUID();
        this.text = text;
        this.createdAtMs = System.currentTimeMillis();
        this.lifetimeMs = lifetimeMs;
    }

    public UUID getId() { return id; }

    public String getText() { return text; }

    /**
     * Returns the text as {@link OrderedText}, created lazily and cached.
     * Avoids allocating a new Text + OrderedText every render frame.
     */
    public OrderedText getOrderedText() {
        if (cachedOrderedText == null) {
            cachedOrderedText = Text.literal(text).asOrderedText();
        }
        return cachedOrderedText;
    }

    public long getCreatedAtMs() { return createdAtMs; }

    public boolean isExpired() { return System.currentTimeMillis() - createdAtMs > lifetimeMs; }

    public float getAgeRatio() { return (float)(System.currentTimeMillis() - createdAtMs) / lifetimeMs; }
}

