package de.mmerlin.mmxp.whatabubble.client.gui;

import de.mmerlin.mmxp.whatabubble.client.WhatABubbleClient;
import de.mmerlin.mmxp.whatabubble.config.BubbleVisibility;
import de.mmerlin.mmxp.whatabubble.config.ConfigManager;
import de.mmerlin.mmxp.whatabubble.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

public class WhatABubbleSettingsScreen extends Screen {
    private static final int FIELD_WIDTH = 220;
    private static final int SMALL_FIELD_WIDTH = 48;
    private static final int ROW_HEIGHT = 24;

    /** Sentinel value shown in the UI for the system-default microphone. */
    private static final String DEFAULT_MIC_LABEL = "Standard (System)";
    /** Stored in config when the default device is selected. */
    private static final String DEFAULT_MIC_VALUE = "";

    private final Screen parent;
    private CyclingButtonWidget<String>          micButton;
    private CyclingButtonWidget<BubbleVisibility> visibilityButton;
    private CyclingButtonWidget<Boolean>          debugModeButton;
    private TextFieldWidget bubbleRedField;
    private TextFieldWidget bubbleGreenField;
    private TextFieldWidget bubbleBlueField;
    private TextFieldWidget textRedField;
    private TextFieldWidget textGreenField;
    private TextFieldWidget textBlueField;
    private TextFieldWidget maxCharsField;

    /** Mic names as stored in config (empty string = default). */
    private List<String> micValues;

    public WhatABubbleSettingsScreen(Screen parent) {
        super(Text.literal("WhatABubble Settings"));
        this.parent = parent;
    }

    // ────────────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        super.init();

        ConfigManager configManager = WhatABubbleClient.getInstance().getConfigManager();
        ModConfig config = configManager.getConfig();

        int cx   = this.width / 2;
        int topY = this.height / 2 - 120;

        // ── Build microphone list ────────────────────────────────────────────
        micValues = buildMicList();
        String savedMic    = config.getSelectedMicrophone();
        String initialMic  = micValues.contains(savedMic) ? savedMic : DEFAULT_MIC_VALUE;

        micButton = CyclingButtonWidget.<String>builder(
                        mic -> Text.literal(mic.isEmpty() ? DEFAULT_MIC_LABEL : mic),
                        initialMic
                )
                .values(micValues)
                .build(cx - FIELD_WIDTH / 2, topY, FIELD_WIDTH, 20, Text.literal("Mikrofon"));
        this.addDrawableChild(micButton);

        // ── Bubble-Visibility dropdown ───────────────────────────────────────
        visibilityButton = CyclingButtonWidget.<BubbleVisibility>builder(
                        vis -> switch (vis) {
                            case ALL          -> Text.literal("Alle sehen");
                            case OTHERS_ONLY  -> Text.literal("Alle außer eigene");
                            case NONE         -> Text.literal("Keine sehen");
                        },
                        config.getBubbleVisibility()
                )
                .values(BubbleVisibility.values())
                .build(cx - FIELD_WIDTH / 2, topY + ROW_HEIGHT, FIELD_WIDTH, 20, Text.literal("Bubbles"));
        this.addDrawableChild(visibilityButton);

        bubbleRedField = createNumberField(cx - 74, topY + ROW_HEIGHT * 2, config.getBubbleRed(), 3);
        bubbleGreenField = createNumberField(cx - 24, topY + ROW_HEIGHT * 2, config.getBubbleGreen(), 3);
        bubbleBlueField = createNumberField(cx + 26, topY + ROW_HEIGHT * 2, config.getBubbleBlue(), 3);

        textRedField = createNumberField(cx - 74, topY + ROW_HEIGHT * 3, config.getTextRed(), 3);
        textGreenField = createNumberField(cx - 24, topY + ROW_HEIGHT * 3, config.getTextGreen(), 3);
        textBlueField = createNumberField(cx + 26, topY + ROW_HEIGHT * 3, config.getTextBlue(), 3);

        maxCharsField = createNumberField(cx - FIELD_WIDTH / 2, topY + ROW_HEIGHT * 4, FIELD_WIDTH, config.getMaxBubbleLineChars(), 3);

        // ── Debug-Modus toggle ───────────────────────────────────────────────
        debugModeButton = CyclingButtonWidget.<Boolean>builder(
                        v -> v ? Text.literal("An") : Text.literal("Aus"),
                        config.isDebugMode()
                )
                .values(Boolean.FALSE, Boolean.TRUE)
                .build(cx - FIELD_WIDTH / 2, topY + ROW_HEIGHT * 5, FIELD_WIDTH, 20, Text.literal("Debug"));
        this.addDrawableChild(debugModeButton);

