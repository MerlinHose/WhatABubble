package de.mmerlin.mmxp.whatabubble.speech;

import java.util.function.Consumer;

public interface SpeechService {

    default void setReadyCallback(Runnable readyCallback) { }

    void start(Consumer<SpeechResult> resultCallback);

    void stop();

    boolean isRunning();

    void setLanguage(String languageCode);
}

