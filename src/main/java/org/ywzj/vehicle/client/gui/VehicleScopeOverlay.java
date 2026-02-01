package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.RadarUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
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
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        // 准心
        if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit
                && weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.CRT) {
            RenderHelper.drawRect(guiGraphics, centerX, centerY, 15, 15, color, 1f);
            guiGraphics.pose().pushPose();
            {
                Vec3 posO = VectorUtil.worldToScreen(LocalVehiclePlayer.instance.weaponHitPosO);
                Vec3 pos = VectorUtil.worldToScreen(LocalVehiclePlayer.instance.weaponHitPos);
                guiGraphics.pose().translate(
                        Mth.lerp(partialTick, posO.x, pos.x),
                        Mth.lerp(partialTick, posO.y, pos.y),
                        0);
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
            }
            guiGraphics.pose().popPose();
        }
        // 稳定器锁定的位置
        renderAimLockTarget(guiGraphics);
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

    public static void renderAimLockTarget(GuiGraphics guiGraphics) {
        WeaponUnit weaponUnit = LocalVehiclePlayer.instance.getWeaponUnit();
        if (weaponUnit == null) {
            return;
        }
        // 已锁定的目标
        if (weaponUnit.getAimLockEntity() != null) {
            Entity entity = weaponUnit.getAimLockEntity();
            AABB aabb = entity.getBoundingBox();
            Vec3 screenPos = VectorUtil.worldToScreen(aabb.getCenter());
            if (screenPos.z >= 0) {
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                {
                    poseStack.translate(screenPos.x, screenPos.y, 0);
                    WeaponUnitData.FireControlSensorType sensorType = weaponUnit.getFireControlSensorType();
                    if (sensorType == WeaponUnitData.FireControlSensorType.IR) {
                        GuiHelper.drawCircle(poseStack, 0, 0, 15, Color.RED, 0.03f, 0, 0);
                    } else {
                        RenderHelper.drawSquare(guiGraphics, 0, 0, 15, Color.GREEN);
                        // 雷达锁定则显示详细的目标信息
                        if (sensorType == WeaponUnitData.FireControlSensorType.RF) {
                            poseStack.pushPose();
                            {
                                poseStack.translate(12, -12, 0);
                                poseStack.scale(0.8f, 0.8f, 0.8f);
                                double distance = aabb.getCenter().distanceTo(LocalVehiclePlayer.instance.getVehicle().position());
                                guiGraphics.drawString(Minecraft.getInstance().font, String.format("%.2f m", distance), 0, 0, Color.GREEN, false);
                            }
                            poseStack.popPose();
                        }
                    }
                }
                poseStack.popPose();
            }
        }
        // 可锁定的目标
        for (RadarUnit.DetectedObject detectedObject : weaponUnit.getRadarDetectedEntities()) {
            if (detectedObject.entity == weaponUnit.getAimLockEntity()) {
                continue;
            }
            Vec3 screenPos = VectorUtil.worldToScreen(detectedObject.detectedPosition);
            if (screenPos.z < 0) {
                continue;
            }
            guiGraphics.pose().pushPose();
            {
                guiGraphics.pose().translate(screenPos.x, screenPos.y, 0);
                RenderHelper.drawSquareCorners(guiGraphics, 0, 0, 15, 5, Color.GREEN);
            }
            guiGraphics.pose().popPose();
        }
    }

}
