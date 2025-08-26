package org.ywzj.vehicle.vehicle;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;

public class LocalVehiclePlayer {

    public static LocalVehiclePlayer instance;
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
    private WeaponUnit lastWeaponUnit;
    private boolean mouseTurnedAfterScope;
    public ViewType viewType = ViewType.DEFAULT;
    static {
        instance = new LocalVehiclePlayer();
    }

    public LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    public void tick() {
        // 根据视角与载具地形适应来更新摄像头位置
        if (onVehicle()) {
            AbstractVehicle vehicle = getVehicle();
            WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(getPlayer());
            if (viewType == ViewType.DEFAULT) {
                Vec3 vehicleCameraPos = weaponUnit != null ? weaponUnit.worldOperatorPosition()
                        : vehicle.relativeRotPos(getPlayer().position()
                        .add(new Vec3(0, getPlayer().getEyeHeight(), 0))
                        .add(vehicle.getCameraOffset()));
                cameraXO = cameraX;
                cameraYO = cameraY;
                cameraZO = cameraZ;
                cameraX = vehicleCameraPos.x;
                cameraY = vehicleCameraPos.y;
                cameraZ = vehicleCameraPos.z;
            } else if (viewType == ViewType.SCOPE) {
                if (weaponUnit == null) {
                    return;
                }
                Vector2f barrelAimRot = weaponUnit.worldRot();
                cameraAimRotXO = cameraAimRotX;
                cameraAimRotYO = cameraAimRotY;
                cameraAimRotX = barrelAimRot.x;
                cameraAimRotY = barrelAimRot.y;
                float yDiff = cameraAimRotY - cameraAimRotYO;
                if (Math.abs(yDiff) > 90) {
                    cameraAimRotYO += cameraAimRotYO < 0 ? 360f : -360f;
                }
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

    public void switchViewType(ViewType toViewType) {
        if (!onVehicle()) {
            return;
        }
        WeaponUnit weaponUnit = getVehicle().getOwnWeaponUnit(getPlayer());
        if (weaponUnit == null) {
            return;
        }
        if (toViewType == null) {
            if (viewType == ViewType.DEFAULT) {
                toViewType = ViewType.SCOPE;
            } else if (viewType == ViewType.SCOPE) {
                toViewType = ViewType.DEFAULT;
            }
        }
        if (toViewType == ViewType.DEFAULT) {
            Vec3 start = weaponUnit.ammoSpawnPosition();
            Vec3 end = start.add(VectorUtil.calculateViewVector(cameraAimRotX, cameraAimRotY).normalize().scale(128));
            BlockHitResult result = getPlayer().level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, getPlayer()));
            Vec3 hitPos = result.getLocation();
            Vec3 eyePos = getPlayer().position().add(new Vec3(0, getPlayer().getEyeHeight(), 0));
            double d0 = hitPos.x - eyePos.x;
            double d1 = hitPos.y - eyePos.y;
            double d2 = hitPos.z - eyePos.z;
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            getPlayer().setXRot(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * (double)(180F / (float)Math.PI))) + 10f));
            getPlayer().setYRot(Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F));
        } else if (toViewType == ViewType.SCOPE) {
            Vector2f barrelAimRot = weaponUnit.worldRot();
            // 切换开镜后，若鼠标未移动，仍向开镜前第三人称预瞄的方向自动旋转
            mouseTurnedAfterScope = false;
            cameraAimRotX = barrelAimRot.x;
            cameraAimRotY = barrelAimRot.y;
        }
        viewType = toViewType;
    }

    public void mouseTurn(double pYRot, double pXRot) {
        if (pYRot == 0 && pXRot == 0) {
            return;
        }
        if (!onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = getVehicle();
        WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(getPlayer());
        if (weaponUnit == null) {
            return;
        }
        Vector2f barrelAimRot = weaponUnit.worldRot();
        if (lastWeaponUnit != weaponUnit) {
            lastWeaponUnit = weaponUnit;
            scopeAimRotX = barrelAimRot.x;
            scopeAimRotY = barrelAimRot.y;
        }
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
                v1 = Math.max(0.001f, (70 - t1 * 10) / 100);
            }
            scopeAimRotX = Mth.wrapDegrees((float) (scopeAimRotX + pXRot * v1));
            float t2 = Mth.abs(Mth.wrapDegrees(scopeAimRotY - barrelAimRot.y)) / weaponUnit.yRotSpeed;
            float v2 = 1;
            if (t2 > 5f) {
                // 运动平滑
                v2 = Math.max(0.001f, (70 - t2 * 10) / 100);
            }
            scopeAimRotY = Mth.wrapDegrees((float) (scopeAimRotY + pYRot * v2));
        }
    }

    public boolean onVehicle() {
        return getPlayer().getVehicle() instanceof AbstractVehicle;
    }

    public AbstractVehicle getVehicle() {
        if (onVehicle()) {
            return (AbstractVehicle) getPlayer().getVehicle();
        }
        return null;
    }

    /**
     * 第三人称瞄准方法
     * 摄像头自由预瞄某落点，返回让炮塔旋转去瞄准该落点的XY转向
     */
    public Vec2 cameraToWeaponRot() {
        AbstractVehicle vehicle = getVehicle();
        if (vehicle == null) {
            return null;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        cameraAimRotX = camera.getXRot() - 10;
        cameraAimRotY = camera.getYRot();
        Vec3 start = camera.getPosition();
        Vec3 end = start.add(VectorUtil.calculateViewVector(cameraAimRotX, cameraAimRotY).normalize().scale(128));
        BlockHitResult result = getPlayer().level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, getPlayer()));
        Vec3 hitPos = result.getLocation();
        WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(getPlayer());
        if (weaponUnit == null) {
            return null;
        }
        return weaponUnit.aim(hitPos);
    }

    /**
     * 第一人称瞄准方法
     * 鼠标运动后控制炮塔旋转，返回炮塔应去的XY转向
     * 该转向受mouseTurn方法更新与约束
     */
    public Vec2 scopeAimRot() {
        if (!mouseTurnedAfterScope) {
            return null;
        }
        AbstractVehicle vehicle = getVehicle();
        if (vehicle == null) {
            return null;
        }
        WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(getPlayer());
        if (weaponUnit == null) {
            return null;
        }
        Vec3 worldAim = VectorUtil.calculateViewVector(scopeAimRotX, scopeAimRotY);
        return weaponUnit.vecToRot(worldAim);
    }

    public void sendMessage(String message) {
        getPlayer().displayClientMessage(Component.translatable(message), true);
    }

    public enum ViewType {
        DEFAULT, SCOPE
    }

}
