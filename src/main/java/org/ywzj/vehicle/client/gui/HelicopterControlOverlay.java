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
        if (!LocalVehiclePlayer.instance.onVehicle() || LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.THIRD_PERSON) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
        if (!(vehicle instanceof HelicopterVehicle helicopterVehicle)) {
            return;
        }
        // 屏幕中心点
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // 主信息
        renderMainInfo(guiGraphics, centerX, centerY, helicopterVehicle);

        // 高度信息
        renderHeightInfo(guiGraphics, centerX, centerY, helicopterVehicle);

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
            guiGraphics.fill(-17, 0, -13, 1, 0xFF00FF00);
            guiGraphics.fill(-12, 0, -8, 1, 0xFF00FF00);
            guiGraphics.fill(-7, 0, -3, 1, 0xFF00FF00);
            guiGraphics.fill(3, 0, 7, 1, 0xFF00FF00);
            guiGraphics.fill(8, 0, 12, 1, 0xFF00FF00);
            guiGraphics.fill(13, 0, 17, 1, 0xFF00FF00);
            pose.mulPose(Axis.ZP.rotation((float) Math.toRadians(helicopterVehicle.getZRot())));
            pose.scale(1f, 0.6f, 1f);
            guiGraphics.fill(-11, 0, -2, 1, 0xFF00FF00);
            guiGraphics.fill(-3, 0, -2, 3, 0xFF00FF00);
            guiGraphics.fill(2, 0, 3, 3, 0xFF00FF00);
            guiGraphics.fill(2, 0, 11, 1, 0xFF00FF00);
        }
        pose.popPose();

    }

    public static void renderMainInfo(GuiGraphics guiGraphics, int centerX, int centerY, HelicopterVehicle helicopterVehicle) {
        int leftX = centerX - 120;
        int leftY = centerY - 21;
        // 信息
        guiGraphics.drawString(Minecraft.getInstance().font, "转速: " +
                        (int) helicopterVehicle.getPower(),
                leftX, leftY, 0x00FF00);
        guiGraphics.drawString(Minecraft.getInstance().font, "总距: " +
                        helicopterVehicle.getCollectivePitch(),
                leftX, leftY + 12, 0x00FF00);
        guiGraphics.drawString(Minecraft.getInstance().font, "速度: " +
                        (int) (new Vec3(helicopterVehicle.getDeltaMovement().x, 0, helicopterVehicle.getDeltaMovement().z).length() * 20 * 3600 / 1000),
                leftX, leftY + 24, 0x00FF00);
        guiGraphics.drawString(Minecraft.getInstance().font, "燃油: " +
                        secondsToHms(helicopterVehicle.getFuel() / helicopterVehicle.fuelConsumptionPerTick / 20),
                leftX, leftY + 36, 0x00FF00);
        guiGraphics.drawString(Minecraft.getInstance().font, "高度: " + (int) helicopterVehicle.getY(),
                centerX + 62, leftY + 24, 0x00FF00);
    }

    public static void renderHeightInfo(GuiGraphics guiGraphics, int centerX, int centerY, HelicopterVehicle helicopterVehicle) {
        // 高度变化
        int heightY = centerY - 21;
        int heightX = centerX + 110;
        for (int step = 0; step < 17; step++) {
            boolean flag = step % 4 == 0;
            guiGraphics.fill((flag ? heightX : heightX + 5), heightY, heightX + 10, heightY + 1, 0xFF00FF00);
            heightY += 3;
        }
        // 高度变化箭头
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            heightY = (int) (centerY + 3 + (24 * -helicopterVehicle.getDeltaMovement().y));
            heightX = centerX + 123;
            pose.translate(heightX, heightY, 0);
            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(pose.last().pose(), 0, 0, 0).color(0, 255, 0, 255).endVertex();
            buf.vertex(pose.last().pose(), 6, 3, 0).color(0, 255, 0, 255).endVertex();
            buf.vertex(pose.last().pose(), 6, -3, 0).color(0, 255, 0, 255).endVertex();
            Tesselator.getInstance().end();
            guiGraphics.drawString(Minecraft.getInstance().font, String.valueOf((int) (helicopterVehicle.getDeltaMovement().y * 20)), 12, -4, 0x00FF00);

            RenderSystem.disableBlend();
        }
        pose.popPose();
    }

    public static String secondsToHms(float seconds) {
        boolean neg = seconds < 0;
        float abs = Math.abs(seconds);
        long whole = (long) abs;
        long hh = whole / 3600;
        long mm = (whole % 3600) / 60;
        long ss = whole % 60;
        return String.format("%s%02d:%02d:%02d", neg ? "-" : "", hh, mm, ss);
    }

}
