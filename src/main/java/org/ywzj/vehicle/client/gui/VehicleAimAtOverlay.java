package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.render.util.GuiHelper;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.entity.weapon.AmmoEntity;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.util.VehicleHudText;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.RadarUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.weapon.VehicleMultiWeapons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static org.ywzj.vehicle.all.AllKeys.FREE_CAMERA;
import static org.ywzj.vehicle.util.RenderHelper.*;

public class VehicleAimAtOverlay implements IGuiOverlay {

    private static final Map<AbstractVehicleWeapon<?>, ReloadAnimation> reloadAnimations = new WeakHashMap<>();

    private static final class ReloadAnimation {
        int remainingTicks;
        int totalTicks;
        double sampleTick;
        double lastTick;
        float progress;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            boolean isScope = LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE;
            boolean isHelicopter = vehicle instanceof RotaryWingVehicle;
            float markerScale = isHelicopter || vehicle instanceof FixedWingVehicle
                    ? guiGraphics.guiHeight() / VehicleHudLayout.HEIGHT * 1.2f : 1;
            int color = isHelicopter ? Color.GREEN : Color.WHITE;
            PartUnit<?> operatorUnit = vehicle.getOwnOperatorUnit(player);
            if (operatorUnit instanceof WeaponUnit weaponUnit) {
                Vec3 aimScreenPos = aimScreenPosition(weaponUnit, partialTick);
                if (aimScreenPos.z >= 0) {
                    PoseStack poseStack = guiGraphics.pose();
                    poseStack.pushPose();
                    {
                        poseStack.translate(aimScreenPos.x, aimScreenPos.y, 0);
                        poseStack.scale(markerScale, markerScale, 1);
                        float alpha = !isScope && FREE_CAMERA.isDown() ? 0.25f : 0.5f;
                        int aimCircleColor = (color & 0x00FFFFFF) | ((int) (alpha * 255) << 24);
                        GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 8, aimCircleColor, 0.05f, 0, 0);
                        // 装填进度
                        weaponUnit.getCurrentWeapon().ifPresent(weapon -> renderReloadProgress(guiGraphics, weapon, 7f, 0.9f, partialTick));
                        weaponUnit.getCurrentSecondaryWeapon().ifPresent(weapon -> renderReloadProgress(guiGraphics, weapon, 5.6f, 0.75f, partialTick));
                        if (!weaponUnit.independentWeapons.isEmpty()) {
                            renderReloadProgress(guiGraphics, weaponUnit.independentWeapons.get(0), 4.4f, 0.6f, partialTick);
                        }
                    }
                    poseStack.popPose();
                }
                weaponUnit.getCurrentWeapon().ifPresent(weapon -> {
                    List<AbstractVehicleWeapon<?>> aimingWeapons = new ArrayList<>();
                    aimingWeapons.add(weapon);
                    if (weaponUnit.getFiringMode() == WeaponUnitData.FiringMode.FULL_SALVO) {
                        aimingWeapons.addAll(weaponUnit.fullSalvoWeapons);
                    }
                    if (!isScope) {
                        // 落点
                        for (AbstractVehicleWeapon<?> aimingWeapon : aimingWeapons) {
                            WeaponUnit aimingWeaponUnit = aimingWeapon.getWeaponUnit();
                            WeaponUnitData.CrosshairStyle crosshairStyle = aimingWeaponUnit.crosshairStyle;
                            if (crosshairStyle != null) {
                                if (aimingWeaponUnit.isParentWeaponUnitAim()
                                        || (aimingWeaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.CCIP && aimingWeaponUnit.getCurrentWeapon().isEmpty())) {
                                    aimingWeaponUnit = weaponUnit;
                                }
                                Vec3 weaponHitPos = aimingWeaponUnit.weaponHitPos;
                                Vec3 weaponHitPosO = aimingWeaponUnit.weaponHitPosO;
                                if (weaponHitPos != null && weaponHitPosO != null) {
                                    Vec3 screenHitPos = VectorUtil.worldToScreen(weaponHitPos);
                                    Vec3 screenHitPosO = VectorUtil.worldToScreen(weaponHitPosO);
                                    if (screenHitPos.z >= 0) {
                                        double x = Mth.lerp(partialTick, screenHitPosO.x, screenHitPos.x);
                                        double y = Mth.lerp(partialTick, screenHitPosO.y, screenHitPos.y);
                                        PoseStack poseStack = guiGraphics.pose();
                                        poseStack.pushPose();
                                        {
                                            poseStack.translate(x, y, 0);
                                            poseStack.scale(markerScale, markerScale, 1);
                                            drawCrosshair(guiGraphics, aimingWeapon.getWeaponUnit().getFireControlSensorType() == WeaponUnitData.FireControlSensorType.CCIP ? Color.GREEN : color, poseStack, crosshairStyle);
                                        }
                                        poseStack.popPose();
                                    }
                                }
                            }
                        }
                    }
                    // 导引头
                    if (weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR
                            || weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.RF) {
                        if (weaponUnit.isSeekerOn()) {
                            WeaponUnit currentWeaponUnit = weapon.getWeaponUnit();
                            Vec3 screenHitPos = VectorUtil.worldToScreen(currentWeaponUnit.weaponHitPos);
                            Vec3 screenHitPosO = VectorUtil.worldToScreen(currentWeaponUnit.weaponHitPosO);
                            if (screenHitPos.z >= 0) {
                                double x = Mth.lerp(partialTick, screenHitPosO.x, screenHitPos.x);
                                double y = Mth.lerp(partialTick, screenHitPosO.y, screenHitPos.y);
                                PoseStack poseStack = guiGraphics.pose();
                                poseStack.pushPose();
                                {
                                    poseStack.translate(x, y, 0);
                                    poseStack.scale(markerScale, markerScale, 1);
                                    float alpha = 0.4f;
                                    // 导引头冷却中提示
                                    alpha += (float) Math.sin((double) weaponUnit.getLockCoolingTick() / 5 * Math.PI) * 0.2f;
                                    int aimCircleColor = (color & (weaponUnit.getLockedEntity() == null ? 0x00FFFFFF : 0x00FF0000)) | ((int) (alpha * 255) << 24);
                                    // 导引头小圈
                                    if (weaponUnit.getLockedEntity() == null) {
                                        if (weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR) {
                                            GuiHelper.drawCircle(poseStack, 0, 0, 15, aimCircleColor, 0.03f, 0, 0);
                                        } else if (weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.RF) {
                                            GuiHelper.drawCircle(poseStack, 0, 0, 5, aimCircleColor, 0.05f, 0, 0);
                                            GuiHelper.drawCircle(poseStack, 0, 0, 4, aimCircleColor, 0.06f, 0, 0);
                                        }
                                    }
                                    // 导引头大圈
                                    GuiHelper.drawCircle(poseStack, 0, 0, 2.8f * currentWeaponUnit.getSeekerFov(), aimCircleColor, 0.01f, 0, 0);
                                }
                                poseStack.popPose();
                            }
                        }
                    }
                });
                // 目标
                renderAimLockTarget(guiGraphics, partialTick);
            }
        }
    }

    private static void renderAimLockTarget(GuiGraphics guiGraphics, float partialTick) {
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
                RenderHelper.drawLine(poseStack, direction, 1f, Color.GREEN, -1, -1);
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
                teamColor = Color.GREEN;
            } else {
                teamColor = 0xFF000000 | teamColor;
            }
            guiGraphics.hLine(-6, 6, -11, teamColor);
        }
    }

    public static void renderReloadProgress(GuiGraphics guiGraphics, AbstractVehicleWeapon<?> weapon, float arcRadius, float thickness, float partialTick) {
        float progress = reloadProgress(weapon, partialTick);
        if (progress >= 0) {
            GuiHelper.drawArc(guiGraphics, 0, 0, arcRadius, thickness, 0f, 1f, Color.RELOAD_ARC_BG);
            GuiHelper.drawArc(guiGraphics, 0, 0, arcRadius, thickness, 0f, progress, Color.RELOAD_ARC);
        }
    }

    private static float reloadProgress(AbstractVehicleWeapon<?> weapon, float partialTick) {
        while (weapon instanceof VehicleMultiWeapons multi) {
            weapon = multi.getSelectedWeapon();
        }
        int remainingTicks = weapon.getReloadTime();
        int totalTicks = weapon.getData().getReload().getTime();
        Minecraft mc = Minecraft.getInstance();
        if (remainingTicks <= 0 || totalTicks <= 0 || mc.level == null) {
            reloadAnimations.remove(weapon);
            return -1;
        }
        double frameTick = (double) mc.level.getGameTime() + partialTick;
        ReloadAnimation animation = reloadAnimations.computeIfAbsent(weapon, key -> new ReloadAnimation());
        boolean reset = animation.totalTicks != totalTicks || remainingTicks > animation.remainingTicks
                || frameTick < animation.lastTick || frameTick - animation.lastTick > 20;
        if (reset || remainingTicks != animation.remainingTicks) {
            animation.remainingTicks = remainingTicks;
            animation.totalTicks = totalTicks;
            animation.sampleTick = frameTick;
        }
        if (reset) {
            animation.progress = 0;
        }
        double remaining = Math.max(0, remainingTicks - (frameTick - animation.sampleTick));
        animation.progress = Math.max(animation.progress, Mth.clamp((float) (1 - remaining / totalTicks), 0, 1));
        animation.lastTick = frameTick;
        return animation.progress;
    }

    public static Vec3 aimScreenPosition(WeaponUnit weaponUnit, float partialTick) {
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        if (instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
            return VectorUtil.worldToScreen(weaponUnit.aimAtPosition(partialTick));
        } else {
            Window window = Minecraft.getInstance().getWindow();
            double y = 0.5 - Math.tan(Math.toRadians(LocalVehiclePlayer.CAMERA_UPWARD_ANGLE)) * VectorUtil.projectionMatrix.m11() * 0.5;
            return new Vec3(window.getGuiScaledWidth() * 0.5, window.getGuiScaledHeight() * y, 1);
        }
    }

    private void drawCrosshair(GuiGraphics guiGraphics, int color, PoseStack poseStack, WeaponUnitData.CrosshairStyle crosshairStyle) {
        switch (crosshairStyle) {
            case CIRCLE -> GuiHelper.drawCircle(poseStack, 0, 0, 3, color, 0.05f, 0, 0);
            case SQUARE -> drawSquare(guiGraphics, 0 ,0, 5, color);
            case CROSS -> drawCrossDiagonal(guiGraphics, 0, 0, 8, 1, color);
            case BIG_CROSS -> drawCross(guiGraphics, 0, 0, 16, color);
        }
    }

}
