package de.mmerlin.mmxp.whatabubble.client.render;
public class RenderUtils {
    private RenderUtils() { }
    /** Combines an RGB color with an alpha value into a packed ARGB int. */
    public static int withAlpha(int rgb, int alpha) {
        return (rgb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}