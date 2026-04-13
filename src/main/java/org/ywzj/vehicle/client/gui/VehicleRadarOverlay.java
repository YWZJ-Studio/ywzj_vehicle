package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.render.util.GuiHelper;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.RadarUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.passenger.WarningReceiver;
import org.ywzj.vehicle.vehicle.pojo.WarnType;

import java.util.List;
import java.util.Map;

public class VehicleRadarOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        WeaponUnit weaponUnit = LocalVehiclePlayer.instance.getWeaponUnit();
        if (weaponUnit == null) {
            return;
        }
        AbstractVehicle vehicle = weaponUnit.getVehicle();
        PoseStack poseStack = guiGraphics.pose();
        int centerX = screenWidth / 2 + 128;
        int centerY = screenHeight - 80;
        poseStack.pushPose();
        {
            List<RadarUnit> radarUnits = weaponUnit.getRadarUnits();
            if (!radarUnits.isEmpty()) {
                poseStack.translate(centerX - (radarUnits.size() - 1) * 32, centerY, 0);
                float radius = 50.0f / radarUnits.size() / 0.6f;
                for (RadarUnit radarUnit : radarUnits) {
                    // 雷达
                    if (!radarUnit.isOn()) {
                        continue;
                    }
                    double maxScanDistance = radarUnit.getMaxScanDistance();
                    double yRotMin = radarUnit.getYRotMin();
                    double yRotMax = radarUnit.getYRotMax();
                    double yRotO = radarUnit.yRotO;
                    double yRot = radarUnit.getYRot();
                    int radarColor = 0x4433FF33;
                    int lineColor = 0xFF00FF00;
                    Matrix4f matrix = poseStack.last().pose();
                    // 扫描扇区
                    if (yRotMax - yRotMin >= 360) {
                        yRotMin = 0;
                        yRotMax = 360;
                    } else {
                        drawRotatedText(guiGraphics, yRotMin + "°", -8, 0, radius + 8, (float) -yRotMin, Color.GREEN);
                        drawRotatedText(guiGraphics, yRotMax + "°", 8, 0, radius + 8, (float) -yRotMax, Color.GREEN);
                    }
                    drawRadarSector(matrix, 0, 0, radius, (float) yRotMin, (float) yRotMax, 32, radarColor);
                    drawRotatedText(guiGraphics, maxScanDistance + " m", 0, 0, radius + 8, 0, Color.GREEN);
                    // 扫描线
                    if (radarUnit.getLockedEntity() == null) {
                        drawScanLine(matrix, 0, 0, radius, (float) Mth.lerp(partialTick, yRotO, yRot), 0.4f, lineColor);
                    }
                    radarUnit.getDetectedEntities().values().forEach(detectedObject -> {
                        Vec3 v = detectedObject.detectedPosition.subtract(radarUnit.worldRadarPosition());
                        v = vehicle.relativeRotDirection(v, true);
                        double l = v.length() / maxScanDistance * radius;
                        v = v.normalize().scale(-l);
                        // 目标
                        drawTarget(guiGraphics, v.x, v.z, 1, Color.GREEN, detectedObject, radarUnit);
                        // 锁定线
                        Entity lockedEntity = radarUnit.getLockedEntity();
                        if (lockedEntity != null && lockedEntity == detectedObject.entity) {
                            RenderHelper.drawLine(poseStack, v, 0.5f, Color.GREEN, 3, 1);
                        }
                    });
                    poseStack.translate(-radius * Math.sin(yRotMax) * 1.3f, 0, 0);
                }
            }
        }
        poseStack.popPose();
        // RWR
        WarningReceiver warningReceiver = vehicle.warningReceiver;
        if (warningReceiver != null) {
            poseStack.pushPose();
            {
                poseStack.pushPose();
                {
                    poseStack.translate((float) screenWidth / 2, (float) screenHeight / 2, 0);
                    if (warningReceiver.targets.values().stream().anyMatch(warnTarget -> warnTarget.warnType() == WarnType.MISSILE_LAUNCH)) {
                        guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("ui.missile_incoming"), 0, -55, Color.RED);
                    } else if (warningReceiver.targets.values().stream().anyMatch(warnTarget -> warnTarget.warnType() == WarnType.RADAR_LOCK)) {
                        guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("ui.being_locked"), 0, -55, Color.RED);
                    }
                }
                poseStack.popPose();
                poseStack.translate(centerX, centerY - 100, 0);
                // RWR
                poseStack.pushPose();
                {
                    poseStack.scale(0.8f, 0.8f, 0.8f);
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, "RWR", 0, -40, Color.GREEN);
                }
                poseStack.popPose();
                GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 3, Color.GREEN, 0.1f, 0, 0);
                GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 25, Color.GREEN, 0.01f, 0, 0);
                GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 20, Color.GREEN, 0.01f, 0, 0);
                GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 25, 0x33000000, 1f, 0, 0);
                for (Map.Entry<Integer, WarningReceiver.WarnTarget> warnTargetEntry : warningReceiver.targets.entrySet()) {
                    Entity entity = LocalVehiclePlayer.instance.getPlayer().level().getEntity(warnTargetEntry.getKey());
                    WarningReceiver.WarnTarget warnTarget = warnTargetEntry.getValue();
                    if (entity == null) {
                        LocalVehiclePlayer.ServerEntity serverEntity = LocalVehiclePlayer.instance.serverEntities.get(warnTargetEntry.getKey());
                        if (serverEntity == null || serverEntity.entity == null) {
                            continue;
                        }
                        entity = serverEntity.entity;
                    }
                    double distance = entity.position().distanceTo(vehicle.position());
                    double scale = Math.min(1, distance / 512);
                    Vec3 direction = vehicle
                            .relativeRotDirection(entity.position().subtract(vehicle.position()), true)
                            .normalize()
                            .scale(scale * -25);
                    poseStack.pushPose();
                    {
                        if (warnTarget.warnType() ==  WarnType.RADAR_LOCK || warnTarget.warnType() == WarnType.MISSILE_LAUNCH) {
                            RenderHelper.drawLine(poseStack, direction, 0.8f, Color.GREEN, 3, 1);
                        }
                        direction = direction.normalize().scale(scale * 28);
                        poseStack.translate(direction.x, direction.z, 0);
                        poseStack.scale(0.6f, 0.6f, 0.6f);
                        String info = warnTargetEntry.getValue().info();
                        int color = Color.GREEN;
                        if (warnTarget.warnType() == WarnType.RADAR_SEARCH) {
                            float timeDiff = (float) (System.currentTimeMillis() - warnTarget.receivedTime());
                            float alpha = 1.0f - Math.min(1000f, timeDiff) / 1000f;
                            int alphaInt = Math.max(4, (int) (alpha * 255));
                            color = (alphaInt << 24) | (Color.GREEN & 0x00FFFFFF);
                        }
                        guiGraphics.drawCenteredString(Minecraft.getInstance().font, StringUtils.isEmpty(info) ? "?" : info, 0, 0, color);
                    }
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
    }

    public void drawTarget(GuiGraphics guiGraphics, double x, double y, int r, int color, RadarUnit.DetectedObject detectedObject, RadarUnit radarUnit) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(x - r, y - r, 0);
            guiGraphics.fill(0, 0, r * 2, r * 2, color);
            if (!(detectedObject.entity instanceof MissileEntity)) {
                guiGraphics.vLine(-2, -2, 3, color);
                guiGraphics.vLine(3, -2, 3, color);
                Team team = detectedObject.entity.getTeam();
                if (team != null && team.isAlliedTo(LocalVehiclePlayer.instance.getPlayer().getTeam())) {
                    guiGraphics.hLine(-2, 3, -3, color);
                }
            }
            poseStack.translate(1, 1, 0);
            Vec3 velocity = detectedObject.entity.getDeltaMovement();
            if (velocity.length() != 0) {
                Vec3 direction = velocity.normalize().scale(-5);
                direction = radarUnit.getVehicle().relativeRotDirection(direction, true);
                RenderHelper.drawLine(poseStack, direction, 0.8f, color, -1, -1);
            }
        }
        poseStack.popPose();
    }

    private void drawRotatedText(GuiGraphics guiGraphics, String text, int cx, int cy, float r, float angleDeg, int color) {
        Font font = Minecraft.getInstance().font;
        float rad = -(float) Math.toRadians(angleDeg + 90);
        float x = cx + (float) Math.cos(rad) * r;
        float y = cy + (float) Math.sin(rad) * r;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(x, y, 0);
            poseStack.scale(0.8f, 0.8f, 0.8f);
            guiGraphics.drawCenteredString(font, text, 0, 0, color);
        }
        poseStack.popPose();
    }

    /**
     * 绘制扇形区域
     */
    private void drawRadarSector(Matrix4f matrix, int cx, int cy, float r, float startDeg, float endDeg, int segments, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();

        bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // 颜色拆解
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r_col = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        // 中心点
        bufferbuilder.vertex(matrix, (float)cx, (float)cy, 0).color(r_col, g, b, a).endVertex();

        for (int i = 0; i <= segments; i++) {
            float angle = startDeg + (endDeg - startDeg) * ((float)i / segments);
            float rad = -(float) Math.toRadians(angle + 90);
            float x = cx + (float)Math.cos(rad) * r;
            float y = cy + (float)Math.sin(rad) * r;
            bufferbuilder.vertex(matrix, x, y, 0).color(r_col, g, b, a).endVertex();
        }

        tesselator.end();
        RenderSystem.disableBlend();
    }

    /**
     * 绘制扫描线
     */
    private void drawScanLine(Matrix4f matrix, int cx, int cy, float r, float angleDeg, float thickness, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();

        // 使用 QUADS (四边形) 模式来模拟粗线
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float a = (float)(color >> 24 & 255) / 255.0F;
        float r_col = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        // 计算方向弧度
        float rad = (float) Math.toRadians(angleDeg - 90);

        // 方向向量 (cos, sin)
        float dirX = (float) Math.cos(rad);
        float dirY = (float) Math.sin(rad);

        // 垂直向量 (-sin, cos) 用于计算厚度偏移
        float perpX = (float) -Math.sin(rad);
        float perpY = (float) Math.cos(rad);

        float halfWidth = thickness / 2.0f;

        // 四个顶点的坐标逻辑：
        // A: 中心点左偏移 | B: 中心点右偏移
        // C: 终点右偏移   | D: 终点左偏移

        // 点 A (靠近圆心左侧)
        bufferbuilder.vertex(matrix, cx - perpX * halfWidth, cy - perpY * halfWidth, 0).color(r_col, g, b, a).endVertex();
        // 点 B (靠近圆心右侧)
        bufferbuilder.vertex(matrix, cx + perpX * halfWidth, cy + perpY * halfWidth, 0).color(r_col, g, b, a).endVertex();
        // 点 C (靠近边缘右侧)
        bufferbuilder.vertex(matrix, cx + dirX * r + perpX * halfWidth, cy + dirY * r + perpY * halfWidth, 0).color(r_col, g, b, a).endVertex();
        // 点 D (靠近边缘左侧)
        bufferbuilder.vertex(matrix, cx + dirX * r - perpX * halfWidth, cy + dirY * r - perpY * halfWidth, 0).color(r_col, g, b, a).endVertex();

        tesselator.end();
    }

}
