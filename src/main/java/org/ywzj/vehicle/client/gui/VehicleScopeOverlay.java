package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.render.util.GuiHelper;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.AmmoEntity;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.util.VehicleHudText;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.RadarUnit;
import org.ywzj.vehicle.vehicle.part.RotatableUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;
import org.ywzj.vehicle.vehicle.weapon.VehicleCannon;
import org.ywzj.vehicle.vehicle.weapon.VehicleMissile;
import org.ywzj.vehicle.vehicle.weapon.VehicleMultiWeapons;

import java.util.Locale;

import static org.ywzj.vehicle.util.RenderHelper.drawRectByCorner;

public class VehicleScopeOverlay implements IGuiOverlay {

    private static final float[] CRT_RETICLE_SEGMENTS = {
            -90, 0, -50, 0, 50, 0, 90, 0,
            0, -90, 0, -50, 0, 50, 0, 90,
            -24, 0, -6, 0, 6, 0, 24, 0,
            0, -24, 0, -6, 0, 6, 0, 24,
            -6, -1.5f, -6, 1.5f, 6, -1.5f, 6, 1.5f,
            -1.5f, -6, 1.5f, -6, -1.5f, 6, 1.5f, 6
    };
    private static final float[] CRT_FOCUS_LOCK_SEGMENTS = {
            -27, -18, -16, -10, 16, -10, 27, -18,
            -27, 18, -16, 10, 16, 10, 27, 18
    };

