package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
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
        float arrowLength = (float) (15.0f * v.length() / helicopterVehicle.maxAirSpeed);
        float arrowSize = 4.0f;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            pose.translate(centerX, centerY, 0);
            pose.mulPose(Axis.ZP.rotation((float) Math.atan2(-v.x, v.z)));

            // 速度线主干
            pose.pushPose();
            {
                pose.scale(0.5f, 1f, 0.5f);
                guiGraphics.fill(-1, 0, 1, (int) -arrowLength, 0xFF00FF00);
            }
            pose.popPose();

            // 速度线箭头
            float endY = -arrowLength - 3;
            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(pose.last().pose(), 0, endY, 0).color(0, 255, 0, 255).endVertex();
            buf.vertex(pose.last().pose(), -arrowSize / 2, endY + arrowSize, 0).color(0, 255, 0, 255).endVertex();
            buf.vertex(pose.last().pose(), arrowSize / 2, endY + arrowSize, 0).color(0, 255, 0, 255).endVertex();
            Tesselator.getInstance().end();

            RenderSystem.disableBlend();
        }
        pose.popPose();

        // 滚转线
        pose.pushPose();
        {
            pose.translate(centerX, centerY, 0);
            pose.scale(1f, 0.6f, 1f);
            guiGraphics.fill(-17, 0, -13, 1, 0xFF00FF00);
            guiGraphics.fill(-12, 0, -8, 1, 0xFF00FF00);
            guiGraphics.fill(-7, 0, -3, 1, 0xFF00FF00);
            guiGraphics.fill(3, 0, 7, 1, 0xFF00FF00);
            guiGraphics.fill(8, 0, 12, 1, 0xFF00FF00);
            guiGraphics.fill(13, 0, 17, 1, 0xFF00FF00);
            pose.mulPose(Axis.ZP.rotation((float) Math.toRadians(helicopterVehicle.getZRot())));
            guiGraphics.fill(-11, 0, -2, 1, 0xFF00FF00);
            guiGraphics.fill(-3, 0, -2, 3, 0xFF00FF00);
            guiGraphics.fill(2, 0, 3, 3, 0xFF00FF00);
            guiGraphics.fill(2, 0, 11, 1, 0xFF00FF00);
        }
        pose.popPose();

    }

}
