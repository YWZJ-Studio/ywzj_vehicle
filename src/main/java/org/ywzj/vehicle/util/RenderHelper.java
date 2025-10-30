package org.ywzj.vehicle.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class RenderHelper {

    public static void drawCircle(GuiGraphics guiGraphics, int x, int y, int r, int color) {
        float c = 2 * 3.1415f / 32;
        for (int i = 0; i < 32; i += 1) {
            float rx = Mth.cos(c * i) * r;
            float ry = Mth.sin(c * i) * r;
            guiGraphics.fill((int) (x + rx), (int) (y + ry), (int) (x + rx) + 1, (int) (y + ry) + 1, color);
        }
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

}
