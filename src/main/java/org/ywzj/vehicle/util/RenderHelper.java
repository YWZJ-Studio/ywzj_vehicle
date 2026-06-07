package org.ywzj.vehicle.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.ywzj.vehicle.YwzjVehicle;

public class RenderHelper {

    public static void drawCenteredString(GuiGraphics guiGraphics, Font pFont, String pText, int pX, int pY, int pColor) {
        guiGraphics.drawString(pFont, pText, pX - pFont.width(pText) / 2, pY, pColor, false);
    }

    public static void drawCircle(GuiGraphics guiGraphics, int cx, int cy, int radius, int color) {
        float a = ((color >> 24) & 255) / 255.0F;
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        PoseStack poseStack = guiGraphics.pose();
        for (int i = 0; i <= 64; i++) {
            double ang = Math.PI * 2 * i / 64;
            float cos = (float) Math.cos(ang);
            float sin = (float) Math.sin(ang);
            // 外圈
            buf.vertex(poseStack.last().pose(),
                            cx + cos * radius,
                            cy + sin * radius,
                            0)
                    .color(r, g, b, a)
                    .endVertex();
            // 内圈
            buf.vertex(poseStack.last().pose(),
                            cx + cos * (radius - 0.5f),
                            cy + sin * (radius - 0.5f),
                            0)
                    .color(r, g, b, a)
                    .endVertex();
        }
        tesselator.end();
        RenderSystem.disableBlend();
    }

    public static void drawCross(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int half = size / 2;
        // 横线
        guiGraphics.hLine(x - half + 1, x + half - 1, y, color);
        // 竖线
        guiGraphics.vLine(x, y - half, y + half, color);
    }

    public static void drawCrossHollow(GuiGraphics guiGraphics, int x, int y, int size, int gap, int color) {
        int half = size / 2;
        int gapHalf = gap / 2;
        guiGraphics.hLine(x - half + 1, x - gapHalf - 1, y, color);
        guiGraphics.hLine(x + gapHalf + 1, x + half - 1, y, color);
        guiGraphics.vLine(x, y - half, y - gapHalf, color);
        guiGraphics.vLine(x, y + gapHalf, y + half, color);
    }

    public static void drawSquare(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int half = size / 2;
        int left = x - half;
        int right = x + half;
        int top = y - half;
        int bottom = y + half;
        // 上下边
        guiGraphics.hLine(left, right, top, color);
        guiGraphics.hLine(left, right, bottom, color);
        // 左右边
        guiGraphics.vLine(left, top, bottom, color);
        guiGraphics.vLine(right, top, bottom, color);
    }

    public static void drawSquareCorners(GuiGraphics guiGraphics, int x, int y, int size, int corner, int color) {
        int half = size / 2;
        int left = x - half;
        int right = x + half;
        int top = y - half;
        int bottom = y + half;

        // 左上
        guiGraphics.hLine(left, left + corner, top, color);
        guiGraphics.vLine(left, top, top + corner, color);

        // 右上
        guiGraphics.hLine(right - corner, right, top, color);
        guiGraphics.vLine(right, top, top + corner, color);

        // 左下
        guiGraphics.hLine(left, left + corner, bottom, color);
        guiGraphics.vLine(left, bottom - corner, bottom, color);

        // 右下
        guiGraphics.hLine(right - corner, right, bottom, color);
        guiGraphics.vLine(right, bottom - corner, bottom, color);
    }

    public static void drawRectByCorner(GuiGraphics guiGraphics, int left, int right, int top, int bottom, int color, float scale) {
        int cx = (left + right) / 2;
        int cy = (top + bottom) / 2;
        int halfW = Math.round((right - left) * 0.5f * scale);
        int halfH = Math.round((bottom - top) * 0.5f * scale);
        left = cx - halfW;
        right = cx + halfW;
        top = cy - halfH;
        bottom = cy + halfH;
        // 上下
        guiGraphics.hLine(left, right, top, color);
        guiGraphics.hLine(left, right, bottom, color);
        // 左右
        guiGraphics.vLine(left, top, bottom, color);
        guiGraphics.vLine(right, top, bottom, color);
    }

