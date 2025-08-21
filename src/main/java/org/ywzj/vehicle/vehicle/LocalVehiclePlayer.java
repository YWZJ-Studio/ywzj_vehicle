package org.ywzj.vehicle.vehicle;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;

public class LocalVehiclePlayer {

    public static LocalVehiclePlayer instance;
    public LocalPlayer player;
    public double cameraX;
    public double cameraY;
    public double cameraZ;
    public double cameraXO;
    public double cameraYO;
    public double cameraZO;
    public float cameraAimRotX;
    public float cameraAimRotY;
    public float cameraAimRotXO;
    public float cameraAimRotYO;
    public float scopeAimRotX;
    public float scopeAimRotY;
    private boolean mouseTurnedAfterScope;
    public ViewType viewType = ViewType.DEFAULT;
    static {
        instance = new LocalVehiclePlayer();
    }

    private LocalVehiclePlayer() {
        player = Minecraft.getInstance().player;
    }

    public void tick() {
        if (player == null) {
            player = Minecraft.getInstance().player;
            return;
        }
        // 根据视角与载具地形适应来更新摄像头位置
        if (onVehicle()) {
            AbstractVehicle vehicle = getVehicle();
            if (viewType == ViewType.DEFAULT) {
                Vec3 vehicleCameraPos = vehicle.relativeRotPos(player.position()
                        .add(new Vec3(0, player.getEyeHeight(), 0))
                        .add(vehicle.getCameraOffset()));
                cameraXO = cameraX;
                cameraYO = cameraY;
                cameraZO = cameraZ;
                cameraX = vehicleCameraPos.x;
                cameraY = vehicleCameraPos.y;
                cameraZ = vehicleCameraPos.z;
            } else if (viewType == ViewType.SCOPE) {
                WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(player);
                if (weaponUnit == null) {
                    return;
                }
                Vector2f barrelAimRot = weaponUnit.worldRot();
                cameraAimRotXO = cameraAimRotX;
                cameraAimRotYO = cameraAimRotY;
                cameraAimRotX = barrelAimRot.x;
                cameraAimRotY = barrelAimRot.y;
                Vec3 ammoSpawnPosition = weaponUnit.ammoSpawnPosition();
                cameraXO = cameraX;
                cameraYO = cameraY;
                cameraZO = cameraZ;
                cameraX = ammoSpawnPosition.x;
                cameraY = ammoSpawnPosition.y;
                cameraZ = ammoSpawnPosition.z;
            }
        }
    }

    public void switchViewType() {
        if (!onVehicle()) {
            return;
        }
        WeaponUnit weaponUnit = getVehicle().getOwnWeaponUnit(player);
        if (weaponUnit == null) {
            return;
        }
        Vector2f barrelAimRot = weaponUnit.worldRot();
        if (viewType == ViewType.DEFAULT) {
            // 切换开镜后，若鼠标未移动，仍向开镜前第三人称预瞄的方向自动旋转
            mouseTurnedAfterScope = false;
            cameraAimRotX = barrelAimRot.x;
            cameraAimRotY = barrelAimRot.y;
            viewType = ViewType.SCOPE;
        } else if (viewType == ViewType.SCOPE) {
            Vec3 start = weaponUnit.ammoSpawnPosition();
            Vec3 end = start.add(VectorUtil.calculateViewVector(cameraAimRotX, cameraAimRotY).normalize().scale(128));
            BlockHitResult result = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 hitPos = result.getLocation();
            Vec3 eyePos = player.position().add(new Vec3(0, player.getEyeHeight(), 0));
            double d0 = hitPos.x - eyePos.x;
            double d1 = hitPos.y - eyePos.y;
            double d2 = hitPos.z - eyePos.z;
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            player.setXRot(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * (double)(180F / (float)Math.PI))) + 10f));
            player.setYRot(Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F));
            viewType = ViewType.DEFAULT;
        }
    }

    public void mouseTurn(double pYRot, double pXRot) {
        if (pYRot == 0 && pXRot == 0) {
            return;
        }
        if (!onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = getVehicle();
        WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(player);
        if (weaponUnit == null) {
            return;
        }
        Vector2f barrelAimRot = weaponUnit.worldRot();
        if (viewType == LocalVehiclePlayer.ViewType.DEFAULT) {
            scopeAimRotX = barrelAimRot.x;
            scopeAimRotY = barrelAimRot.y;
        } else if (viewType == LocalVehiclePlayer.ViewType.SCOPE) {
            if (!mouseTurnedAfterScope) {
                scopeAimRotX = barrelAimRot.x;
                scopeAimRotY = barrelAimRot.y;
            }
            if (Math.abs(pXRot) >= 0.5 || Math.abs(pYRot) >= 0.5) {
                mouseTurnedAfterScope = true;
            }
            if (!mouseTurnedAfterScope) {
                return;
            }
            pXRot *= 0.15f;
            pYRot *= 0.15f;
            scopeAimRotX = Mth.clamp(scopeAimRotX, weaponUnit.xRotMin, weaponUnit.xRotMax);
            float t1 = Mth.abs(Mth.wrapDegrees(scopeAimRotX - barrelAimRot.x)) / weaponUnit.xRotSpeed;
            float v1 = 1;
            if (t1 > 5f) {
                // 运动平滑
                v1 = Math.max(0, (70 - t1 * 10) / 100);
            }
            scopeAimRotX = (float) (scopeAimRotX + pXRot * v1);
            float t2 = Mth.abs(Mth.wrapDegrees(scopeAimRotY - barrelAimRot.y)) / weaponUnit.yRotSpeed;
            float v2 = 1;
            if (t2 > 5f) {
                // 运动平滑
                v2 = Math.max(0, (70 - t2 * 10) / 100);
            }
            scopeAimRotY = (float) (scopeAimRotY + pYRot * v2);
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

    /**
     * 第三人称瞄准方法
     * 摄像头自由预瞄某落点，返回让炮塔旋转去瞄准该落点的XY转向
     */
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

    /**
     * 第一人称瞄准方法
     * 鼠标运动后控制炮塔旋转，返回炮塔应去的XY转向
     * 该转向受mouseTurn方法更新与约束
     */
    public Vector2f scopeAimRot() {
        if (!mouseTurnedAfterScope) {
            return null;
        }
        AbstractVehicle vehicle = getVehicle();
        if (vehicle == null) {
            return null;
        }
        WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(player);
        if (weaponUnit == null) {
            return null;
        }
        Vec3 v = vehicle.relativeRotDirection(VectorUtil.calculateViewVector(scopeAimRotX, scopeAimRotY), true);
        float yaw = (float) Math.toDegrees(Math.atan2(v.x, v.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-v.y, Math.hypot(v.x, v.z)));
        return new Vector2f(pitch, yaw);
    }

    public enum ViewType {
        DEFAULT, SCOPE
    }

}
