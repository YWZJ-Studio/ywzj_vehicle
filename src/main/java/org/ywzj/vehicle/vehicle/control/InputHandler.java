package org.ywzj.vehicle.vehicle.control;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.network.message.ClientVehicleChangeSeat;
import org.ywzj.vehicle.network.message.ClientVehicleMoveControl;
import org.ywzj.vehicle.network.message.ClientVehicleSwitchWeapon;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.RadarUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.weapon.VehicleDecoyFlare;
import org.ywzj.vehicle.vehicle.weapon.VehicleGrenade;

import static org.ywzj.vehicle.all.AllKeys.*;

@EventBusSubscriber(value = Dist.CLIENT)
public class InputHandler {

    public static boolean freeCamera;
    public static float playerXRotO;
    public static float playerYRotO;
    public static float controlXRotO;
    public static float controlYRotO;
    private static long waitSwitchSeatTime;

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        handleVehicleAction(event.getKey(), event.getScanCode(), event.getAction());
    }

    @SubscribeEvent
    public static void onKey(InputEvent.MouseButton.Pre event) {
        handleVehicleAction(event.getButton(), 0, event.getAction());
        handleMagnificationChange();
    }

    private static boolean matchesKey(KeyMapping mapping, int key, int scanCode) {
        return mapping.matches(key, scanCode) || mapping.matchesMouse(key);
    }

    private static void handleVehicleAction(int key, int scanCode, int action) {
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.gameMode == null || mc.screen != null) {
            return;
        }
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        if (action == GLFW.GLFW_PRESS) {
            if (instance.onVehicle()) {
                AbstractVehicle vehicle = instance.getVehicle();
                WeaponUnit weaponUnit = instance.getWeaponUnit();
                if (matchesKey(SWITCH_VIEW, key, scanCode)) {
                    instance.switchViewType(null);
                } else if (matchesKey(SWITCH_SCOPE, key, scanCode)) {
                    if (weaponUnit != null) {
                        if (instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
                            instance.switchViewType(LocalVehiclePlayer.ViewType.SCOPE);
                        } else {
                            instance.switchViewType(LocalVehiclePlayer.ViewType.THIRD_PERSON);
                        }
                    }
                } else if (matchesKey(OPEN_INVENTORY, key, scanCode)) {
                    player.sendOpenInventory();
                } else if (matchesKey(TOGGLE_ENGINE, key, scanCode)) {
                    sendToggleEngine(vehicle);
                } else if (matchesKey(TOGGLE_LANDING_GEAR, key, scanCode)) {
                    sendToggleLandingGear(vehicle);
                } else if (matchesKey(FIRE_CONTROL_LOCK, key, scanCode)) {
                    if (weaponUnit != null) {
                        weaponUnit.fireControlLock();
                    }
                } else if (matchesKey(TOGGLE_HOVER_MODE, key, scanCode)) {
                    sendToggleHoverMode(vehicle);
                } else if (matchesKey(TOGGLE_RADAR, key, scanCode)) {
                    if (weaponUnit != null) {
                        for (RadarUnit radarUnit : weaponUnit.getRadarUnits()) {
                            radarUnit.toggle(null);
                        }
                    }
                } else if (matchesKey(SECONDARY_WEAPON_SWITCH, key, scanCode)) {
                    if (weaponUnit != null) {
                        PacketDistributor.sendToServer(new ClientVehicleSwitchWeapon(vehicle.getId(), ClientVehicleSwitchWeapon.WeaponSwitchType.SECONDARY, true));
                    }
                } else if (matchesKey(DECOY_FLARE_LAUNCH, key, scanCode)) {
                    if (weaponUnit != null) {
                        weaponUnit.independentWeapons.stream()
                                .filter(vehicleWeapon -> vehicleWeapon instanceof VehicleDecoyFlare)
                                .forEach(AbstractVehicleWeapon::doClientShoot);
                    }
                } else if (matchesKey(SMOKE_GRENADE_LAUNCH, key, scanCode)) {
                    if (weaponUnit != null) {
                        weaponUnit.independentWeapons.stream()
                                .filter(vehicleWeapon -> vehicleWeapon instanceof VehicleGrenade grenade
                                        && ("smoke".equals(grenade.getData().getGrenade()) || "frag".equals(grenade.getData().getGrenade())))
                                .forEach(AbstractVehicleWeapon::doClientShoot);
                    }
                } else if (matchesKey(TOGGLE_SEEKER, key, scanCode)) {
                    if (weaponUnit != null) {
                        weaponUnit.toggleSeeker(null);
                    }
                } else if (matchesKey(TOGGLE_THERMAL_IMAGING, key, scanCode)) {
                    if (weaponUnit != null && weaponUnit.withThermalImager()
                            && instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                        instance.thermalImaging = !instance.thermalImaging;
                    }
                } else if (matchesKey(TOGGLE_WEAPON_BAY, key, scanCode)) {
                    if (weaponUnit != null) {
                        weaponUnit.toggleCurrentWeaponBay();
                    }
                } else if (matchesKey(TOGGLE_AEROBATIC_SMOKE, key, scanCode)) {
                    if (vehicle instanceof FixedWingVehicle) {
                        sendToggleAerobaticSmoke(vehicle);
                    }
                } else if (matchesKey(MULTI_WEAPON_SWITCH, key, scanCode)) {
                    if (weaponUnit != null) {
                        PacketDistributor.sendToServer(new ClientVehicleSwitchWeapon(vehicle.getId(), ClientVehicleSwitchWeapon.WeaponSwitchType.MULTI, true));
                    }
                }
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            if (instance.onVehicle()) {
                if (matchesKey(FREE_CAMERA, key, scanCode)) {
                    LocalVehiclePlayer localVehiclePlayer = instance;
                    localVehiclePlayer.playerLerpXRot = playerXRotO;
                    localVehiclePlayer.playerLerpYRot = playerYRotO;
                    localVehiclePlayer.playerLerpSteps = 8;
                }
            }
        }
    }

    private static void handleMagnificationChange() {
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.gameMode == null || mc.screen != null) {
            return;
        }
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        if (instance.onVehicle() && MAGNIFICATION_CHANGE.isDown() && instance.onVehicleTickCount > 5) {
            AbstractVehicle vehicle = instance.getVehicle();
            if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit) {
                if (instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                    weaponUnit.switchZoom();
                }
            }
            if (instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR
                    || instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
                vehicle.toggleViewZoom();
            }
        }
    }

    @SubscribeEvent
    public static void checkKey(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.gameMode == null || mc.screen != null) {
            return;
        }
        freeCamera = FREE_CAMERA.isDown();
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            if (LEAVE_VEHICLE.isDown()) {
                sendLeaveVehicle(vehicle);
                return;
            }
            if (CHANGE_SEAT.isDown()) {
                long window = Minecraft.getInstance().getWindow().getWindow();
                for (int seatIndex = 0; seatIndex < 9; seatIndex++) {
                    int key = GLFW.GLFW_KEY_1 + seatIndex;
                    if (InputConstants.isKeyDown(window, key)) {
                        sendChangeSeat(vehicle, seatIndex);
                        waitSwitchSeatTime = System.currentTimeMillis();
                        return;
                    }
                }
                if (System.currentTimeMillis() - waitSwitchSeatTime > 500) {
                    int seatsCount = vehicle.seats.size();
                    AbstractVehicle.Seat playerSeat = LocalVehiclePlayer.instance.seat;
                    if (playerSeat != null) {
                        for (int seatIndex = (playerSeat.seatIndex + 1) % seatsCount; seatIndex < seatsCount; seatIndex++) {
                            if (vehicle.seats.get(seatIndex).partUnit.getOwner() == null) {
                                sendChangeSeat(vehicle, seatIndex);
                                waitSwitchSeatTime = System.currentTimeMillis();
                                return;
                            }
                        }
                    }
                }
            }
            if (!CHANGE_SEAT.isDown()) {
                waitSwitchSeatTime = System.currentTimeMillis();
            }
            if (player.equals(vehicle.controlUnit.getOperator())) {
                if (LocalVehiclePlayer.instance.lostControl) {
                    ControlUnit controlUnit = new ControlUnit(vehicle);
                    controlUnit.xRot = 0;
                    controlUnit.yRot = playerYRotO;
                    sendControl(vehicle, controlUnit);
                    return;
                }
                ControlUnit controlUnit = new ControlUnit(vehicle);
                controlUnit.forward = FORWARD.isDown();
                controlUnit.backward = BACKWARD.isDown();
                controlUnit.left = LEFT.isDown();
                controlUnit.right = RIGHT.isDown();
                controlUnit.up = UP.isDown();
                controlUnit.down = DOWN.isDown();
                controlUnit.leftYaw = LEFT_YAW.isDown();
                controlUnit.rightYaw = RIGHT_YAW.isDown();
                controlUnit.functionalUp = FUNCTIONAL_UP.isDown();
                controlUnit.functionalDown = FUNCTIONAL_DOWN.isDown();
                controlUnit.functionalLeft = FUNCTIONAL_LEFT.isDown();
                controlUnit.functionalRight = FUNCTIONAL_RIGHT.isDown();
                if (freeCamera || LocalVehiclePlayer.instance.playerLerpSteps > 0) {
                    controlUnit.xRot = controlXRotO;
                    controlUnit.yRot = controlYRotO;
                } else if ((vehicle instanceof RotaryWingVehicle || vehicle instanceof FixedWingVehicle)
                        && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                    controlUnit.xRot = 0;
                    controlUnit.yRotKeep = true;
                } else {
                    if (vehicle instanceof RotaryWingVehicle) {
                        controlUnit.xRot = player.getXRot();
                    } else if (vehicle instanceof FixedWingVehicle) {
                        controlUnit.xRot = player.getXRot() - LocalVehiclePlayer.CAMERA_UPWARD_ANGLE;
                    }
                    controlUnit.yRot = player.getYRot();
                    controlXRotO = controlUnit.xRot;
                    controlYRotO = controlUnit.yRot;
                    playerXRotO = player.getXRot();
                    playerYRotO = player.getYRot();
                }
                vehicle.controlUnit.update(controlUnit);
                sendControl(vehicle, controlUnit);
            }
            handleShoot(vehicle, player);
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.MouseScrollingEvent event) {
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.gameMode == null) {
            return;
        }
        if (LocalVehiclePlayer.instance.onVehicle()) {
            AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
            boolean previous = event.getScrollDeltaY() < 0;
            if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit) {
                PacketDistributor.sendToServer(new ClientVehicleSwitchWeapon(vehicle.getId(), ClientVehicleSwitchWeapon.WeaponSwitchType.PRIMARY, previous));
                // 阻止滚轮事件传递给原版以避免物品栏切换
                event.setCanceled(true);
            }
        }
    }

    private static void handleShoot(AbstractVehicle vehicle, LocalPlayer player) {
        if (MAIN_WEAPON_SHOOT.isDown()) {
            if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit) {
                weaponUnit.getCurrentWeapon().ifPresent(AbstractVehicleWeapon::doClientShoot);
            } else {
                LocalVehiclePlayer.instance.sendMessage("tips.spotter");
            }
        }
        if (SECONDARY_WEAPON_SHOOT.isDown()) {
            if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit) {
                weaponUnit.getCurrentSecondaryWeapon().ifPresent(AbstractVehicleWeapon::doClientShoot);
            } else {
                LocalVehiclePlayer.instance.sendMessage("tips.spotter");
            }
        }
    }

    private static void sendControl(AbstractVehicle vehicle, ControlUnit controlUnit) {
        ClientVehicleMoveControl control = new ClientVehicleMoveControl();
        control.vehicleEntityId = vehicle.getId();
        control.forward = controlUnit.forward;
        control.backward = controlUnit.backward;
        control.left = controlUnit.left;
        control.right = controlUnit.right;
        control.up = controlUnit.up;
        control.down = controlUnit.down;
        control.leftYaw = controlUnit.leftYaw;
        control.rightYaw = controlUnit.rightYaw;
        control.functionalUp = controlUnit.functionalUp;
        control.functionalDown = controlUnit.functionalDown;
        control.functionalLeft = controlUnit.functionalLeft;
        control.functionalRight = controlUnit.functionalRight;
        control.xRot = controlUnit.xRot;
        control.xRotKeep = controlUnit.xRotKeep;
        control.yRot = controlUnit.yRot;
        control.yRotKeep = controlUnit.yRotKeep;
        PacketDistributor.sendToServer(control);
    }

    private static void sendChangeSeat(AbstractVehicle vehicle, int toSeat) {
        ClientVehicleChangeSeat changeSeat = new ClientVehicleChangeSeat();
        changeSeat.vehicleEntityId = vehicle.getId();
        changeSeat.toSeat = toSeat;
        PacketDistributor.sendToServer(changeSeat);
    }

    private static void sendLeaveVehicle(AbstractVehicle vehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = vehicle.getId();
        action.leaveVehicle = true;
        PacketDistributor.sendToServer(action);
    }

    private static void sendToggleEngine(AbstractVehicle vehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = vehicle.getId();
        action.toggleEngine = true;
        PacketDistributor.sendToServer(action);
    }

    private static void sendToggleLandingGear(AbstractVehicle vehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = vehicle.getId();
        action.toggleLandingGear = true;
        PacketDistributor.sendToServer(action);
    }

    private static void sendToggleHoverMode(AbstractVehicle vehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = vehicle.getId();
        action.toggleHoverMode = true;
        PacketDistributor.sendToServer(action);
    }

    private static void sendToggleAerobaticSmoke(AbstractVehicle vehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = vehicle.getId();
        action.toggleAerobaticSmoke = true;
        AllConfigs.CommonConfig common = AllConfigs.common;
        action.aerobaticSmokeR = common.aerobaticSmokeR.get();
        action.aerobaticSmokeG = common.aerobaticSmokeG.get();
        action.aerobaticSmokeB = common.aerobaticSmokeB.get();
        PacketDistributor.sendToServer(action);
    }

}
