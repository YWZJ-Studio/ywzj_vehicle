package org.ywzj.vehicle.client.render.entity.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.misc.Rope;

public class RopeRenderer extends EntityRenderer<Rope> {

    private static final ResourceLocation ROPE = YwzjVehicle.modLocation("textures/entity/rope.png");
    private static final float ROPE_WIDTH = 0.06f;

    public RopeRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public boolean shouldRender(Rope pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return true;
    }

    @Override
    public void render(Rope rope, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        if (rope.ropeNodes.size() < 2) {
            return;
        }
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(ROPE));
        double renderX = Mth.lerp(pPartialTick, rope.xo, rope.getX());
        double renderY = Mth.lerp(pPartialTick, rope.yo, rope.getY());
        double renderZ = Mth.lerp(pPartialTick, rope.zo, rope.getZ());
        pPoseStack.pushPose();
        for (int i = 0; i < rope.ropeNodes.size() - 1; i++) {
            Rope.RopeNode n1 = rope.ropeNodes.get(i);
            Rope.RopeNode n2 = rope.ropeNodes.get(i + 1);
            float x1 = (float) (Mth.lerp(pPartialTick, n1.lastPos.x, n1.pos.x) - renderX);
            float y1 = (float) (Mth.lerp(pPartialTick, n1.lastPos.y, n1.pos.y) - renderY);
            float z1 = (float) (Mth.lerp(pPartialTick, n1.lastPos.z, n1.pos.z) - renderZ);
            float x2 = (float) (Mth.lerp(pPartialTick, n2.lastPos.x, n2.pos.x) - renderX);
            float y2 = (float) (Mth.lerp(pPartialTick, n2.lastPos.y, n2.pos.y) - renderY);
            float z2 = (float) (Mth.lerp(pPartialTick, n2.lastPos.z, n2.pos.z) - renderZ);
            renderSegment(pPoseStack, builder, x1, y1, z1, x2, y2, z2, pPackedLight);
        }
        pPoseStack.popPose();
        super.render(rope, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
    }

    private void renderSegment(PoseStack poseStack, VertexConsumer builder, float x1, float y1, float z1, float x2, float y2, float z2, int packedLight) {
        // 1. 获取相机的旋转信息
        // 这代表了玩家眼睛的“右”方向
        Quaternionf cameraRotation = this.entityRenderDispatcher.camera.rotation();
        Vector3f lookRight = new Vector3f(1.0F, 0.0F, 0.0F);
        lookRight.rotate(cameraRotation); // 将局部右向量转到世界/视图空间

        // 2. 设定绳子的半宽
        float halfWidth = ROPE_WIDTH * 0.5f;

        // 3. 计算垂直于视线的偏移向量
        // dx, dy, dz 是相对于原点的偏移量
        float dx = lookRight.x() * halfWidth;
        float dy = lookRight.y() * halfWidth;
        float dz = lookRight.z() * halfWidth;

        Matrix4f matrix = poseStack.last().pose();

        // 4. 绘制顶点 (像火一样，始终基于相机平面的宽度偏移)
        // 节点 1 的左右两点
        addVertex(builder, matrix, x1 - dx, y1 - dy, z1 - dz, 0, 1, packedLight);
        addVertex(builder, matrix, x1 + dx, y1 + dy, z1 + dz, 1, 1, packedLight);
        // 节点 2 的左右两点
        addVertex(builder, matrix, x2 + dx, y2 + dy, z2 + dz, 1, 0, packedLight);
        addVertex(builder, matrix, x2 - dx, y2 - dy, z2 - dz, 0, 0, packedLight);
    }

    private void addVertex(VertexConsumer builder, Matrix4f matrix, float x, float y, float z, float u, float v, int light) {
        builder.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(Rope pEntity) {
        return ROPE;
    }

}
