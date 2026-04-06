package de.mmerlin.mmxp.whatabubble.client.render;

import de.mmerlin.mmxp.whatabubble.bubble.BubbleStack;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubble;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubbleManager;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Renders speech bubbles above players' heads exactly like vanilla nametags.
 * Called inside the entity render pipeline — visible for all players and
 * for yourself in third-person (F5).
 */
@SuppressWarnings("rawtypes")
public class SpeechBubbleFeatureRenderer
        extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    /** How many blocks above the nametag the first (newest) bubble appears. */
    private static final float HEAD_OFFSET    = 0.3f;
    /** Vertical gap (in world units) between stacked bubbles. */
    private static final float BUBBLE_SPACING = 0.28f;
    /** Fraction of lifetime at which the fade-out starts. */
    private static final float FADE_START     = 0.75f;

    private final SpeechBubbleManager bubbleManager;

    @SuppressWarnings("unchecked")
    public SpeechBubbleFeatureRenderer(
            FeatureRendererContext context,
            SpeechBubbleManager bubbleManager) {
        super(context);
        this.bubbleManager = bubbleManager;
    }

    @Override
    public void render(MatrixStack matrices,
                       OrderedRenderCommandQueue renderQueue,
                       int light,
                       PlayerEntityRenderState state,
                       float limbAngle,
                       float limbDistance) {

        // Resolve UUID from the entity ID stored in the render state
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Entity entity = client.world.getEntityById(state.id);
        if (entity == null) {
            ModLogger.warn("[SpeechBubbleFR] Could not find entity for id={}", state.id);
            return;
        }

        BubbleStack stack = bubbleManager.getStack(entity.getUuid());
        if (stack == null || stack.isEmpty()) return;

        // nameLabelPos is null when the vanilla nametag is not rendered
        // (e.g. the local player in first-person, invisible players, etc.).
        // In that case we compute the position ourselves from world coordinates.
        Vec3d labelBase = state.nameLabelPos;
        if (labelBase == null) {
            labelBase = new Vec3d(entity.getX(), entity.getY() + state.height + 0.5, entity.getZ());
            ModLogger.info("[SpeechBubbleFR] nameLabelPos null for id={} – using fallback {}", state.id, labelBase);
        }

        // Camera state was captured just before entity rendering
        CameraRenderState camState = CameraStateHolder.get();
        if (camState == null) {
            ModLogger.warn("[SpeechBubbleFR] CameraRenderState not available – skipping render for id={}", state.id);
            return;
        }

        List<SpeechBubble> bubbles = stack.getAll();

        for (int i = 0; i < bubbles.size(); i++) {
            SpeechBubble bubble = bubbles.get(i);

            // Fade alpha near end of lifetime
            float ratio = bubble.getAgeRatio();
            float alpha = ratio < FADE_START
                    ? 1.0f
                    : 1.0f - (ratio - FADE_START) / (1.0f - FADE_START);
            int bgAlpha = Math.max(4, (int) (alpha * 0x60));
            int bgColor  = bgAlpha << 24; // dark background, no RGB

            // Bubble 0 = closest to nametag (newest), each older bubble is higher
            Vec3d bubblePos = labelBase.add(0.0, HEAD_OFFSET + i * BUBBLE_SPACING, 0.0);

            ModLogger.info("[SpeechBubbleFR]   bubble[{}] \"{}\" at {}", i, bubble.getText(), bubblePos);

            renderQueue.submitLabel(
                    matrices,
                    bubblePos,
                    light,
                    Text.literal(bubble.getText()),
                    state.sneaking,
                    bgColor,
                    state.squaredDistanceToCamera,
                    camState
            );
        }
    }
}
