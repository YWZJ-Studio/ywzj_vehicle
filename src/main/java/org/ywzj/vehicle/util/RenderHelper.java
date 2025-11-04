package org.ywzj.vehicle.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RenderHelper {

    public static void drawCircle(GuiGraphics guiGraphics, int x, int y, int r, int color) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.scale(0.5f, 0.5f, 0.5f);
            float c = 2 * 3.1415f / 32;
            for (int i = 0; i < 32; i += 1) {
                float rx = Mth.cos(c * i) * r;
                float ry = Mth.sin(c * i) * r;
                guiGraphics.fill((int) (x + rx), (int) (y + ry), (int) (x + rx) + 1, (int) (y + ry) + 1, color);
            }
        }
        poseStack.popPose();
    }

    public static void drawCross(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int half = size / 2;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.scale(0.7f, 0.7f, 0.7f);
            // 横线
            for (int i = x - half; i <= x + half; i++) {
                guiGraphics.fill(i, y, i + 1, y + 1, color);
            }
            // 竖线
            for (int j = y - half; j <= y + half; j++) {
                guiGraphics.fill(x, j, x + 1, j + 1, color);
            }
        }
        poseStack.popPose();
    }

    public static void drawSquare(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int half = size / 2;
        int left = x - half;
        int right = x + half;
        int top = y - half;
        int bottom = y + half;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.scale(0.7f, 0.7f, 0.7f);
            for (int i = left; i <= right; i++) {
                guiGraphics.fill(i, top, i + 1, top + 1, color);
                guiGraphics.fill(i, bottom, i + 1, bottom + 1, color);
            }
            for (int j = top; j <= bottom; j++) {
                guiGraphics.fill(left, j, left + 1, j + 1, color);
                guiGraphics.fill(right, j, right + 1, j + 1, color);
            }
        }
        poseStack.popPose();
    }

    public static void drawRectByCorner(GuiGraphics guiGraphics, int left, int right, int top, int bottom, int color, float scale) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.scale(scale, scale, scale);
            // 上边框
            for (int i = left; i <= right; i++) {
                guiGraphics.fill(i, top, i + 1, top + 1, color);
            }
            // 下边框
            for (int i = left; i <= right; i++) {
                guiGraphics.fill(i, bottom, i + 1, bottom + 1, color);
            }
            // 左边框
            for (int j = top; j <= bottom; j++) {
                guiGraphics.fill(left, j, left + 1, j + 1, color);
            }
            // 右边框
            for (int j = top; j <= bottom; j++) {
                guiGraphics.fill(right, j, right + 1, j + 1, color);
            }
        }
        poseStack.popPose();
    }

    public static void drawRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int color, float scale) {
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        int left = x - halfWidth;
        int right = x + halfWidth;
        int top = y - halfHeight;
        int bottom = y + halfHeight;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.scale(scale, scale, scale);
            // 上边框
            for (int i = left; i <= right; i++) {
                guiGraphics.fill(i, top, i + 1, top + 1, color);
            }
            // 下边框
            for (int i = left; i <= right; i++) {
                guiGraphics.fill(i, bottom, i + 1, bottom + 1, color);
            }
            // 左边框
            for (int j = top; j <= bottom; j++) {
                guiGraphics.fill(left, j, left + 1, j + 1, color);
            }
            // 右边框
            for (int j = top; j <= bottom; j++) {
                guiGraphics.fill(right, j, right + 1, j + 1, color);
            }
        }
        poseStack.popPose();
    }

    public static void drawReticle(GuiGraphics guiGraphics, int x, int y, int size, int thickness, int color) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.scale(0.5f, 0.5f, 0.5f);
            if (thickness < 1) thickness = 1;
            int top = y;
            int bottom = y + size;
            // 竖线
            int vHalf = thickness / 2;
            int vLeft = x - vHalf;
            int vRight = vLeft + thickness;
            guiGraphics.fill(vLeft, top, vRight, bottom, color);
            // 横线
            int halfLen = Math.max(1, (int)(size * 0.45));
            int segments = 4;
            int gap = size / segments; // 每段间距
            int hHalf = thickness / 2;
            for (int i = 0; i <= segments; i++) {
                int yPos = top + gap * i;
                int left = x - halfLen;
                int right = x + halfLen;
                guiGraphics.fill(left + 1, yPos - hHalf, right, yPos + (thickness - hHalf), color);
                halfLen -= 1;
            }
        }
        poseStack.popPose();
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
        guiGraphics.flush();
    }

}
