package de.mmerlin.mmxp.whatabubble.client.render;

import net.minecraft.client.render.state.CameraRenderState;

/**
 * Holds the latest CameraRenderState captured just before entity rendering.
 * Used by SpeechBubbleFeatureRenderer which runs inside the entity pipeline
 * and needs camera information for submitLabel().
 */
public final class CameraStateHolder {
    private static volatile CameraRenderState state;

    private CameraStateHolder() {}

    public static void set(CameraRenderState s) { state = s; }

    public static CameraRenderState get() { return state; }
}

