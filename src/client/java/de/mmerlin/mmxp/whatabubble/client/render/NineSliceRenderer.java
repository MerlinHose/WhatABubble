package de.mmerlin.mmxp.whatabubble.client.render;

import de.mmerlin.mmxp.whatabubble.client.WhatABubbleClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renders a nine-patch (9-slice) background rectangle using a single 3×3 atlas texture.
 *
 * <p>The renderer samples the currently active resource directly, so resource-pack overrides
 * may use any resolution/aspect ratio as long as the atlas is still laid out as a 3×3 grid.
 * This is important for custom bubble atlases whose X/Y proportions are no longer square.
 *
 * <p>Atlas layout:
 * <pre>
 *  [TL (transparent)] [T  (white)] [TR (white)]
 *  [L  (white)      ] [C  (blue) ] [R  (blue) ]
 *  [BL (white)      ] [B  (blue) ] [BR (blue) ]
 * </pre>
 *
 * <p>Using one atlas instead of nine separate textures reduces the number of RenderLayer
 * objects from 9 to 1 per bubble – this prevents repeated Dynamic Transforms UBO resizes
 * and drops draw-call overhead by 9×.
 */
public final class NineSliceRenderer {

    private static final int GRID_SIZE = 3;

    /** Small UV inset in physical texture pixels to prevent sampling from neighboring cells. */
    private static final float UV_INSET_PIXELS = 0.5f;

    /** Built-in fallback atlas texture. */
    private static final Identifier DEFAULT_TEX_ATLAS =
            Identifier.of("whatabubble", "textures/gui/bubble_atlas.png");

    /**
     * Pre-cached render layer for the atlas.
     * Uses the text_see_through shader (no Dynamic Transforms UBO required),
     * so the 9-slice renders correctly in any rendering context.
     */
    private static RenderLayer atlasLayer;
    private static Identifier atlasLayerTextureId;

    /** Returns the cached RenderLayer, creating it on first use. */
    private static RenderLayer layer() {
        Identifier activeTexture = textureId();
        if (atlasLayer == null || !activeTexture.equals(atlasLayerTextureId)) {
            atlasLayer = RenderLayers.textSeeThrough(activeTexture);
            atlasLayerTextureId = activeTexture;
        }
        return atlasLayer;
    }

    private static AtlasMetrics metrics() {
        WhatABubbleClient client = WhatABubbleClient.getInstance();
        if (client == null || client.getBubbleTextureManager() == null) {
            return AtlasMetrics.fallback();
        }
        return AtlasMetrics.of(
                client.getBubbleTextureManager().getActiveWidth(),
                client.getBubbleTextureManager().getActiveHeight()
        );
    }

    private static Identifier textureId() {
        WhatABubbleClient client = WhatABubbleClient.getInstance();
        if (client == null || client.getBubbleTextureManager() == null) {
            return DEFAULT_TEX_ATLAS;
        }
        return client.getBubbleTextureManager().getActiveTextureId();
    }

    private NineSliceRenderer() {}

