package org.ywzj.vehicle.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.HelicopterVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.network.message.ClientVehicleChangeSeat;
import org.ywzj.vehicle.network.message.ClientVehicleMoveControl;
import org.ywzj.vehicle.vehicle.ControlUnit;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.WeaponUnit;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InputHandler {

    public static final KeyMapping FORWARD = new KeyMapping("key.ywzj_vehicle.forward.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_W,
            "key.category.ywzj_vehicle");
    public static final KeyMapping BACKWARD = new KeyMapping("key.ywzj_vehicle.backward.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_S,
            "key.category.ywzj_vehicle");
    public static final KeyMapping LEFT = new KeyMapping("key.ywzj_vehicle.left.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_A,
            "key.category.ywzj_vehicle");
    public static final KeyMapping RIGHT = new KeyMapping("key.ywzj_vehicle.right.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_D,
            "key.category.ywzj_vehicle");
    public static final KeyMapping LEFT_YAW = new KeyMapping("key.ywzj_vehicle.left_yaw.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Q,
            "key.category.ywzj_vehicle");
    public static final KeyMapping RIGHT_YAW = new KeyMapping("key.ywzj_vehicle.right_yaw.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_E,
            "key.category.ywzj_vehicle");
    public static final KeyMapping COLLECTIVE_PITCH_UP = new KeyMapping("key.ywzj_vehicle.collective_pitch_up.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE,
            "key.category.ywzj_vehicle");
    public static final KeyMapping COLLECTIVE_PITCH_DOWN = new KeyMapping("key.ywzj_vehicle.collective_pitch_down.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            "key.category.ywzj_vehicle");
    public static final KeyMapping MAIN_WEAPON_SHOOT = new KeyMapping("key.ywzj_vehicle.main_weapon_shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.category.ywzj_vehicle");
    public static final KeyMapping SWITCH_VIEW = new KeyMapping("key.ywzj_vehicle.switch_view.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.category.ywzj_vehicle");
    public static final KeyMapping FREE_CAMERA = new KeyMapping("key.ywzj_vehicle.free_camera.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.category.ywzj_vehicle");
    public static final KeyMapping OPEN_INVENTORY = new KeyMapping("key.ywzj_vehicle.open_inventory.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.category.ywzj_vehicle");
    public static final KeyMapping LEAVE_VEHICLE = new KeyMapping("key.ywzj_vehicle.leave_vehicle.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.category.ywzj_vehicle");
    public static final KeyMapping CHANGE_SEAT_1 = new KeyMapping("key.ywzj_vehicle.change_seat_1.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_1,
            "key.category.ywzj_vehicle");
    public static final KeyMapping CHANGE_SEAT_2 = new KeyMapping("key.ywzj_vehicle.change_seat_2.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_2,
            "key.category.ywzj_vehicle");
    public static final KeyMapping CHANGE_SEAT_3 = new KeyMapping("key.ywzj_vehicle.change_seat_3.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_3,
            "key.category.ywzj_vehicle");
    public static final KeyMapping CHANGE_SEAT_4 = new KeyMapping("key.ywzj_vehicle.change_seat_4.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_4,
            "key.category.ywzj_vehicle");
    public static final KeyMapping DEBUG_GUI = new KeyMapping("key.ywzj_vehicle.debug_gui.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SLASH,
            "key.category.ywzj_vehicle");
    private static long lastFireTimeMillis;
    public static boolean freeCamera;
    public static boolean debugGui;
    public static float xRotO;
    public static float yRotO;

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return;
        if (event.getAction() == GLFW.GLFW_PRESS){
            if (LocalVehiclePlayer.instance.onVehicle()) {
                AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
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
                } else if (DEBUG_GUI.matches(event.getKey(), event.getScanCode())) {
                    debugGui = !debugGui;
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
                } else {
                    controlUnit.xRot = player.getXRot();
                    controlUnit.yRot = player.getYRot();
                    xRotO = controlUnit.xRot;
                    yRotO = controlUnit.yRot;
                }
                sendControl(vehicle, controlUnit);
            }
            if (MAIN_WEAPON_SHOOT.isDown()) {
                if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit) {
                    if (weaponUnit == vehicle.spotterUnit) {
                        LocalVehiclePlayer.instance.sendMessage("tips.spotter");
                        return;
                    }

                    // todo 武器配置
                    Vec3 ammoSpawnPosition = weaponUnit.ammoSpawnPosition();
                    Vec2 rot = weaponUnit.worldRot();

                    weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon->{
                        long interval = vehicleWeapon.getShootInterval();
                        if (System.currentTimeMillis() - lastFireTimeMillis > interval) {
                            sendShoot(vehicle, weaponUnit.getIndex(), ammoSpawnPosition, rot.x, rot.y);
                            lastFireTimeMillis = System.currentTimeMillis();
                        }
                    });
                }
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

    private static void sendShoot(AbstractVehicle abstractVehicle, int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = abstractVehicle.getId();
        action.weaponIndex = weaponIndex;
        action.shoot = true;
        action.ammoX = (float) ammoSpawnPosition.x;
        action.ammoY = (float) ammoSpawnPosition.y;
        action.ammoZ = (float) ammoSpawnPosition.z;
        action.ammoXRot = ammoXRot;
        action.ammoYRot = ammoYRot;
        Channel.CHANNEL.sendToServer(action);
    }

    private static void sendLeaveVehicle(AbstractVehicle abstractVehicle) {
        ClientVehicleAction action = new ClientVehicleAction();
        action.vehicleEntityId = abstractVehicle.getId();
        action.leaveVehicle = true;
        Channel.CHANNEL.sendToServer(action);
    }

}
