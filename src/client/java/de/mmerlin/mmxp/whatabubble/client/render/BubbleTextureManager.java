package de.mmerlin.mmxp.whatabubble.client.render;

import de.mmerlin.mmxp.whatabubble.config.ModConfig;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Loads the bubble atlas either from the bundled mod resource, a local file path,
 * or a remote URL, and exposes the active texture + dimensions to the renderer.
 */
public final class BubbleTextureManager {
    private static final Identifier DEFAULT_TEXTURE_ID = Identifier.of("whatabubble", "textures/gui/bubble_atlas.png");
    private static final Identifier CUSTOM_TEXTURE_ID = Identifier.of("whatabubble", "dynamic/bubble_atlas");
    private static final int FALLBACK_WIDTH = 6;
    private static final int FALLBACK_HEIGHT = 6;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024;

    private final AtomicInteger reloadGeneration = new AtomicInteger();

    private volatile Identifier activeTextureId = DEFAULT_TEXTURE_ID;
    private volatile int activeWidth = FALLBACK_WIDTH;
    private volatile int activeHeight = FALLBACK_HEIGHT;
    private volatile String activeSource = "";

    public void reloadFromConfig(ModConfig config) {
        reload(config == null ? "" : config.getBubbleTexture());
    }

    public void reload(String configuredSource) {
        String source = configuredSource == null ? "" : configuredSource.trim();
        int generation = reloadGeneration.incrementAndGet();

        if (source.isEmpty()) {
            applyDefaultTexture(generation);
            return;
        }

        CompletableFuture
                .supplyAsync(() -> loadCustomTexture(source))
                .whenComplete((result, throwable) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client == null) {
                        closeQuietly(result);
                        return;
                    }

                    client.execute(() -> {
                        if (generation != reloadGeneration.get()) {
                            closeQuietly(result);
                            return;
                        }

                        if (throwable != null || result == null) {
                            String message = throwable != null ? throwable.getMessage() : "unknown error";
                            ModLogger.warn("[BubbleRenderer] Failed to load custom bubble atlas '{}': {}. Using default atlas.", source, message);
                            applyDefaultTexture(generation);
                            return;
                        }

                        applyCustomTexture(result, source);
                    });
                });
    }

    public Identifier getActiveTextureId() {
        return activeTextureId;
    }

    public int getActiveWidth() {
        return activeWidth;
    }

    public int getActiveHeight() {
        return activeHeight;
    }

    public String getActiveSource() {
        return activeSource;
    }

    private void applyDefaultTexture(int generation) {
        if (generation != reloadGeneration.get()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            destroyCustomTexture(client);
            int[] size = readBundledAtlasSize(client);
            activeWidth = size[0];
            activeHeight = size[1];
        } else {
            activeWidth = FALLBACK_WIDTH;
            activeHeight = FALLBACK_HEIGHT;
        }

        activeTextureId = DEFAULT_TEXTURE_ID;
        activeSource = "";
        ModLogger.info("[BubbleRenderer] Using bundled bubble atlas ({}x{}).", activeWidth, activeHeight);
    }

    private void applyCustomTexture(LoadedTexture result, String source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            result.close();
            return;
        }

        try {
            destroyCustomTexture(client);
            client.getTextureManager().registerTexture(
                    CUSTOM_TEXTURE_ID,
                    new NativeImageBackedTexture(() -> "whatabubble_dynamic_bubble", result.image())
            );
            activeTextureId = CUSTOM_TEXTURE_ID;
            activeWidth = result.width();
            activeHeight = result.height();
            activeSource = source;
            ModLogger.info("[BubbleRenderer] Loaded custom bubble atlas '{}' ({}x{}).", source, activeWidth, activeHeight);
        } catch (RuntimeException ex) {
            result.close();
            ModLogger.warn("[BubbleRenderer] Failed to register custom bubble atlas '{}': {}. Using default atlas.", source, ex.getMessage());
            applyDefaultTexture(reloadGeneration.get());
        }
    }

    private static void destroyCustomTexture(MinecraftClient client) {
        try {
            client.getTextureManager().destroyTexture(CUSTOM_TEXTURE_ID);
        } catch (RuntimeException ignored) {
            // Texture may not exist yet.
        }
    }

    private static int[] readBundledAtlasSize(MinecraftClient client) {
        try {
            Optional<?> resource = client.getResourceManager().getResource(DEFAULT_TEXTURE_ID);
            if (resource.isEmpty()) {
                return new int[]{FALLBACK_WIDTH, FALLBACK_HEIGHT};
            }

            try (InputStream input = ((net.minecraft.resource.Resource) resource.get()).getInputStream();
                 NativeImage image = NativeImage.read(input)) {
                return new int[]{image.getWidth(), image.getHeight()};
            }
        } catch (IOException | RuntimeException ex) {
            ModLogger.warn("[BubbleRenderer] Failed to read bundled bubble atlas dimensions: {}", ex.getMessage());
            return new int[]{FALLBACK_WIDTH, FALLBACK_HEIGHT};
        }
    }

    private static LoadedTexture loadCustomTexture(String source) {
        try {
            if (isHttpUrl(source)) {
                return loadFromUrl(source);
            }
            return loadFromPath(source);
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private static LoadedTexture loadFromUrl(String source) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(source).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "WhatABubble/1.0");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            connection.disconnect();
            throw new IOException("HTTP " + responseCode);
        }

        try (InputStream input = connection.getInputStream()) {
            byte[] bytes = readWithLimit(input, MAX_DOWNLOAD_BYTES);
            try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes)) {
                NativeImage image = NativeImage.read(byteStream);
                if (image == null) {
                    throw new IOException("Image data could not be decoded");
                }
                return new LoadedTexture(image, image.getWidth(), image.getHeight());
            }
        } finally {
            connection.disconnect();
        }
    }

    private static LoadedTexture loadFromPath(String source) throws IOException {
        Path path = resolvePath(source);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }
        try (InputStream input = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(input);
            if (image == null) {
                throw new IOException("Image data could not be decoded");
            }
            return new LoadedTexture(image, image.getWidth(), image.getHeight());
        }
    }

    private static Path resolvePath(String source) {
        if (source.startsWith("file:")) {
            return Path.of(URI.create(source));
        }

        Path path = Path.of(source);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return FabricLoader.getInstance().getGameDir().resolve(path).normalize();
    }

    private static boolean isHttpUrl(String source) {
        String lower = source.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static byte[] readWithLimit(InputStream input, int maxBytes) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8_192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > maxBytes) {
                    throw new IOException("Image exceeds maximum size of " + maxBytes + " bytes");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static void closeQuietly(LoadedTexture result) {
        if (result != null) {
            result.close();
        }
    }

    private record LoadedTexture(NativeImage image, int width, int height) implements AutoCloseable {
        @Override
        public void close() {
            image.close();
        }
    }
}

