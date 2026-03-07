package org.ywzj.vehicle.client.gui;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.client.render.util.GuiHelper;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import static org.ywzj.vehicle.all.AllKeys.FREE_CAMERA;
import static org.ywzj.vehicle.util.RenderHelper.drawReticle;
import static org.ywzj.vehicle.util.RenderHelper.drawSquare;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class VehicleCrossHairOverlay implements IGuiOverlay {

    private static double screenHitXO = 0;
    private static double screenHitYO = 0;
    private static double screenHitX = 0;
    private static double screenHitY = 0;
    private static boolean showHit = true;

    private static double screenAimXO = 0;
    private static double screenAimYO = 0;
    private static double screenAimX = 0;
    private static double screenAimY = 0;
    private static boolean showAim = true;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
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
            int color = isHelicopter ?  0xFF00FF00 : 0xFFFFFFFF;
            PartUnit<?> operatorUnit = vehicle.getOwnOperatorUnit(player);
            if (operatorUnit instanceof WeaponUnit weaponUnit) {
                if (showAim) {
                    double x = Mth.lerp(partialTick, screenAimXO, screenAimX);
                    double y = Mth.lerp(partialTick, screenAimYO, screenAimY);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(x, y, 0);
                    float alpha =  FREE_CAMERA.isDown() ? 0.25f : 0.5f;
                    int aimCircleColor = (color & 0x00FFFFFF) | ((int) (alpha * 255) << 24);
                    GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 8, aimCircleColor, 0.05f, 0, 0);
                    guiGraphics.pose().popPose();
                }
                if (showHit) {
                    double x = Mth.lerp(partialTick, screenHitXO, screenHitX);
                    double y = Mth.lerp(partialTick, screenHitYO, screenHitY);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(x, y, 0);
                    // 导引头冷却提示
                    if (weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR && weaponUnit.isIrSensorOn()) {
                        float alpha = 0.4f;
                        if (weaponUnit.isIrSensorOn()) {
                            alpha += (float) Math.sin((double) weaponUnit.getIrCoolingTick() / 5 * Math.PI) * 0.2f;
                        }
                        int aimCircleColor = (color & 0x00FFFFFF) | ((int) (alpha * 255) << 24);
                        if (weaponUnit.getFireControlLockType() == WeaponUnitData.FireControlLockType.AIM_FRUSTUM) {
                            // 导引头小圈
                            if (weaponUnit.getAimLockEntity() == null && weaponUnit.getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR) {
                                GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 15, aimCircleColor, 0.03f, 0, 0);
                            }
                            // 导引头/雷达大圈
                            GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 64, aimCircleColor, 0.01f, 0, 0);
                        }
                    }
                    weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon -> {
                        WeaponUnitData.CrosshairStyle crosshairStyle = vehicleWeapon.getWeaponUnit().crosshairStyle;
                        if (crosshairStyle != null) {
                            switch (crosshairStyle) {
                                case CIRCLE -> GuiHelper.drawCircle(guiGraphics.pose(), 0, 0, 3, color, 0.05f, 0, 0);
                                case SQUARE -> drawSquare(guiGraphics, 0 ,0, 5, color);
                                case RETICLE -> drawReticle(guiGraphics, 0 ,0, 15, 1, color);
                            }
                        }
                    });
                    guiGraphics.pose().popPose();
                }
                // 稳定器锁定的位置
                VehicleScopeOverlay.renderAimLockTarget(guiGraphics);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }
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
                // 瞄准落点
                weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon -> {
                    WeaponUnit currentWeaponUnit = vehicleWeapon.getWeaponUnit();
                    if (currentWeaponUnit.isParentWeaponUnitAim()) {
                        currentWeaponUnit = currentWeaponUnit.getParentWeaponUnit();
                    }
                    Vec3 screenHitPos = VectorUtil.worldToScreen(currentWeaponUnit.aimHitPosition());
                    if (screenHitPos.z >= 0) {
                        screenHitXO = screenHitX;
                        screenHitYO = screenHitY;
                        screenHitX = screenHitPos.x;
                        screenHitY = screenHitPos.y;
                        showHit = true;
                    } else {
                        showHit = false;
                    }
                });
            }
        }
    }

    private static @NotNull Vec3 getHitScreenPos(Vec3 start, float xRot, float yRot, Player player) {
        Vec3 end = start.add(VectorUtil.rotToVec(xRot, yRot).normalize().scale(128));
        var result = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hitPos = result.getLocation();
        return VectorUtil.worldToScreen(hitPos);
    }

    public static double getScreenAimX() {
        return screenAimX;
    }

    public static double getScreenAimY() {
        return screenAimY;
    }

}
