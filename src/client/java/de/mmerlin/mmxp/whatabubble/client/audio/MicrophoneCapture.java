package de.mmerlin.mmxp.whatabubble.client.audio;

import de.mmerlin.mmxp.whatabubble.util.ModLogger;

import javax.sound.sampled.*;
import java.util.function.Consumer;

/**
 * Captures PCM audio from the default microphone and streams
 * raw byte chunks to a consumer callback.
 * Runs in its own daemon thread.
 */
public class MicrophoneCapture {

    private final int bufferSize;
    private volatile boolean capturing = false;
    private Thread captureThread;
    private TargetDataLine dataLine;

    public MicrophoneCapture(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void start(Consumer<byte[]> audioDataConsumer) {
        if (capturing) return;
        capturing = true;
        captureThread = new Thread(() -> captureLoop(audioDataConsumer), "whatabubble-mic");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void stop() {
        capturing = false;
        if (dataLine != null) {
            dataLine.stop();
            dataLine.close();
            dataLine = null;
        }
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
    }

    public boolean isCapturing() { return capturing; }

    private void captureLoop(Consumer<byte[]> consumer) {
        AudioFormat format = AudioFormatProvider.get();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            ModLogger.warn("MicrophoneCapture: audio line not supported.");
            capturing = false;
            return;
        }

        try {
            dataLine = (TargetDataLine) AudioSystem.getLine(info);
            dataLine.open(format, bufferSize * 2);
            dataLine.start();
            ModLogger.info("MicrophoneCapture: started.");

            byte[] buffer = new byte[bufferSize];
            while (capturing && !Thread.currentThread().isInterrupted()) {
                int read = dataLine.read(buffer, 0, buffer.length);
                if (read > 0) {
                    byte[] chunk = new byte[read];
                    System.arraycopy(buffer, 0, chunk, 0, read);
                    consumer.accept(chunk);
                }
            }
        } catch (LineUnavailableException e) {
            ModLogger.error("MicrophoneCapture: line unavailable.", e);
        } finally {
            if (dataLine != null && dataLine.isOpen()) {
                dataLine.stop();
                dataLine.close();
            }
            capturing = false;
            ModLogger.info("MicrophoneCapture: stopped.");
        }
    }
}


