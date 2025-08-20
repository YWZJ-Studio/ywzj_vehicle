package org.ywzj.vehicle.vehicle;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;

public class LocalVehiclePlayer {

    public static LocalVehiclePlayer instance;
    private final LocalPlayer player;
    public double cameraX;
    public double cameraY;
    public double cameraZ;
    public double cameraXO;
    public double cameraYO;
    public double cameraZO;
    public float cameraAimRotX;
    public float cameraAimRotY;
    public int view;
    static {
        instance = new LocalVehiclePlayer();
    }

    private LocalVehiclePlayer() {
        player = Minecraft.getInstance().player;
    }

    public void tick() {
        if (onVehicle()) {
            AbstractVehicle vehicle = getVehicle();
            Vec3 vehicleCameraPos = vehicle.relativeRotPos(player.position()
                    .add(new Vec3(0, player.getEyeHeight(), 0))
                    .add(vehicle.getCameraOffset()));
            cameraXO = cameraX;
            cameraYO = cameraY;
            cameraZO = cameraZ;
            cameraX = vehicleCameraPos.x;
            cameraY = vehicleCameraPos.y;
            cameraZ = vehicleCameraPos.z;
        }
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
        cameraAimRotX = camera.getXRot() - 10;
        cameraAimRotY = camera.getYRot();
        Vec3 start = camera.getPosition();
        Vec3 end = start.add(VectorUtil.calculateViewVector(cameraAimRotX, cameraAimRotY).normalize().scale(128));
        BlockHitResult result = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hitPos = result.getLocation();
        WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(player);
        if (weaponUnit == null) {
            return null;
        }
        Vec3 breechBoltWorldPos = weaponUnit.boltPosition();
        Vec3 v = vehicle.relativeRotDirection(new Vec3(hitPos.x - breechBoltWorldPos.x, hitPos.y - breechBoltWorldPos.y, hitPos.z - breechBoltWorldPos.z), true);
        float pitch = (float) Math.toDegrees(Math.atan2(-v.y, Math.hypot(v.x, v.z)));
        float yaw = (float) Math.toDegrees(Math.atan2(v.x, v.z));
        return new Vector2f(pitch, yaw);
    }

}
