package org.ywzj.vehicle.all;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.ywzj.vehicle.YwzjVehicle;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AllKeys {

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

    public static final KeyMapping FIRE_CONTROL_STABILIZER = new KeyMapping("key.ywzj_vehicle.fire_control_stabilizer.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.category.ywzj_vehicle");

    public static final KeyMapping FIRE_CONTROL_LOCK = new KeyMapping("key.ywzj_vehicle.fire_control_lock.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.category.ywzj_vehicle");

    public static final KeyMapping MAIN_WEAPON_SHOOT = new KeyMapping("key.ywzj_vehicle.main_weapon_shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.category.ywzj_vehicle");

    public static final KeyMapping MAGNIFICATION_CHANGE = new KeyMapping("key.ywzj_vehicle.magnification_change.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
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

    @SubscribeEvent
    public static void onClientSetup(RegisterKeyMappingsEvent event) {
        event.register(FORWARD);
        event.register(BACKWARD);
        event.register(LEFT);
        event.register(RIGHT);
        event.register(COLLECTIVE_PITCH_UP);
        event.register(COLLECTIVE_PITCH_DOWN);
        event.register(FIRE_CONTROL_STABILIZER);
        event.register(FIRE_CONTROL_LOCK);
        event.register(MAIN_WEAPON_SHOOT);
        event.register(MAGNIFICATION_CHANGE);
        event.register(SWITCH_VIEW);
        event.register(LEAVE_VEHICLE);
        event.register(CHANGE_SEAT_1);
        event.register(CHANGE_SEAT_2);
        event.register(CHANGE_SEAT_3);
        event.register(CHANGE_SEAT_4);
        event.register(DEBUG_GUI);
    }

}
