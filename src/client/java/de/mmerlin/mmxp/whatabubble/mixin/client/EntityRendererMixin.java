package de.mmerlin.mmxp.whatabubble.mixin.client;

import de.mmerlin.mmxp.whatabubble.bubble.BubbleStack;
import de.mmerlin.mmxp.whatabubble.bubble.SpeechBubble;
import de.mmerlin.mmxp.whatabubble.client.WhatABubbleClient;
import de.mmerlin.mmxp.whatabubble.config.BubbleVisibility;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Renders speech bubbles for all visible players (other players + self in F5)
 * by injecting into {@code renderLabelIfPresent} which provides the correct
 * {@link OrderedRenderCommandQueue} for MC 1.21.4+ label rendering.
 *
 * <p>The local player in first-person view is NOT handled here (entity is not
 * rendered); it is handled separately via {@code WorldRenderEvents.BEFORE_ENTITIES}.
 *
 * <p>Important: {@code state.squaredDistanceToCamera} is 0 for the local player
 * (vanilla skips their own nametag). We compute the actual camera-to-label
 * distance instead to prevent {@code submitLabel} from silently discarding it.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class EntityRendererMixin {

    @Unique private static final float HEAD_OFFSET    = 0.3f;
    @Unique private static final float BUBBLE_SPACING = 0.28f;
    @Unique private static final float FADE_START     = 0.75f;

    @Inject(
        method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At("RETURN")
    )
    private void whatabubble_onRenderLabelIfPresent(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue renderQueue,
            CameraRenderState camState,
            CallbackInfo ci) {

        WhatABubbleClient modClient = WhatABubbleClient.getInstance();
        if (modClient == null) return;

        BubbleVisibility vis = modClient.getConfigManager().getConfig().getBubbleVisibility();

        // NineSlice mode → BubbleRenderer handles rendering, skip submitLabel.
        if (modClient.getConfigManager().getConfig().isUseNineSlice()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Entity entity = client.world.getEntityById(state.id);
        if (entity == null) return;

        boolean isSelf = client.player != null && entity.getUuid().equals(client.player.getUuid());
        boolean debugMode = modClient.getConfigManager().getConfig().isDebugMode();

        // Sichtbarkeitsfilter: Debug-Modus umgeht die Einschränkungen für den lokalen Spieler,
        // damit die Test-Bubble (aus dem BubbleManager) immer sichtbar ist.
        if (vis == BubbleVisibility.NONE && !(debugMode && isSelf)) return;
        if (vis == BubbleVisibility.OTHERS_ONLY && isSelf && !debugMode) return;

        BubbleStack stack = modClient.getBubbleManager().getStack(entity.getUuid());
        boolean hasStack = stack != null && !stack.isEmpty();
        if (!hasStack) return;

        Vec3d labelBase = state.nameLabelPos;
        if (labelBase == null) {
            labelBase = new Vec3d(entity.getX(), entity.getY() + state.height + 0.5, entity.getZ());
        }

        List<SpeechBubble> bubbles = stack.getAll();

        // For the local player, state.squaredDistanceToCamera is 0 because vanilla
        // skips their own nametag. submitLabel silently discards distance=0 entries.
        // Compute the real camera-to-label distance to avoid this.
        double distSq = state.squaredDistanceToCamera;
        if (distSq == 0) {
            Vec3d firstBubblePos = labelBase.add(0.0, HEAD_OFFSET, 0.0);
            distSq = firstBubblePos.squaredDistanceTo(camState.pos);
            ModLogger.info("[Mixin] Local player bubble: squaredDistanceToCamera was 0, computed distSq={}", distSq);
        }


        for (int i = 0; i < bubbles.size(); i++) {
            SpeechBubble bubble = bubbles.get(i);
            float ratio = bubble.getAgeRatio();
            float alpha = ratio < FADE_START
                    ? 1.0f
                    : 1.0f - (ratio - FADE_START) / (1.0f - FADE_START);
            int bgAlpha = Math.max(4, (int) (alpha * 0x60));
            int bgColor  = bgAlpha << 24;

            Vec3d bubblePos = labelBase.add(0.0, HEAD_OFFSET + i * BUBBLE_SPACING, 0.0);

            renderQueue.submitLabel(
                    matrices,
                    bubblePos,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    Text.literal(bubble.getText()),
                    state.sneaking,
                    bgColor,
                    distSq,
                    camState
            );
        }
    }
}
