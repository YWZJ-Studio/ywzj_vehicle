package org.ywzj.vehicle.vehicle;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class LocalVehiclePlayer {

    public static LocalVehiclePlayer instance;
    private final LocalPlayer player;
    public int view;
    static {
        instance = new LocalVehiclePlayer();
    }

    private LocalVehiclePlayer() {
        player = Minecraft.getInstance().player;
    }

    public boolean onVehicle() {
        return player.getVehicle() instanceof AbstractVehicle;
    }

    public AbstractVehicle getVehicle() {
        if (onVehicle()) {
            return (AbstractVehicle) player.getVehicle();
        }
        return null;
    }

    public Vector2f cameraToWeaponRot() {
        AbstractVehicle vehicle = getVehicle();
        if (vehicle == null) {
            return null;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        float xRotR = (float) Math.toRadians(camera.getXRot() - 10);
        float yRotR = (float) Math.toRadians(camera.getYRot());
        Vector3f cameraDirection = new Vector3f(Mth.cos(xRotR) * Mth.sin(yRotR) , -Mth.sin(xRotR), Mth.cos(xRotR) * Mth.cos(yRotR));
        Matrix3f axisRollMatT = vehicle.calculateVehicleRot().transpose();
        Vector3f v = axisRollMatT.transform(cameraDirection);
        float yaw = (float) Math.toDegrees(Math.atan2(v.x, v.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-v.y, Math.hypot(v.x, v.z)));
        return new Vector2f(pitch, yaw);
    }

}
