package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.ywzj.vehicle.all.AllDamageTypes;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.particle.BulletHoleOption;
import org.ywzj.vehicle.util.BulletHitResult;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

import java.util.List;

public abstract class AmmoEntity extends Projectile implements IEntityAdditionalSpawnData {

    public AbstractVehicle vehicle;
    private ResourceLocation weaponId;
    public Component name;
    public int life;
    public float headShot;
    public float damage;
    public Explosion explosion;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    private int lerpSteps;
    protected boolean keepChunkLoaded = false;

    public AmmoEntity(EntityType<? extends Projectile> pEntityType, Level pLevel, ResourceLocation weaponId) {
        super(pEntityType, pLevel);
        this.weaponId = weaponId;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickLerp();
        } else {
            if (keepChunkLoaded) {
                EntityUtil.keepChunkLoaded(this, this.position());
                EntityUtil.keepChunkLoaded(this, this.position().add(getLookAngle().normalize().scale(16)));
            }
        }
    }

    protected void tickHit() {
        // 子弹在 tick 起始的位置
        Vec3 startVec = this.position();
        // 子弹在 tick 结束的位置
        Vec3 endVec = startVec.add(this.getDeltaMovement());
        // 子弹的碰撞检测
        BlockHitResult result = this.level().clip(new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (result.getType() != HitResult.Type.MISS) {
            BlockPos hitPos = result.getBlockPos();
            BlockState hitBlock = this.level().getBlockState(hitPos);
            if (this.level() instanceof ServerLevel serverLevel) {
                Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
                Vec3 loc1 = result.getLocation().add(normal.scale(0.3));
                Vec3 loc2 = result.getLocation().add(normal.scale(0.01));
                BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, hitBlock);
                for (ServerPlayer player : serverLevel.players()) {
                    if (player.distanceToSqr(loc1) < 128 * 128) {
                        // 方块材质破坏粒子
                        serverLevel.sendParticles(player, option, true,
                                loc1.x, loc1.y, loc1.z,
                                5, 0, 0, 0, 0.1);
                        // 弹孔
                        BulletHoleOption bulletHole = new BulletHoleOption(result.getDirection(), hitPos, 1, 0, 0, getCaliber());
                        serverLevel.sendParticles(player, bulletHole, true,
                                loc2.x, loc2.y, loc2.z,
                                1, 0, 0, 0, 0);
                    }
                }
            }
            // 子弹击中方块时，设置击中方块的位置为子弹的结束位置
            if (explosion != null && explosion.explode) {
                VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), this.vehicle, result.getLocation(), explosion.radius, explosion.damage, explosion.destroyBlock);
                vehicleExplosion.explode();
            }
            this.discard();
            return;
        }
        BulletHitResult entityResult = EntityUtil.findEntityOnPath(this, startVec, endVec);
        // 将单个命中是实体创建为单个内容的 list
        if (entityResult != null
                && entityResult.getEntity() != vehicle
                && !vehicle.getPassengers().contains(entityResult.getEntity())) {
            Entity entity = entityResult.getEntity();
            Entity owner = this.getOwner();
            // 伤害实体
            boolean headshot = entityResult.isHeadshot();
            DamageSource source = AllDamageTypes.Sources.bullet(level().registryAccess(), this, owner, entityResult.getLocation());
            EntityUtil.hurt(source, entity, headshot ? damage * this.headShot : damage);
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.invulnerableTime = 0;
            }
            if (explosion != null && explosion.explode) {
                VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), owner, this.vehicle, entityResult.getLocation(), explosion.radius, explosion.damage, explosion.destroyBlock);
                vehicleExplosion.explode();
            }
            this.discard();
        }
        // 近炸
        else if (explosion != null && explosion.proximityFuze && tickCount > 5 && entityResult == null) {
            AABB detectionBox = this.getBoundingBox().inflate(explosion.proximityRadius)
                    .move(getLookAngle().normalize().scale(-explosion.proximityRadius));
            List<Entity> nearbyEntities = this.level().getEntities(this, detectionBox,
                    entity -> entity != vehicle && !vehicle.getPassengers().contains(entity));
            if (!nearbyEntities.isEmpty() && explosion != null) {
                VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), this.vehicle, position(), explosion.radius, explosion.damage, explosion.destroyBlock);
                vehicleExplosion.explode();
                this.discard();
            }
        }
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeComponent(name);
        buffer.writeInt(getOwner() == null ? -1 : getOwner().getId());
        buffer.writeResourceLocation(weaponId);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        name = additionalData.readComponent();
        additionalData.readInt();
        weaponId = additionalData.readResourceLocation();
    }

    @Override
    public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pLerpSteps, boolean pTeleport) {
        this.lerpX = pX;
        this.lerpY = pY;
        this.lerpZ = pZ;
        this.lerpYRot = pYRot;
        this.lerpXRot = pXRot;
        this.lerpSteps = 1;
    }

    private void tickLerp() {
        if (this.lerpSteps > 0) {
            double d0 = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
            double d1 = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
            double d2 = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
            double d3 = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
            this.setYRot(this.getYRot() + (float)d3 / (float)this.lerpSteps);
            this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.lerpSteps);
            --this.lerpSteps;
            this.setPos(d0, d1, d2);
            this.setRot(this.getYRot(), this.getXRot());
        }
    }

    public ResourceLocation getWeaponId() {
        return weaponId;
    }

    public float getCaliber() {
        return 5.8f;
    }

}
