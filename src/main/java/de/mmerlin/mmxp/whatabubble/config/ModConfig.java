package de.mmerlin.mmxp.whatabubble.config;

import de.mmerlin.mmxp.whatabubble.mode.BubbleMode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ModConfig {
    private static final int DEFAULT_MAX_BUBBLE_LINE_CHARS = 40;
    private static final int MIN_MAX_BUBBLE_LINE_CHARS = 10;
    private static final int MAX_MAX_BUBBLE_LINE_CHARS = 100;
    private static final int[] DEFAULT_PADDING = {4, 2, 4, 2};
    private static final int[] DEFAULT_SLICE_BORDERS = {3, 3, 3, 3};
    private static final int MIN_PADDING = 0;
    private static final int MAX_PADDING = 64;
    private static final List<String> DEFAULT_VOSK_HINTS = List.of(
            "Creeper",
            "Zombie",
            "Skelett",
            "Enderman",
            "Villager",
            "Dorfbewohner",
            "Redstone",
            "Nether",
            "Netherite",
            "Diamant",
            "Diamantschwert",
            "Diamantspitzhacke",
            "Obsidian",
            "Spawner",
            "Bedrock",
            "Piglin",
            "Blaze",
            "Ghast",
            "Wither",
            "Enderdrache"
    );

    private BubbleMode selectedMode = BubbleMode.BUBBLES_ONLY;
    private String selectedLanguage = "de";
    /** Empty string = system default microphone. */
    private String selectedMicrophone = "";
    private BubbleVisibility bubbleVisibility = BubbleVisibility.ALL;
    /** Max characters per bubble line before word-wrapping. */
    private int maxBubbleLineChars = DEFAULT_MAX_BUBBLE_LINE_CHARS;
    /** When true, a permanent "WhatABubble" bubble is shown above the local player for render testing. */
    private boolean debugMode = false;
    private int bubbleRed = 203;
    private int bubbleGreen = 219;
    private int bubbleBlue = 252;
    private int textRed = 255;
    private int textGreen = 255;
    private int textBlue = 255;
    /** Empty string = bundled built-in bubble atlas. Otherwise local path or URL to a 3x3 bubble atlas image. */
    private String bubbleTexture = "";
    /** Content padding in pixels: [left, top, right, bottom]. */
    private int[] padding = DEFAULT_PADDING.clone();
    /** Nine-slice border thickness in pixels: [left, top, right, bottom]. */
    private int[] sliceBorders = DEFAULT_SLICE_BORDERS.clone();
    /** When enabled, received bubbles may be translated into the local selectedLanguage. */
    private boolean translateReceivedBubbles = false;
    /** Optional LibreTranslate-compatible endpoint, e.g. https://host.tld/translate */
    private String translationApiUrl = "";
    /** Optional API key for the translation backend. */
    private String translationApiKey = "";
    /** Relative to the Minecraft config dir unless absolute. Default = config/whatabubble/libretranslate */
    private String translationLocalDir = "whatabubble/libretranslate";
    /** Starter inside translationLocalDir, e.g. start.bat, start.cmd, start.ps1 or libretranslate.exe */
    private String translationLocalStartScript = "start.bat";
    /** Automatically start the local translation service from translationLocalDir when needed. */
    private boolean translationAutoStartLocalService = false;
    private List<String> additionalVoskHints = new ArrayList<>();

    public BubbleMode getSelectedMode() { return selectedMode; }
    public void setSelectedMode(BubbleMode mode) { this.selectedMode = mode; }

    public String getSelectedLanguage() { return selectedLanguage; }
    public void setSelectedLanguage(String language) { this.selectedLanguage = language; }

    public String getSelectedMicrophone() { return selectedMicrophone == null ? "" : selectedMicrophone; }
    public void setSelectedMicrophone(String mic) { this.selectedMicrophone = mic == null ? "" : mic; }

    public BubbleVisibility getBubbleVisibility() { return bubbleVisibility == null ? BubbleVisibility.ALL : bubbleVisibility; }
    public void setBubbleVisibility(BubbleVisibility v) { this.bubbleVisibility = v; }

    public int getMaxBubbleLineChars() { return clamp(maxBubbleLineChars, MIN_MAX_BUBBLE_LINE_CHARS, MAX_MAX_BUBBLE_LINE_CHARS, DEFAULT_MAX_BUBBLE_LINE_CHARS); }
    public void setMaxBubbleLineChars(int n) { this.maxBubbleLineChars = clamp(n, MIN_MAX_BUBBLE_LINE_CHARS, MAX_MAX_BUBBLE_LINE_CHARS, DEFAULT_MAX_BUBBLE_LINE_CHARS); }

    public boolean isUseNineSlice() { return true; }
    public void setUseNineSlice(boolean v) { }

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean v) { this.debugMode = v; }

    public int getBubbleRed() { return clampChannel(bubbleRed); }
    public void setBubbleRed(int bubbleRed) { this.bubbleRed = clampChannel(bubbleRed); }

    public int getBubbleGreen() { return clampChannel(bubbleGreen); }
    public void setBubbleGreen(int bubbleGreen) { this.bubbleGreen = clampChannel(bubbleGreen); }

    public int getBubbleBlue() { return clampChannel(bubbleBlue); }
    public void setBubbleBlue(int bubbleBlue) { this.bubbleBlue = clampChannel(bubbleBlue); }

    public int getTextRed() { return clampChannel(textRed); }
    public void setTextRed(int textRed) { this.textRed = clampChannel(textRed); }

    public int getTextGreen() { return clampChannel(textGreen); }
    public void setTextGreen(int textGreen) { this.textGreen = clampChannel(textGreen); }

    public int getTextBlue() { return clampChannel(textBlue); }
    public void setTextBlue(int textBlue) { this.textBlue = clampChannel(textBlue); }

    public int getBubbleColorRgb() {
        return (getBubbleRed() << 16) | (getBubbleGreen() << 8) | getBubbleBlue();
    }

    public int getTextColorRgb() {
        return (getTextRed() << 16) | (getTextGreen() << 8) | getTextBlue();
    }

    public String getBubbleTexture() {
        return bubbleTexture == null ? "" : bubbleTexture.trim();
    }

    public void setBubbleTexture(String bubbleTexture) {
        this.bubbleTexture = bubbleTexture == null ? "" : bubbleTexture.trim();
    }

    public int[] getPadding() {
        return sanitizePadding(padding);
    }

    public void setPadding(int[] padding) {
        this.padding = sanitizePadding(padding);
    }

    public int getPaddingLeft() {
        return getPadding()[0];
    }

    public int getPaddingTop() {
        return getPadding()[1];
    }

    public int getPaddingRight() {
        return getPadding()[2];
    }

    public int getPaddingBottom() {
        return getPadding()[3];
    }

    public int[] getSliceBorders() {
        return sanitizeBorders(sliceBorders, DEFAULT_SLICE_BORDERS);
    }

    public void setSliceBorders(int[] sliceBorders) {
        this.sliceBorders = sanitizeBorders(sliceBorders, DEFAULT_SLICE_BORDERS);
    }

    public int getSliceLeft() {
        return getSliceBorders()[0];
    }

    public int getSliceTop() {
        return getSliceBorders()[1];
    }

    public int getSliceRight() {
        return getSliceBorders()[2];
    }

    public int getSliceBottom() {
        return getSliceBorders()[3];
    }

    public boolean isTranslateReceivedBubbles() {
        return translateReceivedBubbles;
    }

    public void setTranslateReceivedBubbles(boolean translateReceivedBubbles) {
        this.translateReceivedBubbles = translateReceivedBubbles;
    }

    public String getTranslationApiUrl() {
        return translationApiUrl == null ? "" : translationApiUrl.trim();
    }

    public void setTranslationApiUrl(String translationApiUrl) {
        this.translationApiUrl = translationApiUrl == null ? "" : translationApiUrl.trim();
    }

    public String getTranslationApiKey() {
        return translationApiKey == null ? "" : translationApiKey.trim();
    }

    public void setTranslationApiKey(String translationApiKey) {
        this.translationApiKey = translationApiKey == null ? "" : translationApiKey.trim();
    }

    public String getTranslationLocalDir() {
        return translationLocalDir == null ? "whatabubble/libretranslate" : translationLocalDir.trim();
    }

    public void setTranslationLocalDir(String translationLocalDir) {
        this.translationLocalDir = translationLocalDir == null || translationLocalDir.isBlank()
                ? "whatabubble/libretranslate"
                : translationLocalDir.trim();
    }

    public String getTranslationLocalStartScript() {
        return translationLocalStartScript == null ? "start.bat" : translationLocalStartScript.trim();
    }

    public void setTranslationLocalStartScript(String translationLocalStartScript) {
        this.translationLocalStartScript = translationLocalStartScript == null || translationLocalStartScript.isBlank()
                ? "start.bat"
                : translationLocalStartScript.trim();
    }

    public boolean isTranslationAutoStartLocalService() {
        return translationAutoStartLocalService;
    }

    public void setTranslationAutoStartLocalService(boolean translationAutoStartLocalService) {
        this.translationAutoStartLocalService = translationAutoStartLocalService;
    }

    public List<String> getAdditionalVoskHints() {
        return additionalVoskHints == null ? new ArrayList<>() : new ArrayList<>(additionalVoskHints);
    }

    public void setAdditionalVoskHints(List<String> additionalVoskHints) {
        this.additionalVoskHints = additionalVoskHints == null ? new ArrayList<>() : new ArrayList<>(additionalVoskHints);
    }

    public List<String> getVoskHints() {
        Set<String> hints = new LinkedHashSet<>();
        DEFAULT_VOSK_HINTS.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(hints::add);
        getAdditionalVoskHints().stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(hints::add);
        return new ArrayList<>(hints);
    }

    private static int clampChannel(int value) {
        return clamp(value, 0, 255, 255);
    }

    private static int[] sanitizePadding(int[] value) {
        return sanitizeBorders(value, DEFAULT_PADDING);
    }

    private static int[] sanitizeBorders(int[] value, int[] defaults) {
        int[] sanitized = defaults.clone();
        if (value == null || value.length < 4) {
            return sanitized;
        }
        for (int i = 0; i < 4; i++) {
            sanitized[i] = clampPaddingValue(value[i]);
        }
        return sanitized;
    }

    private static int clampPaddingValue(int value) {
        return Math.max(MIN_PADDING, Math.min(MAX_PADDING, value));
    }

    private static int clamp(int value, int min, int max, int defaultValue) {
        if (value < min || value > max) {
            return Math.max(min, Math.min(max, value == 0 ? defaultValue : value));
        }
        return value;
    }
}
