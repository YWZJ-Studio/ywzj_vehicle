package org.ywzj.vehicle.vehicle.control;

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
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.network.message.ClientVehicleChangeSeat;
import org.ywzj.vehicle.network.message.ClientVehicleMoveControl;
import org.ywzj.vehicle.network.message.ClientVehicleSwitchWeapon;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.weapon.VehicleDecoyFlare;

import static org.ywzj.vehicle.all.AllKeys.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InputHandler {

    public static boolean freeCamera;
    public static boolean debugGui;
    public static float xRotO;
    public static float yRotO;

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS){
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
                    Minecraft.getInstance().player.sendOpenInventory();
                } else if (TOGGLE_ENGINE.matches(event.getKey(), event.getScanCode())) {
                    sendToggleEngine(vehicle);
                } else if (CHANGE_SEAT_1.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 0);
                } else if (CHANGE_SEAT_2.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 1);
                } else if (CHANGE_SEAT_3.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 2);
                } else if (CHANGE_SEAT_4.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 3);
                } else if (CHANGE_SEAT_5.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 4);
                } else if (CHANGE_SEAT_6.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 5);
                } else if (FIRE_CONTROL_STABILIZER.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        weaponUnit.switchStabilizer();
                    }
                } else if (FIRE_CONTROL_LOCK.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        weaponUnit.fireControlLock();
                    }
                } else if (DECOY_FLARE_LAUNCH.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        weaponUnit.independentWeapons.stream()
                                .filter(vehicleWeapon -> vehicleWeapon instanceof VehicleDecoyFlare)
                                .forEach(AbstractVehicleWeapon::doClientShoot);
                    }
                } else if (DEBUG_GUI.matches(event.getKey(), event.getScanCode())) {
                    debugGui = !debugGui;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.MouseButton event) {
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.gameMode == null) {
            return;
        }
        if (event.getAction() == 0) {
            if (LocalVehiclePlayer.instance.onVehicle()) {
                if (MAGNIFICATION_CHANGE.isDown() && LocalVehiclePlayer.instance.tickCount > 5) {
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
        if (player == null || player.isSpectator() || mc.gameMode == null) {
            return;
        }
        freeCamera = FREE_CAMERA.isDown();
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            if (LEAVE_VEHICLE.isDown()) {
                sendLeaveVehicle(vehicle);
                return;
            }
            if (player.equals(vehicle.controlUnit.getOperator())) {
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
                if (freeCamera) {
                    controlUnit.xRot = xRotO;
                    controlUnit.yRot = yRotO;
                } else if (vehicle instanceof RotaryWingVehicle
                        && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                    controlUnit.xRot = 0;
                    controlUnit.yRotKeep = true;
                } else {
                    controlUnit.xRot = player.getXRot();
                    controlUnit.yRot = player.getYRot();
                    xRotO = controlUnit.xRot;
                    yRotO = controlUnit.yRot;
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
                Channel.CHANNEL.sendToServer(new ClientVehicleSwitchWeapon(vehicle.getId(), previous));
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

}
