package de.mmerlin.mmxp.whatabubble.speech;

import de.mmerlin.mmxp.whatabubble.speech.dummy.DummySpeechService;
import de.mmerlin.mmxp.whatabubble.speech.vosk.VoskSpeechService;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SpeechServiceFactory {

    private SpeechServiceFactory() { }

    /**
     * Tries to create a VoskSpeechService using the model at
     * {@code {gameDir}/config/whatabubble/vosk-model/}.
     * Falls back to DummySpeechService if no model is found.
     */
    public static SpeechService createBestAvailable(String language, Path gameDir, String microphoneName, List<String> grammarHints) {
        Path modelPath = findModelPath(language, gameDir);
        if (modelPath != null) {
            ModLogger.info("Vosk model found at: {}", modelPath);
            ensureModelConf(modelPath);
            disableIncompatibleCarpa(modelPath);
            return new VoskSpeechService(modelPath.toAbsolutePath().toString(), microphoneName, grammarHints);
        }
        Path defaultModelPath = gameDir.resolve("config/whatabubble/vosk-model");
        ModLogger.warn("No Vosk model found. Falling back to DummySpeechService.");
        ModLogger.warn("Download a model from https://alphacephei.com/vosk/models");
        ModLogger.warn("and place it at: {}", defaultModelPath);
        return new DummySpeechService();
    }

    public static Path findModelPath(String language, Path gameDir) {
        Path modelPath = gameDir.resolve("config/whatabubble/vosk-model");
        if (Files.isDirectory(modelPath)) {
            return modelPath;
        }

        Path langModelPath = gameDir.resolve("config/whatabubble/vosk-model-" + language);
        if (Files.isDirectory(langModelPath)) {
            return langModelPath;
        }

        return null;
    }

    /**
     * Vosk requires {@code conf/model.conf} and {@code conf/mfcc.conf}.
     * Small models include them; some large model distributions do not.
     * This method auto-creates both files with standard TDNN-chain values
     * so that both small and large models work without manual intervention.
     */
    private static void ensureModelConf(Path modelRoot) {
        Path confDir   = modelRoot.resolve("conf");
        Path modelConf = confDir.resolve("model.conf");
        Path mfccConf  = confDir.resolve("mfcc.conf");

        boolean needsModelConf = !Files.exists(modelConf);
        boolean needsMfccConf  = !Files.exists(mfccConf);

        if (!needsModelConf && !needsMfccConf) return; // nothing to do

        ModLogger.warn("conf/ files missing from Vosk model – creating defaults for TDNN chain model.");

        // Read frame_subsampling_factor written by Kaldi training (default = 3)
        int subsampling = 3;
        Path subfactorFile = modelRoot.resolve("am/frame_subsampling_factor");
        if (Files.exists(subfactorFile)) {
            try {
                subsampling = Integer.parseInt(
                        Files.readString(subfactorFile, StandardCharsets.UTF_8).trim());
            } catch (Exception ignored) { }
        }

        try {
            Files.createDirectories(confDir);

            if (needsModelConf) {
                // Standard options for online2-wav-nnet3-latgen-faster (TDNN chain)
                String content =
                        "--min-active=200\n" +
                        "--max-active=3000\n" +
                        "--beam=10\n" +
                        "--lattice-beam=2\n" +
                        "--acoustic-scale=1.0\n" +
                        "--frame-subsampling-factor=" + subsampling + "\n" +
                        "--endpoint.silence-phones=1:2:3:4:5:6:7:8:9:10\n" +
                        "--endpoint.rule2.min-trailing-silence=0.5\n" +
                        "--endpoint.rule3.min-trailing-silence=1.0\n" +
                        "--endpoint.rule4.min-trailing-silence=2.0\n";
                Files.writeString(modelConf, content, StandardCharsets.UTF_8);
                ModLogger.info("Created conf/model.conf (frame-subsampling-factor={})", subsampling);
            }

            if (needsMfccConf) {
                int melBins = detectMelBins(modelRoot);
                // low-freq=20 matches typical wideband models (16 kHz recording);
                // high-freq=-400 means 8000-400=7600 Hz, filtering extreme HF noise.
                String content =
                        "--use-energy=false\n" +
                        "--sample-frequency=16000\n" +
                        "--num-mel-bins=" + melBins + "\n" +
                        "--num-ceps=" + melBins + "\n" +
                        "--low-freq=20\n" +
                        "--high-freq=-400\n" +
                        "--snip-edges=false\n";
                Files.writeString(mfccConf, content, StandardCharsets.UTF_8);
                ModLogger.info("Created conf/mfcc.conf (num-mel-bins={})", melBins);
            }
        } catch (IOException e) {
            ModLogger.error("Could not create conf/ files: " + e.getMessage(), e);
        }
    }

    /**
     * Detects the correct MFCC feature dimension for this model by reading the
     * ivector LDA transform matrix ({@code ivector/final.mat}).
     *
     * <p>Kaldi binary matrix format: {@code \0B FM \4<int32 rows> \4<int32 cols>}
     * <p>cols = feature_dim * splice_frames + 1 (bias column)
     * <p>splice_frames defaults to 7 ([-3…3]) → feature_dim = (cols - 1) / 7
     *
     * <p>Falls back to 30 (works for both small and known large German models).
     */
    private static int detectMelBins(Path modelRoot) {
        Path lda = modelRoot.resolve("ivector/final.mat");
        if (Files.exists(lda)) {
            try (var in = Files.newInputStream(lda)) {
                byte[] hdr = in.readNBytes(20);
                // Scan for \4 size marker (int32) which marks the "rows" value
                for (int i = 0; i < hdr.length - 9; i++) {
                    if (hdr[i] == 0x04) {                       // rows marker
                        // skip 4 bytes (rows value)
                        if (hdr[i + 5] == 0x04) {              // cols marker
                            int cols = ((hdr[i + 6] & 0xFF))
                                     | ((hdr[i + 7] & 0xFF) << 8)
                                     | ((hdr[i + 8] & 0xFF) << 16)
                                     | ((hdr[i + 9] & 0xFF) << 24);
                            // cols = feature_dim * splice_frames + 1 (bias)
                            // default splice: [-3, 3] = 7 frames
                            int inputDim = cols - 1; // remove bias column
                            for (int spliceFrames : new int[]{7, 3, 1, 9, 5}) {
                                if (inputDim % spliceFrames == 0) {
                                    int detected = inputDim / spliceFrames;
                                    if (detected >= 20 && detected <= 100) {
                                        ModLogger.info("LDA matrix cols={} → detected feature dim={} (splice={})",
                                                cols, detected, spliceFrames);
                                        return detected;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                ModLogger.warn("Could not read ivector/final.mat: {}", e.getMessage());
            }
        }
        // Final fallback: 30 works for vosk-model-small-de AND vosk-model-de large
        ModLogger.info("Could not detect feature dim from LDA – defaulting to 30");
        return 30;
    }

    /**
     * Vosk 0.3.45 uses an older Kaldi build that cannot read the new int64-based
     * ConstArpaLm binary format used by large models (>2 GB G.carpa).
     *
     * Detection: the old format writes state counts as int32 (4 bytes after
     * the &lt;ConstArpaLm&gt; token); the new format writes them as int64 (8 bytes).
     * If the file looks like the new format, rename it to {@code G.carpa.disabled}
     * so Vosk falls back to the smaller G.fst trigram rescoring model instead.
     */
    private static void disableIncompatibleCarpa(Path modelRoot) {
        Path carpa = modelRoot.resolve("rescore/G.carpa");
        if (!Files.exists(carpa)) return;

        // Check file format version: old Kaldi (int32 counts) vs new Kaldi (int64 counts).
        // After the binary header "\0B<ConstArpaLm> " the first int is the num_lm_states.
        // Old format: "\4" + 4-byte int32; new format: "\8" + 8-byte int64.
        boolean incompatible = false;
        try (var fis = Files.newInputStream(carpa)) {
            byte[] header = fis.readNBytes(64);
            // Scan for the size-byte marker after the "<ConstArpaLm>" token
            for (int i = 0; i < header.length - 1; i++) {
                if (header[i] == 0x08) { // \8 = 8-byte (int64) marker → new format
                    incompatible = true;
                    break;
                }
                if (header[i] == 0x04) { // \4 = 4-byte (int32) marker → old format, OK
                    break;
                }
            }
        } catch (IOException e) {
            ModLogger.warn("Could not inspect rescore/G.carpa: {}", e.getMessage());
        }

        if (incompatible) {
            Path disabled = modelRoot.resolve("rescore/G.carpa.disabled");
            try {
                Files.move(carpa, disabled);
                ModLogger.warn("rescore/G.carpa uses the new int64 Kaldi format which Vosk 0.3.45 cannot read.");
                ModLogger.warn("Renamed G.carpa → G.carpa.disabled. Falling back to G.fst rescoring.");
                ModLogger.warn("Recognition still works; accuracy is slightly reduced without CARPA.");
            } catch (IOException e) {
                ModLogger.error("Could not rename G.carpa: " + e.getMessage(), e);
            }
        }
    }
}


