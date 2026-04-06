package de.mmerlin.mmxp.whatabubble.speech.vosk;

import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import de.mmerlin.mmxp.whatabubble.speech.SpeechResult;
import de.mmerlin.mmxp.whatabubble.speech.SpeechService;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
public class VoskSpeechService implements SpeechService {
    private static final Gson GSON = new Gson();

    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 4096;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    /**
     * RMS energy threshold below which a frame is considered silence.
     * 16-bit audio max is 32767. ~300 filters fans/idle noise without cutting soft speech.
     * Raise this value if still too many false positives.
     */
    private static final double SILENCE_THRESHOLD = 300.0;

    /**
     * How many consecutive silent frames before the recognizer is reset.
     * BUFFER_SIZE/2 samples per frame at 16 kHz → each frame ≈ 128 ms.
     * 15 frames ≈ ~2 seconds of silence → reset.
     */
    private static final int SILENT_FRAMES_RESET = 15;

    /** Minimum number of characters in a final result before it is dispatched. */
    private static final int MIN_RESULT_CHARS = 3;

    private final String modelPath;
    private final String microphoneName;   // empty = system default
    private final List<String> grammarHints;
    private volatile boolean running = false;
    private Thread recognitionThread;
    private Consumer<SpeechResult> callback;
    private volatile Runnable readyCallback;

    public VoskSpeechService(String modelPath, String microphoneName) {
        this(modelPath, microphoneName, List.of());
    }

    public VoskSpeechService(String modelPath, String microphoneName, List<String> grammarHints) {
        this.modelPath = modelPath;
        this.microphoneName = microphoneName == null ? "" : microphoneName;
        this.grammarHints = normalizeGrammarHints(grammarHints);
    }

    @Override
    public void setReadyCallback(Runnable readyCallback) {
        this.readyCallback = readyCallback;
    }

    @Override
    public void start(Consumer<SpeechResult> resultCallback) {
        if (running) return;
        this.callback = resultCallback;
        this.running = true;
        recognitionThread = new Thread(this::recognitionLoop, "whatabubble-vosk");
        recognitionThread.setDaemon(true);
        recognitionThread.start();
        ModLogger.info("VoskSpeechService started with model: {}", modelPath);
    }

