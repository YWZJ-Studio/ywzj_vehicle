package org.ywzj.vehicle.vehicle;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.entity.RemoteTickEntity;
import org.ywzj.vehicle.client.screen.CoordinateInputScreen;
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
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class LocalVehiclePlayer {

    public static LocalVehiclePlayer instance;
    public static final float CAMERA_UPWARD_ANGLE = 10;
    public float cameraAimRotX;
    public float cameraAimRotY;
    public float cameraAimRotZ;
    public float cameraAimRotXO;
    public float cameraAimRotYO;
    public float cameraAimRotZO;
    public float playerLerpSteps;
    public float playerLerpXRot;
    public float playerLerpYRot;
    public float playerLocalXRot;
    public float playerLocalYRot;
    public float currentG = 1;
    public float stamina = 100;
    public boolean lostControl;
    public int endureTick;
    public int unconsciousnessTick;
    public Vec3 lastVelocity = Vec3.ZERO;
    public double aimLocationDistance;
    public boolean outOfRangeFinding;
    public boolean thermalImaging;
    public boolean mouseTurnedAfterScope;
    public AbstractVehicle.Seat seat;
    public boolean toLeave;
    public AbstractVehicle vehicle;
    public ViewType viewType = ViewType.THIRD_PERSON;
    public int onVehicleTickCount;
    public AbstractVehicle lookAtVehicle;
    public PartUnit<?> lookAtPartUnit;
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

    public boolean onVehicle() {
        return vehicle instanceof AbstractVehicle;
    }

    public static double renderDistance() {
        return Minecraft.getInstance().options.renderDistance().get() * 16;
    }

    public void tick() {
        tickLookAt();
        tickOverload();
        tickRemote();
        tickLerp();
        tickAim();
        checkState();
    }

    private void tickLookAt() {
        Player player = getPlayer();
        float rot = 0;
        if (onVehicle()) {
            rot = viewType != LocalVehiclePlayer.ViewType.SCOPE ? LocalVehiclePlayer.CAMERA_UPWARD_ANGLE : 0;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        float xRot = camera.getXRot() - rot;
        float yRot = camera.getYRot();
        Vec3 start = camera.getPosition();
        Vec3 end = start.add(VectorUtil
                .rotToVec(xRot, yRot)
                .normalize()
                .scale(LocalVehiclePlayer.renderDistance()));
        Pair<Entity, Vec3> hitResult = VectorUtil.hitObbPosition(player, start, end);
        if (hitResult != null) {
            Entity entity = hitResult.getLeft();
            if (entity instanceof AbstractVehicle vehicle && !vehicle.equals(player.getVehicle())) {
                lookAtVehicle = vehicle;
                lookAtPartUnit = VectorUtil.hitPartUnit(lookAtVehicle, start, end);
            }
        } else {
            lookAtVehicle = null;
            lookAtPartUnit = null;
        }
    }

    private void tickOverload() {
        if (!AllConfigs.common.overload.get()) {
            stamina = 100;
            lostControl = false;
            endureTick = 0;
            unconsciousnessTick = 0;
            currentG = 1;
            lastVelocity = Vec3.ZERO;
            if (OverloadHandler.isActive()) {
                OverloadHandler.setActive(false);
            }
            return;
        }
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
        AbstractVehicle vehicle = this.vehicle;
        if (vehicle == null || vehicle.uav || vehicle.onGround()) {
            endureTick = 0;
            currentG = 1;
            lastVelocity = Vec3.ZERO;
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
            float overloadCapacityMultiplier = AllConfigs.common.overloadCapacityMultiplier.get().floatValue();
            float positiveGLimit = 100 - 100 * overloadCapacityMultiplier;
            float negativeGLimit = 100 + 60 * overloadCapacityMultiplier;
            stamina = Mth.clamp(stamina - endureG * 0.8f, positiveGLimit, negativeGLimit);
            if (stamina <= positiveGLimit || stamina >= negativeGLimit) {
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
        AbstractVehicle vehicle = this.vehicle;
        vehicle.getPartUnits().forEach(partUnit -> {
            if (partUnit instanceof WeaponUnit weaponUnit) {
                weaponUnit.tickHit();
            }
        });
        if (Minecraft.getInstance().screen instanceof CoordinateInputScreen) {
            return;
        }
        Player player = getPlayer();
        PartUnit<?> partUnit = vehicle.getOwnOperatorUnit(player);
        if (partUnit == null) {
            return;
        }
        cameraAimRotXO = cameraAimRotX;
        cameraAimRotYO = cameraAimRotY;
        cameraAimRotZO = cameraAimRotZ;
        if (viewType == ViewType.THIRD_PERSON || viewType == ViewType.OPERATOR) {
            try {
                if (partUnit instanceof WeaponUnit weaponUnit) {
                    if (!(InputHandler.freeCamera || playerLerpSteps > 0) || vehicle instanceof RotaryWingVehicle) {
                        weaponUnit.aim(freeAimPos());
                    }
                }
                if (viewType == ViewType.OPERATOR) {
                    Quaternionf vehicleRot = vehicle.rotYXZ();
                    Quaternionf playerWorldRot = new Quaternionf();
                    playerWorldRot.rotateY((float) Math.toRadians(-player.getYRot()));
                    playerWorldRot.rotateX((float) Math.toRadians(player.getXRot()));
                    Quaternionf invVehicleRot = new Quaternionf(vehicleRot).invert();
                    Quaternionf localRot = invVehicleRot.mul(playerWorldRot);
                    Vector3f localEuler = new Vector3f();
                    localRot.getEulerAnglesYXZ(localEuler);
                    playerLocalXRot = (float) Math.toDegrees(localEuler.x);
                    playerLocalYRot = (float) Math.toDegrees(-localEuler.y);
                    vehicleRot.rotateY((float) Math.toRadians(-playerLocalYRot));
                    vehicleRot.rotateX((float) Math.toRadians(playerLocalXRot));
                    Vector3f rot = new Vector3f();
                    vehicleRot.getEulerAnglesYXZ(rot);
                    cameraAimRotX = (float) Math.toDegrees(rot.x);
                    cameraAimRotY = (float) Math.toDegrees(-rot.y);
                    cameraAimRotZ = (float) Math.toDegrees(rot.z);
                } else {
                    cameraAimRotX = player.getXRot();
                    cameraAimRotY = player.getYRot();
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
                try {
                    cameraAimRotZ = weaponUnit.worldZRot();
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
        try {
            AbstractVehicle vehicle = this.vehicle;
            PartUnit<?> partUnit = this.vehicle.getOwnOperatorUnit(getPlayer());
            if (partUnit instanceof WeaponUnit weaponUnit) {
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
                if (toViewType == ViewType.THIRD_PERSON || toViewType == ViewType.OPERATOR) {
                    thirdPersonCameraAimAt(weaponUnit.aimHitPosition(), vehicle);
                } else if (toViewType == ViewType.SCOPE) {
                    if (weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.NONE) {
                        return;
                    }
                    scopeAimWeaponHit(weaponUnit);
                    cameraAimRotZ = weaponUnit.worldZRot();
                    // 切换开镜后，若鼠标未移动，仍向开镜前第三人称预瞄的方向自动旋转
                    mouseTurnedAfterScope = false;
                }
                cameraAimRotXO = cameraAimRotX;
                cameraAimRotYO = cameraAimRotY;
                cameraAimRotZO = cameraAimRotZ;
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
        } catch (Exception exception) {
            YwzjVehicle.LOGGER.error("Failed while switch view type", exception);
        }
    }

    public void toSeat(AbstractVehicle.Seat seat, AbstractVehicle vehicle) {
        boolean enter = this.seat == null && seat != null;
        if (seat == null) {
            toLeave = false;
            clear();
            return;
        }
        this.seat = seat;
        this.vehicle = vehicle;
        if (seat.partUnit instanceof WeaponUnit weaponUnit) {
            if (viewType == ViewType.SCOPE) {
                scopeAimWeaponHit(weaponUnit);
                cameraAimRotXO = cameraAimRotX;
                cameraAimRotYO = cameraAimRotY;
                cameraAimRotZO = cameraAimRotZ;
            } else {
                Vec3 position = enter ? weaponUnit.aimHitPosition() : weaponUnit.toAimPosition();
                Vec3 cameraAnchor = vehicle.thirdPersonPosition(1.0F);
                Vec3 direction = position.subtract(cameraAnchor);
                if (direction.lengthSqr() < 1.0E-8) {
                    return;
                }
                Vec2 targetRot = VectorUtil.vecToRot(direction);
                playerLerpXRot = Mth.wrapDegrees(targetRot.x + CAMERA_UPWARD_ANGLE);
                playerLerpYRot = Mth.wrapDegrees(targetRot.y);
                playerLerpSteps = 8;
            }
        } else {
            playerLerpXRot = getPlayer().getXRot();
            playerLerpYRot = seat.partUnit.getSeatRot();
            playerLerpSteps = 8;
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
        if (viewType == ViewType.THIRD_PERSON) {
            if ((player.getXRot() > 85 && pXRot > 0) || (player.getXRot() < -85 && pXRot < 0)) {
                pYRot += Math.abs(pXRot) * (pYRot > 0 ? 1 : -1);
                pXRot = 0;
            }
        } else if (viewType == ViewType.OPERATOR) {
            if (playerLocalXRot >= 60 && pXRot > 0) {
                pXRot = 0;
            } else if (playerLocalXRot <= -60 && pXRot < 0) {
                pXRot = 0;
            }
        }
        AbstractVehicle vehicle = this.vehicle;
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
                // 焦点锁定
                if (weaponUnit.getFocusLockPos() != null) {
                    AimContext aimContext = weaponUnit.aimContext();
                    Vec3 start = weaponUnit.worldCurrentBoltPosition();
                    Vec3 end = start.add(VectorUtil
                            .rotToVec((float) (aimContext.direction.x + pXRot * 0.33f), (float) (aimContext.direction.y + pYRot * 0.33f))
                            .normalize()
                            .scale(1024));
                    weaponUnit.setFocusLockPos(VectorUtil.hitPosition(vehicle, start, end));
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
        float fX = (float) (pXRot * 0.15F);
        float fY = (float) (pYRot * 0.15F);
        Quaternionf rot;
        if (viewType == ViewType.OPERATOR) {
            playerLocalYRot = Mth.wrapDegrees(playerLocalYRot + fY);
            playerLocalXRot = Mth.wrapDegrees(playerLocalXRot + fX);
            rot = vehicle.rotYXZ();
            rot.rotateY((float) Math.toRadians(-playerLocalYRot));
            rot.rotateX((float) Math.toRadians(playerLocalXRot));
        } else {
            rot = new Quaternionf();
            rot.rotateY((float) Math.toRadians(-player.getYRot() - fY));
            rot.rotateX((float) Math.toRadians(player.getXRot() + fX));
        }
        Vector3f euler = new Vector3f();
        rot.getEulerAnglesYXZ(euler);
        float xRot = (float) Math.toDegrees(euler.x);
        float yRot = (float) Math.toDegrees(-euler.y);
        player.xRotO += xRot - player.getXRot();
        player.yRotO += yRot - player.getYRot();
        player.setXRot(xRot);
        player.setYRot(yRot);
        vehicle.onPassengerTurned(player);
        return true;
    }

    public Vec3 scopeAimWeaponHit(WeaponUnit weaponUnit) {
        Vec3 hitPosition = weaponUnit.aimHitPosition();
        Vec3 boltPosition = weaponUnit.worldCurrentBoltPosition();
        Vec3 cameraPosition = weaponUnit.getOpticalSightType() != WeaponUnitData.OpticalSightType.OPERATOR
                ? weaponUnit.worldOpticalSightPosition(1.0F)
                : weaponUnit.worldOwnerViewPosition(1.0F);
        if (hitPosition.distanceTo(boltPosition) < 128) {
            cameraAimAt(cameraPosition, hitPosition.subtract(boltPosition).normalize().scale(128).add(boltPosition));
        } else {
            cameraAimAt(cameraPosition, hitPosition);
        }
        return hitPosition;
    }

    public void cameraAimAt(Vec3 cameraPosition, Vec3 worldPos) {
        if (cameraPosition != null && worldPos != null) {
            Vec2 rot = VectorUtil.vecToRot(worldPos.subtract(cameraPosition));
            cameraAimRotX = rot.x;
            cameraAimRotY = rot.y;
            getPlayer().setXRot(rot.x);
            getPlayer().setYRot(rot.y);
        }
    }

    public void thirdPersonCameraAimAt(Vec3 worldPos, AbstractVehicle vehicle) {
        Vec3 cameraAnchor = vehicle.thirdPersonPosition(1.0F);
        Vec3 direction = worldPos.subtract(cameraAnchor);
        if (direction.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec2 targetRot = VectorUtil.vecToRot(direction);
        cameraAimRotX = Mth.wrapDegrees(targetRot.x + CAMERA_UPWARD_ANGLE);
        cameraAimRotY = Mth.wrapDegrees(targetRot.y);
        cameraAimRotZ = 0;
        getPlayer().setXRot(cameraAimRotX);
        getPlayer().setYRot(cameraAimRotY);
    }

    public Vec3 cameraAimHit(float xRot) {
        Vec3 start = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 direction = cameraDirection(xRot);
        Vec3 end = start.add(direction.scale(renderDistance()));
        return VectorUtil.hitPosition(getPlayer(), start, end);
    }

    public Vec3 cameraDirection(float upward) {
        Quaternionf cameraRot = cameraRotation().rotateX((float) Math.toRadians(-upward));
        Vector3f direction = new Vector3f(0, 0, 1);
        cameraRot.transform(direction);
        return new Vec3(direction);
    }

    public Quaternionf cameraRotation() {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-cameraAimRotY))
                .rotateX((float) Math.toRadians(cameraAimRotX))
                .rotateZ((float) Math.toRadians(cameraAimRotZ));
    }

    public Quaternionf cameraRotationO() {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-cameraAimRotYO))
                .rotateX((float) Math.toRadians(cameraAimRotXO))
                .rotateZ((float) Math.toRadians(cameraAimRotZO));
    }

    public Vec3 cameraPosition(PartUnit<?> operatorUnit, float partialTick) {
        Vec3 position;
        if (viewType == LocalVehiclePlayer.ViewType.SCOPE && operatorUnit instanceof WeaponUnit weaponUnit) {
            position = weaponUnit.getOpticalSightType() != WeaponUnitData.OpticalSightType.OPERATOR ?
                    weaponUnit.worldOpticalSightPosition(partialTick) : weaponUnit.worldOwnerViewPosition(partialTick);
        } else if (viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
            position = operatorUnit.worldOwnerViewPosition(partialTick);
        } else if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
            position = vehicle.thirdPersonPosition(partialTick);
        } else {
            position = vehicle.position().add(0, vehicle.getBoundingBox().getYsize() + getPlayer().getEyeHeight(), 0);
        }
        return position;
    }

    private void fixLerp() {
        if (Math.abs(cameraAimRotX - cameraAimRotXO) > 180) {
            cameraAimRotXO += cameraAimRotXO < 0 ? 360f : -360f;
        }
        if (Math.abs(cameraAimRotY - cameraAimRotYO) > 180) {
            cameraAimRotYO += cameraAimRotYO < 0 ? 360f : -360f;
        }
        if (Math.abs(cameraAimRotZ - cameraAimRotZO) > 180) {
            cameraAimRotZO += cameraAimRotZO < 0 ? 360f : -360f;
        }
    }

    public void checkState() {
        if (onVehicle()) {
            onVehicleTickCount += 1;
        } else {
            viewType = ViewType.THIRD_PERSON;
            onVehicleTickCount = 0;
        }
        Player player = getPlayer();
        AbstractVehicle vehicle = this.vehicle;
        WeaponUnit weaponUnit = null;
        if (vehicle != null && vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit ownWeaponUnit) {
            weaponUnit = ownWeaponUnit;
        }
        boolean crt = viewType == ViewType.SCOPE
                && weaponUnit != null
                && weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.CRT;
        if (CrtHandler.isActive() && !crt) {
            CrtHandler.setActive(false);
        } else if (!CrtHandler.isActive() && crt) {
            CrtHandler.setActive(true);
        }
        if (CrtHandler.isActive()) {
            if (weaponUnit != null && weaponUnit.withThermalImager()) {
                if (ThermalHandler.isActive() && !thermalImaging) {
                    ThermalHandler.setActive(false);
                } else if (!ThermalHandler.isActive() && thermalImaging) {
                    ThermalHandler.setActive(true);
                }
            } else if (ThermalHandler.isActive()) {
                ThermalHandler.setActive(false);
            }
        } else {
            if (ThermalHandler.isActive()) {
                ThermalHandler.setActive(false);
            }
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
        seat = null;
        vehicle = null;
        serverEntities.clear();
        missiles.clear();
        CrtHandler.setActive(false);
        ThermalHandler.setActive(false);
        OverloadHandler.setActive(false);
    }

    /**
     * 抬头视线落点
     */
    public Vec3 freeAimPos() {
        return cameraAimHit(CAMERA_UPWARD_ANGLE);
    }

    /**
     * 视线落点
     */
    public Vec3 scopeAimPos() {
        return cameraAimHit(0);
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
