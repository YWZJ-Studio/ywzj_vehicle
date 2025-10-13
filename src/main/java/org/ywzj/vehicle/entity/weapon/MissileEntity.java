package org.ywzj.vehicle.entity.weapon;

import com.tacz.guns.util.block.BlockRayTrace;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.BulletHitResult;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class MissileEntity extends Projectile implements IEntityAdditionalSpawnData {

    public AbstractVehicle vehicle;
    public Entity targetEntity;
    public Vec3 targetPos;
    public LivingEntity shooter;
    public String name;
    private VehicleSound sound;

    public MissileEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public MissileEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.MISSILE.get(), level);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickParticle();
            tickSound();
        }
        if (targetEntity != null) {
            this.lookAt(EntityAnchorArgument.Anchor.EYES, targetEntity.position());
        } else if (targetPos != null) {
            this.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos);
        } else if (shooter != null) {
            if (vehicle != null) {
                if (vehicle.getOwnOperatorUnit(shooter) instanceof WeaponUnit weaponUnit) {
                    Vec2 rot = weaponUnit.worldRot();
                    Vec3 start = weaponUnit.worldBoltPosition();
                    Vec3 dir = VectorUtil.calculateViewVector(rot.x, rot.y).normalize();
                    Vec3 pos = this.position();
                    // 计算实体在射线上的投影点
                    Vec3 startToPos = pos.subtract(start);
                    double t = startToPos.dot(dir); // 投影系数（沿射线方向的距离）
                    if (t < 0) t = 0; // 限制在射线范围内
                    Vec3 proj = start.add(dir.scale(t));
                    // 当前点逐渐靠近射线（朝投影点移动）
                    double speed = 0.2; // 每 tick 靠近速度
                    Vec3 delta = proj.subtract(pos);
                    double dist = delta.length();
                    if (dist > speed) {
                        delta = delta.normalize().scale(speed);
                    }
                    // 更新实体位置
                    this.setPos(pos.add(delta));
                    // 朝向与射线方向一致（可选）
                    this.setRot(rot.y, rot.x);
                }
            }
        }

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
                //todo自己实现爆炸
                this.level().explode(this, this.getX(), this.getY(0.0625D), this.getZ(), 8.0F,
                        AllConfigs.common.explosionBreakBlocks.get() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
                this.kill();
                return;
            }
            BulletHitResult entityResult = EntityUtil.findEntityOnPath(this, startVec, endVec);
            // 将单个命中是实体创建为单个内容的 list
            if (entityResult != null && entityResult.getEntity() != vehicle) {
                //todo自己实现爆炸
                this.level().explode(this, this.getX(), this.getY(0.0625D), this.getZ(), 8.0F,
                        AllConfigs.common.explosionBreakBlocks.get() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
                this.kill();
                return;
            }
        }

        Vec3 vec3 = this.getDeltaMovement();
        double d0 = this.getX() + vec3.x;
        double d1 = this.getY() + vec3.y;
        double d2 = this.getZ() + vec3.z;
        this.setPos(d0, d1, d2);
        Vec3 v = this.getLookAngle().normalize();
        this.setDeltaMovement(v.scale(5));
    }

    @OnlyIn(Dist.CLIENT)
    public void tickParticle() {
        Vec3 pos = this.position();
        level().addParticle(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
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

    @Override
    protected void defineSynchedData() {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUtf("akd10");
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        name = additionalData.readUtf();
    }

}
