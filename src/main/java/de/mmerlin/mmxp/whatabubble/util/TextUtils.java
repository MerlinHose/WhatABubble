package de.mmerlin.mmxp.whatabubble.util;

import java.util.ArrayList;
import java.util.List;

public final class TextUtils {

    private TextUtils() {}

    /**
     * Splits {@code text} into lines that are at most {@code maxChars} characters long.
     * Lines are broken at whitespace boundaries only – words are never cut in half.
     *
     * <p>If a single word is longer than {@code maxChars} it is placed on its own line
     * (it cannot be broken further without cutting characters).
     *
     * @param text     the input string (may be null or blank)
     * @param maxChars maximum number of characters per line (≤ 0 means no limit)
     * @return list of wrapped lines; never null, never contains blank entries
     */
    public static List<String> wordWrap(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;

        String trimmed = text.trim();
        // No wrapping needed
        if (maxChars <= 0 || trimmed.length() <= maxChars) {
            lines.add(trimmed);
            return lines;
        }

        String[] words = trimmed.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.isEmpty()) {
                // First word of a new line – always add even if it exceeds maxChars alone
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxChars) {
                // Word fits on the current line
                current.append(' ').append(word);
            } else {
                // Flush current line and start a new one
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }
}

