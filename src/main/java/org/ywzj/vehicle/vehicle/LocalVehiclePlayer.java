package org.ywzj.vehicle.vehicle;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.HashSet;

@OnlyIn(Dist.CLIENT)
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
    public float cameraAimRotZ;
    public float cameraAimRotXO;
    public float cameraAimRotYO;
    public float cameraAimRotZO;
    public float scopeAimRotX;
    public float scopeAimRotY;
    public double aimLocationDistance;
    public boolean outOfRangeFinding;
    public HashSet<Integer> controllingMissileIds = new HashSet<>();
    private WeaponUnit lastWeaponUnit;
    private boolean mouseTurnedAfterScope;
    public ViewType viewType = ViewType.THIRD_PERSON;
    static {
        instance = new LocalVehiclePlayer();
    }

    public enum ViewType {
        THIRD_PERSON, SCOPE, OPERATOR
    }

    public Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public void tick() {
        // 根据视角与载具地形适应来更新摄像头位置
        if (onVehicle()) {
            AbstractVehicle vehicle = getVehicle();
            PartUnit partUnit = vehicle.getOwnOperatorUnit(getPlayer());
            if (partUnit != null) {
                if (viewType == ViewType.THIRD_PERSON || viewType == ViewType.OPERATOR) {
                    Vec3 vehicleCameraPos = viewType == ViewType.THIRD_PERSON ?
                            vehicle.thirdPersonPosition(getPlayer()) : partUnit.worldOwnerViewPosition();
                    cameraXO = cameraX;
                    cameraYO = cameraY;
                    cameraZO = cameraZ;
                    cameraX = vehicleCameraPos.x;
                    cameraY = vehicleCameraPos.y;
                    cameraZ = vehicleCameraPos.z;
                } else if (viewType == ViewType.SCOPE && partUnit instanceof WeaponUnit weaponUnit) {
                    Vec3 worldOpticalSightPosition = weaponUnit.getOpticalSightType() != WeaponUnit.OpticalSightType.OPERATOR ?
                            weaponUnit.worldOpticalSightPosition() : weaponUnit.worldOwnerViewPosition();
                    cameraXO = cameraX;
                    cameraYO = cameraY;
                    cameraZO = cameraZ;
                    cameraX = worldOpticalSightPosition.x;
                    cameraY = worldOpticalSightPosition.y;
                    cameraZ = worldOpticalSightPosition.z;
                    cameraAimRotXO = cameraAimRotX;
                    cameraAimRotYO = cameraAimRotY;
                    cameraAimRotZO = cameraAimRotZ;
                    if (weaponUnit.isStabilizerOn()) {
                        Vec3 pos = weaponUnit.aimHitPosition();
                        Vec2 scopeAimRot = cameraAim(pos, vehicle);
                        scopeAimRotX = scopeAimRot.x;
                        scopeAimRotY = scopeAimRot.y;
                        cameraAimAt(pos);
                    } else if (!mouseTurnedAfterScope) {
                        scopeAimRotX = weaponUnit.xRot;
                        scopeAimRotY = weaponUnit.yRot;
                        Vec2 barrelAimRot = weaponUnit.worldRot();
                        cameraAimRotX = barrelAimRot.x;
                        cameraAimRotY = barrelAimRot.y;
                    } else {
                        Vec2 aimRot = weaponUnit.worldRot(scopeAimRotX, scopeAimRotY);
                        cameraAimRotX = aimRot.x;
                        cameraAimRotY = aimRot.y;
                    }
                    Quaternionf rot = new Quaternionf();
                    rot.rotateY((float) Math.toRadians(-weaponUnit.combineYRot()));
                    rot.rotateX((float) Math.toRadians(-weaponUnit.xRot));
                    rot = vehicle.rotYXZ().mul(rot);
                    Vector3f eulerAngles = new Vector3f();
                    rot.getEulerAnglesYXZ(eulerAngles);
                    cameraAimRotZ = (float) Math.toDegrees(eulerAngles.z);
                    float yDiff = cameraAimRotY - cameraAimRotYO;
                    if (Math.abs(yDiff) > 90) {
                        cameraAimRotYO += cameraAimRotYO < 0 ? 360f : -360f;
                    }
                }
            }
        }
    }

    public void switchViewType(ViewType toViewType) {
        if (!onVehicle()) {
            return;
        }
        PartUnit partUnit = getVehicle().getOwnOperatorUnit(getPlayer());
        if (partUnit instanceof WeaponUnit weaponUnit) {
            if (toViewType == null) {
                if (viewType == ViewType.THIRD_PERSON) {
                    if (weaponUnit.getOpticalSightType() == WeaponUnit.OpticalSightType.NONE) {
                        return;
                    }
                    toViewType = ViewType.SCOPE;
                } else if (viewType == ViewType.SCOPE) {
                    toViewType = ViewType.OPERATOR;
                } else if (viewType == ViewType.OPERATOR) {
                    toViewType = ViewType.THIRD_PERSON;
                }
            }
            if (toViewType == ViewType.THIRD_PERSON) {
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
                // 切换开镜后，若鼠标未移动，仍向开镜前第三人称预瞄的方向自动旋转
                mouseTurnedAfterScope = false;
                scopeAimRotX = weaponUnit.xRot;
                scopeAimRotY = weaponUnit.yRot;
                Vec2 barrelAimRot = weaponUnit.worldRot();
                cameraAimRotX = barrelAimRot.x;
                cameraAimRotY = barrelAimRot.y;
            }
        } else {
            if (toViewType == null) {
                if (viewType == ViewType.THIRD_PERSON) {
                    toViewType = ViewType.OPERATOR;
                } else if (viewType == ViewType.OPERATOR) {
                    toViewType = ViewType.THIRD_PERSON;
                }
            }
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
        if (vehicle.getOwnOperatorUnit(getPlayer()) instanceof WeaponUnit weaponUnit) {
            if (lastWeaponUnit != weaponUnit) {
                lastWeaponUnit = weaponUnit;
                scopeAimRotX = weaponUnit.xRot;
                scopeAimRotY = weaponUnit.yRot;
            }
            if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
                scopeAimRotX = weaponUnit.xRot;
                scopeAimRotY = weaponUnit.yRot;
            } else if (viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                if (Math.abs(pXRot) >= 0.5 || Math.abs(pYRot) >= 0.5) {
                    mouseTurnedAfterScope = true;
                }
                if (!mouseTurnedAfterScope) {
                    return;
                }
                pXRot *= 0.15f;
                pYRot *= 0.15f;
                if (weaponUnit.isStabilizerOn()) {
                    Vec3 pos = cameraAimHit((float) pXRot, (float) pYRot).getLocation();
                    weaponUnit.setAimLockPosition(pos);
                } else {
                    float t1 = Mth.abs(scopeAimRotX - weaponUnit.xRot) / weaponUnit.getXRotSpeed();
                    float v1 = 1;
                    if (t1 > 5f) {
                        // 运动平滑
                        v1 = Math.max(0.00001f, (70 - t1 * 10) / 100);
                    }
                    scopeAimRotX = (float) (scopeAimRotX + pXRot * v1);
                    float t2 = Mth.abs(scopeAimRotY -  weaponUnit.yRot) / weaponUnit.getYRotSpeed();
                    float v2 = 1;
                    if (t2 > 5f) {
                        // 运动平滑
                        v2 = Math.max(0.00001f, (70 - t2 * 10) / 100);
                    }
                    scopeAimRotY = (float) (scopeAimRotY + pYRot * v2);
                    scopeAimRotX = Mth.clamp(scopeAimRotX, weaponUnit.getXRotMin(), weaponUnit.getXRotMax());
                    scopeAimRotY = Mth.clamp(scopeAimRotY, weaponUnit.getYRotMin(), weaponUnit.getYRotMax());
                }
            }
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

    public Vec2 cameraAim(Vec3 worldPos, AbstractVehicle vehicle) {
        Vec3 worldAim = new Vec3(worldPos.x - cameraX, worldPos.y - cameraY, worldPos.z - cameraZ);
        Vec3 vehicleVec = vehicle.relativeRotDirection(worldAim, true);
        float pitch = (float) Math.toDegrees(Math.atan2(-vehicleVec.y, Math.sqrt(worldAim.x * worldAim.x + worldAim.z * worldAim.z)));
        float yaw = (float) Math.toDegrees(-Math.atan2(vehicleVec.x, vehicleVec.z));
        return new Vec2(pitch, yaw);
    }

    public void cameraAimAt(Vec3 worldPos) {
        if (worldPos != null) {
            double dx = worldPos.x - cameraX;
            double dy = worldPos.y - cameraY;
            double dz = worldPos.z - cameraZ;
            double d = Math.sqrt(dx * dx + dz * dz);
            cameraAimRotX = Mth.wrapDegrees((float)(-(Mth.atan2(dy, d) * (double)(180F / (float)Math.PI))));
            cameraAimRotY = Mth.wrapDegrees((float)(Mth.atan2(dz, dx) * (double)(180F / (float)Math.PI)) - 90.0F);
        }
    }

    public BlockHitResult cameraAimHit(float xRot, float yRot) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 start = camera.getPosition();
        Vec3 end = start.add(VectorUtil.calculateViewVector(camera.getXRot() + xRot, camera.getYRot() + yRot)
                .normalize().scale(Minecraft.getInstance().options.renderDistance().get() * 16));
        return getPlayer().level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, getPlayer()));
    }

    /**
     * 摄像头自由预瞄某落点
     */
    public Vec3 freeAimRot() {
        AbstractVehicle vehicle = getVehicle();
        if (vehicle == null) {
            return null;
        }
        if (vehicle.getOwnOperatorUnit(getPlayer()) instanceof WeaponUnit) {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            cameraAimRotX = camera.getXRot() - 10;
            cameraAimRotY = camera.getYRot();
            return cameraAimHit(-10, 0).getLocation();
        }
        return null;
    }

    /**
     * 摄像头受限运动下预瞄某落点
     */
    public Vec3 scopeAimRot() {
        if (!mouseTurnedAfterScope) {
            return null;
        }
        AbstractVehicle vehicle = getVehicle();
        if (vehicle == null) {
            return null;
        }
        if (vehicle.getOwnOperatorUnit(getPlayer()) instanceof WeaponUnit) {
            BlockHitResult result = cameraAimHit(0, 0);
            Vec3 hitPos = result.getLocation();
            aimLocationDistance = getPlayer().position().distanceTo(hitPos);
            outOfRangeFinding = result.getType() == HitResult.Type.MISS;
            if (vehicle.getOwnOperatorUnit(getPlayer()) instanceof WeaponUnit weaponUnit) {
                return hitPos;
            }
        }
        return null;
    }

    public WeaponUnit getWeaponUnit() {
        if (getPlayer().getVehicle() instanceof AbstractVehicle vehicle) {
            if (vehicle.getOwnOperatorUnit(getPlayer()) instanceof WeaponUnit weaponUnit) {
                return weaponUnit;
            }
        }
        return null;
    }

    public void sendMessage(String message) {
        getPlayer().displayClientMessage(Component.translatable(message), true);
    }

}
