package org.ywzj.vehicle.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.ywzj.vehicle.Vehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleMoveControl;
import org.ywzj.vehicle.network.message.ClientWeaponUnitControl;
import org.ywzj.vehicle.vehicle.ControlUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

@Mod.EventBusSubscriber(modid = Vehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
    public static final KeyMapping MAIN_WEAPON_SHOOT_KEY = new KeyMapping("key.ywzj_vehicle.main_weapon_shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.category.ywzj_vehicle");
    private static long lastFireTimeMillis;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.gameMode == null) {
            return;
        }
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            if (player.equals(vehicle.controlUnit.operator)) {
                ControlUnit controlUnit = new ControlUnit();
                controlUnit.forward = FORWARD.isDown();
                controlUnit.backward = BACKWARD.isDown();
                controlUnit.left = LEFT.isDown();
                controlUnit.right = RIGHT.isDown();
                sendControl(vehicle, controlUnit);
            }
            if (MAIN_WEAPON_SHOOT_KEY.isDown()) {
                WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(player);
                if (weaponUnit == null) {
                    return;
                }
                Vec3 ammoSpawnPosition = weaponUnit.ammoSpawnPosition();
                Vector2f rot = weaponUnit.worldRot();
                int rpm = 400;
                float interval = 60f / rpm * 1000;
                if (System.currentTimeMillis() - lastFireTimeMillis > interval) {
                    sendShoot(vehicle, vehicle.weaponUnits.indexOf(weaponUnit), ammoSpawnPosition, rot.x, rot.y);
                    lastFireTimeMillis = System.currentTimeMillis();
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
        Channel.CHANNEL.sendToServer(control);
    }

    private static void sendShoot(AbstractVehicle abstractVehicle, int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        ClientWeaponUnitControl control = new ClientWeaponUnitControl();
        control.vehicleEntityId = abstractVehicle.getId();
        control.weaponIndex = weaponIndex;
        control.shoot = true;
        control.ammoX = (float) ammoSpawnPosition.x;
        control.ammoY = (float) ammoSpawnPosition.y;
        control.ammoZ = (float) ammoSpawnPosition.z;
        control.ammoXRot = ammoXRot;
        control.ammoYRot = ammoYRot;
        Channel.CHANNEL.sendToServer(control);
    }

}
