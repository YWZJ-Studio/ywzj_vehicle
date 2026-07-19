package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

public class RotaryWingVehicleOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        if (!LocalVehiclePlayer.instance.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
        if (!(vehicle instanceof RotaryWingVehicle rotaryWingVehicle)
                || !LocalVehiclePlayer.instance.getPlayer().equals(rotaryWingVehicle.getDriver())) {
            return;
        }
        float centerX = (float) screenWidth / 2;
        float centerY = (float) screenHeight / 2;
        LocalVehiclePlayer.ViewType viewType = LocalVehiclePlayer.instance.viewType;
        renderMainInfo(guiGraphics, centerX, centerY, rotaryWingVehicle);
        renderHeightInfo(guiGraphics, centerX, centerY, rotaryWingVehicle);
        if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON || viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            {
                if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
                    renderSpeedInfo(guiGraphics, partialTick, centerX, centerY, rotaryWingVehicle);
                    renderRollInfo(guiGraphics, partialTick, centerX, centerY, 1f, rotaryWingVehicle, viewType);
                } else if (viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                    renderSpeedInfo(guiGraphics, partialTick, centerX - 95, centerY + 75, rotaryWingVehicle);
                    renderRollInfo(guiGraphics, partialTick, centerX - 95, centerY + 75, 0.7f, rotaryWingVehicle, viewType);
                }
            }
            pose.popPose();
        }
    }

    /**
     * 主信息
     */
    public static void renderMainInfo(GuiGraphics guiGraphics, float centerX, float centerY, RotaryWingVehicle rotaryWingVehicle) {
        int leftX = (int) (centerX - 120);
        int leftY = (int) (centerY - 21);
        // 信息
        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font,
                Component.translatable("ui.vehicle_rotary_wing.rpm", (int) rotaryWingVehicle.getPower()),
                leftX, leftY, Color.GREEN);
        guiGraphics.drawString(font,
                Component.translatable("ui.vehicle_rotary_wing.collective_pitch", (int) rotaryWingVehicle.getCollectivePitch()),
                leftX, leftY + 12, Color.GREEN);
        int speed = (int) (new Vec3(rotaryWingVehicle.getDeltaMovement().x, 0, rotaryWingVehicle.getDeltaMovement().z).length() * 72); // 20 * 3.6 = 72
        guiGraphics.drawString(font,
                Component.translatable("ui.vehicle_rotary_wing.speed", speed),
                leftX, leftY + 24, Color.GREEN);
        String fuelTime = secondsToHms(rotaryWingVehicle.getEnergy() / rotaryWingVehicle.energyInfo.energyConsumptionPerTick / 20);
        guiGraphics.drawString(font,
                Component.translatable("ui.vehicle_rotary_wing.fuel", fuelTime),
                leftX, leftY + 36, Color.GREEN);
        guiGraphics.drawString(font,
                Component.translatable("ui.vehicle_rotary_wing.altitude", (int) rotaryWingVehicle.getY()),
                (int) (centerX + 62), (int) (leftY + 24), Color.GREEN);
    }

    /**
     * 高度信息
     */
    public static void renderHeightInfo(GuiGraphics guiGraphics, float centerX, float centerY, RotaryWingVehicle rotaryWingVehicle) {
        // 高度变化
        int heightY = (int) (centerY - 21);
        int heightX = (int) (centerX + 110);
        for (int step = 0; step < 17; step++) {
            boolean flag = step % 4 == 0;
            guiGraphics.fill((flag ? heightX : heightX + 5), heightY, heightX + 10, heightY + 1, Color.GREEN);
            heightY += 3;
        }
        // 高度变化箭头
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            heightY = (int) (centerY + 3 + (24 * -rotaryWingVehicle.getDeltaMovement().y));
            heightX = (int) (centerX + 123);
            pose.translate(heightX, heightY, 0);
            var buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.addVertex(pose.last().pose(), 0, 0, 0).setColor(0, 255, 0, 255);
            buf.addVertex(pose.last().pose(), 6, 3, 0).setColor(0, 255, 0, 255);
            buf.addVertex(pose.last().pose(), 6, -3, 0).setColor(0, 255, 0, 255);
            BufferUploader.drawWithShader(buf.buildOrThrow());
            guiGraphics.drawString(Minecraft.getInstance().font, String.valueOf((int) (rotaryWingVehicle.getDeltaMovement().y * 20)), 12, -4, Color.GREEN);

            RenderSystem.disableBlend();
        }
        pose.popPose();
    }

    /**
     * 速度信息
     */
    public static void renderSpeedInfo(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY, RotaryWingVehicle rotaryWingVehicle) {
        Quaternionf q = new Quaternionf();
        q.rotateY((float) Math.toRadians(-Mth.lerp(partialTick, rotaryWingVehicle.yRotO, rotaryWingVehicle.getYRot())))
                .rotateX((float) Math.toRadians(Mth.lerp(partialTick, rotaryWingVehicle.xRotO, rotaryWingVehicle.getXRot())))
                .rotateZ((float) Math.toRadians(Mth.lerp(partialTick, rotaryWingVehicle.zRotO, rotaryWingVehicle.getZRot())));
        Matrix3f axisRollMat = new Matrix3f();
        q.get(axisRollMat);
        axisRollMat = axisRollMat.transpose();
        Vec3 vo = rotaryWingVehicle.deltaMovementO;
        Vec3 v = rotaryWingVehicle.getDeltaMovement();
        v = new Vec3(Mth.lerp(partialTick, vo.x, v.x), Mth.lerp(partialTick, vo.y, v.y), Mth.lerp(partialTick, vo.z, v.z));
        v = new Vec3(axisRollMat.transform(v.toVector3f()));
        float arrowLength = (float) (15.0f * v.length() / rotaryWingVehicle.maxAirSpeed);
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
                guiGraphics.fill(-1, 0, 1, (int) -arrowLength, Color.GREEN);
            }
            pose.popPose();

            // 速度线箭头
            float endY = -arrowLength - 3;
            var buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.addVertex(pose.last().pose(), 0, endY, 0).setColor(0, 255, 0, 255);
            buf.addVertex(pose.last().pose(), -arrowSize / 2, endY + arrowSize, 0).setColor(0, 255, 0, 255);
            buf.addVertex(pose.last().pose(), arrowSize / 2, endY + arrowSize, 0).setColor(0, 255, 0, 255);
            BufferUploader.drawWithShader(buf.buildOrThrow());

            RenderSystem.disableBlend();
        }
        pose.popPose();
    }

    /**
     * 滚转信息
     */
    public static void renderRollInfo(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY, float scale, AbstractVehicle vehicle, LocalVehiclePlayer.ViewType viewType) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        {
            pose.translate(centerX, centerY, 0);
            pose.scale(scale, scale, scale);
            if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
                guiGraphics.fill(-17, 0, -13, 1, Color.GREEN);
                guiGraphics.fill(-12, 0, -8, 1, Color.GREEN);
                guiGraphics.fill(-7, 0, -3, 1, Color.GREEN);
                guiGraphics.fill(3, 0, 7, 1, Color.GREEN);
                guiGraphics.fill(8, 0, 12, 1, Color.GREEN);
                guiGraphics.fill(13, 0, 17, 1, Color.GREEN);
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, vehicle.zRotO, vehicle.getZRot())));
                pose.scale(1f, 0.6f, 1f);
                guiGraphics.fill(-11, 0, -2, 1, Color.GREEN);
                guiGraphics.fill(-3, 0, -2, 3, Color.GREEN);
                guiGraphics.fill(2, 0, 3, 3, Color.GREEN);
                guiGraphics.fill(2, 0, 11, 1, Color.GREEN);
            } else if (viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                pose.pushPose();
                {
                    float arrowSize = 6.0f;
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    var buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
                    buf.addVertex(pose.last().pose(), -19, 0, 0).setColor(0, 255, 0, 255);
                    buf.addVertex(pose.last().pose(), -19 -arrowSize / 2, -arrowSize / 2, 0).setColor(0, 255, 0, 255);
                    buf.addVertex(pose.last().pose(), -19 -arrowSize / 2, arrowSize / 2, 0).setColor(0, 255, 0, 255);
                    BufferUploader.drawWithShader(buf.buildOrThrow());
                    RenderSystem.disableBlend();
                }
                pose.popPose();
                double xRot = Mth.wrapDegrees(-Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot()));
                int range = 50;
                float interval = 5;
                float gap = range / interval;
                int baseY = -range;
                double value = xRot + range / gap * interval;
                int rot = (int) (Math.floor(value / interval) * interval);
                double mod = ((xRot % interval) + interval) % interval;
                pose.mulPose(Axis.ZP.rotationDegrees(-Mth.lerp(partialTick, vehicle.zRotO, vehicle.getZRot())));
                pose.translate(0, mod / interval * gap, 0);
                guiGraphics.enableScissor(0, (int) (centerY - 35), guiGraphics.guiWidth(), (int) (centerY + 33));
                while (baseY <= range) {
                    guiGraphics.fill(-17, baseY, -13, baseY + 1, Color.GREEN);
                    guiGraphics.fill(-12, baseY, -8, baseY + 1, Color.GREEN);
                    guiGraphics.fill(-7, baseY, -3, baseY + 1, Color.GREEN);
                    guiGraphics.fill(3, baseY, 7, baseY + 1, Color.GREEN);
                    guiGraphics.fill(8, baseY, 12, baseY + 1, Color.GREEN);
                    guiGraphics.fill(13, baseY, 17, baseY + 1, Color.GREEN);
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, rot + "", 32, baseY - 3, Color.GREEN);
                    baseY += gap;
                    rot -= interval;
                }
                guiGraphics.disableScissor();
            }
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
