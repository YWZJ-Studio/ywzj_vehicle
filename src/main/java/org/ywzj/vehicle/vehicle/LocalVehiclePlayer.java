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
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
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
    public Vec3 weaponHitPos;
    public Vec3 weaponHitPosO;
    public double aimLocationDistance;
    public boolean outOfRangeFinding;
    public HashSet<MissileEntity> controllingMissiles = new HashSet<>();
    public boolean mouseTurnedAfterScope;
    public ViewType viewType = ViewType.THIRD_PERSON;
    public int tickCount;
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

    public static double renderDistance() {
        return Minecraft.getInstance().options.renderDistance().get() * 16;
    }

    public void tick() {
        if (onVehicle()) {
            tickCount += 1;
        } else {
            tickCount = 0;
        }
        tickAim();
    }

    private void tickAim() {
        if (onVehicle()) {
            if (lock.isLocked()) {
                return;
            }
            AbstractVehicle vehicle = getVehicle();
            Player player = getPlayer();
            PartUnit<?> partUnit = vehicle.getOwnOperatorUnit(player);
            if (partUnit instanceof WeaponUnit weaponUnit) {
                weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon -> {
                    WeaponUnit currentWeaponUnit = vehicleWeapon.getWeaponUnit();
                    if (currentWeaponUnit.isParentWeaponUnitAim()) {
                        currentWeaponUnit = currentWeaponUnit.getParentWeaponUnit();
                    }
                    weaponHitPosO = weaponHitPos;
                    weaponHitPos = currentWeaponUnit.aimHitPosition();
                });
            }
            if (partUnit != null) {
                cameraXO = cameraX;
                cameraYO = cameraY;
                cameraZO = cameraZ;
                cameraAimRotXO = cameraAimRotX;
                cameraAimRotYO = cameraAimRotY;
                cameraAimRotZO = cameraAimRotZ;
                if (viewType == ViewType.THIRD_PERSON || viewType == ViewType.OPERATOR) {
                    Vec3 vehicleCameraPos = viewType == ViewType.THIRD_PERSON ?
                            vehicle.thirdPersonPosition(player, null) : partUnit.worldOwnerViewPosition();
                    cameraX = vehicleCameraPos.x;
                    cameraY = vehicleCameraPos.y;
                    cameraZ = vehicleCameraPos.z;
                    try {
                        if (partUnit instanceof WeaponUnit weaponUnit) {
                            if (!InputHandler.freeCamera || vehicle instanceof RotaryWingVehicle) {
                                weaponUnit.aim(freeAimPos());
                            }
                        }
                        if (viewType == ViewType.THIRD_PERSON) {
                            cameraAimRotX = player.getXRot();
                            cameraAimRotY = player.getYRot();
                        } else {
                            float xRot = player.getXRot() - vehicle.getXRot();
                            float yRot = player.getYRot() - vehicle.getYRot();
                            Vec3 worldVec = vehicle.relativeRotDirection(VectorUtil.rotToVec(xRot, yRot), false);
                            cameraAimRotX = (float) Math.toDegrees(Math.atan2(-worldVec.y, Math.sqrt(worldVec.x * worldVec.x + worldVec.z * worldVec.z)));
                            cameraAimRotY = (float) Math.toDegrees(-Math.atan2(worldVec.x, worldVec.z));
                            cameraAimRotZ = vehicle.getZRot();
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    } finally {
                        fixLerp();
                    }
                } else if (viewType == ViewType.SCOPE) {
                    if (partUnit instanceof WeaponUnit weaponUnit) {
                        Vec3 worldScopePosition = weaponUnit.getOpticalSightType() != WeaponUnitData.OpticalSightType.OPERATOR ?
                                weaponUnit.worldOpticalSightPosition() : weaponUnit.worldOwnerViewPosition();
                        cameraX = worldScopePosition.x;
                        cameraY = worldScopePosition.y;
                        cameraZ = worldScopePosition.z;
                        try {
                            Quaternionf rot = new Quaternionf();
                            rot.rotateY((float) Math.toRadians(-weaponUnit.getYRot()));
                            rot.rotateX((float) Math.toRadians(-weaponUnit.getXRot()));
                            rot = weaponUnit.baseRot().mul(rot);
                            Vector3f eulerAngles = new Vector3f();
                            rot.getEulerAnglesYXZ(eulerAngles);
                            cameraAimRotZ = (float) Math.toDegrees(eulerAngles.z);
                            Vec3 hitPosition = scopeAimWeaponHit(weaponUnit);
                            ClientVehicleAction control = new ClientVehicleAction();
                            control.vehicleEntityId = vehicle.getId();
                            control.partUnitIndex = weaponUnit.getIndex();
                            control.xAimRot = weaponUnit.getXAimRot();
                            control.yAimRot = weaponUnit.getYAimRot();
                            Channel.CHANNEL.sendToServer(control);
                            weaponUnit.getSubWeaponUnits().forEach(subWeaponUnit -> subWeaponUnit.aim(hitPosition));
                            rangeFinding(hitPosition);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        } finally {
                            fixLerp();
                        }
                    } else {
                        switchViewType(ViewType.OPERATOR);
                    }
                }
            } else {
                cameraX = player.getX();
                cameraY = player.getEyeY() + 4;
                cameraZ = player.getZ();
                cameraXO = cameraX;
                cameraYO = cameraY;
                cameraZO = cameraZ;
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
                        if (weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.NONE) {
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
                    Vec3 worldScopePosition = weaponUnit.getOpticalSightType() != WeaponUnitData.OpticalSightType.OPERATOR ?
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
                if (!mouseTurnedAfterScope && (Math.abs(pXRot) >= 0.5 || Math.abs(pYRot) >= 0.5)) {
                    weaponUnit.setXAimRot(weaponUnit.getXRot());
                    weaponUnit.setYAimRot(weaponUnit.getYRot());
                    mouseTurnedAfterScope = true;
                }
                if (!mouseTurnedAfterScope) {
                    return;
                }
                pXRot *= 0.15f;
                pYRot *= 0.15f;
               float tx = Mth.abs(weaponUnit.getXAimRot() - weaponUnit.getXRot()) % 360 / weaponUnit.getXRotSpeed();
               float vx = Math.min(weaponUnit.getXRotSpeed() / 16, weaponUnit.getXRotSpeed() / (tx * 10));
               if ((pXRot > 0 && weaponUnit.getXAimRot() < weaponUnit.xRotMax) || (pXRot < 0 && weaponUnit.getXAimRot() > weaponUnit.xRotMin)) {
                   float rx = (float) (weaponUnit.getXAimRot() + pXRot * vx);
                   if (rx > 180) {
                       rx = -180 + (rx - 180);
                   } else if (rx < -180) {
                       rx = 180 + (rx + 180);
                   }
                   weaponUnit.setXAimRot(rx);
               }
               float ty = Mth.abs(weaponUnit.getYAimRot() - weaponUnit.getYRot()) % 360 / weaponUnit.getYRotSpeed();
               float vy = Math.min(weaponUnit.getYRotSpeed() / 16, weaponUnit.getYRotSpeed() / (ty * 10));
               if ((pYRot > 0 && weaponUnit.getYAimRot() < weaponUnit.yRotMax) || (pYRot < 0 && weaponUnit.getYAimRot() > weaponUnit.yRotMin)) {
                   float ry = (float) (weaponUnit.getYAimRot() + pYRot * vy);
                   if (ry > 180) {
                       ry = -180 + (ry - 180);
                   } else if (ry < -180) {
                       ry = 180 + (ry + 180);
                   }
                   weaponUnit.setYAimRot(ry);
               }
            }
        }
    }

    public boolean onVehicle() {
        Player player = getPlayer();
        if (player == null) {
            return false;
        }
        return player.getVehicle() instanceof AbstractVehicle;
    }

    public AbstractVehicle getVehicle() {
        if (onVehicle()) {
            return (AbstractVehicle) getPlayer().getVehicle();
        }
        return null;
    }

    public Vec3 scopeAimWeaponHit(WeaponUnit weaponUnit) {
        Vec3 hitPosition = weaponUnit.aimHitPosition();
        Vec3 boltPosition = weaponUnit.worldCurrentBoltPosition();
        if (hitPosition.distanceTo(boltPosition) < 128) {
            cameraAimAt(hitPosition.subtract(boltPosition).normalize().scale(128).add(boltPosition));
        } else {
            cameraAimAt(hitPosition);
        }
        return hitPosition;
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
        Vec3 offset = vehicle.isViewZoomed() ? vehicle.getViewInfo().thirdPersonCenterOffsetZoomed : vehicle.getViewInfo().thirdPersonCenterOffset;
        double distance = vehicle.isViewZoomed() ? vehicle.getViewInfo().thirdPersonDistanceZoomed : vehicle.getViewInfo().thirdPersonDistance;
        Vec3 thirdPersonPos = vehicle.relativeRotPos(vehicle.position().add(offset), false);
        double r = distance;
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
        Quaternionf rotation = new Quaternionf();
        rotation.rotateYXZ((float) -Math.toRadians(cameraAimRotY + yRot),
                (float) Math.toRadians(cameraAimRotX + xRot),
                (float) Math.toRadians(cameraAimRotZ));
        Vector3f direction = new Vector3f(0, 0, 1);
        rotation.transform(direction);
        Vec3 end = start.add(new Vec3(direction).scale(renderDistance()));
        return VectorUtil.hitPosition(getPlayer(), start, end);
    }

    private void fixLerp() {
        if (Math.abs(cameraAimRotY - cameraAimRotYO) > 90) {
            cameraAimRotYO += cameraAimRotYO < 0 ? 360f : -360f;
        }
        if (Math.abs(cameraAimRotZ - cameraAimRotZO) > 90) {
            cameraAimRotZO += cameraAimRotZO < 0 ? 360f : -360f;
        }
    }

    /**
     * 抬头视线落点
     */
    public Vec3 freeAimPos() {
        return cameraAimHit(-CAMERA_UPWARD_ANGLE, 0);
    }

    /**
     * 视线落点
     */
    public Vec3 scopeAimPos() {
        return cameraAimHit(0, 0);
    }

    public void rangeFinding(Vec3 worldPos) {
        aimLocationDistance = getPlayer().position().distanceTo(worldPos);
        outOfRangeFinding = aimLocationDistance > renderDistance();
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
