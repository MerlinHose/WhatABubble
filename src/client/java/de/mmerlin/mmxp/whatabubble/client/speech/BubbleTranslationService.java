package de.mmerlin.mmxp.whatabubble.client.speech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.mmerlin.mmxp.whatabubble.config.ModConfig;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Optional client-side translation for received bubbles.
 *
 * <p>This uses a LibreTranslate-compatible HTTP endpoint when configured.
 * If translation is disabled, unavailable, or fails, the original text is returned unchanged.
 */
public final class BubbleTranslationService {
    public static final String DEFAULT_LOCAL_API_URL = "http://127.0.0.1:5000/translate";
    private static final int CONNECT_TIMEOUT_MS = 4_000;
    private static final int READ_TIMEOUT_MS = 8_000;
    private static final long INITIAL_RETRY_DELAY_MS = 15_000L;
    private static final long MAX_RETRY_DELAY_MS = 300_000L;
    private static final ConcurrentMap<String, RetryState> RETRY_STATES = new ConcurrentHashMap<>();
    private static final Object LOCAL_SERVICE_LOCK = new Object();
    private static final long LOCAL_START_RETRY_DELAY_MS = 30_000L;
    private static Process managedLocalServiceProcess;
    private static String managedLocalServiceSignature = "";
    private static long nextLocalStartAttemptAtMs;
    private static boolean localServiceStartupLogged;

    private BubbleTranslationService() {}

    public static void reloadLocalServiceConfig(ModConfig config) {
        Path localDir = resolveLocalServiceDir(config);
        try {
            Files.createDirectories(localDir);
        } catch (IOException ex) {
            ModLogger.warn("[Translate] Failed to create local LibreTranslate dir '{}': {}", localDir, ex.getMessage());
        }

        synchronized (LOCAL_SERVICE_LOCK) {
            String newSignature = buildLocalServiceSignature(config, localDir);
            if (managedLocalServiceProcess != null && managedLocalServiceProcess.isAlive()
                    && !newSignature.equals(managedLocalServiceSignature)) {
                stopManagedLocalServiceLocked("local translation config changed");
            }
            managedLocalServiceSignature = newSignature;
            localServiceStartupLogged = false;
        }
    }

    public static void shutdownManagedLocalService() {
        synchronized (LOCAL_SERVICE_LOCK) {
            stopManagedLocalServiceLocked("client shutting down");
        }
    }

    public static void prepareTranslationBackend(ModConfig config) {
        if (config == null || !config.isTranslateReceivedBubbles()) {
            return;
        }
        maybeStartLocalService(config, resolveApiUrl(config));
    }

    public static CompletableFuture<String> translateReceivedBubble(ModConfig config,
                                                                    String text,
                                                                    String sourceLanguage,
                                                                    String targetLanguage) {
        String normalizedText = text == null ? "" : text.trim();
        String source = normalizeLanguage(sourceLanguage);
        String target = normalizeLanguage(targetLanguage);

        if (normalizedText.isBlank()) {
            return CompletableFuture.completedFuture(normalizedText);
        }
        if (config == null || !config.isTranslateReceivedBubbles()) {
            return CompletableFuture.completedFuture(normalizedText);
        }
        if (source.isBlank() || target.isBlank() || source.equals(target)) {
            return CompletableFuture.completedFuture(normalizedText);
        }

        String apiUrl = resolveApiUrl(config);
        maybeStartLocalService(config, apiUrl);
        String retryKey = buildRetryKey(apiUrl, source, target);
        long now = System.currentTimeMillis();
        RetryState retryState = RETRY_STATES.computeIfAbsent(retryKey, key -> new RetryState());
        if (!retryState.canAttempt(now)) {
            return CompletableFuture.completedFuture(normalizedText);
        }

        return CompletableFuture.supplyAsync(() -> {
            String translated = translateViaApi(
                    apiUrl,
                    config.getTranslationApiKey(),
                    normalizedText,
                    source,
                    target
            );
            retryState.recordSuccess();
            return translated;
        }).exceptionally(ex -> {
            long failureTime = System.currentTimeMillis();
            long retryAfter = retryState.recordFailure(failureTime);
            if (retryState.shouldLogFailure(failureTime)) {
                ModLogger.warn("[Translate] Bubble translation failed ({} -> {}). Next retry in {}s: {}",
                        source, target, retryAfter / 1000L, ex.getMessage());
            }
            return normalizedText;
        }).thenApply(translatedText -> {
            if (!normalizedText.equals(translatedText)) {
                RETRY_STATES.remove(retryKey);
            } else if (retryState.wasSuccessfulAttempt()) {
                RETRY_STATES.remove(retryKey);
            }
            return translatedText;
        });
    }

