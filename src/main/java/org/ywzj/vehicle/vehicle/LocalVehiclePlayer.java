package org.ywzj.vehicle.vehicle;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.HelicopterVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.control.InputHandler;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

@OnlyIn(Dist.CLIENT)
public class LocalVehiclePlayer {

    public static LocalVehiclePlayer instance;
    public static final float CAMERA_UPWARD_ANGLE = 10;
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
    public WeaponUnit lastWeaponUnit;
    public Vec3 weaponHitPos;
    public Vec3 weaponHitPosO;
    public float scopeAimRotX;
    public float scopeAimRotY;
    public double aimLocationDistance;
    public boolean outOfRangeFinding;
    public HashSet<Integer> controllingMissileIds = new HashSet<>();
    private boolean mouseTurnedAfterScope;
    public ViewType viewType = ViewType.THIRD_PERSON;
    private final ReentrantLock lock = new ReentrantLock();
    static {
        instance = new LocalVehiclePlayer();
    }

    public enum ViewType {
        THIRD_PERSON, SCOPE, OPERATOR
    }

    public Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public double renderDistance() {
        return Minecraft.getInstance().options.renderDistance().get() * 16;
    }

    public void tick() {
        tickAim();
    }

    private void tickAim() {
        if (onVehicle()) {
            if (lock.isLocked()) {
                return;
            }
            AbstractVehicle vehicle = getVehicle();
            PartUnit<?> partUnit = vehicle.getOwnOperatorUnit(getPlayer());
            if (partUnit instanceof WeaponUnit weaponUnit) {
                weaponHitPosO = weaponHitPos;
                weaponHitPos = weaponUnit.aimHitPosition();
            }
            if (partUnit != null) {
                cameraXO = cameraX;
                cameraYO = cameraY;
                cameraZO = cameraZ;
                if (viewType == ViewType.THIRD_PERSON || viewType == ViewType.OPERATOR) {
                    Vec3 vehicleCameraPos = viewType == ViewType.THIRD_PERSON ?
                            vehicle.thirdPersonPosition(getPlayer()) : partUnit.worldOwnerViewPosition();
                    cameraX = vehicleCameraPos.x;
                    cameraY = vehicleCameraPos.y;
                    cameraZ = vehicleCameraPos.z;
                    if (InputHandler.freeCamera && !(vehicle instanceof HelicopterVehicle)) {
                        return;
                    }
                    if (viewType == ViewType.THIRD_PERSON && partUnit instanceof WeaponUnit weaponUnit) {
                        if (!weaponUnit.isStabilizerOn()) {
                            weaponUnit.aim(freeAimPos());
                        }
                    } else {
                        cameraAimRotZO = cameraAimRotZ;
                        cameraAimRotZ = vehicle.getZRot();
                    }
                } else if (viewType == ViewType.SCOPE && partUnit instanceof WeaponUnit weaponUnit) {
                    Vec3 worldScopePosition = weaponUnit.getOpticalSightType() != WeaponUnit.OpticalSightType.OPERATOR ?
                            weaponUnit.worldOpticalSightPosition() : weaponUnit.worldOwnerViewPosition();
                    cameraX = worldScopePosition.x;
                    cameraY = worldScopePosition.y;
                    cameraZ = worldScopePosition.z;
                    cameraAimRotXO = cameraAimRotX;
                    cameraAimRotYO = cameraAimRotY;
                    cameraAimRotZO = cameraAimRotZ;
                    try {
                        Quaternionf rot = new Quaternionf();
                        rot.rotateY((float) Math.toRadians(-weaponUnit.combineYRot()));
                        rot.rotateX((float) Math.toRadians(-weaponUnit.getXRot()));
                        rot = vehicle.rotYXZ().mul(rot);
                        Vector3f eulerAngles = new Vector3f();
                        rot.getEulerAnglesYXZ(eulerAngles);
                        cameraAimRotZ = (float) Math.toDegrees(eulerAngles.z);
                        if (lastWeaponUnit != weaponUnit) {
                            lastWeaponUnit = weaponUnit;
                            scopeAimWeaponHit(weaponUnit);
                            return;
                        }
                        if (weaponUnit.isStabilizerOn() || !mouseTurnedAfterScope) {
                            scopeAimWeaponHit(weaponUnit);
                        } else {
                            weaponAimScopeHit(weaponUnit);
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    } finally {
                        if (Math.abs(cameraAimRotY - cameraAimRotYO) > 90) {
                            cameraAimRotYO += cameraAimRotYO < 0 ? 360f : -360f;
                        }
                        if (Math.abs(cameraAimRotZ - cameraAimRotZO) > 90) {
                            cameraAimRotZO += cameraAimRotZO < 0 ? 360f : -360f;
                        }
                    }
                }
            }
        }
    }

    public void switchViewType(ViewType toViewType) {
        if (!onVehicle()) {
            return;
        }
        lock.lock();
        try {
            AbstractVehicle vehicle = getVehicle();
            PartUnit<?> partUnit = getVehicle().getOwnOperatorUnit(getPlayer());
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
                    thirdPersonCameraAimAt(weaponUnit.aimHitPosition(), vehicle);
                } else if (toViewType == ViewType.SCOPE) {
                    Vec3 worldScopePosition = weaponUnit.getOpticalSightType() != WeaponUnit.OpticalSightType.OPERATOR ?
                            weaponUnit.worldOpticalSightPosition() : weaponUnit.worldOwnerViewPosition();
                    cameraX = worldScopePosition.x;
                    cameraY = worldScopePosition.y;
                    cameraZ = worldScopePosition.z;
                    scopeAimWeaponHit(weaponUnit);
                    // 切换开镜后，若鼠标未移动，仍向开镜前第三人称预瞄的方向自动旋转
                    mouseTurnedAfterScope = false;
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
            // 消除插值
            cameraXO = cameraX;
            cameraYO = cameraY;
            cameraZO = cameraZ;
        } catch (Exception exception) {
            YwzjVehicle.LOGGER.error("Failed while switch view type", exception);
        } finally {
            lock.unlock();
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
        if (vehicle.getOwnOperatorUnit(getPlayer()) instanceof WeaponUnit weaponUnit) {
           if (viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                if (Math.abs(pXRot) >= 0.5 || Math.abs(pYRot) >= 0.5) {
                    mouseTurnedAfterScope = true;
                }
                if (!mouseTurnedAfterScope) {
                    return;
                }
                pXRot *= 0.15f;
                pYRot *= 0.15f;
                if (weaponUnit.isStabilizerOn()) {
                    Vec3 pos = cameraAimHit((float) pXRot, (float) pYRot);
                    weaponUnit.setAimLockPosition(pos);
                } else {
                    float t1 = Mth.abs(weaponUnit.getXAimRot() - weaponUnit.getXRot()) / weaponUnit.getXRotSpeed();
                    float v1 = Math.min(weaponUnit.getXRotSpeed() / 15, 1 / t1 / 10);
                    if ((pXRot > 0 && weaponUnit.getXAimRot() < weaponUnit.xRotMax) || (pXRot < 0 && weaponUnit.getXAimRot() > weaponUnit.xRotMin)) {
                        scopeAimRotX = (float) (scopeAimRotX + pXRot * v1);
                    }
                    float t2 = Mth.abs(weaponUnit.getYAimRot() - weaponUnit.getYRot()) / weaponUnit.getYRotSpeed();
                    float v2 = Math.min(weaponUnit.getYRotSpeed() / 15, 1 / t2 / 10);
                    if ((pYRot > 0 && weaponUnit.getYAimRot() < weaponUnit.yRotMax) || (pYRot < 0 && weaponUnit.getYAimRot() > weaponUnit.yRotMin)) {
                        scopeAimRotY = (float) (scopeAimRotY + pYRot * v2);
                    }
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

    public void scopeAimWeaponHit(WeaponUnit weaponUnit) {
        Vec3 hitPosition = weaponUnit.aimHitPosition();
        cameraAimAt(hitPosition);
        Vec3 worldAim = new Vec3(hitPosition.x - cameraX, hitPosition.y - cameraY, hitPosition.z - cameraZ);
        Vec3 vehicleVec = weaponUnit.getVehicle().relativeRotDirection(worldAim, true);
        scopeAimRotX = (float) Math.toDegrees(Math.atan2(-vehicleVec.y, Math.sqrt(worldAim.x * worldAim.x + worldAim.z * worldAim.z)));
        scopeAimRotY = (float) Math.toDegrees(-Math.atan2(vehicleVec.x, vehicleVec.z));
    }

    public void weaponAimScopeHit(WeaponUnit weaponUnit) {
        Vec3 worldVec = weaponUnit.getVehicle().relativeRotDirection(VectorUtil.calculateViewVector(scopeAimRotX, scopeAimRotY), false);
        cameraAimRotX = (float) Math.toDegrees(Math.atan2(-worldVec.y, Math.sqrt(worldVec.x * worldVec.x + worldVec.z * worldVec.z)));
        cameraAimRotY = (float) Math.toDegrees(-Math.atan2(worldVec.x, worldVec.z));
        getPlayer().setXRot(cameraAimRotX);
        getPlayer().setYRot(cameraAimRotY);
        Vec3 hitPosition = scopeAimPos();
        weaponUnit.aim(hitPosition);
    }

    public void cameraAimAt(Vec3 worldPos) {
        if (worldPos != null) {
            double dx = worldPos.x - cameraX;
            double dy = worldPos.y - cameraY;
            double dz = worldPos.z - cameraZ;
            double d = Math.sqrt(dx * dx + dz * dz);
            cameraAimRotX = Mth.wrapDegrees((float)(-(Mth.atan2(dy, d) * (double)(180F / (float)Math.PI))));
            cameraAimRotY = Mth.wrapDegrees((float)(Mth.atan2(dz, dx) * (double)(180F / (float)Math.PI)) - 90.0F);
            getPlayer().setXRot(cameraAimRotX);
            getPlayer().setYRot(cameraAimRotY);
        }
    }

    public void thirdPersonCameraAimAt(Vec3 worldPos, AbstractVehicle vehicle) {
        Vec3 thirdPersonPos = vehicle.relativeRotPos(vehicle.position().add(vehicle.thirdPersonCenterOffset), false);
        double r = vehicle.thirdPersonDistance;
        double a = worldPos.distanceTo(thirdPersonPos);
        double c = Math.asin(r / (a / Math.sin(Math.PI * CAMERA_UPWARD_ANGLE / 180)));
        Vec3 dir = thirdPersonPos.subtract(worldPos).normalize();
        double yaw = Math.atan2(dir.z, dir.x);
        double pitch = Math.asin(dir.y) + c;
        Vec3 rotatedDir = new Vec3(Math.cos(pitch) * Math.cos(yaw), Math.sin(pitch), Math.cos(pitch) * Math.sin(yaw)).normalize();
        Vec3 oc = worldPos.subtract(thirdPersonPos);
        double b2 = 2 * rotatedDir.dot(oc);
        double c2 = oc.dot(oc) - r * r;
        double discriminant = b2 * b2 - 4 * c2;
        if (discriminant >= 0) {
            double sqrtD = Math.sqrt(discriminant);
            double t1 = (-b2 - sqrtD) / 2.0;
            double t2 = (-b2 + sqrtD) / 2.0;
            double t = Math.max(t1, t2);
            Vec3 intersection = worldPos.add(rotatedDir.scale(t));
            cameraX = intersection.x;
            cameraY = intersection.y;
            cameraZ = intersection.z;
            cameraAimAt(thirdPersonPos);
        }
    }

    public Vec3 cameraAimHit(float xRot, float yRot) {
        Vec3 start = new Vec3(cameraX, cameraY, cameraZ);
        Vec3 end = start.add(VectorUtil
                .calculateViewVector(getPlayer().getXRot() + xRot, getPlayer().getYRot() + yRot)
                .normalize()
                .scale(renderDistance()));
        return VectorUtil.hitPosition(getPlayer(), start, end);
    }

    /**
     * 抬头视线落点
     */
    public Vec3 freeAimPos() {
        return cameraAimHit(-CAMERA_UPWARD_ANGLE, 0);
    }

    /**
     * 视线落点与测距
     */
    public Vec3 scopeAimPos() {
        Vec3 hitPosition = cameraAimHit(0, 0);
        aimLocationDistance = getPlayer().position().distanceTo(hitPosition);
        outOfRangeFinding = aimLocationDistance > renderDistance();
        return hitPosition;
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
