package org.ywzj.vehicle.entity.vehicle;

import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.*;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.OBBEntity;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientWeaponUnitControl;
import org.ywzj.vehicle.util.OBB;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.WeaponUnit;

import java.lang.Math;
import java.util.List;

public class Lav150 extends AbstractVehicle implements OBBEntity {

    public static final EntityDataAccessor<Float> FORWARD_SPEED = SynchedEntityData.defineId(Lav150.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN_SPEED = SynchedEntityData.defineId(Lav150.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURRET_X_ROT = SynchedEntityData.defineId(Lav150.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURRET_Y_ROT = SynchedEntityData.defineId(Lav150.class, EntityDataSerializers.FLOAT);
    public static final float GROUND_FRICTION_ACCELERATION = 0.005f;
    public static final float FORWARD_ACCELERATION = 0.005f + GROUND_FRICTION_ACCELERATION;
    public static final float BACKWARD_ACCELERATION = 0.005f + GROUND_FRICTION_ACCELERATION;
    public static final float TURN_ACCELERATION = 0.1f;
    public static final float TURRET_X_ROT_SPEED = 3f;
    public static final float TURRET_Y_ROT_SPEED = 3f;
    public static final float MAX_TURRET_X_ROT = 15;
    public static final float MIN_TURRET_X_ROT = -30;
    public static final float MAX_SPEED = 0.5f;
    public static final float MAX_TURN = 2f;
    public float wheelRotation;
    public long lastRenderTime;
    private VehicleSound engineIdleSound;
    private VehicleSound engineRunSound;

    private OBB obb1;
    private OBB obb2;

    public Lav150(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        WeaponUnit machineGunTurret = new WeaponUnit(this, new Vec3(0d, 2.5d, 0d), 2.7f);
        machineGunTurret.xRotSpeed = TURRET_X_ROT_SPEED;
        machineGunTurret.yRotSpeed = TURRET_Y_ROT_SPEED;
        machineGunTurret.xRotMax = MAX_TURRET_X_ROT;
        machineGunTurret.xRotMin = MIN_TURRET_X_ROT;
        this.weaponUnits.add(machineGunTurret);
        obb1 = new OBB(this.position().toVector3f(), new Vector3f(0.65f, 0.35f, 1f), new Quaternionf());
        obb2 = new OBB(this.position().toVector3f(), new Vector3f(1.25f, 1f, 2.25f), new Quaternionf());
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
    public Vec3 getCameraOffset() {
        return new Vec3(0, 1.5, 0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FORWARD_SPEED, 0f);
        this.entityData.define(TURN_SPEED, 0f);
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
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickAim();
            tickParticle();
            tickSound();
        } else {
            tickMove();
        }
        tickWeapon();
        updateOBBs();
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        if (!this.level().isClientSide) {
            if (!pPlayer.startRiding(this)) {
                return InteractionResult.PASS;
            }
            int index = getPassengers().indexOf(pPlayer);
            if (index != -1) {
                if (index == 0) {
                    controlUnit.setOperator(pPlayer);
                }
                weaponUnits.get(index).setOperator(pPlayer);
                level().playSound(null, this.blockPosition(), AllSounds.LAV_150_ENGINE_START.get(), SoundSource.HOSTILE);
                level().playSound(null, this.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.8f;
    }

    @OnlyIn(Dist.CLIENT)
    private void tickAim() {
        if (!(getDriver() instanceof LocalPlayer)) {
            return;
        }
        Vector2f v = LocalVehiclePlayer.instance.cameraToWeaponRot();
        WeaponUnit machineGunTurret = weaponUnits.get(0);
        if (machineGunTurret.xAimRot != v.x || machineGunTurret.yAimRot != v.y) {
            machineGunTurret.xAimRot = v.x;
            machineGunTurret.yAimRot = v.y;
            ClientWeaponUnitControl control = new ClientWeaponUnitControl();
            control.vehicleEntityId = this.getId();
            control.weaponIndex = 0;
            control.xRot = v.x;
            control.yRot = v.y;
            Channel.CHANNEL.sendToServer(control);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tickParticle() {
        if (!this.getPassengers().isEmpty() && tickCount % 10 == 0) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(-2f)).add(v2.scale(-1.2)).add(0, 2, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tickSound() {
        if (getPassengers().isEmpty()) {
            if (engineIdleSound != null) {
                engineIdleSound.stop();
                engineIdleSound = null;
            }
            if (engineRunSound != null) {
                engineRunSound.stop();
                engineRunSound = null;
            }
        } else {
            float vf = entityData.get(FORWARD_SPEED);
            if (vf == 0) {
                if (engineRunSound != null) {
                    engineRunSound.stop();
                    engineRunSound = null;
                }
                if (engineIdleSound == null) {
                    engineIdleSound = new VehicleSound(AllSounds.LAV_150_ENGINE_IDLE.get(), 1f, 1f, true, true, this.getId());
                    engineIdleSound.play();
                }
            } else {
                if (engineIdleSound != null) {
                    engineIdleSound.stop();
                    engineIdleSound = null;
                }
                float volume = Math.max(0.4f, vf / MAX_SPEED);
                if (engineRunSound == null) {
                    engineRunSound = new VehicleSound(AllSounds.LAV_150_ENGINE_RUN.get(), volume, 1f, true, false, this.getId());
                    engineRunSound.play();
                } else {
                    engineRunSound.setVolume(volume);
                }
            }
        }
    }

    private void tickMove() {
        if (getDriver() == null) {
            controlUnit.reset();
        }
        float vf = entityData.get(FORWARD_SPEED);
        float vt = entityData.get(TURN_SPEED);
        // 考虑碰撞停滞
        if (new Vec3(getDeltaMovement().x, 0, getDeltaMovement().z).length() == 0) {
            vf = 0;
        }
        // 前后控制
        if (controlUnit.forward || controlUnit.backward) {
            vf += controlUnit.forward ? FORWARD_ACCELERATION : -BACKWARD_ACCELERATION;
            vf = Mth.clamp(vf, -MAX_SPEED, MAX_SPEED);
        }
        // 地面摩擦力
        if (vf < 0) {
            vf += GROUND_FRICTION_ACCELERATION;
            vf = Math.min(vf, 0);
        } else if (vf > 0) {
            vf -= GROUND_FRICTION_ACCELERATION;
            vf = Math.max(vf, 0);
        }
        entityData.set(FORWARD_SPEED, vf);
        // 转向控制
        if (controlUnit.left || controlUnit.right) {
            vt += controlUnit.right ? TURN_ACCELERATION : -TURN_ACCELERATION;
            vt = Mth.clamp(vt, -MAX_TURN, MAX_TURN);
        } else {
            if (vt < 0) {
                vt += TURN_ACCELERATION;
                vt = Math.min(vt, 0);
            } else if (vt > 0) {
                vt -= TURN_ACCELERATION;
                vt = Math.max(vt, 0);
            }
        }
        entityData.set(TURN_SPEED, vt);
        // 转向幅度应用于车身朝向
        // 轮式载具仅存在前进速度时可运动转向
        if (Math.abs(vf) > 0.03) {
            if (vf < 0) {
                vt *= -1;
            }
            this.setYRot(this.getYRot() + vt);
        }
        // 前进速度应用于车身朝向
        Vec3 direction = getLookAngle();
        Vec3 motion = direction.normalize().scale(vf);
        // 重力影响
        motion = motion.add(0, -1f, 0);
        this.setDeltaMovement(motion);
    }

    private void tickWeapon() {
        WeaponUnit machineGunTurret = weaponUnits.get(0);
        machineGunTurret.tick();
        if (!level().isClientSide()) {
            this.entityData.set(TURRET_X_ROT, machineGunTurret.xRot);
            this.entityData.set(TURRET_Y_ROT, machineGunTurret.yRot);
        }
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
