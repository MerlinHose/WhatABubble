package de.mmerlin.mmxp.whatabubble.client.audio;

import javax.sound.sampled.AudioFormat;

public class AudioFormatProvider {

    public static final int SAMPLE_RATE = 16000;
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int CHANNELS = 1;
    public static final boolean SIGNED = true;
    public static final boolean BIG_ENDIAN = false;

    private AudioFormatProvider() { }

    public static AudioFormat get() {
        return new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
    }
}