    /**
     * Draws the 9-slice background.
     *
     * @param matrices    MatrixStack in text-pixel space (after BillboardHelper + per-bubble translate)
     * @param consumers   VertexConsumerProvider from the world render context
     * @param x0          left edge (pixel coords)
     * @param y0          top edge (pixel coords)
     * @param x1          right edge (pixel coords)
     * @param y1          bottom edge (pixel coords)
     * @param alpha       opacity 0–255
     * @param packedLight packed lightmap value (e.g. {@code LightmapTextureManager.MAX_LIGHT_COORDINATE})
     */
    public static void draw(MatrixStack matrices, VertexConsumerProvider consumers,
                            float x0, float y0, float x1, float y1,
                            float leftBorder, float topBorder, float rightBorder, float bottomBorder,
                            int rgb, int alpha, int packedLight) {
        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer vc = consumers.getBuffer(layer());
        AtlasMetrics metrics = metrics();

        float u0 = uvMinX(0, metrics);
        float u1 = uvMaxX(0, metrics);
        float u2 = uvMinX(1, metrics);
        float u3 = uvMaxX(1, metrics);
        float u4 = uvMinX(2, metrics);
        float u5 = uvMaxX(2, metrics);

        float v0 = uvMinY(0, metrics);
        float v1 = uvMaxY(0, metrics);
        float v2 = uvMinY(1, metrics);
        float v3 = uvMaxY(1, metrics);
        float v4 = uvMinY(2, metrics);
        float v5 = uvMaxY(2, metrics);

        float safeLeftBorder = Math.max(0.0f, leftBorder);
        float safeTopBorder = Math.max(0.0f, topBorder);
        float safeRightBorder = Math.max(0.0f, rightBorder);
        float safeBottomBorder = Math.max(0.0f, bottomBorder);

        float xl = x0 + safeLeftBorder;    // left inner edge
        float xr = x1 - safeRightBorder;   // right inner edge
        float yt = y0 + safeTopBorder;     // top inner edge
        float yb = y1 - safeBottomBorder;  // bottom inner edge

        if (xl > xr) {
            float midX = (x0 + x1) * 0.5f;
            xl = midX;
            xr = midX;
        }
        if (yt > yb) {
            float midY = (y0 + y1) * 0.5f;
            yt = midY;
            yb = midY;
        }

        // Top row
        quad(vc, entry, x0, y0, xl, yt,  u0, v0, u1, v1,  rgb, alpha, packedLight); // TL
        quad(vc, entry, xl, y0, xr, yt,  u2, v0, u3, v1,  rgb, alpha, packedLight); // T
        quad(vc, entry, xr, y0, x1, yt,  u4, v0, u5, v1,  rgb, alpha, packedLight); // TR
        // Middle row
        quad(vc, entry, x0, yt, xl, yb,  u0, v2, u1, v3,  rgb, alpha, packedLight); // L
        quad(vc, entry, xl, yt, xr, yb,  u2, v2, u3, v3,  rgb, alpha, packedLight); // C
        quad(vc, entry, xr, yt, x1, yb,  u4, v2, u5, v3,  rgb, alpha, packedLight); // R
        // Bottom row
        quad(vc, entry, x0, yb, xl, y1,  u0, v4, u1, v5,  rgb, alpha, packedLight); // BL
        quad(vc, entry, xl, yb, xr, y1,  u2, v4, u3, v5,  rgb, alpha, packedLight); // B
        quad(vc, entry, xr, yb, x1, y1,  u4, v4, u5, v5,  rgb, alpha, packedLight); // BR
    }

    private static float uvMinX(int cellIndex, AtlasMetrics metrics) {
        return cellIndex / (float) GRID_SIZE + metrics.uInset();
    }

    private static float uvMaxX(int cellIndex, AtlasMetrics metrics) {
        return (cellIndex + 1) / (float) GRID_SIZE - metrics.uInset();
    }

    private static float uvMinY(int cellIndex, AtlasMetrics metrics) {
        return cellIndex / (float) GRID_SIZE + metrics.vInset();
    }

    private static float uvMaxY(int cellIndex, AtlasMetrics metrics) {
        return (cellIndex + 1) / (float) GRID_SIZE - metrics.vInset();
    }

    // ── internals ────────────────────────────────────────────────────────────

    private static void quad(VertexConsumer vc, MatrixStack.Entry entry,
                             float x0, float y0, float x1, float y1,
                             float u0, float v0, float u1, float v1,
                             int rgb, int alpha, int packedLight) {
        vtx(vc, entry, x0, y0, u0, v0, rgb, alpha, packedLight);
        vtx(vc, entry, x0, y1, u0, v1, rgb, alpha, packedLight);
        vtx(vc, entry, x1, y1, u1, v1, rgb, alpha, packedLight);
        vtx(vc, entry, x1, y0, u1, v0, rgb, alpha, packedLight);
    }

    private static void vtx(VertexConsumer vc, MatrixStack.Entry entry,
                            float x, float y, float u, float v,
                            int rgb, int alpha, int packedLight) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        vc.vertex(entry, x, y, 0f)
          .color(red, green, blue, alpha)
          .texture(u, v)
          .light(packedLight);
        // Note: no .overlay() / .normal() – text_see_through uses
        // POSITION_COLOR_TEXTURE_LIGHT vertex format only.
    }

    private record AtlasMetrics(float width, float height) {
        static AtlasMetrics of(int width, int height) {
            if (width <= 0 || height <= 0) {
                return fallback();
            }
            return new AtlasMetrics(width, height);
        }

        static AtlasMetrics fallback() {
            return new AtlasMetrics(6f, 6f);
        }

        float uInset() {
            return UV_INSET_PIXELS / width;
        }

        float vInset() {
            return UV_INSET_PIXELS / height;
        }
    }
}

