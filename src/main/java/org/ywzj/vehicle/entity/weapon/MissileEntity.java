package org.ywzj.vehicle.entity.weapon;

import com.tacz.guns.util.block.BlockRayTrace;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.BulletHitResult;
import org.ywzj.vehicle.util.CustomExplosion;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class MissileEntity extends AmmoEntity {

    public float speed = 5f;
    public Entity targetEntity;
    public Vec3 targetPos;
    public int operatorId;
    private VehicleSound sound;

    public MissileEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public MissileEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.MISSILE.get(), level);
    }

    public void shoot(AbstractVehicle vehicle, Component name, Vec3 spawnPos, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        this.vehicle = vehicle;
        this.name = name;
        this.setPos(spawnPos);
        this.setRot(ammoYRot, ammoXRot);
        this.setOwner(shooter);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickParticle();
            tickSound();
        } else {
            tickGuidance();
            tickMove();
            tickHit();
        }
    }

    private void tickGuidance() {
        if (targetEntity != null) {
            this.lookAt(EntityAnchorArgument.Anchor.EYES, targetEntity.position());
        } else if (targetPos != null) {
            this.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos);
        } else if (getOwner() != null) {
            if (vehicle != null) {
                if (vehicle.getOwnOperatorUnit((LivingEntity) getOwner()) instanceof WeaponUnit weaponUnit) {
                    Vec2 rot = weaponUnit.worldRot();
                    Vec3 start = weaponUnit.worldPivotPosition();
                    Vec3 dir = VectorUtil.calculateViewVector(rot.x, rot.y).normalize();
                    Vec3 pos = this.position();
                    // 计算实体在驾束射线上的投影点
                    Vec3 startToPos = pos.subtract(start);
                    double t = startToPos.dot(dir); // 投影系数（沿射线方向的距离）
                    if (t < 0) t = 0; // 限制在射线范围内
                    Vec3 proj = start.add(dir.scale(t));
                    // 当前点逐渐靠近射线（朝投影点移动）
                    double speed = 0.2;
                    // 逐步解锁机动
                    float maneuverability = Math.min((float) tickCount / 20, 1);
                    // 每 tick 靠近速度
                    speed *= maneuverability;
                    Vec3 delta = proj.subtract(pos);
                    if (delta.length() > speed) {
                        delta = delta.normalize().scale(speed);
                    }
                    this.setPos(pos.add(delta));
                    this.setRot(Mth.lerp(maneuverability, this.getYRot(), rot.y), Mth.lerp(maneuverability, this.getXRot(), rot.x));
                }
            }
        }
    }

    private void tickMove() {
        Vec3 velocity = this.getDeltaMovement();
        double dx = this.getX() + velocity.x;
        double dy = this.getY() + velocity.y;
        double dz = this.getZ() + velocity.z;
        this.setPos(dx, dy, dz);
        Vec3 v = this.getLookAngle().normalize();
        this.setDeltaMovement(v.scale(speed));
    }

    private void tickHit() {
        //todo: 细化
        if (!level().isClientSide()) {
            // 子弹在 tick 起始的位置
            Vec3 startVec = this.position();
            // 子弹在 tick 结束的位置
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            // 子弹的碰撞检测
            HitResult result = BlockRayTrace.rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            BlockHitResult resultB = (BlockHitResult) result;
            if (resultB.getType() != HitResult.Type.MISS) {
                // 子弹击中方块时，设置击中方块的位置为子弹的结束位置
                endVec = resultB.getLocation();
                CustomExplosion.explode((ServerLevel) level(), this, this.position(), 8, 20);
                this.kill();
                return;
            }
            BulletHitResult entityResult = EntityUtil.findEntityOnPath(this, startVec, endVec);
            // 将单个命中是实体创建为单个内容的 list
            if (entityResult != null && entityResult.getEntity() != vehicle) {
                //todo自己实现爆炸
                CustomExplosion.explode((ServerLevel) level(), this, this.position(), 8, 20);
                this.kill();
            }
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (level().isClientSide()) {
            localRemoveMissile();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tickParticle() {
        Vec3 pos = this.position();
        level().addParticle(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                pos.x, pos.y, pos.z,
                0.0D, 0.0D, 0.0D
        );
    }

    @OnlyIn(Dist.CLIENT)
    public void tickSound() {
        if (sound == null) {
            sound = new VehicleSound(AllSounds.ROCKET_FLYING.get(), 1, 1f, false, 50, true, true, this.getId());
            sound.play();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void localAddMissile() {
        if (operatorId == LocalVehiclePlayer.instance.getPlayer().getId()) {
            LocalVehiclePlayer.instance.controllingMissileIds.add(this.getId());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void localRemoveMissile() {
        if (operatorId == LocalVehiclePlayer.instance.getPlayer().getId()) {
            LocalVehiclePlayer.instance.controllingMissileIds.remove(this.getId());
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeInt(getOwner() == null ? -1 : getOwner().getId());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        super.readSpawnData(additionalData);
        operatorId = additionalData.readInt();
        if (level().isClientSide()) {
            localAddMissile();
        }
    }

}