    public static double fov;
    public static int color = Color.GREEN;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!LocalVehiclePlayer.instance.onVehicle() || LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.vehicle;
        // 准心
        renderCrosshair(guiGraphics, partialTick, vehicle);
        // 射界
        renderWeaponEngagementEnvelope(guiGraphics, partialTick, screenWidth, screenHeight, vehicle);
        // 目标
        renderAimLockTarget(guiGraphics, partialTick);
        // 机身朝向
        renderVehicleHeading(guiGraphics, partialTick, screenWidth, screenHeight, vehicle);
    }

    public void renderVehicleHeading(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(screenWidth / 2f - 80f, screenHeight / 2f + 80f, 0f);
            float scale = (float) (5.0 / Math.max(4.0, vehicle.getStructureLength()));
            poseStack.scale(scale, scale, scale);
            float zRot = 0;
            Player player = LocalVehiclePlayer.instance.getPlayer();
            PartUnit<?> playerPartUnit = vehicle.getOwnOperatorUnit(player);
            if (playerPartUnit instanceof RotatableUnit<?> rotatableUnit) {
                zRot = lerpZRotDiff(rotatableUnit, vehicle, partialTick);
            }
            poseStack.mulPose(Axis.ZP.rotationDegrees(180 - zRot));
            Vec3 vehiclePos = new Vec3(Mth.lerp(partialTick, vehicle.xo, vehicle.getX()),
                    Mth.lerp(partialTick, vehicle.yo, vehicle.getY()),
                    Mth.lerp(partialTick, vehicle.zo, vehicle.getZ()));
            Vector3f axisX = new Vector3f(1, 0, 0);
            Vector3f axisZ = new Vector3f(0, 0, 1);
            Quaternionf rotation = new Quaternionf()
                    .rotateY((float) Math.toRadians(-vehicle.getViewYRot(partialTick)))
                    .rotateX((float) Math.toRadians(vehicle.getViewXRot(partialTick)))
                    .rotateZ((float) Math.toRadians(vehicle.getViewZRot(partialTick)));
            rotation.transform(axisX);
            rotation.transform(axisZ);
            for (VehicleCubeOBB vehicleCubeOBB : vehicle.getVehicleCubeOBBs()) {
                renderCubeOBB(vehicleCubeOBB, vehiclePos, axisX, axisZ, 0, poseStack, guiGraphics, partialTick, Color.GREEN);
            }
            for (PartUnit<?> partUnit : vehicle.getPartUnits()) {
                if (partUnit instanceof WeaponUnit weaponUnit && !weaponUnit.isSeat()) {
                    continue;
                }
                int color = partUnit.getOwner() == player ? Color.BLUE : Color.GREEN;
                for (VehicleCubeOBB partCubeOBB : partUnit.getPartCubeOBBs()) {
                    renderCubeOBB(partCubeOBB, vehiclePos, axisX, axisZ, lerpZRotDiff(partCubeOBB, vehicle, partialTick),
                            poseStack, guiGraphics, partialTick, partUnit.isDestroyed() ? Color.RED : color);
                }
                if (partUnit instanceof WeaponUnit weaponUnit) {
                    for (WeaponUnit subWeaponUnit : weaponUnit.getSubWeaponUnits()) {
                        for (VehicleCubeOBB partCubeOBB : subWeaponUnit.getPartCubeOBBs()) {
                            renderCubeOBB(partCubeOBB, vehiclePos, axisX, axisZ, lerpZRotDiff(partCubeOBB, vehicle, partialTick),
                                    poseStack, guiGraphics, partialTick, color);
                        }
                    }
                }
            }
        }
        poseStack.popPose();
    }

    private void renderCubeOBB(VehicleCubeOBB cubeOBB, Vec3 vehiclePos, Vector3f axisX, Vector3f axisZ, float rot, PoseStack poseStack, GuiGraphics guiGraphics, float partialTick, int color) {
        Vec3 cubePos = new Vec3(Mth.lerp(partialTick, cubeOBB.positionO.x, cubeOBB.position.x),
                Mth.lerp(partialTick, cubeOBB.positionO.y, cubeOBB.position.y),
                Mth.lerp(partialTick, cubeOBB.positionO.z, cubeOBB.position.z));
        Vec3 offset = cubePos.subtract(vehiclePos);
        float offsetX = (float) ((offset.x * axisX.x() + offset.y * axisX.y() + offset.z * axisX.z()) * 10.0);
        float offsetZ = (float) ((offset.x * axisZ.x() + offset.y * axisZ.y() + offset.z * axisZ.z()) * 10.0);
        poseStack.pushPose();
        {
            poseStack.translate(offsetX, offsetZ, 0f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
            int hw = (int) (cubeOBB.width * 5.0);
            int hd = (int) (cubeOBB.depth * 5.0);
            drawRectByCorner(guiGraphics, -hw, hw, -hd, hd, color, 1);
        }
        poseStack.popPose();
    }

    public void renderCrosshair(GuiGraphics guiGraphics, float partialTick, AbstractVehicle vehicle) {
        if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
            if (weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.OPTICAL_SCOPE) {
                GuiHelper.drawOpticalScope(guiGraphics, guiGraphics.guiWidth(), guiGraphics.guiHeight());
            }
            Vec3 posO = VectorUtil.worldToScreen(weaponUnit.weaponHitPosO);
            Vec3 pos = VectorUtil.worldToScreen(weaponUnit.weaponHitPos);
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            {
                poseStack.translate(
                        Mth.lerp(partialTick, posO.x, pos.x),
                        Mth.lerp(partialTick, posO.y, pos.y),
                        0);
                if (weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.CRT) {
                    boolean stabilizerOn = weaponUnit.withStabilizer() && !weaponUnit.isDestroyed()
                            && (!weaponUnit.needPower || vehicle.hasPower());
                    boolean focusLocked = weaponUnit.withFocusLocker() && weaponUnit.getFocusLockPos() != null;
                    renderCrtReticle(guiGraphics, focusLocked);
                    float sightScale = guiGraphics.guiHeight() / VehicleHudLayout.HEIGHT;
                    renderSightText(guiGraphics, (LocalVehiclePlayer.instance.outOfRangeFinding ? ">" : "") + (int) LocalVehiclePlayer.instance.aimLocationDistance + " m", 0, 33 * sightScale, true);
                    renderSightText(guiGraphics, "x" + String.format(Locale.ROOT, "%.1f", weaponUnit.getZoom()), 44 * sightScale, 14 * sightScale, false);
                    renderSightText(guiGraphics, stabilizerOn ? Component.translatable("ui.stabilizer_on").getString() : "", 44 * sightScale, 28 * sightScale, false);
                    if (focusLocked) {
                        renderSightText(guiGraphics, Component.translatable("ui.focus_lock").getString(), 44 * sightScale, 42 * sightScale, false);
                    }
                } else if (weaponUnit.getOpticalSightType() != WeaponUnitData.OpticalSightType.OPTICAL_SCOPE) {
                    poseStack.pushPose();
                    {
                        poseStack.translate(-0.5, -0.5, 0);
                        RenderHelper.drawRect(guiGraphics, 0, 0, 1, 1, color, 1f);
                    }
                    poseStack.popPose();
                }
                // 装填进度
                weaponUnit.getCurrentWeapon().ifPresent(weapon -> VehicleAimAtOverlay.renderReloadProgress(guiGraphics, weapon, 7f, 0.9f, partialTick));
                weaponUnit.getCurrentSecondaryWeapon().ifPresent(weapon -> VehicleAimAtOverlay.renderReloadProgress(guiGraphics, weapon, 5.6f, 0.75f, partialTick));
                if (!weaponUnit.independentWeapons.isEmpty()) {
                    VehicleAimAtOverlay.renderReloadProgress(guiGraphics, weaponUnit.independentWeapons.get(0), 4.4f, 0.6f, partialTick);
                }
            }
            poseStack.popPose();
        }
    }

    private static void renderCrtReticle(GuiGraphics guiGraphics, boolean focusLocked) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        try {
            float scale = guiGraphics.guiHeight() / VehicleHudLayout.HEIGHT;
            float halfWidth = Math.max(0.5f, 0.75f / (scale * (float) Minecraft.getInstance().getWindow().getGuiScale()));
            poseStack.scale(scale, scale, 1);
            renderCrtSegments(guiGraphics, CRT_RETICLE_SEGMENTS, halfWidth);
            if (focusLocked) {
                renderCrtSegments(guiGraphics, CRT_FOCUS_LOCK_SEGMENTS, halfWidth);
            }
            guiGraphics.flush();
        } finally {
            poseStack.popPose();
        }
    }

    private static void renderCrtSegments(GuiGraphics guiGraphics, float[] segments, float halfWidth) {
        PoseStack poseStack = guiGraphics.pose();
        for (int i = 0; i < segments.length; i += 4) {
            float x = segments[i];
            float y = segments[i + 1];
            float dx = segments[i + 2] - x;
            float dy = segments[i + 3] - y;
            float length = (float) Math.hypot(dx, dy);
            poseStack.pushPose();
            try {
                poseStack.translate(x, y, 0);
                poseStack.mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), 0, -halfWidth, length, halfWidth, 0, color);
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static void renderSightText(GuiGraphics guiGraphics, String text, float x, float y, boolean centered) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(x, y, 0);
            float scale = guiGraphics.guiHeight() / VehicleHudLayout.HEIGHT * 0.9f;
            poseStack.scale(scale, scale, 1);
            if (centered) {
                VehicleHudText.drawCenteredString(guiGraphics, text, 0, 0, color);
            } else {
                VehicleHudText.drawString(guiGraphics, text, 0, 0, color);
            }
        } finally {
            poseStack.popPose();
        }
    }

    public void renderWeaponEngagementEnvelope(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        // 射击诸元
        if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
            weaponUnit.getCurrentWeapon().ifPresent(weapon -> {
                var selectedWeapon = weapon instanceof VehicleMultiWeapons multiWeapons ? multiWeapons.getSelectedWeapon() : weapon;
                if (selectedWeapon instanceof VehicleCannon cannon && cannon.getData().isArtillery()) {
                    WeaponUnit aimingWeaponUnit = weapon.getWeaponUnit();
                    if (aimingWeaponUnit.isParentWeaponUnitAim()) {
                        aimingWeaponUnit = aimingWeaponUnit.getRootParentWeaponUnit();
                    }
                    AimContext aimContext = aimingWeaponUnit.aimContext();
                    float azimuth = Mth.wrapDegrees(aimContext.direction.y);
                    double range = weaponUnit.weaponHitRange;
                    int x = screenWidth / 2 - 120;
                    int y = screenHeight / 2 - 69;

                    VehicleHudText.drawString(guiGraphics, Component.translatable("ui.artillery.azimuth", String.format(Locale.ROOT, "%.1f", azimuth)), x, y, color);
                    VehicleHudText.drawString(guiGraphics, Component.translatable("ui.artillery.elevation", String.format(Locale.ROOT, "%.1f", -aimContext.direction.x)), x, y + 12, color);
                    VehicleHudText.drawString(guiGraphics, Component.translatable("ui.artillery.range", Double.isFinite(range) ? String.format(Locale.ROOT, "%.0f", range) : "--"), x, y + 24, color);
                }
            });
        }
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(centerX, centerY, 0);
            if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit
                    && weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.CRT) {
                weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon -> {
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                    int baseX = 0;
                    int baseY = 140;
                    float yRotRange = weaponUnit.getYRotMax() - weaponUnit.getYRotMin();
                    if (yRotRange <= 0 || yRotRange >= 360 || weaponUnit.yRotSpeed == 0) {
                        return;
                    }
                    // 光瞄视野框
                    drawOpticalSightFrame(guiGraphics,
                            (int) (baseX + weaponUnit.getYRotMin() - weaponUnit.ySelfRot),
                            (int) (baseX + weaponUnit.getYRotMax() - weaponUnit.ySelfRot),
                            (int) (baseY + weaponUnit.getXRotMin() - weaponUnit.xSelfRot),
                            (int) (baseY + weaponUnit.getXRotMax() - weaponUnit.xSelfRot),
                            color);
                    if (vehicleWeapon instanceof VehicleMissile vehicleMissile) {
                        VehicleMissileWeaponData vehicleMissileWeaponData = vehicleMissile.getData();
                        // 导弹射界框
                        drawMissileEngagementFrame(guiGraphics,
                                (int) (baseX + vehicleMissileWeaponData.getYRotMin() - weaponUnit.ySelfRot),
                                (int) (baseX + vehicleMissileWeaponData.getYRotMax() - weaponUnit.ySelfRot),
                                (int) (baseY + Math.max(vehicleMissileWeaponData.getXRotMin(), weaponUnit.getXRotMin()) - weaponUnit.xSelfRot),
                                (int) (baseY + Math.min(vehicleMissileWeaponData.getXRotMax(), weaponUnit.getXRotMax()) - weaponUnit.xSelfRot),
                                color);
                    }
                    int x = (int) Mth.lerp(partialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                    int y = (int) Mth.lerp(partialTick, weaponUnit.xRotO, weaponUnit.getXRot());
                    // 光瞄指向十字
                    guiGraphics.fill(baseX + x, baseY + y - 8, baseX + x + 1, baseY + y - 2, color);
                    guiGraphics.fill(baseX + x, baseY + y + 3, baseX + x + 1, baseY + y + 9, color);
                    guiGraphics.fill(baseX + x - 8, baseY + y, baseX + x - 2, baseY + y + 1, color);
                    guiGraphics.fill(baseX + x + 3, baseY + y, baseX + x + 9, baseY + y + 1, color);
                });
            }
        }
        poseStack.popPose();
    }

    private static void drawOpticalSightFrame(GuiGraphics guiGraphics, int left, int right, int top, int bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        int horizontal = Math.max(1, (right - left) / 5);
        int vertical = Math.max(1, (bottom - top) / 5);
        guiGraphics.hLine(left, left + horizontal, top, color);
        guiGraphics.vLine(left, top, top + vertical, color);
        guiGraphics.hLine(right - horizontal, right, top, color);
        guiGraphics.vLine(right, top, top + vertical, color);
        guiGraphics.hLine(left, left + horizontal, bottom, color);
        guiGraphics.vLine(left, bottom - vertical, bottom, color);
        guiGraphics.hLine(right - horizontal, right, bottom, color);
        guiGraphics.vLine(right, bottom - vertical, bottom, color);
    }

    private static void drawMissileEngagementFrame(GuiGraphics guiGraphics, int left, int right, int top, int bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        int thickness = Math.min(2, Math.min(right - left, bottom - top));
        guiGraphics.fill(left, top, right, top + thickness, color);
        guiGraphics.fill(left, bottom - thickness, right, bottom, color);
        guiGraphics.fill(left, top, left + thickness, bottom, color);
        guiGraphics.fill(right - thickness, top, right, bottom, color);
    }

    public static void renderAimLockTarget(GuiGraphics guiGraphics, float partialTick) {
        WeaponUnit weaponUnit = LocalVehiclePlayer.instance.getWeaponUnit();
        if (weaponUnit == null) {
            return;
        }
        WeaponUnitData.FireControlSensorType sensorType = weaponUnit.getFireControlSensorType();
        // 武器站锁定目标
        if (weaponUnit.getLockedEntity() != null) {
            Entity entity = weaponUnit.getLockedEntity();
            double curX = Mth.lerp(partialTick, entity.xo, entity.getX());
            double curY = Mth.lerp(partialTick, entity.yo, entity.getY());
            double curZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
            Vec3 centerOffset = entity.getBoundingBox().getCenter().subtract(entity.position());
            Vec3 targetPosition = new Vec3(curX, curY, curZ).add(centerOffset);
            Vec3 screenPos = VectorUtil.worldToScreen(targetPosition);
            if (screenPos.z >= 0) {
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                {
                    poseStack.translate(screenPos.x, screenPos.y, 0);
                    if (weaponUnit.isSeekerOn()) {
                        // 导引头小圈
                        if (weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR) {
                            GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 15, Color.RED, 0.03f, 0, 0);
                        } else if (weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.RF) {
                            GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 5, Color.RED, 0.05f, 0, 0);
                            GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 4, Color.RED, 0.06f, 0, 0);
                        }
                    }
                    // 光电锁定
                    if (sensorType == WeaponUnitData.FireControlSensorType.EO) {
                        RenderHelper.drawSquare(guiGraphics, 0, 0, 15, Color.GREEN);
                    }
                }
                poseStack.popPose();
            }
        }
        // 雷达锁定目标
        RadarUnit mainRadarUnit = weaponUnit.getMainRadarUnit();
        if (mainRadarUnit != null) {
            if (sensorType == WeaponUnitData.FireControlSensorType.RF && mainRadarUnit.getLockedEntity() != null) {
                RadarUnit.DetectedObject detectedObject = weaponUnit.getMainRadarUnit().getDetectedEntities().get(mainRadarUnit.getLockedEntity().getId());
                if (detectedObject != null) {
                    Vec3 screenPos = VectorUtil.worldToScreen(detectedObject.entity.getBoundingBox().getCenter());
                    if (screenPos.z >= 0) {
                        PoseStack poseStack = guiGraphics.pose();
                        poseStack.pushPose();
                        {
                            poseStack.translate(screenPos.x, screenPos.y, 0);
                            RenderHelper.drawSquare(guiGraphics, 0, 0, 15, Color.GREEN);
                            RenderHelper.drawSquare(guiGraphics, 0, 0, 10, Color.GREEN);
                            alliesInfo(guiGraphics, detectedObject);
                            radarInfo(guiGraphics, poseStack, detectedObject);
                        }
                        poseStack.popPose();
                    }
                }
            }
            // 雷达可锁定的目标
            for (RadarUnit.DetectedObject detectedObject : weaponUnit.getRadarDetectedEntities()) {
                Entity lockedEntity = mainRadarUnit.getLockedEntity();
                if (lockedEntity != null && detectedObject.entity.getId() == lockedEntity.getId()) {
                    continue;
                }
                Vec3 screenPos = VectorUtil.worldToScreen(detectedObject.detectedPosition);
                if (screenPos.z < 0) {
                    continue;
                }
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                {
                    poseStack.translate(screenPos.x, screenPos.y, 0);
                    if (detectedObject.entity instanceof AmmoEntity) {
                        RenderHelper.drawSquareCorners(guiGraphics, 0, 0, 10, 3, Color.GREEN);
                    } else {
                        RenderHelper.drawSquareCorners(guiGraphics, 0, 0, 15, 5, Color.GREEN);
                        alliesInfo(guiGraphics, detectedObject);
                        radarInfo(guiGraphics, poseStack, detectedObject);
                    }
                }
                poseStack.popPose();
            }
        }
    }

    private static void radarInfo(GuiGraphics guiGraphics, PoseStack poseStack, RadarUnit.DetectedObject detectedObject) {
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.vehicle;
        if (vehicle == null) {
            return;
        }
        Vec3 velocity = detectedObject.entity.getDeltaMovement();
        // 速度线
        poseStack.pushPose();
        {
            poseStack.translate(0, 12, 0);
            RenderHelper.drawSquare(guiGraphics, 0, 0, 4, Color.GREEN);
            if (velocity.length() != 0) {
                poseStack.translate(0.5, 0, 0);
                Vec3 direction = velocity.normalize().scale(-8);
                direction = vehicle.relativeRotDirection(direction, true);
                RenderHelper.drawLine(poseStack, direction, 1f, color, -1, -1);
            }
        }
        poseStack.popPose();
        poseStack.pushPose();
        {
            if (detectedObject.entity instanceof AmmoEntity) {
                poseStack.translate(8, 12, 0);
            } else {
                poseStack.translate(12, -12, 0);
            }
            poseStack.scale(0.8f, 0.8f, 0.8f);
            // 距离
            double distance = detectedObject.detectedPosition.distanceTo(vehicle.position());
            VehicleHudText.drawString(guiGraphics, String.format("%.2f m", distance), 0, 0, Color.GREEN, false);
            poseStack.translate(0, 12, 0);
            // 接近率
            Vec3 approach = velocity.subtract(vehicle.getDeltaMovement());
            Vec3 relative = detectedObject.entity.position().subtract(vehicle.position());
            int sig = approach.dot(relative) > 0 ? -1 : 1;
            double approachRate = sig * approach.length() * 20;
            VehicleHudText.drawString(guiGraphics, String.format("%.2f m/s", approachRate), 0, 0, Color.GREEN, false);
        }
        poseStack.popPose();
    }

    private static void alliesInfo(GuiGraphics guiGraphics, RadarUnit.DetectedObject detectedObject) {
        Team team = detectedObject.entity.getTeam();
        // 友军标记
        if (team != null && team.isAlliedTo(LocalVehiclePlayer.instance.getPlayer().getTeam())) {
            Integer teamColor = team.getColor().getColor();
            if (teamColor == null) {
                teamColor = color;
            } else {
                teamColor = 0xFF000000 | teamColor;
            }
            guiGraphics.hLine(-6, 6, -11, teamColor);
        }
    }

    private float lerpZRotDiff(RotatableUnit<?> rotatableUnit, AbstractVehicle vehicle, float partialTick) {
        float zRot = Mth.wrapDegrees(rotatableUnit.worldRot().y - vehicle.getYRot());
        float zRotO = Mth.wrapDegrees(rotatableUnit.worldRot(rotatableUnit.xRotO, rotatableUnit.yRotO).y - vehicle.getYRot());
        float zRotDiff = Mth.wrapDegrees(zRot - zRotO);
        return Mth.lerp(partialTick, zRot - zRotDiff, zRot);
    }

    private float lerpZRotDiff(VehicleCubeOBB cubeOBB, AbstractVehicle vehicle, float partialTick) {
        if (cubeOBB.rotationO == null || cubeOBB.rotation == null) {
            return 0;
        }
        Quaternionf rotation = new Quaternionf(cubeOBB.rotationO).slerp(cubeOBB.rotation, partialTick);
        Vector3f forward = rotation.transform(new Vector3f(0, 0, 1));
        float worldYRot = VectorUtil.vecToRot(new Vec3(forward)).y;
        return Mth.wrapDegrees(worldYRot - vehicle.getViewYRot(partialTick));
    }

}
