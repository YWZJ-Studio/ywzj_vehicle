package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.function.Predicate;

public abstract class GrenadeEntity extends AmmoEntity {

    protected static final EntityDataAccessor<Boolean> EXPLODED = SynchedEntityData.defineId(GrenadeEntity.class, EntityDataSerializers.BOOLEAN);
    private float gravity = 0.07f;
    private double bounceFactor = 0.75;
    private boolean shouldBounce = true;
    private boolean brokeOnGround = false;
    private ParticleOptions tailParticle = null;

    public record BounceResult(Vec3 location, Vec3 deltaMovement) {}

    public GrenadeEntity(EntityType<? extends Projectile> type, Level level, ResourceLocation weaponId) {
        super(type, level, weaponId);
    }

    public GrenadeEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level, null);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(EXPLODED, false);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        super.readSpawnData(additionalData);
        VehicleWeaponIndex<?, ?> index = CommonAssetsManager.vehicleWeaponManager().getIndex(getWeaponId()).orElse(null);
        if (index != null && index.data() instanceof VehicleGrenadeWeaponData data) {
            initGrenade(data);
        }
    }

    protected void initGrenade(VehicleGrenadeWeaponData data) {
        this.life = data.getLife();
        this.damage = data.getHitDamage();
        this.gravity = data.getGravity();
        this.bounceFactor = data.getBounceFactor();
        this.shouldBounce = data.isShouldBounce();
        this.brokeOnGround = data.isBrokeOnGround();
        this.tailParticle = data.getTailParticles();
        this.explosion = data.getExplosion();
    }

    public void shoot(AbstractVehicle vehicle, Component name, Vec3 spawnPos,
                      float ammoXRot, float ammoYRot, float velocity, float inaccuracy,
                      LivingEntity shooter) {
        this.vehicle = vehicle;
        this.name = name;
        Vec3 direction = VectorUtil.rotToVec(ammoXRot, ammoYRot);
        direction = direction.add(
                this.random.triangle(0.0D, 0.0172275D * inaccuracy),
                this.random.triangle(0.0D, 0.0172275D * inaccuracy),
                this.random.triangle(0.0D, 0.0172275D * inaccuracy))
                .normalize().scale(velocity);
        this.setDeltaMovement(direction.add(vehicle.getDeltaMovement()));
        this.setPos(spawnPos);
        this.setRot(ammoYRot, ammoXRot);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setOwner(shooter);
    }

    @Override
    public void tick() {
        super.tick();
        var result = this.doMultiBounce(this.getDeltaMovement());

        this.checkInsideBlocks();

        Vec3 vec3 = result.deltaMovement();
        double x = result.location().x();
        double y = result.location().y();
        double z = result.location().z();

        this.setDeltaMovement(vec3);
        this.updateRotation();

        float f;
        if (this.isInWater()) {
            for (int i = 0; i < 4; ++i) {
                this.level().addParticle(ParticleTypes.BUBBLE, x - vec3.x * 0.25D, y - vec3.y * 0.25D, z - vec3.z * 0.25D, vec3.x, vec3.y, vec3.z);
            }
            f = 0.8F;
        } else {
            f = 0.99F;
        }

        this.setDeltaMovement(vec3.scale(f));
        if (!this.isNoGravity()) {
            Vec3 vec31 = this.getDeltaMovement();
            this.setDeltaMovement(vec31.x, vec31.y - (double) this.getGravity(), vec31.z);
        }

        this.setPos(x, y, z);

        if (this.tickCount >= life) {
            if (!this.level().isClientSide()) {
                this.onDeath(null);
            }
        }

        if (this.level().isClientSide()) {
            this.renderTailParticle();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() != HitResult.Type.MISS) {
            boolean flag = this.brokeOnGround && result instanceof BlockHitResult blockHitResult
                    && blockHitResult.getDirection() == Direction.UP;
            if (!this.shouldBounce || flag) {
                this.onDeath(result);
                return;
            }
        }
        switch (result.getType()) {
            case BLOCK -> {
                BlockHitResult blockResult = (BlockHitResult) result;
                BlockPos resultPos = blockResult.getBlockPos();
                BlockState state = this.level().getBlockState(resultPos);
                SoundEvent event = state.getBlock().getSoundType(state, this.level(), resultPos, this).getStepSound();
                double speed = this.getDeltaMovement().length();
                if (speed > 0.1) {
                    this.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                            event, SoundSource.AMBIENT, 2.0F, 1.0F);
                    this.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                            AllSounds.SMOKE_GRENADE_EXPLOSION.get(), SoundSource.AMBIENT, 2.0F, 1.0F);
                    this.entityData.set(EXPLODED, true);
                }
                state.onProjectileHit(this.level(), state, blockResult, this);
            }
            case ENTITY -> {
                EntityHitResult entityResult = (EntityHitResult) result;
                Entity entity = entityResult.getEntity();
                if (entity == this.getOwner() || entity == this.getVehicle()) return;
                double speed = this.getDeltaMovement().length();
                if (speed > 0.1) {
                    entity.hurt(entity.damageSources().thrown(this, this.getOwner()), this.damage);
                }
            }
            default -> {}
        }
    }

    /**
     * 进行一系列碰撞检测，返回最终实体应该处于的点和下一tick开始时应该具有的速度向量<br/>
     */
    public BounceResult doMultiBounce(Vec3 deltaMovement) {
        Vec3 start = this.position();
        Vec3 end = start.add(deltaMovement);
        Vec3 endVecOffset = new Vec3(deltaMovement.x, deltaMovement.y, deltaMovement.z);
        for (int i = 0; i < 3; i++) {
            HitResult hitResult = this.getHitResult(start, end, endVecOffset, this::canHitEntity, this.level());
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) hitResult;
                Vec3 hit = blockResult.getLocation();
                if (blockResult.getDirection() == Direction.UP && start.y() - hit.y() < 0.01) {
                    hit = new Vec3(hit.x(), start.y(), hit.z());
                }
                if (i < 2) {
                    start = start.lerp(hit, 0.8);
                    Vec3 rest = end.subtract(start);
                    endVecOffset = this.bounce(blockResult.getDirection(), rest);
                    end = start.add(endVecOffset);
                    deltaMovement = this.bounce(blockResult.getDirection(), deltaMovement);
                } else {
                    end = start.lerp(hit, 0.8);
                    deltaMovement = Vec3.ZERO;
                }
            } else if (hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityResult = (EntityHitResult) hitResult;
                Entity entity = entityResult.getEntity();
                if (entity == this.getOwner() || entity == this.getVehicle()) break;

                Direction direction = Direction.getNearest(endVecOffset.x(), endVecOffset.y(), endVecOffset.z()).getOpposite();
                Vec3 hit = hitResult.getLocation();
                start = start.lerp(hit, 0.8);
                Vec3 rest = end.subtract(start);
                endVecOffset = this.bounce(direction, rest);
                end = start.add(endVecOffset);
                deltaMovement = this.bounce(direction, deltaMovement);
            } else if (hitResult.getType() == HitResult.Type.MISS) {
                break;
            }
            this.onHit(hitResult);
        }
        return new BounceResult(end, deltaMovement);
    }

    public Vec3 bounce(Direction direction, Vec3 deltaMovement) {
        double factor = this.getBounceFactor();
        return switch (direction.getAxis()) {
            case X -> deltaMovement.multiply(-factor / 1.5, factor, factor);
            case Y -> {
                Vec3 newVec = deltaMovement.multiply(factor, -factor / 2.5, factor);
                if (newVec.y() < this.getGravity()) {
                    newVec = newVec.multiply(1, 0, 1);
                }
                yield newVec;
            }
            case Z -> deltaMovement.multiply(factor, factor, -factor / 1.5);
        };
    }

    public HitResult getHitResult(Vec3 start, Vec3 end, Vec3 endVecOffset, Predicate<Entity> pFilter, Level pLevel) {
        HitResult hitresult = pLevel.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hitresult.getType() != HitResult.Type.MISS) {
            end = hitresult.getLocation();
        }
        HitResult hitresult1 = ProjectileUtil.getEntityHitResult(pLevel, this, start, end, this.getBoundingBox().expandTowards(endVecOffset).inflate(1.0D), pFilter);
        if (hitresult1 != null) {
            hitresult = hitresult1;
        }
        return hitresult;
    }

    public void onDeath(@Nullable HitResult hitResult) {
        this.discard();
    }

    /** @deprecated 请使用 {@link #onDeath(HitResult)} <br/>
     * 注意，不应该在重写的方法中调用！
     */
    @Deprecated
    public void onDeath() {
        this.onDeath(null);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        double d0 = this.getBoundingBox().getSize() * 4.0D;
        if (Double.isNaN(d0)) {
            d0 = 4.0D;
        }
        d0 *= 64.0D;
        return pDistance < d0 * d0;
    }

    public float getGrenadeGravity() {
        return gravity;
    }

    public void setGrenadeGravity(float gravity) {
        this.gravity = gravity;
    }

    public double getBounceFactor() {
        return bounceFactor;
    }

    public void setBounceFactor(double bounceFactor) {
        this.bounceFactor = bounceFactor;
    }

    public boolean shouldBounce() {
        return shouldBounce;
    }

    public void setShouldBounce(boolean shouldBounce) {
        this.shouldBounce = shouldBounce;
    }

    public int getLife() {
        return life;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public float getHitDamage() {
        return damage;
    }

    public void setHitDamage(float hitDamage) {
        this.damage = hitDamage;
    }

    public boolean isBrokeOnGround() {
        return brokeOnGround;
    }

    public void setBrokeOnGround(boolean brokeOnGround) {
        this.brokeOnGround = brokeOnGround;
    }

    public ParticleOptions getTailParticle() {
        return tailParticle;
    }

    public void setTailParticle(ParticleOptions tailParticle) {
        this.tailParticle = tailParticle;
    }

    public void renderTailParticle() {
        if (this.getTailParticle() != null) {
            this.level().addParticle(this.getTailParticle(), true, this.getX(), this.getY() + 0.1, this.getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

}
