package org.ywzj.vehicle.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class FirstPersonHandler {

    public static float zRot;
    
    // Screen shake system
    private static float shakeIntensity = 0.0f;
    private static float shakeDecay = 0.0f;
    private static long lastShakeTime = 0;

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
            
            // Apply screen shake
            if (shakeIntensity > 0.01f) {
                updateScreenShake();
                
                // Random shake offsets
                float shakeX = (float) (Math.random() - 0.5) * shakeIntensity;
                float shakeY = (float) (Math.random() - 0.5) * shakeIntensity;
                float shakeRoll = (float) (Math.random() - 0.5) * shakeIntensity * 0.5f;
                
                event.setPitch((float) (event.getPitch() + shakeX));
                event.setYaw((float) (event.getYaw() + shakeY));
                event.setRoll((float) (event.getRoll() + shakeRoll));
            }
        }
    }

    /**
     * Triggers screen shake effect.
     * 
     * @param intensity Shake intensity (0.0 to 10.0, recommended 2.0-5.0 for cannon)
     * @param duration Duration in milliseconds
     */
    public static void triggerScreenShake(float intensity, long duration) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shakeDecay = intensity / duration;
        lastShakeTime = System.currentTimeMillis();
    }

    /**
     * Updates and decays screen shake over time.
     */
    private static void updateScreenShake() {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastShakeTime;
        lastShakeTime = currentTime;
        
        // Decay shake intensity
        shakeIntensity -= shakeDecay * deltaTime;
        if (shakeIntensity < 0) {
            shakeIntensity = 0;
        }
    }
}
