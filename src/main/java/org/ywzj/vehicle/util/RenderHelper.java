package org.ywzj.vehicle.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;

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
        guiGraphics.hLine(x - half, x + half, y, color);
        // 竖线
        guiGraphics.vLine(x, y - half, y + half, color);
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
    }

}
