package de.mmerlin.mmxp.whatabubble.bubble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BubbleStack {

    public static final int MAX_BUBBLES = 4;

    // ArrayList gives O(1) indexed access needed by the renderer.
    // Tick and render are both on the main client thread → no synchronisation needed.
    private final List<SpeechBubble> bubbles = new ArrayList<>(MAX_BUBBLES);

    /** Adds bubble, removing oldest if stack is full (FIFO). */
    public void push(SpeechBubble bubble) {
        if (bubbles.size() >= MAX_BUBBLES) {
            bubbles.remove(0);
        }
        bubbles.add(bubble);
    }

    /** Removes all expired bubbles from the stack. */
    public void removeExpired() {
        bubbles.removeIf(SpeechBubble::isExpired);
    }

    /**
     * Returns a live unmodifiable view of all bubbles (oldest first).
     * No copy is made – avoids a heap allocation on every render frame.
     */
    public List<SpeechBubble> getAll() {
        return Collections.unmodifiableList(bubbles);
    }

    public boolean isEmpty() { return bubbles.isEmpty(); }

    public int size() { return bubbles.size(); }
}