    @Override
    public void stop() {
        running = false;
        if (recognitionThread != null) {
            recognitionThread.interrupt();
            recognitionThread = null;
        }
        ModLogger.info("VoskSpeechService stopped.");
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public void setLanguage(String languageCode) {
        // Language is determined by the model path; restart needed to change language.
        ModLogger.warn("Language change requires restarting VoskSpeechService with a different model.");
    }

    private void recognitionLoop() {
        // Only log native warnings; we've diagnosed startup issues already
        LibVosk.setLogLevel(LogLevel.WARNINGS);
        ModLogger.info("[Vosk] Loading model from: {}", modelPath);

        // Log all available capture mixers so the user can see which device will be used
        ModLogger.info("Available audio capture devices:");
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            DataLine.Info testInfo = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (mixer.isLineSupported(testInfo)) {
                ModLogger.info("  [MIC] {}", mixerInfo.getName());
            }
        }

        String grammarJson = buildGrammarJson(grammarHints);
        if (grammarJson != null) {
            ModLogger.info("[Vosk] Using {} grammar hint(s) for recognition.", grammarHints.size());
        }

        try (Model model = new Model(modelPath);
             Recognizer recognizer = grammarJson != null
                     ? new Recognizer(model, SAMPLE_RATE, grammarJson)
                     : new Recognizer(model, SAMPLE_RATE)) {
            Runnable onReady = readyCallback;
            if (onReady != null) {
                onReady.run();
            }

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                ModLogger.warn("Default microphone line not supported!");
                ModLogger.warn("Check your system default recording device.");
                return;
            }

            try (TargetDataLine line = openMicrophone()) {
                line.open(FORMAT, BUFFER_SIZE * 2);
                line.start();
                ModLogger.info("Microphone opened. Listening on: {}",
                        line.getLineInfo().toString());
                ModLogger.info("(If wrong device, set your Windows default recording device to your real mic)");

                byte[] buffer = new byte[BUFFER_SIZE];
                int silentFrameCount = 0;
                boolean hasPendingPartial = false;

                while (running && !Thread.currentThread().isInterrupted()) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead <= 0) continue;

                    // --- Energy-based Voice Activity Detection ---
                    double rms = computeRms(buffer, bytesRead);
                    if (rms < SILENCE_THRESHOLD) {
                        silentFrameCount++;
                        if (silentFrameCount >= SILENT_FRAMES_RESET) {
                            // Extended silence: force-finalize any pending speech, then reset
                            if (hasPendingPartial) {
                                String finalJson = recognizer.getFinalResult();
                                SpeechResult result = parseResult(finalJson, true);
                                if (result != null && result.getText().length() >= MIN_RESULT_CHARS) {
                                    ModLogger.info("[Vosk] Recognized (forced final): \"{}\"", result.getText());
                                    callback.accept(result);
                                }
                                hasPendingPartial = false;
                            }
                            recognizer.reset();
                            silentFrameCount = 0;
                        }
                    } else {
                        silentFrameCount = 0;
                    }

                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        // Final result – Vosk detected endpoint naturally (e.g. 0.5s silence rule)
                        String json = recognizer.getResult();
                        SpeechResult result = parseResult(json, true);
                        hasPendingPartial = false;
                        // NOTE: do NOT check rms here – endpoint fires on a silence frame
                        if (result != null && result.getText().length() >= MIN_RESULT_CHARS) {
                            ModLogger.info("[Vosk] Recognized (final): \"{}\"", result.getText());
                            callback.accept(result);
                        }
                    } else {
                        // Partial – only log, never dispatch as bubble
                        String json = recognizer.getPartialResult();
                        SpeechResult partial = parseResult(json, false);
                        if (partial != null && !partial.getText().isBlank()) {
                            hasPendingPartial = true;
                        }
                    }
                }
                line.stop();
            }
        } catch (LineUnavailableException e) {
            ModLogger.error("Microphone unavailable: " + e.getMessage(), e);
        } catch (IOException e) {
            ModLogger.error("Vosk model error: " + e.getMessage(), e);
        } catch (Exception e) {
            if (running) {
                ModLogger.error("Unexpected error in Vosk loop: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Opens the configured microphone, or the system default if none is set.
     */
    private TargetDataLine openMicrophone() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        if (!microphoneName.isEmpty()) {
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                if (mixerInfo.getName().equals(microphoneName)) {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    if (mixer.isLineSupported(info)) {
                        TargetDataLine line = (TargetDataLine) mixer.getLine(info);
                        ModLogger.info("[Vosk] Using selected microphone: {}", microphoneName);
                        return line;
                    }
                }
            }
            ModLogger.warn("[Vosk] Microphone '{}' not found – falling back to system default.", microphoneName);
        }
        ModLogger.info("[Vosk] Using system default microphone.");
        return (TargetDataLine) AudioSystem.getLine(info);
    }

    /**
     * Computes the Root Mean Square energy of a 16-bit little-endian PCM buffer.
     * Returns a value in [0, 32767].
     */
    private static double computeRms(byte[] buffer, int bytesRead) {
        long sumSq = 0;
        int samples = bytesRead / 2;
        for (int i = 0; i < bytesRead - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sumSq += (long) sample * sample;
        }
        return samples > 0 ? Math.sqrt((double) sumSq / samples) : 0;
    }

    private SpeechResult parseResult(String json, boolean isFinal) {
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return null;
            String key = isFinal ? "text" : "partial";
            JsonElement textEl = el.getAsJsonObject().get(key);
            if (textEl == null) return null;
            String text = repairMojibake(textEl.getAsString().trim());
            return text.isEmpty() ? null : new SpeechResult(text, 1.0f, isFinal);
        } catch (Exception e) {
            return null;
        }
    }

    private static String repairMojibake(String text) {
        if (text == null || text.isBlank()) return text;
        if (!looksLikeMojibake(text)) return text;

        String repaired = new String(text.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        if (!repaired.equals(text)) {
            ModLogger.info("[Vosk] Repaired mojibake text: \"{}\" -> \"{}\"", text, repaired);
        }
        return repaired;
    }

    private static boolean looksLikeMojibake(String text) {
        return text.contains("Ã")
                || text.contains("â€")
                || text.contains("â€“")
                || text.contains("â€”")
                || text.contains("â€¦")
                || text.contains("Â")
                || text.contains("ðŸ");
    }

    private static List<String> normalizeGrammarHints(List<String> rawHints) {
        if (rawHints == null || rawHints.isEmpty()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String hint : rawHints) {
            if (hint == null) continue;

            String trimmed = hint.trim();
            if (trimmed.isBlank()) continue;

            normalized.add(trimmed);
            normalized.add(trimmed.toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(normalized);
    }

    private static String buildGrammarJson(List<String> grammarHints) {
        if (grammarHints == null || grammarHints.isEmpty()) {
            return null;
        }

        List<String> grammarEntries = new ArrayList<>(grammarHints);
        grammarEntries.add("[unk]");
        return GSON.toJson(grammarEntries);
    }
}