        // ── Save / Refresh / Cancel ──────────────────────────────────────────
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Speichern"), btn -> save())
                .dimensions(cx - 167, topY + ROW_HEIGHT * 7 + 20, 106, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), btn -> refreshFromJson())
                .dimensions(cx - 53, topY + ROW_HEIGHT * 7 + 20, 106, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Abbrechen"), btn -> close())
                .dimensions(cx + 61, topY + ROW_HEIGHT * 7 + 20, 106, 20)
                .build());
    }

    private TextFieldWidget createNumberField(int x, int y, int initialValue, int maxLength) {
        return createNumberField(x, y, SMALL_FIELD_WIDTH, initialValue, maxLength);
    }

    private TextFieldWidget createNumberField(int x, int y, int width, int initialValue, int maxLength) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, width, 20, Text.empty());
        field.setMaxLength(maxLength);
        field.setText(String.valueOf(initialValue));
        field.setChangedListener(text -> {
            if (!text.matches("\\d*")) {
                field.setText(text.replaceAll("[^\\d]", ""));
            }
        });
        this.addDrawableChild(field);
        return field;
    }

    // ────────────────────────────────────────────────────────────────────────
    private static List<String> buildMicList() {
        List<String> list = new ArrayList<>();
        list.add(DEFAULT_MIC_VALUE); // system default is always first
        AudioFormat fmt = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mi);
            if (mixer.isLineSupported(info)) {
                list.add(mi.getName());
            }
        }
        return list;
    }

    // ────────────────────────────────────────────────────────────────────────
    private void save() {
        WhatABubbleClient wab = WhatABubbleClient.getInstance();
        ConfigManager configManager = wab.getConfigManager();
        ModConfig config = configManager.getConfig();
        String previousMicrophone = config.getSelectedMicrophone();
        String previousLanguage = config.getSelectedLanguage();
        List<String> previousHints = config.getVoskHints();

        config.setSelectedMicrophone(micButton.getValue());
        config.setBubbleVisibility(visibilityButton.getValue());
        config.setBubbleRed(parseIntOrDefault(bubbleRedField.getText(), config.getBubbleRed(), 0, 255));
        config.setBubbleGreen(parseIntOrDefault(bubbleGreenField.getText(), config.getBubbleGreen(), 0, 255));
        config.setBubbleBlue(parseIntOrDefault(bubbleBlueField.getText(), config.getBubbleBlue(), 0, 255));
        config.setTextRed(parseIntOrDefault(textRedField.getText(), config.getTextRed(), 0, 255));
        config.setTextGreen(parseIntOrDefault(textGreenField.getText(), config.getTextGreen(), 0, 255));
        config.setTextBlue(parseIntOrDefault(textBlueField.getText(), config.getTextBlue(), 0, 255));
        config.setMaxBubbleLineChars(parseIntOrDefault(maxCharsField.getText(), config.getMaxBubbleLineChars(), 10, 100));
        config.setUseNineSlice(true);
        config.setDebugMode(debugModeButton.getValue());
        configManager.save();

        wab.applyCurrentConfig(previousMicrophone, previousLanguage, previousHints, false);

        if (this.client != null) this.client.setScreen(parent);
    }

    private void refreshFromJson() {
        WhatABubbleClient wab = WhatABubbleClient.getInstance();
        wab.reloadConfigFromDisk();
        if (this.client != null) {
            this.client.setScreen(new WhatABubbleSettingsScreen(parent));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx   = this.width / 2;
        int topY = this.height / 2 - 120;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, topY - 12, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Mikrofon"), cx - FIELD_WIDTH / 2, topY - 10, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Bubbles"), cx - FIELD_WIDTH / 2, topY + ROW_HEIGHT - 10, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Bubble RGB"), cx - FIELD_WIDTH / 2, topY + ROW_HEIGHT * 2 - 10, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Text RGB"), cx - FIELD_WIDTH / 2, topY + ROW_HEIGHT * 3 - 10, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Zeilenumbruch (10-100)"), cx - FIELD_WIDTH / 2, topY + ROW_HEIGHT * 4 - 10, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Debug"), cx - FIELD_WIDTH / 2, topY + ROW_HEIGHT * 5 - 10, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("R"), cx - 59, topY + ROW_HEIGHT * 2 + 6, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("G"), cx - 9, topY + ROW_HEIGHT * 2 + 6, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("B"), cx + 41, topY + ROW_HEIGHT * 2 + 6, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("R"), cx - 59, topY + ROW_HEIGHT * 3 + 6, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("G"), cx - 9, topY + ROW_HEIGHT * 3 + 6, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("B"), cx + 41, topY + ROW_HEIGHT * 3 + 6, 0xFFFFFF);
    }

    private static int parseIntOrDefault(String text, int fallback, int min, int max) {
        if (text == null || text.isBlank()) return fallback;
        try {
            int value = Integer.parseInt(text);
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}