    private static String buildRetryKey(String apiUrl, String sourceLanguage, String targetLanguage) {
        return apiUrl + '|' + sourceLanguage + '|' + targetLanguage;
    }

    private static String resolveApiUrl(ModConfig config) {
        if (config == null) {
            return DEFAULT_LOCAL_API_URL;
        }
        String configuredUrl = config.getTranslationApiUrl();
        if (configuredUrl.isBlank()) {
            return DEFAULT_LOCAL_API_URL;
        }
        return configuredUrl;
    }

    private static void maybeStartLocalService(ModConfig config, String apiUrl) {
        if (config == null || !config.isTranslationAutoStartLocalService()) {
            return;
        }
        if (!DEFAULT_LOCAL_API_URL.equals(apiUrl)) {
            return;
        }
        if (isLocalServiceReachable(apiUrl)) {
            synchronized (LOCAL_SERVICE_LOCK) {
                nextLocalStartAttemptAtMs = 0L;
            }
            return;
        }

        long now = System.currentTimeMillis();
        synchronized (LOCAL_SERVICE_LOCK) {
            if (managedLocalServiceProcess != null && !managedLocalServiceProcess.isAlive()) {
                managedLocalServiceProcess = null;
            }
            if (managedLocalServiceProcess != null && managedLocalServiceProcess.isAlive()) {
                return;
            }
            if (now < nextLocalStartAttemptAtMs) {
                return;
            }

            Path localDir = resolveLocalServiceDir(config);
            Path starter = resolveLocalStarter(localDir, config.getTranslationLocalStartScript());
            if (!Files.exists(starter)) {
                nextLocalStartAttemptAtMs = now + LOCAL_START_RETRY_DELAY_MS;
                if (!localServiceStartupLogged) {
                    localServiceStartupLogged = true;
                    ModLogger.warn("[Translate] Local LibreTranslate starter not found at '{}'. Put your service there or disable translationAutoStartLocalService.",
                            starter);
                }
                return;
            }

            try {
                ProcessBuilder builder = buildLocalStarterProcess(starter);
                builder.directory(localDir.toFile());
                builder.redirectErrorStream(true);
                managedLocalServiceProcess = builder.start();
                managedLocalServiceSignature = buildLocalServiceSignature(config, localDir);
                nextLocalStartAttemptAtMs = now + LOCAL_START_RETRY_DELAY_MS;
                localServiceStartupLogged = true;
                consumeProcessOutput(managedLocalServiceProcess, starter.getFileName().toString());
                ModLogger.info("[Translate] Started local LibreTranslate helper from '{}'.", starter);
            } catch (IOException ex) {
                nextLocalStartAttemptAtMs = now + LOCAL_START_RETRY_DELAY_MS;
                if (!localServiceStartupLogged) {
                    localServiceStartupLogged = true;
                    ModLogger.warn("[Translate] Failed to start local LibreTranslate helper '{}': {}", starter, ex.getMessage());
                }
            }
        }
    }

    private static boolean isLocalServiceReachable(String apiUrl) {
        try {
            URI uri = URI.create(apiUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null || host.isBlank() || port < 0) {
                return false;
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 750);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private static ProcessBuilder buildLocalStarterProcess(Path starter) {
        String fileName = starter.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".ps1")) {
            return new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", starter.toString());
        }
        if (fileName.endsWith(".bat") || fileName.endsWith(".cmd")) {
            return new ProcessBuilder("cmd.exe", "/c", starter.toString());
        }
        return new ProcessBuilder(starter.toString());
    }

    private static Path resolveLocalServiceDir(ModConfig config) {
        String configuredDir = config != null ? config.getTranslationLocalDir() : "whatabubble/libretranslate";
        Path path = Path.of(configuredDir);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return FabricLoader.getInstance().getConfigDir().resolve(path).normalize();
    }

    private static Path resolveLocalStarter(Path localDir, String starterName) {
        if (starterName == null || starterName.isBlank()) {
            return localDir.resolve("start.bat").normalize();
        }
        Path starterPath = Path.of(starterName);
        Path resolved = starterPath.isAbsolute() ? starterPath.normalize() : localDir.resolve(starterPath).normalize();
        return resolved.startsWith(localDir.normalize()) || starterPath.isAbsolute() ? resolved : localDir.resolve("start.bat").normalize();
    }

