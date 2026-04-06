package de.mmerlin.mmxp.whatabubble.client.event;

import de.mmerlin.mmxp.whatabubble.client.render.CameraStateHolder;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

/**
 * Captures the CameraRenderState before entity rendering begins,
 * so SpeechBubbleFeatureRenderer (which runs inside the entity pipeline)
 * can access it via CameraStateHolder.
 */
public class RenderEventHandler {

    public void register() {
        ModLogger.info("[RenderEventHandler] Registering BEFORE_ENTITIES to capture CameraRenderState...");
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (ctx.worldState() != null) {
                CameraStateHolder.set(ctx.worldState().cameraRenderState);
            }
        });
        ModLogger.info("[RenderEventHandler] CameraRenderState capture registered.");
    }
}