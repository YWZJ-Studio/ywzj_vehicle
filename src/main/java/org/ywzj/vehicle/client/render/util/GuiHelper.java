package org.ywzj.vehicle.client.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.render.ModShaders;

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

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, ModShaders.HUD_CIRCLE);

        Matrix4f matrix = poseStack.last().pose();

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        drawCircle(
                matrix, bufferbuilder,
                x, y,
                radius,
                red, green, blue, alpha,
                thickness, start, end
        );

        BufferUploader.drawWithShader(bufferbuilder.end());

        RenderSystem.disableBlend();
    }

    public static void drawCircle(
            Matrix4f matrix, BufferBuilder buf,
            float cx, float cy, float radius,
            float r, float g, float b, float a,
            float thickness, float start, float end
    ) {
        // We encode thickness,start,end into the normal attribute (x,y,z)
        buf.vertex(matrix, cx - radius, cy + radius, 0).color(r, g, b, a).uv(0, 1).normal(thickness, start, end).endVertex();
        buf.vertex(matrix,cx + radius, cy + radius, 0).color(r, g, b, a).uv(1, 1).normal(thickness, start, end).endVertex();
        buf.vertex(matrix,cx + radius, cy - radius, 0).color(r, g, b, a).uv(1, 0).normal(thickness, start, end).endVertex();
        buf.vertex(matrix,cx - radius, cy - radius, 0).color(r, g, b, a).uv(0, 0).normal(thickness, start, end).endVertex();
    }
}