    public static void drawRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int color, float scale) {
        int halfW = Math.round(width * 0.5f * scale);
        int halfH = Math.round(height * 0.5f * scale);
        int left = x - halfW;
        int right = x + halfW;
        int top = y - halfH;
        int bottom = y + halfH;
        // 上下边
        guiGraphics.hLine(left, right, top, color);
        guiGraphics.hLine(left, right, bottom, color);
        // 左右边
        guiGraphics.vLine(left, top, bottom, color);
        guiGraphics.vLine(right, top, bottom, color);
    }

    public static void drawCrossDiagonal(GuiGraphics guiGraphics, int x, int y, int size, int thickness, int color) {
        int gap = 3;
        int len = size / 2;
        PoseStack poseStack = guiGraphics.pose();
        // 中心点
        poseStack.pushPose();
        {
            poseStack.translate(x, y, 0);
            guiGraphics.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
        poseStack.popPose();
        // \ 方向 (45°)
        poseStack.pushPose();
        {
            poseStack.translate(x, y - 0.5, 0);
            poseStack.rotateAround(Axis.ZP.rotationDegrees(45), 0, 0.5f, 0);
            guiGraphics.fill(gap, -thickness / 2, gap + len, thickness - thickness / 2, color);
            guiGraphics.fill(-gap - len, -thickness / 2, -gap, thickness - thickness / 2, color);
        }
        poseStack.popPose();
        // / 方向 (-45°)
        poseStack.pushPose();
        {
            poseStack.translate(x, y - 0.5, 0);
            poseStack.rotateAround(Axis.ZP.rotationDegrees(-45), 0, 0.5f, 0);
            guiGraphics.fill(gap, -thickness / 2, gap + len, thickness - thickness / 2, color);
            guiGraphics.fill(-gap - len, -thickness / 2, -gap, thickness - thickness / 2, color);
        }
        poseStack.popPose();
    }

    public static void drawLine(PoseStack poseStack, Vec3 v, float thickness, int color, float dashLength, float gapLength) {
        float vx = (float) v.x;
        float vy = (float) v.z;
        float totalLen = (float) Math.sqrt(vx * vx + vy * vy);
        if (totalLen < 0.001f) {
            return;
        }
        if (dashLength < 0) {
            dashLength = totalLen;
            gapLength = totalLen;
        }

        float dx = vx / totalLen;
        float dy = vy / totalLen;
        float nx = -dy * (thickness / 2f);
        float ny = dx * (thickness / 2f);

        Matrix4f mat = poseStack.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float a = (color >> 24 & 255) / 255f;
        float rc = (color >> 16 & 255) / 255f;
        float gc = (color >> 8 & 255) / 255f;
        float bc = (color & 255) / 255f;

        for (float cur = 0; cur < totalLen; cur += dashLength + gapLength) {
            float end = Math.min(cur + dashLength, totalLen);

            float xStart = dx * cur;
            float yStart = dy * cur;
            float xEnd = dx * end;
            float yEnd = dy * end;

            bb.vertex(mat, xStart - nx, yStart - ny, 0).color(rc, gc, bc, a).endVertex();
            bb.vertex(mat, xStart + nx, yStart + ny, 0).color(rc, gc, bc, a).endVertex();
            bb.vertex(mat, xEnd + nx, yEnd + ny, 0).color(rc, gc, bc, a).endVertex();
            bb.vertex(mat, xEnd - nx, yEnd - ny, 0).color(rc, gc, bc, a).endVertex();
        }

        Tesselator.getInstance().end();
    }

    /**
     * Fills a rectangle with the specified color and z-level using the given render type and coordinates as the
     * boundaries.
     *
     * @param pRenderType the render type to use.
     * @param pMinX       the minimum x-coordinate of the rectangle.
     * @param pMinY       the minimum y-coordinate of the rectangle.
     * @param pMaxX       the maximum x-coordinate of the rectangle.
     * @param pMaxY       the maximum y-coordinate of the rectangle.
     * @param pZ          the z-level of the rectangle.
     * @param pColor      the color to fill the rectangle with.
     */
    public static void fill(GuiGraphics guiGraphics, RenderType pRenderType, float pMinX, float pMinY, float pMaxX, float pMaxY, float pZ, int pColor) {
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        if (pMinX < pMaxX) {
            float i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            float j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        float f3 = (float) FastColor.ARGB32.alpha(pColor) / 255.0F;
        float f = (float) FastColor.ARGB32.red(pColor) / 255.0F;
        float f1 = (float) FastColor.ARGB32.green(pColor) / 255.0F;
        float f2 = (float) FastColor.ARGB32.blue(pColor) / 255.0F;
        VertexConsumer vertexconsumer = guiGraphics.bufferSource().getBuffer(pRenderType);
        vertexconsumer.vertex(matrix4f, pMinX, pMinY, pZ).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, pMinX, pMaxY, pZ).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, pMaxX, pMaxY, pZ).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, pMaxX, pMinY, pZ).color(f, f1, f2, f3).endVertex();
    }

    public static void renderArrow3D(PoseStack poseStack, MultiBufferSource bufferSource, float w, float h, int r, int g, int b, int a) {
        poseStack.pushPose();
        {
            long time = System.currentTimeMillis();
            float bounce = (float) Math.sin((time % 1000L) / 1000.0f * Math.PI * 2) * 0.1f;
            float hoverHeight = 0.8f;
            poseStack.translate(0, hoverHeight + bounce, 0);

            float spin = (time % 2000L) / 2000.0f * 360.0f;
            poseStack.rotateAround(Axis.YP.rotationDegrees(spin), 0, 0, 0);
            ResourceLocation whiteTexture = YwzjVehicle.resourceLocation("textures/misc/white.png");
            VertexConsumer arrowBuilder = bufferSource.getBuffer(RenderType.entityCutout(whiteTexture));
            Matrix4f pose = poseStack.last().pose();
            Matrix3f normal = poseStack.last().normal();
            int light = 15728880;
            // 前面
            RenderHelper.renderSolidQuad(arrowBuilder, pose, normal, 0, 0, 0,   w, h, w,  -w, h, w,  -w, h, w,  r, g, b, a, light);
            // 后面
            RenderHelper.renderSolidQuad(arrowBuilder, pose, normal, 0, 0, 0,  -w, h, -w,  w, h, -w,  w, h, -w, r, g, b, a, light);
            // 左面
            RenderHelper.renderSolidQuad(arrowBuilder, pose, normal, 0, 0, 0,  -w, h, w,  -w, h, -w, -w, h, -w, r, g, b, a, light);
            // 右面
            RenderHelper.renderSolidQuad(arrowBuilder, pose, normal, 0, 0, 0,   w, h, -w,  w, h, w,   w, h, w,  r, g, b, a, light);
            // 顶面 (封口)
            RenderHelper.renderSolidQuad(arrowBuilder, pose, normal, -w, h, -w, -w, h, w,   w, h, w,   w, h, -w, r, g, b, a, light);
        }
        poseStack.popPose();
    }

    /**
     * 绘制用于纯色渲染的四边形/三角形面片
     */
    public static void renderSolidQuad(VertexConsumer builder, Matrix4f pose, Matrix3f normal,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float x3, float y3, float z3, float x4, float y4, float z4,
                                 int r, int g, int b, int a, int light) {
        // 强制绑定到 UV(0, 0) 来获取贴图单色（搭配纯白贴图即可实现纯色渲染）
        builder.vertex(pose, x1, y1, z1).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        builder.vertex(pose, x2, y2, z2).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        builder.vertex(pose, x3, y3, z3).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        builder.vertex(pose, x4, y4, z4).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
    }

}
