package de.mmerlin.mmxp.whatabubble.client.render;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
public class BillboardHelper {
    private BillboardHelper() { }
    /**
     * Translates to a world position (camera-relative) and applies billboard rotation.
     *
     * @param matrices        current MatrixStack (origin = camera position in AFTER_ENTITIES)
     * @param worldPos        absolute world position to render at
     * @param cameraPos       absolute camera position in world
     * @param cameraRotation  camera orientation quaternion from Camera.getRotation()
     * @param scale           uniform scale using vanilla nametag orientation
     */
    public static void applyBillboard(MatrixStack matrices,
                                      Vec3d worldPos,
                                      Vec3d cameraPos,
                                      Quaternionf cameraRotation,
                                      float scale) {
        matrices.translate(
                worldPos.x - cameraPos.x,
                worldPos.y - cameraPos.y,
                worldPos.z - cameraPos.z
        );
        matrices.multiply(cameraRotation);
        matrices.scale(scale, -scale, scale);
    }
}