package org.ywzj.vehicle.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@EventBusSubscriber(value = Dist.CLIENT)
public class FirstPersonHandler {

    public static float zRot;
    public static Vec3 shakePos;
    public static double shakeRadius;
    public static double shakeTime;
    public static double shakeAmplitude;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderOverlay(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON
                && player.getVehicle() instanceof AbstractVehicle) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (LocalVehiclePlayer.instance.onVehicle()) {
            if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE
                    || LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                event.setRoll(zRot);
            }
        }
        if (!LocalVehiclePlayer.instance.getPlayer().isSpectator()) {
            shake(event);
        }
    }

    /**
     * 参考自Superb Warfare
     */
    public static void shake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean onVehicle = LocalVehiclePlayer.instance.onVehicle();
        double yaw = event.getYaw();
        double pitch = event.getPitch();
        double roll = event.getRoll();
        shakeTime = Mth.lerp(0.05 * Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), shakeTime, 0.0);
        if (shakeTime > 0) {
            double shakeRadiusAmplitude = 1 - Mth.clamp(minecraft.player.position().distanceTo(shakePos) / shakeRadius, 0, 1);
            yaw = yaw + (shakeTime * Math.sin(1.2 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * (onVehicle ? 0.15 : 1.0));
            pitch = pitch - (shakeTime * Math.sin(1.2 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * (onVehicle ? 0.15 : 1.0));
            roll = roll - (shakeTime * Math.sin(1.2 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * (onVehicle ? 0.15 : 1.0));
        }
        event.setYaw((float) yaw);
        event.setPitch((float) pitch);
        event.setRoll((float) roll);
    }

}
