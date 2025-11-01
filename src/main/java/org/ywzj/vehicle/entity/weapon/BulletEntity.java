package org.ywzj.vehicle.entity.weapon;

import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.api.entity.KnockBackModifier;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.util.BlockRayTrace;
import org.ywzj.vehicle.util.BulletHitResult;
import org.ywzj.vehicle.util.CustomExplosion;
import org.ywzj.vehicle.util.EntityUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 动能武器打出的子弹实体。
 */
public class BulletEntity extends AmmoEntity {

    private float damage;
    private int life = 200;
    private float speed = 1;
    private float gravity = 0;
    private float friction = 0.01F;
    private float knockback = 0;
    private boolean explosion = false;
    // 穿透数
    private int pierce = 1;
    // 初始位置
    private Vec3 startPos;
    private float armorIgnore;
    private float headShot;

    // 返回一个距离-伤害乘数
    private Function<Double, Float> distanceDamageFunction = (distance) -> 1.0f;

    public BulletEntity(EntityType<? extends Projectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public BulletEntity(Level level, LivingEntity throwerIn, Vec3 startPos, boolean explosion) {
        this(level, throwerIn, startPos.x, startPos.y, startPos.z, explosion);
    }

    public BulletEntity(Level level, LivingEntity throwerIn, double x, double y, double z, boolean explosion) {
        this(AllEntities.BULLET.get(), level);
        this.setOwner(throwerIn);
        this.explosion = explosion;
        this.setPos(x, y, z);
        this.startPos = this.position();
    }

    public BulletEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.BULLET.get(), level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
        // 调用 TaC 子弹服务器事件
        this.onBulletTick();
        // 粒子效果
        if (this.level().isClientSide) {
//            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AmmoParticleSpawner.addParticle(this));
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

    // 子弹的逻辑处理
    protected void onBulletTick() {
        // 服务器端子弹逻辑
        if (!this.level().isClientSide()) {
            // 子弹在 tick 起始的位置
            Vec3 startVec = this.position();
            // 子弹在 tick 结束的位置
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            // 子弹的碰撞检测
            BlockHitResult result = BlockRayTrace.rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (result.getType() != HitResult.Type.MISS) {
                // 子弹击中方块时，设置击中方块的位置为子弹的结束位置
                endVec = result.getLocation();
            }

            List<BulletHitResult> hitEntities = null;
            // 子弹的击中检测，穿透为 1 或者爆炸类弹药限制为一个实体穿透判定
            if (this.pierce <= 1) {
                BulletHitResult entityResult = EntityUtil.findEntityOnPath(this, startVec, endVec);
                // 将单个命中是实体创建为单个内容的 list
                if (entityResult != null) {
                    hitEntities = Collections.singletonList(entityResult);
                }
            } else {
                hitEntities = EntityUtil.findEntitiesOnPath(this, startVec, endVec);
            }
            // 当子弹击中实体时，进行被命中的实体读取
            if (hitEntities != null && !hitEntities.isEmpty()) {
                hitEntities.stream()
                        .sorted(Comparator.comparingDouble(r -> r.getLocation().distanceToSqr(startVec)))
                        .limit(pierce)
                        .forEach(entityResult -> {
                            // 处理子弹击中实体的逻辑
                            this.onHitEntity(entityResult);
                            this.pierce--;
                        });
                if (this.pierce < 1) {
                    // 子弹已经穿透所有实体，结束子弹的飞行
                    this.discard();
                    return;
                }
            }
            this.onHitBlock(result, startVec, endVec);
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

    public void shootFromRotation(Entity pShooter, float pX, float pY, float pZ, float pVelocity, Vector2d vector2d) {
        this.shoot(pX, pY, pVelocity, vector2d);
        Vec3 vec3 = pShooter.getDeltaMovement();
        this.setDeltaMovement(this.getDeltaMovement().add(vec3.x, pShooter.onGround() ? 0.0D : vec3.y, vec3.z));
    }

    protected void onHitEntity(BulletHitResult result) {
        if (result.getEntity() instanceof ITargetEntity targetEntity) {
            DamageSource source = this.damageSources().thrown(this, this.getOwner());
            targetEntity.onProjectileHit(this, result, source, this.getDamage(result.getLocation()));
            // 打靶直接返回
            return;
        }
        // 获取Pre事件必要的信息
        Entity entity = result.getEntity();
        @Nullable Entity owner = this.getOwner();
        // 攻击者
        LivingEntity attacker = owner instanceof LivingEntity ? (LivingEntity) owner : null;
        boolean headshot = result.isHeadshot();
        float damage = this.getDamage(result.getLocation());
        float headShotMultiplier = Math.max(this.headShot, 0);
        if (headshot) {
            // 默认爆头伤害是 1x
            damage *= headShotMultiplier;
        }
        Pair<DamageSource, DamageSource> sources = Pair.of(
                ModDamageTypes.Sources.bullet(level().registryAccess(), this, attacker, false),
                ModDamageTypes.Sources.bullet(level().registryAccess(), this, attacker, true)
        );
        // 对 LivingEntity 进行击退强度的自定义
        if (entity instanceof LivingEntity livingCore) {
            // 取消击退效果，设定自己的击退强度
            KnockBackModifier modifier = KnockBackModifier.fromLivingEntity(livingCore);
            modifier.setKnockBackStrength(this.knockback);
            // 创建伤害
            performAttack(entity, damage, sources);
            // 恢复原位
            modifier.resetKnockBackStrength();
        } else {
            // 创建伤害
            performAttack(entity, damage, sources);
        }

        if (explosion) {
            CustomExplosion.explode((ServerLevel) level(), this, this.position(), 8, 20);
        }
    }

    protected void onHitBlock(BlockHitResult result, Vec3 startVec, Vec3 endVec) {
        if (result.getType() == HitResult.Type.MISS) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        Vec3 hitVec = result.getLocation();

        super.onHitBlock(result);

        if (explosion) {
            CustomExplosion.explode((ServerLevel) level(), this, this.position(), 8, 20);
        }

        // 弹孔与点燃特效
//        if (this.level() instanceof ServerLevel serverLevel) {
//            BulletHoleOption bulletHoleOption = new BulletHoleOption(result.getDirection(), result.getBlockPos(), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
//            serverLevel.sendParticles(bulletHoleOption, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
//        }
        this.discard();
    }

    // todo 根据距离进行伤害衰减设计
    public float getDamage(Vec3 hitVec) {
        // 遍历进行判断
        double playerDistance = hitVec.distanceTo(this.startPos);
        float multiplier = this.distanceDamageFunction.apply(playerDistance);
        return damage * multiplier;
    }


    private void performAttack(Entity parts, float damage, Pair<DamageSource, DamageSource> sources) {
        var source1 = sources.getLeft();
        var source2 = sources.getRight();
        // 穿甲伤害和普通伤害的比例计算
        float armorDamagePercent = Mth.clamp(this.armorIgnore, 0.0F, 1.0F);
        float normalDamagePercent = 1 - armorDamagePercent;

        if (parts instanceof PartEntity<?> part) {
            part.getParent().invulnerableTime = 0;
        } else {
            parts.invulnerableTime = 0;
        }
        parts.hurt(source1, damage * normalDamagePercent);

        if (parts instanceof PartEntity<?> part) {
            part.getParent().invulnerableTime = 0;
        } else {
            parts.invulnerableTime = 0;
        }
        parts.hurt(source2, damage * armorDamagePercent);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
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
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        setXRot(additionalData.readFloat());
        setYRot(additionalData.readFloat());
        setDeltaMovement(additionalData.readDouble(), additionalData.readDouble(), additionalData.readDouble());
        Entity entity = this.level().getEntity(additionalData.readInt());
        if (entity != null) {
            this.setOwner(entity);
        }
        this.gravity = additionalData.readFloat();
        this.life = additionalData.readInt();
        this.speed = additionalData.readFloat();
        this.friction = additionalData.readFloat();
        this.startPos = this.position();
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

    public void setHeadShot(float headShot) {
        this.headShot = headShot;
    }

    public float getHeadShot() {
        return headShot;
    }

    public void setDistanceDamageFunction(Function<Double, Float> distanceDamageFunction) {
        this.distanceDamageFunction = distanceDamageFunction;
    }

    public float getArmorIgnore() {
        return armorIgnore;
    }

    public void setArmorIgnore(float armorIgnore) {
        this.armorIgnore = armorIgnore;
    }

    @Override
    public boolean ownedBy(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        return super.ownedBy(entity);
    }

}
