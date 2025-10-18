package org.ywzj.vehicle.client.event;

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
import org.ywzj.vehicle.entity.vehicle.HelicopterVehicle;
import org.ywzj.vehicle.misc.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.network.message.ClientVehicleChangeSeat;
import org.ywzj.vehicle.network.message.ClientVehicleMoveControl;
import org.ywzj.vehicle.vehicle.ControlUnit;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.WeaponUnit;

import static org.ywzj.vehicle.all.AllKeys.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InputHandler {

    private static long lastFireTimeMillis;
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
                } else if (OPEN_INVENTORY.matches(event.getKey(), event.getScanCode())) {
                    Minecraft.getInstance().player.sendOpenInventory();
                } else if (CHANGE_SEAT_1.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 0);
                } else if (CHANGE_SEAT_2.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 1);
                } else if (CHANGE_SEAT_3.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 2);
                } else if (CHANGE_SEAT_4.matches(event.getKey(), event.getScanCode())) {
                    sendChangeSeat(vehicle, 3);
                } else if (FIRE_CONTROL_STABILIZER.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        weaponUnit.switchStabilizer();
                    }
                } else if (FIRE_CONTROL_LOCK.matches(event.getKey(), event.getScanCode())) {
                    if (weaponUnit != null) {
                        weaponUnit.fireControlLock();
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
                AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
                if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit) {
                    if (MAGNIFICATION_CHANGE.isDown() && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                        weaponUnit.switchZoom();
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
            if (player.equals(vehicle.controlUnit.operator)) {
                if (LEAVE_VEHICLE.isDown()) {
                    sendLeaveVehicle(vehicle);
                    return;
                }
                ControlUnit controlUnit = new ControlUnit();
                controlUnit.forward = FORWARD.isDown();
                controlUnit.backward = BACKWARD.isDown();
                controlUnit.left = LEFT.isDown();
                controlUnit.right = RIGHT.isDown();
                controlUnit.leftYaw = LEFT_YAW.isDown();
                controlUnit.rightYaw = RIGHT_YAW.isDown();
                if (vehicle instanceof HelicopterVehicle) {
                    controlUnit.up = COLLECTIVE_PITCH_UP.isDown();
                    controlUnit.down = COLLECTIVE_PITCH_DOWN.isDown();
                }
                if (freeCamera) {
                    controlUnit.xRot = xRotO;
                    controlUnit.yRot = yRotO;
                } else if (vehicle instanceof HelicopterVehicle
                        && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                    controlUnit.xRot = 0;
                    controlUnit.yRot = vehicle.getYRot();
                } else {
                    controlUnit.xRot = player.getXRot();
                    controlUnit.yRot = player.getYRot();
                    xRotO = controlUnit.xRot;
                    yRotO = controlUnit.yRot;
                }
                sendControl(vehicle, controlUnit);
            }

            handleShoot(vehicle, player);
        }
    }

    private static void handleShoot(AbstractVehicle vehicle, LocalPlayer player) {
        if (MAIN_WEAPON_SHOOT.isDown()) {
            if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit) {
                if (weaponUnit == vehicle.spotterUnit) {
                    LocalVehiclePlayer.instance.sendMessage("tips.spotter");
                    return;
                }

                weaponUnit.getCurrentWeapon().ifPresent(AbstractVehicleWeapon::doClientShoot);
            }
        }
    }

    private static void sendControl(AbstractVehicle abstractVehicle, ControlUnit controlUnit) {
        ClientVehicleMoveControl control = new ClientVehicleMoveControl();
        control.vehicleEntityId = abstractVehicle.getId();
        control.forward = controlUnit.forward;
        control.backward = controlUnit.backward;
        control.left = controlUnit.left;
        control.right = controlUnit.right;
        control.up = controlUnit.up;
        control.down = controlUnit.down;
        control.leftYaw = controlUnit.leftYaw;
        control.rightYaw = controlUnit.rightYaw;
        control.xRot = controlUnit.xRot;
        control.yRot = controlUnit.yRot;
        Channel.CHANNEL.sendToServer(control);
    }

    private static void sendChangeSeat(AbstractVehicle abstractVehicle, int toSeat) {
        ClientVehicleChangeSeat changeSeat = new ClientVehicleChangeSeat();
        changeSeat.vehicleEntityId = abstractVehicle.getId();
        changeSeat.toSeat = toSeat;
        Channel.CHANNEL.sendToServer(changeSeat);
    }

    private static void sendLeaveVehicle(AbstractVehicle abstractVehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = abstractVehicle.getId();
        action.leaveVehicle = true;
        Channel.CHANNEL.sendToServer(action);
    }

}
