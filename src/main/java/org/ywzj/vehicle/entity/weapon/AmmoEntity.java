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

    /**
     * Handles projectile collision detection and damage application.
     * Processes both block and entity hits with explosion support.
     */
    protected void tickHit() {
        if (!level().isClientSide()) {
            Vec3 startVec = this.position();
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            
            // Check block collision first
            if (handleBlockCollision(startVec, endVec)) {
                return;
            }
            
            // Check entity collision
            handleEntityCollision(startVec, endVec);
        }
    }

    /**
     * Handles collision with blocks.
     * @return true if projectile should be discarded
     */
    private boolean handleBlockCollision(Vec3 startVec, Vec3 endVec) {
        BlockHitResult result = BlockRayTrace.rayTraceBlocks(
            this.level(), 
            new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)
        );
        
        if (result.getType() != HitResult.Type.MISS) {
            if (explosion != null && explosion.explode) {
                createExplosion(position());
            }
            this.discard();
            return true;
        }
        return false;
    }

    /**
     * Handles collision with entities.
     */
    private void handleEntityCollision(Vec3 startVec, Vec3 endVec) {
        BulletHitResult entityResult = EntityUtil.findEntityOnPath(this, startVec, endVec);
        
        if (entityResult != null && entityResult.getEntity() != vehicle) {
            Entity target = entityResult.getEntity();
            applyDamageToEntity(target);
            
            if (explosion != null) {
                createExplosion(position());
            }
            this.discard();
        }
    }

    /**
     * Applies damage to the target entity and notifies the attacker.
     */
    private void applyDamageToEntity(Entity target) {
        @Nullable Entity owner = this.getOwner();
        LivingEntity attacker = owner instanceof LivingEntity ? (LivingEntity) owner : null;
        DamageSource source = AllDamageTypes.Sources.bullet(
            level().registryAccess(), 
            this, 
            attacker, 
            target.position()
        );
        
        boolean destroyedBeforeHurt = target instanceof AbstractVehicle v && v.isDestroyed();
        target.hurt(source, damage);
        boolean kill = determineKillStatus(target, destroyedBeforeHurt);
        
        if (owner instanceof ServerPlayer serverPlayer) {
            notifyPlayerOfHit(serverPlayer, target, kill);
        }
    }

    /**
     * Determines if the target was killed by the attack.
     */
    private boolean determineKillStatus(Entity target, boolean destroyedBeforeHurt) {
        if (target instanceof AbstractVehicle vehicle) {
            return !destroyedBeforeHurt && vehicle.isDestroyed();
        } else if (target instanceof LivingEntity livingEntity) {
            return livingEntity.isDeadOrDying();
        }
        return false;
    }

    /**
     * Notifies the player of a successful hit.
     */
    private void notifyPlayerOfHit(ServerPlayer player, Entity target, boolean kill) {
        Channel.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new ServerVehicleHurtEntity(vehicle.getId(), target.getId(), kill)
        );
    }

    /**
     * Creates an explosion at the specified location.
     */
    private void createExplosion(Vec3 location) {
        VehicleExplosion vehicleExplosion = new VehicleExplosion(
            level(), 
            this.getOwner(), 
            this.vehicle, 
            location, 
            explosion.radius, 
            explosion.damage, 
            explosion.destroyBlock
        );
        vehicleExplosion.explode();
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
