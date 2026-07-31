package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.PhysicsEngine;

/**
 * 动能武器打出的子弹实体。
 */
public class BulletEntity extends AmmoEntity {

    private float friction = 0.01F;
    private Vec3 startPos;
    private float caliber = 5.8f;
    private float tracerR = 1f;
    private float tracerG = 1f;
    private float tracerB = 1f;

    public BulletEntity(EntityType<? extends Projectile> entityType, Level level, VehicleCannonWeaponData data) {
        super(entityType, level, data.getWeaponId());
        initBullet(data);
    }

    public BulletEntity(EntityType<? extends Projectile> type, Level worldIn) {
        super(type, worldIn, null);
    }

    public void shoot(AbstractVehicle vehicle, Component name, Vec3 spawnPos, float ammoXRot, float ammoYRot, float velocity, float inaccuracy, LivingEntity shooter) {
        this.vehicle = vehicle;
        this.name = name;
        this.startPos = spawnPos;
        Vec3 direction = VectorUtil.rotToVec(ammoXRot, ammoYRot);
        direction = direction.add(this.random.triangle(0.0D, 0.0172275D * inaccuracy),
                this.random.triangle(0.0D, 0.0172275D * inaccuracy),
                this.random.triangle(0.0D, 0.0172275D * inaccuracy))
                .scale(velocity);
        this.setDeltaMovement(direction.add(vehicle.getDeltaMovement()));
        this.setPos(spawnPos);
        this.setRot(ammoYRot, ammoXRot);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setOwner(shooter);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeFloat(getXRot());
        buffer.writeFloat(getYRot());
        buffer.writeDouble(getDeltaMovement().x);
        buffer.writeDouble(getDeltaMovement().y);
        buffer.writeDouble(getDeltaMovement().z);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        super.readSpawnData(additionalData);
        setXRot(additionalData.readFloat());
        setYRot(additionalData.readFloat());
        setDeltaMovement(additionalData.readDouble(), additionalData.readDouble(), additionalData.readDouble());
        this.startPos = this.position();
        VehicleWeaponIndex<?, ?> vehicleWeaponIndex = CommonAssetsManager.vehicleWeaponManager().getIndex(getWeaponId()).orElse(null);
        if (vehicleWeaponIndex != null && vehicleWeaponIndex.data() instanceof VehicleCannonWeaponData data) {
            initBullet(data);
        }
    }

    private void initBullet(VehicleCannonWeaponData data) {
        this.damage = data.getDamage();
        this.headShot = data.getHeadshotMultiplier();
        this.explosion = data.getExplosion();
        this.life = data.getLife();
        this.friction = data.getFriction();
        this.caliber = data.getCaliber();
        this.tracerR = data.getTracerR();
        this.tracerG = data.getTracerG();
        this.tracerB = data.getTracerB();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            tickHit();
            life -= 1;
            if (life < 0) {
                this.discard();
            }
        }
        tickMove();
    }

    public void tickMove() {
        // 子弹模型的旋转与抛物线
        Vec3 movement = this.getDeltaMovement();
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        Vec2 rot = VectorUtil.vecToRot(movement);
        this.setYRot(rot.y);
        this.setXRot(rot.x);
        // 子弹初始的朝向设置
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
        // 子弹运动时的旋转（不包含自转）
        this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
        this.setYRot(lerpRotation(this.yRotO, this.getYRot()));
        // 子弹位置更新
        double nextPosX = this.getX() + x;
        double nextPosY = this.getY() + y;
        double nextPosZ = this.getZ() + z;
        this.setPos(nextPosX, nextPosY, nextPosZ);
        float friction = this.friction;
        float gravity = PhysicsEngine.G;
        // 子弹入水后的调整
        if (this.isInWater()) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(ParticleTypes.BUBBLE, nextPosX - x * 0.25F, nextPosY - y * 0.25F, nextPosZ - z * 0.25F, x, y, z);
            }
            // 在水中的阻力
            friction = 0.4F;
            gravity *= 0.6F;
        }
        // 重力与阻力更新速度状态
        this.setDeltaMovement(this.getDeltaMovement().scale(1 - friction));
        this.setDeltaMovement(this.getDeltaMovement().add(0, -gravity, 0));
    }

    public Vec3 getStartPos() {
        return startPos;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return damage;
    }

    public float getCaliber() {
        return caliber;
    }

    public float getTracerR() {
        return tracerR;
    }

    public float getTracerG() {
        return tracerG;
    }

    public float getTracerB() {
        return tracerB;
    }

    @Override
    public boolean ownedBy(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        return super.ownedBy(entity);
    }

}
