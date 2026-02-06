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

    // 控制类
    public static final KeyMapping FORWARD = key("forward", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_W);
    public static final KeyMapping BACKWARD = key("backward", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_S);
    public static final KeyMapping LEFT = key("left", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_A);
    public static final KeyMapping RIGHT = key("right", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_D);
    public static final KeyMapping UP = key("up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE);
    public static final KeyMapping DOWN = key("down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT);
    public static final KeyMapping LEFT_YAW = key("left_yaw", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q);
    public static final KeyMapping RIGHT_YAW = key("right_yaw", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E);
    public static final KeyMapping FUNCTIONAL_UP = key("functional_up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP);
    public static final KeyMapping FUNCTIONAL_DOWN = key("functional_down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN);
    public static final KeyMapping FUNCTIONAL_LEFT = key("functional_left", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping FUNCTIONAL_RIGHT = key("functional_right", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping TOGGLE_ENGINE = key("toggle_engine", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I);
    public static final KeyMapping TOGGLE_LANDING_GEAR = key("toggle_landing_gear", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G);
    public static final KeyMapping TOGGLE_HOVER_MODE = key("toggle_hover_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z);

    // 武器控制类
    public static final KeyMapping FIRE_CONTROL_LOCK = key("fire_control_lock", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R);
    public static final KeyMapping MAIN_WEAPON_SHOOT = key("main_weapon_shoot", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT);
    public static final KeyMapping MAGNIFICATION_CHANGE = key("magnification_change", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    public static final KeyMapping DECOY_FLARE_LAUNCH = key("decoy_flare_launch", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT);

    // 视角与交互类
    public static final KeyMapping SWITCH_VIEW = key("switch_view", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V);
    public static final KeyMapping SWITCH_SCOPE = key("switch_scope", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X);
    public static final KeyMapping FREE_CAMERA = key("free_camera", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C);
    public static final KeyMapping OPEN_INVENTORY = key("open_inventory", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B);
    public static final KeyMapping LEAVE_VEHICLE = key("leave_vehicle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J);

    // 座位切换
    public static final KeyMapping CHANGE_SEAT = key("change_seat", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT);

    private static final KeyMapping[] ALL_KEYS = {
            FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN, LEFT_YAW, RIGHT_YAW, FUNCTIONAL_UP, FUNCTIONAL_DOWN, FUNCTIONAL_LEFT, FUNCTIONAL_RIGHT, TOGGLE_ENGINE, TOGGLE_LANDING_GEAR, TOGGLE_HOVER_MODE,
            FIRE_CONTROL_LOCK, MAIN_WEAPON_SHOOT, MAGNIFICATION_CHANGE, DECOY_FLARE_LAUNCH,
            SWITCH_VIEW, SWITCH_SCOPE, FREE_CAMERA, OPEN_INVENTORY, LEAVE_VEHICLE,
            CHANGE_SEAT
    };

    private static KeyMapping key(String name, InputConstants.Type type, int key) {
        return new KeyMapping("key.ywzj_vehicle." + name + ".desc",
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                type,
                key,
                "key.category.ywzj_vehicle");
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : ALL_KEYS) {
            event.register(mapping);
        }
    }

}
