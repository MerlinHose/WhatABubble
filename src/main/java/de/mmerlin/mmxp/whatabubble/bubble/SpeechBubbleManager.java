package de.mmerlin.mmxp.whatabubble.bubble;

import de.mmerlin.mmxp.whatabubble.util.ModLogger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeechBubbleManager {

    private static final long BUBBLE_LIFETIME_MS = 5_000L;

    private final Map<UUID, BubbleStack> stacks = new ConcurrentHashMap<>();

    /** Adds a new bubble for the given player. Thread-safe. */
    public void addBubble(UUID playerUuid, String text) {
        if (text == null || text.isBlank()) {
            ModLogger.warn("[BubbleManager] addBubble called with blank/null text for player {}", playerUuid);
            return;
        }
        BubbleStack stack = stacks.computeIfAbsent(playerUuid, k -> new BubbleStack());
        int sizeBefore = stack.size();
        stack.push(new SpeechBubble(text.trim(), BUBBLE_LIFETIME_MS));
        ModLogger.info("[BubbleManager] Bubble added for player {}: \"{}\" (stack size: {} -> {})",
                playerUuid, text.trim(), sizeBefore, stack.size());
    }

    /** Removes expired bubbles from all stacks; prunes empty stacks. Called every tick. */
    public void tickAll() {
        stacks.values().forEach(BubbleStack::removeExpired);
        stacks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /** Returns the bubble stack for the given player, or null if none. */
    public BubbleStack getStack(UUID playerUuid) {
        return stacks.get(playerUuid);
    }

    public Map<UUID, BubbleStack> getAllStacks() {
        return stacks;
    }

    /** Returns the total number of active bubbles across all players. */
    public int totalBubbleCount() {
        return stacks.values().stream().mapToInt(BubbleStack::size).sum();
    }

    public void removePlayer(UUID playerUuid) {
        ModLogger.debug("[BubbleManager] Removing bubble stack for player {}", playerUuid);
        stacks.remove(playerUuid);
    }

    public void clear() {
        int total = totalBubbleCount();
        ModLogger.info("[BubbleManager] Clearing all bubble stacks. Active stacks={}, total bubbles={}",
                stacks.size(), total);
        stacks.clear();
    }
}


