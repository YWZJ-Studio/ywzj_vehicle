package org.ywzj.vehicle.vehicle;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.entity.RemoteTickEntity;
import org.ywzj.vehicle.client.shader.CrtHandler;
import org.ywzj.vehicle.client.shader.OverloadHandler;
import org.ywzj.vehicle.client.shader.ThermalHandler;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.control.InputHandler;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.concurrent.ConcurrentHashMap;
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
    public float playerLerpSteps;
    public float playerLerpXRot;
    public float playerLerpYRot;
    public Vec3 weaponHitPos;
    public Vec3 weaponHitPosO;
    public float currentG = 1;
    public float stamina = 100;
    public boolean lostControl;
    public int endureTick;
    public int unconsciousnessTick;
    public Vec3 lastVelocity = Vec3.ZERO;
    public double aimLocationDistance;
    public boolean outOfRangeFinding;
    public boolean mouseTurnedAfterScope;
    public AbstractVehicle.Seat seat;
    public ViewType viewType = ViewType.THIRD_PERSON;
    public int onVehicleTickCount;
    private final ReentrantLock lock = new ReentrantLock();
    public ConcurrentHashMap<Integer, ServerEntity> serverEntities = new ConcurrentHashMap<>();
    public ConcurrentHashMap<MissileEntity, Integer> missiles = new ConcurrentHashMap<>();
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
            onVehicleTickCount += 1;
        } else {
            onVehicleTickCount = 0;
        }
        tickOverload();
        tickRemote();
        tickLerp();
        tickAim();
        checkState();
    }

    private void tickOverload() {
        if (stamina != 100) {
            stamina += stamina < 100 ? 1 : -1;
        }
        if (lostControl) {
            if (unconsciousnessTick > 0) {
                unconsciousnessTick -= 1;
                if (unconsciousnessTick <= 0) {
                    lostControl = false;
                }
            }
        }
        AbstractVehicle vehicle = getVehicle();
        if (vehicle == null || vehicle.uav) {
            endureTick = 0;
            currentG = 1;
            return;
        } else {
            if (!OverloadHandler.isActive()) {
                OverloadHandler.setActive(true);
            }
        }
        float gravity = PhysicsEngine.G * 400;
        Vec3 currentVelocity = vehicle.getDeltaMovement();
        Vec3 deltaV = currentVelocity.subtract(lastVelocity);
        Vec3 accelerationVec = deltaV.scale(400);
        Vec3 gravityVec = new Vec3(0, -gravity, 0);
        Vec3 apparentAcceleration = accelerationVec.subtract(gravityVec);
        Vec3 upDirection = new Vec3(vehicle.getMainCubeOBB().obb().getAxes()[1]).normalize();
        double verticalAcceleration = apparentAcceleration.dot(upDirection);
        currentG = (float) (verticalAcceleration / gravity);
        if (!lostControl) {
            if (currentG >= 2 || currentG <= -1) {
                endureTick += 1;
            } else if (endureTick > 0) {
                endureTick -= 1;
            }
            float endureG = currentG - 1;
            stamina = Mth.clamp(stamina - endureG * 0.8f, 0, 120);
            if (stamina == 0 || stamina == 120) {
                lostControl = true;
                unconsciousnessTick = 60;
            }
        }
        lastVelocity = currentVelocity;
    }

    private void tickRemote() {
        serverEntities.values().forEach(serverEntity -> {
            if (serverEntity.entity instanceof RemoteTickEntity entity) {
                entity.remoteTick();
            }
        });
    }

    private void tickLerp() {
        if (playerLerpSteps > 0) {
            Player player = getPlayer();
            float dXRot = (float) (Mth.wrapDegrees(playerLerpXRot - player.getXRot()) / Math.pow(playerLerpSteps, 0.2));
            float dYRot = (float) (Mth.wrapDegrees(playerLerpYRot - player.getYRot()) / Math.pow(playerLerpSteps, 0.2));
            player.xRotO += dXRot;
            player.yRotO += dYRot;
            player.setXRot(player.getXRot() + dXRot);
            player.setYRot(player.getYRot() + dYRot);
            playerLerpSteps -= 1;
        }
    }

    private void tickAim() {
        if (!onVehicle()) {
            return;
        }
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
                    currentWeaponUnit = currentWeaponUnit.getRootParentWeaponUnit();
                }
                weaponHitPosO = weaponHitPos;
                weaponHitPos = currentWeaponUnit.aimHitPosition();
            });
        }
        cameraXO = cameraX;
        cameraYO = cameraY;
        cameraZO = cameraZ;
        if (partUnit == null) {
            cameraX = player.getX();
            cameraY = vehicle.getAABB().maxY + 4;
            cameraZ = player.getZ();
            return;
        }
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
                    if (!(InputHandler.freeCamera || playerLerpSteps > 0) || vehicle instanceof RotaryWingVehicle) {
                        weaponUnit.aim(freeAimPos());
                    }
                }
                cameraAimRotX = player.getXRot();
                cameraAimRotY = player.getYRot();
                if (viewType != ViewType.THIRD_PERSON) {
                    float yRotDiff = Mth.wrapDegrees(player.getYRot() - vehicle.getYRot());
                    float xRotDiff = Mth.wrapDegrees(player.getXRot() - vehicle.getXRot());
                    Quaternionf q = new Quaternionf();
                    q.rotateY((float) Math.toRadians(vehicle.getYRot()))
                            .rotateX((float) Math.toRadians(vehicle.getXRot()))
                            .rotateZ((float) Math.toRadians(vehicle.getZRot()));
                    q.rotateY((float) Math.toRadians(-yRotDiff));
                    q.rotateX((float) Math.toRadians(xRotDiff));
                    Vector3f rot = new Vector3f();
                    q.getEulerAnglesYXZ(rot);
                    cameraAimRotZ = (float) Math.toDegrees(rot.z);
                } else {
                    cameraAimRotZ = 0;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                fixLerp();
            }
        } else if (viewType == ViewType.SCOPE) {
            if (partUnit instanceof WeaponUnit weaponUnit
                    && weaponUnit.getOpticalSightType() != WeaponUnitData.OpticalSightType.NONE) {
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
                    PacketDistributor.sendToServer(control);
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
                ThermalHandler.setActive(false);
                if (toViewType == null) {
                    if (viewType == ViewType.THIRD_PERSON) {
                        if (weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.NONE) {
                            toViewType = ViewType.OPERATOR;
                        } else {
                            toViewType = ViewType.SCOPE;
                        }
                    } else if (viewType == ViewType.SCOPE) {
                        toViewType = ViewType.OPERATOR;
                    } else if (viewType == ViewType.OPERATOR) {
                        toViewType = ViewType.THIRD_PERSON;
                    }
                }
                if (toViewType == ViewType.THIRD_PERSON && weaponUnit.getYRotSpeed() > 0) {
                    thirdPersonCameraAimAt(weaponUnit.aimHitPosition(), vehicle);
                } else if (toViewType == ViewType.SCOPE) {
                    if (weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.NONE) {
                        return;
                    }
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

    public boolean handlePlayerTurn(double pYRot, double pXRot) {
        if (pYRot == 0 && pXRot == 0) {
            return false;
        }
        Player player = getPlayer();
        if (!onVehicle()) {
            return false;
        }
        if (pYRot > 0.05 || pXRot > 0.05) {
            playerLerpSteps = 0;
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
                    return true;
                }
                pXRot *= 0.15f;
                pYRot *= 0.15f;
                float tx = Mth.abs(weaponUnit.getXAimRot() - weaponUnit.getXRot()) % 360 / weaponUnit.getXRotSpeed();
                float vx = Math.min(weaponUnit.getXRotSpeed() / 16, weaponUnit.getXRotSpeed() / (tx * 10));
                if ((pXRot > 0 && weaponUnit.getXAimRot() < weaponUnit.xRotMax - weaponUnit.xSelfRot) || (pXRot < 0 && weaponUnit.getXAimRot() > weaponUnit.xRotMin - weaponUnit.xSelfRot)) {
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
                if ((pYRot > 0 && weaponUnit.getYAimRot() < weaponUnit.yRotMax - weaponUnit.ySelfRot) || (pYRot < 0 && weaponUnit.getYAimRot() > weaponUnit.yRotMin - weaponUnit.ySelfRot)) {
                    float ry = (float) (weaponUnit.getYAimRot() + pYRot * vy);
                    if (ry > 180) {
                        ry = -180 + (ry - 180);
                    } else if (ry < -180) {
                        ry = 180 + (ry + 180);
                    }
                    weaponUnit.setYAimRot(ry);
                }
                return true;
            }
        }
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
        float fX = (float) Math.toRadians(pXRot * 0.15F);
        float fY = (float) Math.toRadians(pYRot * 0.15F);
        Quaternionf playerRot = new Quaternionf()
                .rotationYXZ((float) Math.toRadians(-player.getYRot()),
                        (float) Math.toRadians(player.getXRot()),
                        (float) Math.toRadians(cameraAimRotZ));
        playerRot.rotateY(-fY);
        playerRot.rotateX(fX);
        Vector3f euler = new Vector3f();
        playerRot.getEulerAnglesYXZ(euler);
        player.setYRot((float) Math.toDegrees(-euler.y));
        player.setXRot(Mth.clamp((float) Math.toDegrees(euler.x), -90.0F, 90.0F));
        vehicle.onPassengerTurned(player);
        return true;
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
        if (Math.abs(cameraAimRotX - cameraAimRotXO) > 90) {
            cameraAimRotXO += cameraAimRotXO < 0 ? 360f : -360f;
        }
        if (Math.abs(cameraAimRotY - cameraAimRotYO) > 90) {
            cameraAimRotYO += cameraAimRotYO < 0 ? 360f : -360f;
        }
        if (Math.abs(cameraAimRotZ - cameraAimRotZO) > 90) {
            cameraAimRotZO += cameraAimRotZO < 0 ? 360f : -360f;
        }
    }

    public void checkState() {
        Player player = getPlayer();
        AbstractVehicle vehicle = getVehicle();
        boolean crt = vehicle != null
                && viewType == ViewType.SCOPE
                && vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit
                && weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.CRT;
        if (CrtHandler.isActive() && !crt) {
            CrtHandler.setActive(false);
        } else if (!CrtHandler.isActive() && crt) {
            CrtHandler.setActive(true);
        }
        if (OverloadHandler.isActive()) {
            if (vehicle == null) {
                OverloadHandler.setActive(false);
            }
        }
        serverEntities.values().removeIf(serverEntity -> serverEntity.updateTick + AllConfigs.server.serverBroadcastEntitiesInterval.get() * 5 < player.tickCount);
        missiles.values().removeIf(tickCount -> tickCount + 2 < player.tickCount);
    }

    public void clear() {
        serverEntities.clear();
        missiles.clear();
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
        double renderDistance = renderDistance();
        double aimLocationDistance = getPlayer().position().distanceTo(worldPos);
        this.outOfRangeFinding = aimLocationDistance > renderDistance;
        this.aimLocationDistance = Math.min(aimLocationDistance, renderDistance);
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

    public static class ServerEntity {
        public Entity entity;
        public Integer updateTick;
    }

}
