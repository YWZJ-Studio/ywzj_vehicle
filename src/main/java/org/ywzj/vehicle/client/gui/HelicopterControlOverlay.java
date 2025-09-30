package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.HelicopterVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

public class HelicopterControlOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!LocalVehiclePlayer.instance.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
        if (!(vehicle instanceof HelicopterVehicle helicopterVehicle)) {
            return;
        }
        // 屏幕中心点
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        guiGraphics.drawString(Minecraft.getInstance().font, "总距: " + helicopterVehicle.getCollectivePitch(), centerX - 100, centerY, 0x00FF00);
        guiGraphics.drawString(Minecraft.getInstance().font, "转速: " + helicopterVehicle.getEngineSpeed(), centerX - 100, centerY + 10, 0x00FF00);
        guiGraphics.drawString(Minecraft.getInstance().font, "速度: " + (int) (helicopterVehicle.getDeltaMovement().length() * 20), centerX - 100, centerY + 20, 0x00FF00);
        guiGraphics.drawString(Minecraft.getInstance().font, "高度: " + (int) helicopterVehicle.getY(), centerX - 100, centerY + 30, 0x00FF00);

        // 速度矢量
        Vec3 v = vehicle.relativeRotDirection(helicopterVehicle.getDeltaMovement(), true);

        // 归一化后放大（控制箭头长度）
        double scale = 30.0; // 缩放因子
        double vx = v.x * scale;
        double vz = v.z * scale;

        // 箭头起点 (屏幕中心)
        float startX = centerX;
        float startY = centerY;
        // 箭头终点
        float endX = (float) (centerX + vx);
        float endY = (float) (centerY + vz);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // 主线段
        buf.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(pose.last().pose(), startX, startY, 0).color(0, 255, 0, 255).endVertex();
        buf.vertex(pose.last().pose(), endX, endY, 0).color(0, 255, 0, 255).endVertex();
        tess.end();

//        // 箭头三角
        double arrowSize = 4.0;
        double angle = Math.atan2(endY - startY, endX - startX);
        double leftX = endX - arrowSize * Math.cos(angle - Math.PI / 6);
        double leftY = endY - arrowSize * Math.sin(angle - Math.PI / 6);
        double rightX = endX - arrowSize * Math.cos(angle + Math.PI / 6);
        double rightY = endY - arrowSize * Math.sin(angle + Math.PI / 6);

        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(pose.last().pose(), endX, endY, 0).color(0, 255, 0, 255).endVertex();
        buf.vertex(pose.last().pose(), (float) leftX, (float) leftY, 0).color(0, 255, 0, 255).endVertex();
        buf.vertex(pose.last().pose(), (float) rightX, (float) rightY, 0).color(0, 255, 0, 255).endVertex();
        tess.end();

        RenderSystem.disableBlend();
        pose.popPose();
    }

}
