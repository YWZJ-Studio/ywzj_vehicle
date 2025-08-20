package org.ywzj.vehicle.entity.vehicle;

import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.entity.OBBEntity;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientWeaponUnitControl;
import org.ywzj.vehicle.util.OBB;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.WeaponUnit;

import java.util.List;

public class Lav150 extends WheeledVehicle implements OBBEntity {

    public static final EntityDataAccessor<Float> TURRET_X_ROT = SynchedEntityData.defineId(Lav150.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURRET_Y_ROT = SynchedEntityData.defineId(Lav150.class, EntityDataSerializers.FLOAT);
    public static float turretXRotSpeed = 3f;
    public static float turretYRotSpeed = 3f;
    public static float maxTurretXRot = 15;
    public static float minTurretXRot = -30;

    private final OBB obb1 = new OBB(this.position().toVector3f(), new Vector3f(0.65f, 0.35f, 1f), new Quaternionf());
    private final OBB obb2 = new OBB(this.position().toVector3f(), new Vector3f(1.25f, 1f, 2.25f), new Quaternionf());

    public Lav150(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.wide = 2.7f;
        this.length = 3.61f;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TURRET_X_ROT, 0f);
        this.entityData.define(TURRET_Y_ROT, 0f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (TURRET_X_ROT.equals(pKey)) {
            WeaponUnit machineGunTurret = weaponUnits.get(0);
            machineGunTurret.xRot = entityData.get(TURRET_X_ROT);
        } else if (TURRET_Y_ROT.equals(pKey)) {
            WeaponUnit machineGunTurret = weaponUnits.get(0);
            machineGunTurret.yRot = entityData.get(TURRET_Y_ROT);
        }
    }

    @Override
    public void initWeaponUnits() {
        WeaponUnit machineGunTurret = new WeaponUnit(this, new Vec3(0d, 2.5d, 0d), 2.7f);
        machineGunTurret.xRotSpeed = turretXRotSpeed;
        machineGunTurret.yRotSpeed = turretYRotSpeed;
        machineGunTurret.xRotMax = maxTurretXRot;
        machineGunTurret.xRotMin = minTurretXRot;
        this.weaponUnits.add(machineGunTurret);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.8f;
    }

    @Override
    public Vec3 getCameraOffset() {
        return new Vec3(0, 1.5, 0);
    }

    @Override
    public SoundEvent getEngineStartSound() {
        return AllSounds.LAV_150_ENGINE_START.get();
    }

    @Override
    public SoundEvent getEngineIdleSound() {
        return AllSounds.LAV_150_ENGINE_IDLE.get();
    }

    @Override
    public SoundEvent getEngineRunSound() {
        return AllSounds.LAV_150_ENGINE_RUN.get();
    }

    @Override
    public void tick() {
        super.tick();
        updateOBBs();
    }

    @Override
    protected void tickAim() {
        if (!(getDriver() instanceof LocalPlayer)) {
            return;
        }
        Vector2f rot = LocalVehiclePlayer.instance.cameraToWeaponRot();
        if (rot == null) {
            return;
        }
        WeaponUnit machineGunTurret = weaponUnits.get(0);
        if (machineGunTurret.xAimRot != rot.x || machineGunTurret.yAimRot != rot.y) {
            machineGunTurret.xAimRot = rot.x;
            machineGunTurret.yAimRot = rot.y;
            ClientWeaponUnitControl control = new ClientWeaponUnitControl();
            control.vehicleEntityId = this.getId();
            control.weaponIndex = 0;
            control.xRot = rot.x;
            control.yRot = rot.y;
            Channel.CHANNEL.sendToServer(control);
        }
    }

    @Override
    protected void tickParticle() {
        if (!this.getPassengers().isEmpty() && tickCount % 10 == 0) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(-2f)).add(v2.scale(-1.2)).add(0, 2, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
        }
    }

    @Override
    protected void tickWeapon() {
        WeaponUnit machineGunTurret = weaponUnits.get(0);
        machineGunTurret.tick();
        if (!level().isClientSide()) {
            this.entityData.set(TURRET_X_ROT, machineGunTurret.xRot);
            this.entityData.set(TURRET_Y_ROT, machineGunTurret.yRot);
        }
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(obb1, obb2);
    }

    @Override
    public void updateOBBs() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f p1 = transformPosition(transform, 0, 2.35f, 0f);
        obb1.setCenter(new Vector3f(p1.x, p1.y, p1.z));
        obb1.setRotation(combineRotations(1));

        Vector4f p2 = transformPosition(transform, 0, 1, 0f);
        obb2.setCenter(new Vector3f(p2.x, p2.y, p2.z));
        obb2.setRotation(combineRotations(1));
    }

    // 合并三个旋转（Yaw -> Pitch -> Roll）
    public Quaternionf combineRotations(float partialTicks) {
        // 1. 获取三个独立的旋转四元数
        Quaternionf yawRot = Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, yRotO, getYRot()));
        Quaternionf pitchRot = Axis.XP.rotationDegrees(Mth.lerp(partialTicks, xRotO, getXRot()));
        Quaternionf rollRot = Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, zRotO, getZRot()));

        // 2. 按照正确顺序合并：先Yaw，再Pitch，最后Roll
        Quaternionf combined = new Quaternionf(yawRot);   // 初始化为Yaw旋转
        combined.mul(pitchRot);  // 应用Pitch旋转
        combined.mul(rollRot);   // 应用Roll旋转

        return combined;
    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        if (weaponIndex == 0) {
            WeaponUnit machineGunTurret = weaponUnits.get(0);
            machineGunTurret.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
            this.level().playSound(null, this, AllSounds.LAV_150_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);
        }
    }

}
