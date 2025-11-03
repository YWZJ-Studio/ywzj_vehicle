package org.ywzj.vehicle.client.gui;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

import static org.ywzj.vehicle.util.RenderHelper.*;

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
                    drawCircle(guiGraphics, 0 ,0, 15, color);
                    guiGraphics.pose().popPose();
                }
                if (showHit) {
                    double x = Mth.lerp(partialTick, screenHitXO, screenHitX);
                    double y = Mth.lerp(partialTick, screenHitYO, screenHitY);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(x, y, 0);
                    weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon -> {
                        WeaponUnit.CrosshairStyle crosshairStyle = vehicleWeapon.getWeaponUnit().crosshairStyle;
                        if (crosshairStyle != null) {
                            switch (crosshairStyle) {
                                case CIRCLE -> drawCircle(guiGraphics, 0 ,0, 5, color);
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
        if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
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
                Vec3 aimScreenPos = getHitScreenPos(camera.getPosition(),
                        LocalVehiclePlayer.instance.cameraAimRotX - LocalVehiclePlayer.CAMERA_UPWARD_ANGLE,
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
                    if (currentWeaponUnit.parentWeaponUnitAim) {
                        currentWeaponUnit = currentWeaponUnit.getParentWeaponUnit();
                    }
                    Vec2 rot = currentWeaponUnit.worldRot();
                    Vec3 screenHitPos;
                    if (weaponUnit.getFiringMode() == WeaponUnit.FiringMode.RIPPLE) {
                        screenHitPos = getHitScreenPos(weaponUnit.ammoSpawnPosition(), rot.x, rot.y, player);
                    } else {
                        List<Vec3> positions = weaponUnit.ammoSpawnPositions();
                        double x = positions.stream().mapToDouble(v -> v.x).average().orElse(0);
                        double y = positions.stream().mapToDouble(v -> v.y).average().orElse(0);
                        double z = positions.stream().mapToDouble(v -> v.z).average().orElse(0);
                        screenHitPos = getHitScreenPos(new Vec3(x, y, z), rot.x, rot.y, player);
                    }
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
        Vec3 end = start.add(VectorUtil.calculateViewVector(xRot, yRot).normalize().scale(128));
        var result = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hitPos = result.getLocation();
        return VectorUtil.worldToScreen(hitPos);
    }

}
