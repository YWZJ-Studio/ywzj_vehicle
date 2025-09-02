package org.ywzj.vehicle.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.OBB;

import java.util.List;

/**
 * Codes based on @AnECanSaiTin's <a href="https://github.com/AnECanSaiTin/HitboxAPI">HitboxAPI</a>
 **/
public class OBBRenderer {

    public static final OBBRenderer INSTANCE = new OBBRenderer();

    public void render(Vec3 position, List<OBB> obbList, PoseStack poseStack, VertexConsumer buffer, float red, float green, float blue, float alpha, float pPartialTicks) {
        for (OBB obb : obbList) {
            Vector3f center = obb.center();
            Vector3f halfExtents = obb.extents();
            Quaternionf rotation = obb.rotation();
            renderOBB(
                    poseStack, buffer,
                    center.x() - position.x(), center.y() - position.y(), center.z() - position.z(),
                    rotation, halfExtents.x(), halfExtents.y(), halfExtents.z(),
                    red, green, blue, alpha
            );
        }
    }

    public static void renderOBB(PoseStack poseStack, VertexConsumer buffer, double centerX, double centerY, double centerZ, Quaternionf rotation, double halfX, double halfY, double halfZ, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ);
        poseStack.mulPose(rotation);
        LevelRenderer.renderLineBox(poseStack, buffer, -halfX, -halfY, -halfZ, halfX, halfY, halfZ, red, green, blue, alpha);
        poseStack.popPose();
    }

}
