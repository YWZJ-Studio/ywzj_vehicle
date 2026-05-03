package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.render.util.GuiHelper;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import static org.ywzj.vehicle.all.AllKeys.FREE_CAMERA;
import static org.ywzj.vehicle.util.RenderHelper.*;

@EventBusSubscriber(value = Dist.CLIENT)
public class VehicleCrossHairOverlay implements LayeredDraw.Layer {

    private static double screenAimXO = 0;
    private static double screenAimYO = 0;
    private static double screenAimX = 0;
    private static double screenAimY = 0;
    private static boolean showAim = true;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            boolean isHelicopter = vehicle instanceof RotaryWingVehicle;
            int color = isHelicopter ? Color.GREEN : Color.WHITE;
            PartUnit<?> operatorUnit = vehicle.getOwnOperatorUnit(player);
            if (operatorUnit instanceof WeaponUnit weaponUnit) {
                if (showAim) {
                    double x = Mth.lerp(partialTick, screenAimXO, screenAimX);
                    double y = Mth.lerp(partialTick, screenAimYO, screenAimY);
                    PoseStack poseStack = guiGraphics.pose();
                    poseStack.pushPose();
                    {
                        poseStack.translate(x, y, 0);
                        float alpha =  FREE_CAMERA.isDown() ? 0.25f : 0.5f;
                        int aimCircleColor = (color & 0x00FFFFFF) | ((int) (alpha * 255) << 24);
                        GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 8, aimCircleColor, 0.05f, 0, 0);
                        // 装填进度
                        weaponUnit.getCurrentWeapon().ifPresent(weapon -> renderReloadProgress(guiGraphics, weapon, 7f, 1.2f));
                        weaponUnit.getCurrentSecondaryWeapon().ifPresent(weapon -> renderReloadProgress(guiGraphics, weapon, 5.6f, 1f));
                        if (!weaponUnit.independentWeapons.isEmpty()) {
                            renderReloadProgress(guiGraphics, weaponUnit.independentWeapons.get(0), 4.4f, 0.8f);
                        }
                    }
                    poseStack.popPose();
                }
                Vec3 weaponHitPosO = LocalVehiclePlayer.instance.weaponHitPosO;
                Vec3 weaponHitPos = LocalVehiclePlayer.instance.weaponHitPos;
                if (weaponHitPosO != null && weaponHitPos != null) {
                    Vec3 screenHitPosO = VectorUtil.worldToScreen(weaponHitPosO);
                    Vec3 screenHitPos = VectorUtil.worldToScreen(weaponHitPos);
                    if (screenHitPos.z >= 0) {
                        double x = Mth.lerp(partialTick, screenHitPosO.x, screenHitPos.x);
                        double y = Mth.lerp(partialTick, screenHitPosO.y, screenHitPos.y);
                        PoseStack poseStack = guiGraphics.pose();
                        poseStack.pushPose();
                        {
                            poseStack.translate(x, y, 0);
                            if (weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR
                                    || weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.RF) {
                                // 导引头
                                if (weaponUnit.isSeekerOn()) {
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
                                    Vec2 rot = weaponUnit.worldRot();
                                    Vec3 screenPosUp = VectorUtil.worldToScreen(weaponUnit.worldPivotPosition()
                                            .add(VectorUtil.rotToVec(rot.x - weaponUnit.getSeekerFov(), rot.y).normalize().scale(256)));
                                    Vec3 screenPosDown = VectorUtil.worldToScreen(weaponUnit.worldPivotPosition()
                                            .add(VectorUtil.rotToVec(rot.x + weaponUnit.getSeekerFov(), rot.y).normalize().scale(256)));
                                    Vec3 screenPosCenter = VectorUtil.worldToScreen(weaponUnit.worldPivotPosition()
                                            .add(VectorUtil.rotToVec(rot.x, rot.y).normalize().scale(256)));
                                    double px = screenPosDown.x - screenPosUp.x;
                                    double py = screenPosDown.y - screenPosUp.y;
                                    float r = (float) Math.sqrt(px * px + py * py) / 2;
                                    poseStack.pushPose();
                                    {
                                        poseStack.translate(screenPosCenter.x - x, screenPosCenter.y - y, 0);
                                        GuiHelper.drawCircle(poseStack, 0, 0, r, aimCircleColor, 0.01f, 0, 0);
                                    }
                                    poseStack.popPose();
                                }
                            }
                            weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon -> {
                                if (vehicleWeapon.getWeaponUnit().getFireControlSensorType() == WeaponUnitData.FireControlSensorType.CCIP) {
                                    drawCross(guiGraphics, 0, 0, 20, Color.GREEN);
                                } else {
                                    WeaponUnitData.CrosshairStyle crosshairStyle = vehicleWeapon.getWeaponUnit().crosshairStyle;
                                    if (crosshairStyle != null) {
                                        switch (crosshairStyle) {
                                            case CIRCLE -> GuiHelper.drawCircle(poseStack, 0, 0, 3, color, 0.05f, 0, 0);
                                            case SQUARE -> drawSquare(guiGraphics, 0 ,0, 5, color);
                                            case RETICLE -> drawReticle(guiGraphics, 0 ,0, 15, 1, color);
                                        }
                                    }
                                }
                            });
                        }
                        poseStack.popPose();
                    }
                }
                // 目标
                VehicleScopeOverlay.renderAimLockTarget(guiGraphics, partialTick);
            }
        }
    }

    public static void renderReloadProgress(GuiGraphics guiGraphics, AbstractVehicleWeapon<?> weapon, float arcRadius, float thickness) {
        int reloadTime = weapon.getReloadTime();
        int maxReload  = weapon.getData().getReload().getTime();
        if (reloadTime > 0 && maxReload > 0) {
            float reloadFrac = 1f - (float) reloadTime / maxReload;
            int reloadArcColor = Color.RELOAD_ARC;
            GuiHelper.drawArc(guiGraphics, 0, 0, arcRadius, thickness, 0f, 1f, Color.RELOAD_ARC_BG);
            GuiHelper.drawArc(guiGraphics, 0, 0, arcRadius, thickness, 0f, reloadFrac, reloadArcColor);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit) {
                // 瞄准位置
                Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                float upward = LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE ? 0 : LocalVehiclePlayer.CAMERA_UPWARD_ANGLE;
                Vec3 aimScreenPos = getHitScreenPos(camera.getPosition(),
                        LocalVehiclePlayer.instance.cameraAimRotX - upward,
                        LocalVehiclePlayer.instance.cameraAimRotY,
                        player);
                if (aimScreenPos.z >= 0) {
                    screenAimXO = screenAimX;
                    screenAimYO = screenAimY;
                    screenAimX = aimScreenPos.x;
                    screenAimY = aimScreenPos.y;
                    showAim = true;
                } else  {
                    showAim = false;
                }
            }
        }
    }

    private static @NotNull Vec3 getHitScreenPos(Vec3 start, float xRot, float yRot, Player player) {
        Vec3 end = start.add(VectorUtil.rotToVec(xRot, yRot).normalize().scale(128));
        return VectorUtil.worldToScreen(VectorUtil.hitPosition(player, start, end));
    }

    public static double getScreenAimX() {
        return screenAimX;
    }

    public static double getScreenAimY() {
        return screenAimY;
    }

}
