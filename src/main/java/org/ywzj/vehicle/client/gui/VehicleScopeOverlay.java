package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.render.util.GuiHelper;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.entity.weapon.AmmoEntity;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.RadarUnit;
import org.ywzj.vehicle.vehicle.part.RotatableUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;
import org.ywzj.vehicle.vehicle.weapon.VehicleMissile;

import static org.ywzj.vehicle.util.RenderHelper.drawRectByCorner;
import static org.ywzj.vehicle.util.RenderHelper.drawSquare;

public class VehicleScopeOverlay implements IGuiOverlay {

    public static double fov;
    public static int color = Color.GREEN;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!LocalVehiclePlayer.instance.onVehicle() || LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
        if (vehicle instanceof WheeledVehicle || vehicle instanceof TrackedVehicle) {
            renderGroundVehicle(guiGraphics, screenWidth, screenHeight, vehicle);
        } else if (vehicle instanceof RotaryWingVehicle rotaryWingVehicle) {
            renderHelicopter(guiGraphics, screenWidth, screenHeight, rotaryWingVehicle);
        }
        // 准心
        if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
            Vec3 posO = VectorUtil.worldToScreen(LocalVehiclePlayer.instance.weaponHitPosO);
            Vec3 pos = VectorUtil.worldToScreen(LocalVehiclePlayer.instance.weaponHitPos);
            guiGraphics.pose().pushPose();
            {
                guiGraphics.pose().translate(
                        Mth.lerp(partialTick, posO.x, pos.x),
                        Mth.lerp(partialTick, posO.y, pos.y),
                        0);
                if (weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.CRT) {
                    drawSquare(guiGraphics, 0, 0, 5, color);
                    guiGraphics.fill(0, -32, 1, -8, color);
                    guiGraphics.fill(0, 8, 1, 32, color);
                    guiGraphics.fill(-32, 0, -8, 1, color);
                    guiGraphics.fill(32, 0, 8, 1, color);
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                            (LocalVehiclePlayer.instance.outOfRangeFinding ? ">" : "")
                                    + (int) LocalVehiclePlayer.instance.aimLocationDistance + " m", 0, 40, color);
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, "x" + String.format("%.1f", weaponUnit.getZoom()), 32, 16, color);
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, weaponUnit.withStabilizer() ? Component.translatable("ui.stabilizer_on").getString() : "", 36, 28, color);
                } else {
                    RenderHelper.drawRect(guiGraphics, 0, 0, 1, 1, color, 1f);
                }
            }
            guiGraphics.pose().popPose();
        }
        // 目标
        renderAimLockTarget(guiGraphics, partialTick);
        // 机身朝向
        renderVehicleHeading(guiGraphics, screenWidth, screenHeight, vehicle);
    }

    public void renderVehicleHeading(GuiGraphics guiGraphics, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(centerX + 116, centerY + 80, 0);
            double length = Math.max(4, vehicle.getStructureLength());
            float scale = (float) (0.5f * 10 / length);
            poseStack.scale(scale, scale, scale);
            PartUnit<?> playerPartUnit = vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer());
            if (playerPartUnit instanceof RotatableUnit rotatableUnit) {
                float yRot = rotatableUnit.worldRot().y - vehicle.getYRot();
                poseStack.mulPose(Axis.ZP.rotationDegrees(-yRot));
            }
            int expansion = 10;
            for (VehicleCubeOBB vehicleCubeOBB : vehicle.getVehicleCubeOBBs()) {
                double offsetX = vehicleCubeOBB.offset().x * expansion;
                double offsetZ = vehicleCubeOBB.offset().z * expansion;
                poseStack.translate(-offsetX, -offsetZ, 0);
                int bodyCubeHalfWidth = (int) (vehicleCubeOBB.width / 2 * expansion);
                int bodyCubeHalfDepth = (int) (vehicleCubeOBB.depth / 2 * expansion);
                drawRectByCorner(guiGraphics, -bodyCubeHalfWidth, bodyCubeHalfWidth, -bodyCubeHalfDepth, bodyCubeHalfDepth, Color.GREEN, 1);
                poseStack.translate(offsetX, offsetZ, 0);
            }
            vehicle.getPartUnits().stream()
                    .filter(partUnit -> partUnit instanceof WeaponUnit weaponUnit
                            && weaponUnit.getParentWeaponUnit() == null
                            && weaponUnit.getBaseRotatableUnit() == null
                            && weaponUnit.isSeat())
                    .map(partUnit -> (WeaponUnit) partUnit)
                    .forEach(weaponUnit -> renderVehiclePart(guiGraphics, poseStack, weaponUnit, expansion));
        }
        poseStack.popPose();
    }

    public void renderVehiclePart(GuiGraphics guiGraphics, PoseStack poseStack, WeaponUnit weaponUnit, int expansion) {
        Vec3 pivotOffset = weaponUnit.getPivotOffset();
        if (weaponUnit.getBaseRotatableUnit() != null) {
            pivotOffset = pivotOffset.subtract(weaponUnit.getBaseRotatableUnit().getPivotOffset());
        }
        int color = Color.GREEN;
        if (weaponUnit.getOwner() == LocalVehiclePlayer.instance.getPlayer()) {
            color = Color.BLUE;
        }
        double pivotOffsetX = pivotOffset.x * expansion;
        double pivotOffsetY = pivotOffset.z * expansion;
        poseStack.translate(-pivotOffsetX, -pivotOffsetY, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(weaponUnit.getYRot()));
        for (VehicleCubeOBB partCubeOBB : weaponUnit.getPartCubeOBBs()) {
            Vec3 pivotOffset2 = weaponUnit.getPivotOffset();
            double offsetX = (partCubeOBB.offset().x - pivotOffset2.x) * expansion;
            double offsetZ = (partCubeOBB.offset().z - pivotOffset2.z) * expansion;
            poseStack.translate(-offsetX, -offsetZ, 0);
            int partHalfWidth = (int) (partCubeOBB.width / 2 * expansion);
            int partHalfDepth = (int) (partCubeOBB.depth / 2 * expansion);
            drawRectByCorner(guiGraphics, -partHalfWidth, partHalfWidth, -partHalfDepth, partHalfDepth, color, 1);
            poseStack.translate(offsetX, offsetZ, 0);
        }
        weaponUnit.getSubRotatableUnits().forEach(subRotatableUnit -> {
            if (subRotatableUnit.isSeat() && subRotatableUnit instanceof WeaponUnit subWeaponUnit) {
                renderVehiclePart(guiGraphics, poseStack, subWeaponUnit, expansion);
            }
        });
        poseStack.mulPose(Axis.ZP.rotationDegrees(-weaponUnit.getYRot()));
        poseStack.translate(pivotOffsetX, pivotOffsetY, 0);
    }

    public void renderGroundVehicle(GuiGraphics guiGraphics, int screenWidth, int screenHeight, AbstractVehicle vehicle) {

    }

    public void renderHelicopter(GuiGraphics guiGraphics, int screenWidth, int screenHeight, RotaryWingVehicle vehicle) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        guiGraphics.pose().pushPose();
        {
            guiGraphics.pose().translate(centerX, centerY, 0);
            if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
                weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon -> {
                    if (vehicleWeapon instanceof VehicleMissile vehicleMissile) {
                        VehicleMissileWeaponData vehicleMissileWeaponData = vehicleMissile.getData();
                        guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
                        int baseX = 0;
                        int baseY = 140;
                        // 光瞄视野框
                        drawRectByCorner(guiGraphics,
                                (int) (baseX + weaponUnit.getYRotMin()),
                                (int) (baseX + weaponUnit.getYRotMax()),
                                (int) (baseY + weaponUnit.getXRotMin()),
                                (int) (baseY + weaponUnit.getXRotMax()),
                                color, 1f);
                        // 导弹射界框
                        drawRectByCorner(guiGraphics,
                                (int) (baseX + vehicleMissileWeaponData.getYRotMin()),
                                (int) (baseX + vehicleMissileWeaponData.getYRotMax()),
                                (int) (baseY + vehicleMissileWeaponData.getXRotMin()),
                                (int) (baseY + vehicleMissileWeaponData.getXRotMax()),
                                color, 1f);
                        int x = (int) weaponUnit.getYRot();
                        int y = (int) weaponUnit.getXRot();
                        // 光瞄指向十字
                        guiGraphics.fill(baseX + x, baseY + y - 8, baseX + x + 1, baseY + y - 2, color);
                        guiGraphics.fill(baseX + x, baseY + y + 3, baseX + x + 1, baseY + y + 9, color);
                        guiGraphics.fill(baseX + x - 8, baseY + y, baseX + x - 2, baseY + y + 1, color);
                        guiGraphics.fill(baseX + x + 3, baseY + y, baseX + x + 9, baseY + y + 1, color);
                    }
                });
            }
        }
        guiGraphics.pose().popPose();
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
                    else if (sensorType == WeaponUnitData.FireControlSensorType.EO) {
                        RenderHelper.drawSquare(guiGraphics, 0, 0, 15, Color.GREEN);
                    }
                }
                poseStack.popPose();
            }
        }
        // 雷达锁定目标
        RadarUnit radarUnit = weaponUnit.getRadarUnit();
        if (radarUnit != null) {
            if (sensorType == WeaponUnitData.FireControlSensorType.RF && radarUnit.getLockedEntity() != null) {
                RadarUnit.DetectedObject detectedObject = weaponUnit.getRadarUnit().getDetectedEntities().get(radarUnit.getLockedEntity().getId());
                if (detectedObject != null) {
                    Vec3 screenPos = VectorUtil.worldToScreen(detectedObject.entity.position());
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
                Entity lockedEntity = radarUnit.getLockedEntity();
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
                    }
                    radarInfo(guiGraphics, poseStack, detectedObject);
                }
                poseStack.popPose();
            }
        }
    }

    private static void radarInfo(GuiGraphics guiGraphics, PoseStack poseStack, RadarUnit.DetectedObject detectedObject) {
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
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
            guiGraphics.drawString(Minecraft.getInstance().font, String.format("%.2f m", distance), 0, 0, Color.GREEN, false);
            poseStack.translate(0, 12, 0);
            // 接近率
            Vec3 approach = velocity.subtract(vehicle.getDeltaMovement());
            Vec3 relative = detectedObject.entity.position().subtract(vehicle.position());
            int sig = approach.dot(relative) > 0 ? -1 : 1;
            double approachRate = sig * approach.length() * 20;
            guiGraphics.drawString(Minecraft.getInstance().font, String.format("%.2f m/s", approachRate), 0, 0, Color.GREEN, false);
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
            guiGraphics.hLine(-6, 6, 17, teamColor);
        }
    }

}
