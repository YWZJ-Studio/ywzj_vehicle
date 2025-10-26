package org.ywzj.vehicle.entity.weapon;

import com.tacz.guns.util.block.BlockRayTrace;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.BulletHitResult;
import org.ywzj.vehicle.util.EntityUtil;

public class RocketEntity extends Projectile implements IEntityAdditionalSpawnData {

    public AbstractVehicle vehicle;
    public Component name;
    public float speed = 5f;
    private VehicleSound sound;

    public RocketEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public RocketEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.ROCKET.get(), level);
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
            tickMove();
            tickHit();
        }
    }

    private void tickMove() {
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = this.getX() + vec3.x;
        double d1 = this.getY() + vec3.y;
        double d2 = this.getZ() + vec3.z;
        this.setPos(d0, d1, d2);
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
    }

    @OnlyIn(Dist.CLIENT)
    public void tickParticle() {
        Vec3 pos = this.position();
        level().addParticle(
                new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 3.0F), true,
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
        buffer.writeComponent(name);
        buffer.writeInt(getOwner() == null ? -1 : getOwner().getId());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        name = additionalData.readComponent();
    }

}
