package de.mmerlin.mmxp.whatabubble.speech.dummy;

import de.mmerlin.mmxp.whatabubble.speech.SpeechResult;
import de.mmerlin.mmxp.whatabubble.speech.SpeechService;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Fallback implementation that emits pre-defined test phrases.
 * Used when no Vosk model is available.
 */
public class DummySpeechService implements SpeechService {

    private static final List<String> PHRASES = List.of(
            "Hallo Welt!", "Das ist ein Test.", "Sprechblase aktiv.",
            "Hello World!", "Speech bubble test.", "Testing microphone."
    );

    private volatile boolean running = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "whatabubble-dummy-stt"); t.setDaemon(true); return t; }
    );
    private ScheduledFuture<?> scheduledTask;
    private int phraseIndex = 0;

    @Override
    public void setReadyCallback(Runnable readyCallback) { }

    @Override
    public void start(Consumer<SpeechResult> resultCallback) {
        if (running) return;
        running = true;
        ModLogger.warn("DummySpeechService active — no Vosk model found. Place model at config/whatabubble/vosk-model/");
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            String text = PHRASES.get(phraseIndex % PHRASES.size());
            phraseIndex++;
            resultCallback.accept(new SpeechResult(text, 1.0f, true));
        }, 4, 7, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        running = false;
        if (scheduledTask != null) scheduledTask.cancel(false);
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public void setLanguage(String languageCode) { /* no-op for dummy */ }
}