    private static String buildLocalServiceSignature(ModConfig config, Path localDir) {
        if (config == null) {
            return localDir.toString();
        }
        return localDir + "|" + config.getTranslationLocalStartScript() + "|" + config.isTranslationAutoStartLocalService();
    }

    private static void consumeProcessOutput(Process process, String name) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ModLogger.info("[TranslateLocal:{}] {}", name, line);
                }
            } catch (IOException ignored) {
                // Process output reader ends naturally when the process exits.
            }
        }, "whatabubble-libretranslate-output");
        thread.setDaemon(true);
        thread.start();
    }

    private static void stopManagedLocalServiceLocked(String reason) {
        if (managedLocalServiceProcess == null) {
            return;
        }
        if (managedLocalServiceProcess.isAlive()) {
            ModLogger.info("[Translate] Stopping managed local LibreTranslate service ({})...", reason);
            managedLocalServiceProcess.destroy();
            try {
                managedLocalServiceProcess.waitFor();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            if (managedLocalServiceProcess.isAlive()) {
                managedLocalServiceProcess.destroyForcibly();
            }
        }
        managedLocalServiceProcess = null;
    }

    private static String translateViaApi(String apiUrl,
                                          String apiKey,
                                          String text,
                                          String sourceLanguage,
                                          String targetLanguage) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");

            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("q", text);
            requestJson.addProperty("source", sourceLanguage);
            requestJson.addProperty("target", targetLanguage);
            requestJson.addProperty("format", "text");
            if (apiKey != null && !apiKey.isBlank()) {
                requestJson.addProperty("api_key", apiKey);
            }

            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestJson.toString());
            }

            int responseCode = connection.getResponseCode();
            Reader reader = responseCode >= 200 && responseCode < 300
                    ? new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                    : new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8);

            try (reader) {
                JsonObject responseJson = JsonParser.parseReader(reader).getAsJsonObject();
                if (responseCode < 200 || responseCode >= 300) {
                    String error = responseJson.has("error") ? responseJson.get("error").getAsString() : ("HTTP " + responseCode);
                    throw new IOException(error);
                }
                if (!responseJson.has("translatedText")) {
                    throw new IOException("translatedText missing in translation response");
                }
                String translatedText = responseJson.get("translatedText").getAsString();
                ModLogger.info("[Translate] Bubble translated {} -> {}: \"{}\" -> \"{}\"",
                        sourceLanguage, targetLanguage, text, translatedText);
                return translatedText == null || translatedText.isBlank() ? text : translatedText.trim();
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String normalizeLanguage(String language) {
        if (language == null) {
            return "";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        int dash = normalized.indexOf('-');
        if (dash > 0) {
            normalized = normalized.substring(0, dash);
        }
        int underscore = normalized.indexOf('_');
        if (underscore > 0) {
            normalized = normalized.substring(0, underscore);
        }
        return normalized;
    }

    private static final class RetryState {
        private int consecutiveFailures;
        private long nextRetryAtMs;
        private long lastFailureLogAtMs;
        private boolean successfulAttemptSinceLastFailure = true;

        synchronized boolean canAttempt(long now) {
            return now >= nextRetryAtMs;
        }

        synchronized long recordFailure(long now) {
            consecutiveFailures++;
            successfulAttemptSinceLastFailure = false;
            long delay = INITIAL_RETRY_DELAY_MS;
            if (consecutiveFailures > 1) {
                delay = Math.min(MAX_RETRY_DELAY_MS, INITIAL_RETRY_DELAY_MS << Math.min(4, consecutiveFailures - 1));
            }
            nextRetryAtMs = now + delay;
            return delay;
        }

        synchronized boolean shouldLogFailure(long now) {
            if (lastFailureLogAtMs >= nextRetryAtMs - INITIAL_RETRY_DELAY_MS) {
                return false;
            }
            lastFailureLogAtMs = now;
            return true;
        }

        synchronized void recordSuccess() {
            consecutiveFailures = 0;
            nextRetryAtMs = 0L;
            lastFailureLogAtMs = 0L;
            successfulAttemptSinceLastFailure = true;
        }

        synchronized boolean wasSuccessfulAttempt() {
            return successfulAttemptSinceLastFailure;
        }
    }
}


