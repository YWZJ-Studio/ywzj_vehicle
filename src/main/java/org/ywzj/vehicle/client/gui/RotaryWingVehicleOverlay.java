package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
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
import org.ywzj.vehicle.util.VehicleHudText;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.Locale;

public class RotaryWingVehicleOverlay implements LayeredDraw.Layer {

    private static final float[] GULL_WING = {-11, 0, -6, 0, -3.3f, 4.5f, 0, 0, 3.3f, 4.5f, 6, 0, 11, 0};
    private static final float HUD_LINE_WIDTH = 0.5f;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        if (!LocalVehiclePlayer.instance.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.vehicle;
        if (!(vehicle instanceof RotaryWingVehicle rotaryWingVehicle)
                || !LocalVehiclePlayer.instance.getPlayer().equals(rotaryWingVehicle.getDriver())) {
            return;
        }
        try (VehicleHudLayout layout = VehicleHudLayout.begin(guiGraphics)) {
            float centerX = layout.width() / 2;
            float centerY = VehicleHudLayout.CENTER_Y;
            LocalVehiclePlayer.ViewType viewType = LocalVehiclePlayer.instance.viewType;
            renderMainInfo(guiGraphics, centerX, centerY, rotaryWingVehicle);
            renderHeightInfo(guiGraphics, centerX, centerY, rotaryWingVehicle);
            if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON || viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                {
                    if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
                        pose.translate(centerX, centerY, 0);
                        pose.scale(2, 2, 1);
                        renderSpeedInfo(guiGraphics, partialTick, 0, 0, rotaryWingVehicle);
                        renderRollInfo(guiGraphics, partialTick, 0, 0, 1f, rotaryWingVehicle, viewType);
                    } else if (viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                        float attitudeX = VehicleHudLayout.operatorAttitudeX(centerX);
                        float attitudeY = VehicleHudLayout.OPERATOR_ATTITUDE_Y;
                        renderSpeedInfo(guiGraphics, partialTick, attitudeX, attitudeY, rotaryWingVehicle);
                        renderRollInfo(guiGraphics, partialTick, attitudeX, attitudeY, 1f, rotaryWingVehicle, viewType);
                    }
                }
                pose.popPose();
            }
        }
    }

    /**
     * 主信息
     */
    public static void renderMainInfo(GuiGraphics guiGraphics, float centerX, float centerY, RotaryWingVehicle rotaryWingVehicle) {
        float leftX = VehicleHudLayout.telemetryX(centerX);
        float leftY = VehicleHudLayout.TELEMETRY_Y;
        int row = VehicleHudLayout.ROW_HEIGHT;
        VehicleHudText.telemetry(guiGraphics, Component.translatable("ui.vehicle_hud.rpm"), Component.literal((int) rotaryWingVehicle.getPower() + " %"),
                leftX, leftY, Color.GREEN);
        VehicleHudText.telemetry(guiGraphics, Component.translatable("ui.vehicle_hud.collective_pitch"), Component.literal((int) rotaryWingVehicle.getCollectivePitch() + " %"),
                leftX, leftY + row, Color.GREEN);
        int speed = (int) (new Vec3(rotaryWingVehicle.getDeltaMovement().x, 0, rotaryWingVehicle.getDeltaMovement().z).length() * 72); // 20 * 3.6 = 72
        VehicleHudText.telemetry(guiGraphics, Component.translatable("ui.vehicle_hud.speed"), Component.literal(speed + " km/h"),
                leftX, leftY + row * 2, Color.GREEN);
        String fuelTime = VehicleHudText.endurance(rotaryWingVehicle.getEnergy(), rotaryWingVehicle.energyInfo.energyConsumptionPerTick);
        VehicleHudText.telemetry(guiGraphics, Component.translatable("ui.vehicle_hud.fuel"), Component.literal(fuelTime),
                leftX, leftY + row * 3, Color.GREEN);
        VehicleHudText.drawRightAlignedString(guiGraphics, Component.literal((int) rotaryWingVehicle.getY() + " m"),
                VehicleHudLayout.varioX(centerX) - 18, centerY - 7, Color.GREEN);
    }

    /**
     * 高度信息
     */
    public static void renderHeightInfo(GuiGraphics guiGraphics, float centerX, float centerY, RotaryWingVehicle rotaryWingVehicle) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        try {
            pose.translate(VehicleHudLayout.varioX(centerX) - 10, centerY - 36, 0);
            for (int step = 0; step < 17; step++) {
                float y = step * 4.5f;
                drawHudPolyline(pose, step % 4 == 0 ? 0 : 6.5f, y, 10, y);
            }
            drawHudPolyline(pose, 10, 36, 10, 72);

            float offsetY = (float) Mth.clamp(36 * rotaryWingVehicle.getDeltaMovement().y, -36, 36);
            pose.translate(13, 36 - offsetY, 0);
            drawHudPolyline(pose, 0, 0, 6, -2, 6, 2, 0, 0);
            VehicleHudText.drawString(guiGraphics, String.format(Locale.ROOT, "%.1f m/s", rotaryWingVehicle.getDeltaMovement().y * 20),
                    12, -7, Color.GREEN);
        } finally {
            pose.popPose();
        }
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
        double planarSpeed = Math.hypot(v.x, v.z);
        if (planarSpeed < 0.001 || rotaryWingVehicle.maxAirSpeed <= 0) {
            return;
        }
        float arrowLength = (float) Math.min(30, 15 * planarSpeed / rotaryWingVehicle.maxAirSpeed);
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        try {
            pose.translate(centerX, centerY, 0);
            pose.mulPose(Axis.ZP.rotation((float) Math.atan2(-v.x, v.z)));
            float headLength = 1.5f;
            drawHudPolyline(pose, 0, 0, 0, -arrowLength);
            drawHudPolyline(pose, -headLength * 0.5f, -arrowLength + headLength,
                    0, -arrowLength, headLength * 0.5f, -arrowLength + headLength);
        } finally {
            pose.popPose();
        }
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
                drawHudPolyline(pose, -17, 0, -12, 0);
                drawHudPolyline(pose, 12, 0, 17, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, vehicle.zRotO, vehicle.getZRot())));
                drawHudPolyline(pose, GULL_WING);
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
                float hudScale = guiGraphics.guiHeight() / VehicleHudLayout.HEIGHT;
                guiGraphics.enableScissor((int) ((centerX - 56 * scale) * hudScale),
                        (int) ((centerY - 32 * scale) * hudScale),
                        (int) ((centerX + 56 * scale) * hudScale),
                        (int) ((centerY + 32 * scale) * hudScale));
                while (baseY <= range) {
                    drawHudLines(pose, 4,
                            -17, baseY, -13, baseY, -12, baseY, -8, baseY, -7, baseY, -3, baseY,
                            3, baseY, 7, baseY, 8, baseY, 12, baseY, 13, baseY, 17, baseY);
                    VehicleHudText.drawCenteredString(guiGraphics, rot + "", 32, baseY - 6, Color.GREEN);
                    baseY += gap;
                    rot -= interval;
                }
                guiGraphics.disableScissor();
            }
        }
        pose.popPose();
    }

    public static String secondsToHms(float seconds) {
        return VehicleHudText.secondsToHms(seconds);
    }

    private static void drawHudPolyline(PoseStack pose, float... points) {
        drawHudLines(pose, 2, points);
    }

    private static void drawHudLines(PoseStack pose, int stride, float... points) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i + 3 < points.length; i += stride) {
            float x1 = points[i];
            float y1 = points[i + 1];
            float x2 = points[i + 2];
            float y2 = points[i + 3];
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float) Math.hypot(dx, dy);
            if (length < 0.001f) {
                continue;
            }
            float nx = -dy / length * HUD_LINE_WIDTH / 2;
            float ny = dx / length * HUD_LINE_WIDTH / 2;
            buf.addVertex(pose.last().pose(), x1 - nx, y1 - ny, 0).setColor(0, 255, 0, 255);
            buf.addVertex(pose.last().pose(), x1 + nx, y1 + ny, 0).setColor(0, 255, 0, 255);
            buf.addVertex(pose.last().pose(), x2 + nx, y2 + ny, 0).setColor(0, 255, 0, 255);
            buf.addVertex(pose.last().pose(), x2 - nx, y2 - ny, 0).setColor(0, 255, 0, 255);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
    }

}
