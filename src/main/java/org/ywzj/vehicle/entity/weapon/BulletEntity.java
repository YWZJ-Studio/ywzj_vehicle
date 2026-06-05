package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

/**
 * 动能武器打出的子弹实体。
 */
public class BulletEntity extends AmmoEntity {

    private ResourceLocation weaponId;
    private int life = 200;
    private float speed = 1;
    private float gravity = 0;
    private float friction = 0.01F;
    // 曳光
    private Vec3 startPos;
    private float caliber = 7.62f;
    private float tracerR = 1f;
    private float tracerG = 1f;
    private float tracerB = 1f;

    public BulletEntity(EntityType<? extends Projectile> type, Level worldIn) {
        super(type, worldIn, null);
    }

    public BulletEntity(Level level, AbstractVehicle vehicle, LivingEntity shooter, Vec3 startPos, Explosion explosion, ResourceLocation weaponId) {
        this(level, vehicle, shooter, startPos.x, startPos.y, startPos.z, explosion);
        this.weaponId = weaponId;
    }

    public BulletEntity(Level level, AbstractVehicle vehicle, LivingEntity shooter, double x, double y, double z, Explosion explosion) {
        this(AllEntities.BULLET.get(), level);
        this.vehicle = vehicle;
        this.setOwner(shooter);
        this.explosion = explosion;
        this.setPos(x, y, z);
        this.startPos = this.position();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            tickHit();
        }
        // 子弹模型的旋转与抛物线
        Vec3 movement = this.getDeltaMovement();
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        double distance = movement.horizontalDistance();
        this.setYRot((float) Math.toDegrees(Mth.atan2(x, z)));
        this.setXRot((float) Math.toDegrees(Mth.atan2(y, distance)));
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
        float gravity = this.gravity;
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
        // 子弹生命结束
        if (this.tickCount >= this.life - 1) {
            this.discard();
        }
    }

    public void shoot(double pitch, double yaw, float pVelocity, Vector2d vector2d) {
        Vector3d left = new Vector3d(vector2d.x, vector2d.y, 8);

        left.rotateX(pitch * Mth.DEG_TO_RAD);
        left.rotateY(-yaw * Mth.DEG_TO_RAD);

        Vec3 vec3 = new Vec3(left.x, left.y, left.z).normalize().scale(pVelocity);

        this.setDeltaMovement(vec3.x, vec3.y, vec3.z);
        double d0 = vec3.horizontalDistance();
        this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
        this.setXRot((float)(Mth.atan2(vec3.y, d0) * (double)(180F / (float)Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(getXRot());
        buffer.writeFloat(getYRot());
        buffer.writeDouble(getDeltaMovement().x);
        buffer.writeDouble(getDeltaMovement().y);
        buffer.writeDouble(getDeltaMovement().z);
        Entity entity = getOwner();
        buffer.writeInt(entity != null ? entity.getId() : 0);
        buffer.writeFloat(this.gravity);
        buffer.writeInt(this.life);
        buffer.writeFloat(this.speed);
        buffer.writeFloat(this.friction);
        buffer.writeResourceLocation(this.weaponId);
        initBullet();
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        setXRot(additionalData.readFloat());
        setYRot(additionalData.readFloat());
        setDeltaMovement(additionalData.readDouble(), additionalData.readDouble(), additionalData.readDouble());
        Entity entity = this.level().getEntity(additionalData.readInt());
        if (entity != null) {
            this.setOwner(entity);
        }
        this.startPos = this.position();
        this.gravity = additionalData.readFloat();
        this.life = additionalData.readInt();
        this.speed = additionalData.readFloat();
        this.friction = additionalData.readFloat();
        this.weaponId = additionalData.readResourceLocation();
        initBullet();
    }

    private void initBullet() {
        VehicleWeaponIndex<?, ?> vehicleWeaponIndex = CommonAssetsManager.vehicleWeaponManager().getIndex(this.weaponId).orElse(null);
        if (vehicleWeaponIndex != null && vehicleWeaponIndex.data() instanceof VehicleCannonWeaponData data) {
            this.caliber = data.getCaliber();
            this.tracerR = data.getTracerR();
            this.tracerG = data.getTracerG();
            this.tracerB = data.getTracerB();
        }
    }

    public Vec3 getStartPos() {
        return startPos;
    }

    public RandomSource getRandom() {
        return this.random;
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
