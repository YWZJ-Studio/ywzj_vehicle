package org.ywzj.vehicle.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class FirstPersonHandler {

    private static final int MAX_SHAKE_IMPULSES = 8;
    private static final double MAX_YAW_PITCH_SHAKE = 12;
    private static final double MAX_ROLL_SHAKE = 16;
    private static final double SHAKE_SETTLE_EPSILON = 0.001;
    private static final List<ShakeImpulse> SHAKE_IMPULSES = new ArrayList<>();
    private static double currentYawShake;
    private static double currentPitchShake;
    private static double currentRollShake;
    public static float zRot;

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
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
        if (localVehiclePlayer != null && localVehiclePlayer.onVehicle()) {
            if (localVehiclePlayer.viewType == LocalVehiclePlayer.ViewType.SCOPE
                    || localVehiclePlayer.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                event.setRoll(zRot);
            }
        }
        if (!player.isSpectator()) {
            shake(event);
        }
    }

    public static void addExplosionShake(Vec3 pos, double explosionRadius) {
        if (pos == null || explosionRadius <= 0) {
            return;
        }
        double radius = Math.max(1.0, 16.0 * explosionRadius);
        double strength = Mth.clamp(1.8 + explosionRadius * 0.42, 2.4, 27.0);
        double duration = Mth.clamp(0.75 + explosionRadius * 0.04, 0.85, 3.8);
        double phaseYaw;
        double phasePitch;
        double phaseRoll;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            RandomSource random = minecraft.level.random;
            phaseYaw = random.nextDouble() * Math.PI * 2.0;
            phasePitch = random.nextDouble() * Math.PI * 2.0;
            phaseRoll = random.nextDouble() * Math.PI * 2.0;
        } else {
            long seed = Double.doubleToLongBits(pos.x) * 31L + Double.doubleToLongBits(pos.y) * 17L + Double.doubleToLongBits(pos.z);
            phaseYaw = phaseFromSeed(seed);
            phasePitch = phaseFromSeed(seed * 31L + 17L);
            phaseRoll = phaseFromSeed(seed * 53L + 29L);
        }
        SHAKE_IMPULSES.add(new ShakeImpulse(pos, radius, strength, duration, phaseYaw, phasePitch, phaseRoll));
        while (SHAKE_IMPULSES.size() > MAX_SHAKE_IMPULSES) {
            SHAKE_IMPULSES.remove(0);
        }
    }

    public static void shake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean onVehicle = LocalVehiclePlayer.instance != null && LocalVehiclePlayer.instance.onVehicle();
        double deltaFrameTime = minecraft.getDeltaFrameTime();
        double dt = Mth.clamp(deltaFrameTime / 20.0, 0.0, 0.01);
        double yawOffset = 0.0;
        double pitchOffset = 0.0;
        double rollOffset = 0.0;
        Iterator<ShakeImpulse> iterator = SHAKE_IMPULSES.iterator();
        while (iterator.hasNext()) {
            ShakeImpulse impulse = iterator.next();
            impulse.age += dt;
            if (impulse.age >= impulse.duration) {
                iterator.remove();
                continue;
            }
            double distanceFactor = 1.0 - Mth.clamp(player.position().distanceTo(impulse.pos) / impulse.radius, 0.0, 1.0);
            distanceFactor *= 0.5 + distanceFactor * 0.5;
            if (distanceFactor <= 0.0) {
                continue;
            }
            double timeFactor = 1.0 - impulse.age / impulse.duration;
            double envelope = timeFactor * timeFactor;
            double base = impulse.strength * distanceFactor * envelope * (onVehicle ? 0.45 : 1.0);
            double time = impulse.age;
            yawOffset += base * (Math.sin(time * 42.0 + impulse.phaseYaw) * 1.05
                    + Math.sin(time * 83.0 + impulse.phaseYaw * 0.7) * 0.36);
            pitchOffset += base * (Math.sin(time * 50.0 + impulse.phasePitch) * 1.35
                    + Math.sin(time * 97.0 + impulse.phasePitch * 0.6) * 0.30);
            rollOffset += base * (Math.sin(time * 31.0 + impulse.phaseRoll) * 0.80
                    + Math.sin(time * 67.0 + impulse.phaseRoll * 0.8) * 0.25);
        }
        yawOffset = Mth.clamp(yawOffset, -MAX_YAW_PITCH_SHAKE, MAX_YAW_PITCH_SHAKE);
        pitchOffset = Mth.clamp(pitchOffset, -MAX_YAW_PITCH_SHAKE, MAX_YAW_PITCH_SHAKE);
        rollOffset = Mth.clamp(rollOffset, -MAX_ROLL_SHAKE, MAX_ROLL_SHAKE);
        double smooth = Mth.clamp((SHAKE_IMPULSES.isEmpty() ? 0.08 : 0.45) * deltaFrameTime, 0.0, 1.0);
        currentYawShake = Mth.lerp(smooth, currentYawShake, yawOffset);
        currentPitchShake = Mth.lerp(smooth, currentPitchShake, pitchOffset);
        currentRollShake = Mth.lerp(smooth, currentRollShake, rollOffset);
        if (SHAKE_IMPULSES.isEmpty()
                && Math.abs(currentYawShake) < SHAKE_SETTLE_EPSILON
                && Math.abs(currentPitchShake) < SHAKE_SETTLE_EPSILON
                && Math.abs(currentRollShake) < SHAKE_SETTLE_EPSILON) {
            currentYawShake = 0.0;
            currentPitchShake = 0.0;
            currentRollShake = 0.0;
            return;
        }
        event.setYaw((float) (event.getYaw() + currentYawShake));
        event.setPitch((float) (event.getPitch() + currentPitchShake));
        event.setRoll((float) (event.getRoll() + currentRollShake));
    }

    private static double phaseFromSeed(long seed) {
        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >>> 33;
        return ((seed >>> 11) * 0x1.0p-53) * Math.PI * 2.0;
    }

    private static class ShakeImpulse {

        private final Vec3 pos;
        private final double radius;
        private final double strength;
        private final double duration;
        private final double phaseYaw;
        private final double phasePitch;
        private final double phaseRoll;
        private double age;

        private ShakeImpulse(Vec3 pos, double radius, double strength, double duration, double phaseYaw, double phasePitch, double phaseRoll) {
            this.pos = pos;
            this.radius = radius;
            this.strength = strength;
            this.duration = duration;
            this.phaseYaw = phaseYaw;
            this.phasePitch = phasePitch;
            this.phaseRoll = phaseRoll;
        }

    }

}
