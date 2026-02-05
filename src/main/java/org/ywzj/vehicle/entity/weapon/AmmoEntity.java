package org.ywzj.vehicle.entity.weapon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllDamageTypes;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerVehicleHurtEntity;
import org.ywzj.vehicle.util.BlockRayTrace;
import org.ywzj.vehicle.util.BulletHitResult;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

public abstract class AmmoEntity extends Projectile implements IEntityAdditionalSpawnData {

    public AbstractVehicle vehicle;
    private ResourceLocation weaponId;
    public Component name;
    public float damage;
    public Explosion explosion;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    private int lerpSteps;

    public AmmoEntity(EntityType<? extends Projectile> pEntityType, Level pLevel, ResourceLocation weaponId) {
        super(pEntityType, pLevel);
        this.weaponId = weaponId;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickLerp();
        }
    }

    protected void tickHit() {
        //todo: 细化
        if (!level().isClientSide()) {
            // 子弹在 tick 起始的位置
            Vec3 startVec = this.position();
            // 子弹在 tick 结束的位置
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            // 子弹的碰撞检测
            BlockHitResult result = BlockRayTrace.rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (result.getType() != HitResult.Type.MISS) {
                // 子弹击中方块时，设置击中方块的位置为子弹的结束位置
                if (explosion != null && explosion.explode) {
                    VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), this.vehicle, position(), explosion.radius, explosion.damage, explosion.destroyBlock);
                    vehicleExplosion.explode();
                }
                this.discard();
                return;
            }
            BulletHitResult entityResult = EntityUtil.findEntityOnPath(this, startVec, endVec);
            // 将单个命中是实体创建为单个内容的 list
            if (entityResult != null && entityResult.getEntity() != vehicle) {
                Entity entity = entityResult.getEntity();
                @Nullable Entity owner = this.getOwner();
                // 攻击者
                LivingEntity attacker = owner instanceof LivingEntity ? (LivingEntity) owner : null;
                DamageSource source = AllDamageTypes.Sources.bullet(level().registryAccess(), this, attacker, result.getLocation());
                boolean kill = false;
                boolean destroyedBeforeHurt = false;
                if (entity instanceof AbstractVehicle vehicle) {
                    destroyedBeforeHurt = vehicle.isDestroyed();
                }
                entity.hurt(source, damage);
                if (entity instanceof AbstractVehicle vehicle) {
                    kill = !destroyedBeforeHurt && vehicle.isDestroyed();
                } else if (entity instanceof LivingEntity livingEntity) {
                    kill = livingEntity.isDeadOrDying();
                }
                if (owner instanceof ServerPlayer serverPlayer) {
                    Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ServerVehicleHurtEntity(vehicle.getId(), entity.getId(), kill));
                }
                if (explosion != null) {
                    VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), this.vehicle, position(), explosion.radius, explosion.damage, explosion.destroyBlock);
                    vehicleExplosion.explode();
                }
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

}
