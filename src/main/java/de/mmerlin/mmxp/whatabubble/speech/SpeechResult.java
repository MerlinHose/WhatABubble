package de.mmerlin.mmxp.whatabubble.speech;

public class SpeechResult {

    private final String text;
    private final float confidence;
    private final boolean isFinal;

    public SpeechResult(String text, float confidence, boolean isFinal) {
        this.text = text;
        this.confidence = confidence;
        this.isFinal = isFinal;
    }

    public String getText() { return text; }

    public float getConfidence() { return confidence; }

    public boolean isFinal() { return isFinal; }
}

