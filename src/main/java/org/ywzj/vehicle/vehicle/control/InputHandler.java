package org.ywzj.vehicle.vehicle.control;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.network.Channel;
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

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InputHandler {

    public static boolean freeCamera;
    public static float playerXRotO;
    public static float playerYRotO;
    public static float controlXRotO;
    public static float controlYRotO;
    private static long waitSwitchSeatTime;

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.gameMode == null || mc.screen != null) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (LocalVehiclePlayer.instance.onVehicle()) {
                AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
                WeaponUnit weaponUnit = LocalVehiclePlayer.instance.getWeaponUnit();
                if (SWITCH_VIEW.matches(event.getKey(), event.getScanCode())) {
                    LocalVehiclePlayer.instance.switchViewType(null);
                } else if (SWITCH_SCOPE.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        if (LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
                            LocalVehiclePlayer.instance.switchViewType(LocalVehiclePlayer.ViewType.SCOPE);
                        } else {
                            LocalVehiclePlayer.instance.switchViewType(LocalVehiclePlayer.ViewType.THIRD_PERSON);
                        }
                    }
                } else if (OPEN_INVENTORY.matches(event.getKey(), event.getScanCode())) {
                    player.sendOpenInventory();
                } else if (TOGGLE_ENGINE.matches(event.getKey(), event.getScanCode())) {
                    sendToggleEngine(vehicle);
                } else if (TOGGLE_LANDING_GEAR.matches(event.getKey(), event.getScanCode())) {
                    sendToggleLandingGear(vehicle);
                } else if (FIRE_CONTROL_LOCK.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        weaponUnit.fireControlLock();
                    }
                } else if (TOGGLE_HOVER_MODE.matches(event.getKey(), event.getScanCode())) {
                    sendToggleHoverMode(vehicle);
                } else if (TOGGLE_RADAR.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        for (RadarUnit radarUnit : weaponUnit.getRadarUnits()) {
                            radarUnit.toggle(null);
                        }
                    }
                } else if (SECONDARY_WEAPON_SWITCH.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        Channel.CHANNEL.sendToServer(new ClientVehicleSwitchWeapon(vehicle.getId(), true, true));
                    }
                } else if (DECOY_FLARE_LAUNCH.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        weaponUnit.independentWeapons.stream()
                                .filter(vehicleWeapon -> vehicleWeapon instanceof VehicleDecoyFlare)
                                .forEach(AbstractVehicleWeapon::doClientShoot);
                    }
                } else if (SMOKE_GRENADE_LAUNCH.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        weaponUnit.independentWeapons.stream()
                                .filter(vehicleWeapon -> vehicleWeapon instanceof VehicleGrenade grenade
                                        && "smoke".equals(grenade.getData().getGrenade()))
                                .forEach(AbstractVehicleWeapon::doClientShoot);
                    }
                } else if (TOGGLE_SEEKER.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        weaponUnit.toggleSeeker(null);
                    }
                }
            }
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (LocalVehiclePlayer.instance.onVehicle()) {
                if (FREE_CAMERA.matches(event.getKey(), event.getScanCode())) {
                    LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
                    localVehiclePlayer.playerLerpXRot = playerXRotO;
                    localVehiclePlayer.playerLerpYRot = playerYRotO;
                    localVehiclePlayer.playerLerpSteps = 8;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.MouseButton event) {
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.gameMode == null || mc.screen != null) {
            return;
        }
        if (event.getAction() == 0) {
            if (LocalVehiclePlayer.instance.onVehicle()) {
                if (MAGNIFICATION_CHANGE.isDown() && LocalVehiclePlayer.instance.onVehicleTickCount > 5) {
                    AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
                    if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit) {
                        if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                            weaponUnit.switchZoom();
                        }
                    }
                    if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
                        vehicle.toggleViewZoom();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void checkKey(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
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
            boolean previous = event.getScrollDelta() == -1;
            if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit) {
                Channel.CHANNEL.sendToServer(new ClientVehicleSwitchWeapon(vehicle.getId(), false, previous));
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
        Channel.CHANNEL.sendToServer(control);
    }

    private static void sendChangeSeat(AbstractVehicle vehicle, int toSeat) {
        ClientVehicleChangeSeat changeSeat = new ClientVehicleChangeSeat();
        changeSeat.vehicleEntityId = vehicle.getId();
        changeSeat.toSeat = toSeat;
        Channel.CHANNEL.sendToServer(changeSeat);
    }

    private static void sendLeaveVehicle(AbstractVehicle vehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = vehicle.getId();
        action.leaveVehicle = true;
        Channel.CHANNEL.sendToServer(action);
    }

    private static void sendToggleEngine(AbstractVehicle vehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = vehicle.getId();
        action.toggleEngine = true;
        Channel.CHANNEL.sendToServer(action);
    }

    private static void sendToggleLandingGear(AbstractVehicle vehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = vehicle.getId();
        action.toggleLandingGear = true;
        Channel.CHANNEL.sendToServer(action);
    }

    private static void sendToggleHoverMode(AbstractVehicle vehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = vehicle.getId();
        action.toggleHoverMode = true;
        Channel.CHANNEL.sendToServer(action);
    }

}
