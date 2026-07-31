package org.ywzj.vehicle.client.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.shader.ModShaders;

public class GuiHelper {

    public static void drawCircle(PoseStack poseStack, float x, float y, float radius, int color, float thickness, float progress) {
        // Backwards-compatible wrapper: treat progress as end, start at 0
        drawCircle(poseStack, x, y, radius, color, thickness, 0.0f, progress);
    }

    public static void drawCircle(PoseStack poseStack, float x, float y, float radius, int color, float thickness, float start, float end) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ShaderInstance shader = ModShaders.getCircleShader();
        if (shader == null) return; // Shader 尚未加载
        RenderSystem.setShader(() -> shader);

        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, ModShaders.HUD_CIRCLE);

        Matrix4f matrix = poseStack.last().pose();

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        RenderSystem.depthMask(false);
        drawCircle(
                matrix, bufferbuilder,
                x, y,
                radius,
                red, green, blue, alpha,
                thickness, start, end
        );
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    public static void drawCircle(
            Matrix4f matrix, BufferBuilder buf,
            float cx, float cy, float radius,
            float r, float g, float b, float a,
            float thickness, float start, float end
    ) {
        int fullBright = LightTexture.FULL_BRIGHT;
        buf.addVertex(matrix, cx - radius, cy + radius, 0)
                .setColor(r, g, b, a)
                .setUv(0, 1)
                .setUv2(fullBright & 0xFFFF, fullBright >> 16 & 0xFFFF)
                .setNormal(thickness, start, end);
        buf.addVertex(matrix, cx + radius, cy + radius, 0)
                .setColor(r, g, b, a)
                .setUv(1, 1)
                .setUv2(fullBright & 0xFFFF, fullBright >> 16 & 0xFFFF)
                .setNormal(thickness, start, end);
        buf.addVertex(matrix, cx + radius, cy - radius, 0)
                .setColor(r, g, b, a)
                .setUv(1, 0)
                .setUv2(fullBright & 0xFFFF, fullBright >> 16 & 0xFFFF)
                .setNormal(thickness, start, end);
        buf.addVertex(matrix, cx - radius, cy - radius, 0)
                .setColor(r, g, b, a)
                .setUv(0, 0)
                .setUv2(fullBright & 0xFFFF, fullBright >> 16 & 0xFFFF)
                .setNormal(thickness, start, end);
    }

    /**
     * 截断字符串至最大像素宽度，超出加省略号
     */
    public static String truncateText(Font font, String text, int maxPx) {
        if (font.width(text) <= maxPx) return text;
        String ellipsis = "..";
        int ellW = font.width(ellipsis);
        while (text.length() > 0 && font.width(text) + ellW > maxPx) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    /**
     * 绘制扇形弧（用于进度圈）。
     * startFrac / endFrac ∈ [0, 1]，从 12 点方向顺时针。
     */
    public static void drawArc(GuiGraphics gg, float cx, float cy, float radius, float thickness,
                               float startFrac, float endFrac, int color) {
        if (endFrac <= startFrac) return;

        float a = ((color >> 24) & 255) / 255f;
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >>  8) & 255) / 255f;
        float b = ((color      ) & 255) / 255f;

        float inner = radius - thickness;
        int   segs  = Math.max(4, (int)(64 * (endFrac - startFrac)));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        PoseStack.Pose last = gg.pose().last();

        for (int i = 0; i <= segs; i++) {
            float frac = startFrac + (endFrac - startFrac) * i / segs;
            // 从 -π/2（12点）顺时针
            double ang = (frac * 2 * Math.PI) - Math.PI / 2;
            float cos = (float) Math.cos(ang);
            float sin = (float) Math.sin(ang);

            buf.addVertex(last.pose(), cx + cos * radius, cy + sin * radius, 0)
                    .setColor(r, g, b, a);
            buf.addVertex(last.pose(), cx + cos * inner, cy + sin * inner, 0)
                    .setColor(r, g, b, a);
        }

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
    }

}
