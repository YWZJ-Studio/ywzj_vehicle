package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllDamageTypes;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.api.entity.KnockBackModifier;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerVehicleHurtEntity;
import org.ywzj.vehicle.util.BlockRayTrace;
import org.ywzj.vehicle.util.BulletHitResult;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Kinetic weapon projectile entity.
 */
public class BulletEntity extends AmmoEntity {

    private ResourceLocation weaponId;
    private int life = 200;
    private float speed = 1;
    private float gravity = 0;
    private float friction = 0.01F;
    private float knockback = 0;
    // Penetration count
    private int pierce = 1;
    // Initial position
    private Vec3 startPos;
    private float armorIgnore;
    private float headShot;
    // Tracer properties
    private float caliber = 7.62f;
    private float tracerR = 1f;
    private float tracerG = 1f;
    private float tracerB = 1f;

    // Returns distance-based damage multiplier
    private Function<Double, Float> distanceDamageFunction = (distance) -> 1.0f;
    
    // Weapon data reference for damage falloff calculation
    private VehicleCannonWeaponData weaponData;

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

    public BulletEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.BULLET.get(), level, null);
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
        // Invoke TaC bullet server event
        this.onBulletTick();
        // Particle effects
        if (this.level().isClientSide) {
//            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AmmoParticleSpawner.addParticle(this));
        }
        // Bullet rotation and trajectory
        Vec3 movement = this.getDeltaMovement();
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        double distance = movement.horizontalDistance();
        this.setYRot((float) Math.toDegrees(Mth.atan2(x, z)));
        this.setXRot((float) Math.toDegrees(Mth.atan2(y, distance)));
        // Initial rotation setup
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
        // Rotation interpolation (excluding spin)
        this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
        this.setYRot(lerpRotation(this.yRotO, this.getYRot()));
        // Position update
        double nextPosX = this.getX() + x;
        double nextPosY = this.getY() + y;
        double nextPosZ = this.getZ() + z;
        this.setPos(nextPosX, nextPosY, nextPosZ);
        float friction = this.friction;
        float gravity = this.gravity;
        // Water adjustments
        if (this.isInWater()) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(ParticleTypes.BUBBLE, nextPosX - x * 0.25F, nextPosY - y * 0.25F, nextPosZ - z * 0.25F, x, y, z);
            }
            friction = 0.4F;
            gravity *= 0.6F;
        }
        // Apply gravity and friction
        this.setDeltaMovement(this.getDeltaMovement().scale(1 - friction));
        this.setDeltaMovement(this.getDeltaMovement().add(0, -gravity, 0));
        // Lifetime expiration
        if (this.tickCount >= this.life - 1) {
            this.discard();
        }
    }

    // Bullet logic processing
    protected void onBulletTick() {
        // Server-side bullet logic
        if (!this.level().isClientSide()) {
            // Bullet position at tick start
            Vec3 startVec = this.position();
            // Bullet position at tick end
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            // Block collision detection
            BlockHitResult result = BlockRayTrace.rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (result.getType() != HitResult.Type.MISS) {
                // Set end position to block hit location
                endVec = result.getLocation();
            }

            List<BulletHitResult> hitEntities = null;
            // Entity hit detection, limited to single penetration for pierce <= 1 or explosive ammo
            if (this.pierce <= 1) {
                BulletHitResult entityResult = EntityUtil.findEntityOnPath(this, startVec, endVec);
                // Create single-element list for single hit
                if (entityResult != null) {
                    hitEntities = Collections.singletonList(entityResult);
                }
            } else {
                hitEntities = EntityUtil.findEntitiesOnPath(this, startVec, endVec);
            }
            // Process entity hits
            if (hitEntities != null && !hitEntities.isEmpty()) {
                hitEntities.stream()
                        .sorted(Comparator.comparingDouble(r -> r.getLocation().distanceToSqr(startVec)))
                        .limit(pierce)
                        .forEach(entityResult -> {
                            // Handle entity hit logic
                            this.onHitEntity(entityResult);
                            this.pierce--;
                        });
                if (this.pierce < 1) {
                    // All penetrations exhausted
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
                AllDamageTypes.Sources.bullet(level().registryAccess(), this, attacker, result.getLocation()),
                AllDamageTypes.Sources.bullet(level().registryAccess(), this, attacker, result.getLocation())
        );

        boolean kill = false;
        boolean destroyedBeforeHurt = false;
        if (entity instanceof AbstractVehicle vehicle) {
            destroyedBeforeHurt = vehicle.isDestroyed();
        }

        // Apply custom knockback for living entities
        if (entity instanceof LivingEntity livingCore) {
            applyDamageWithKnockback(livingCore, entity, damage, sources);
        } else {
            performAttack(entity, damage, sources);
        }

        kill = determineKillStatus(entity, destroyedBeforeHurt);

        if (owner instanceof ServerPlayer serverPlayer) {
            notifyPlayerOfHit(serverPlayer, entity, kill);
        }

        if (explosion.explode) {
            createExplosion(result.getLocation());
        }
    }

    /**
     * Applies damage with custom knockback strength for living entities.
     */
    private void applyDamageWithKnockback(LivingEntity livingCore, Entity entity, float damage, Pair<DamageSource, DamageSource> sources) {
        KnockBackModifier modifier = KnockBackModifier.fromLivingEntity(livingCore);
        modifier.ywzj_vehicle$setKnockBackStrength(this.knockback);
        performAttack(entity, damage, sources);
        modifier.ywzj_vehicle$resetKnockBackStrength();
    }

    /**
     * Determines if the entity was killed by the attack.
     */
    private boolean determineKillStatus(Entity entity, boolean destroyedBeforeHurt) {
        if (entity instanceof AbstractVehicle vehicle) {
            return !destroyedBeforeHurt && vehicle.isDestroyed();
        } else if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.isDeadOrDying();
        }
        return false;
    }

    /**
     * Notifies the player of a successful hit.
     */
    private void notifyPlayerOfHit(ServerPlayer serverPlayer, Entity entity, boolean kill) {
        Channel.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> serverPlayer),
            new ServerVehicleHurtEntity(vehicle.getId(), entity.getId(), kill)
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

    protected void onHitBlock(BlockHitResult result, Vec3 startVec, Vec3 endVec) {
        if (result.getType() == HitResult.Type.MISS) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        Vec3 hitVec = result.getLocation();

        super.onHitBlock(result);

        // Armor-piercing explosion
        if (explosion.explode) {
            handleArmorPiercingExplosion(pos, hitVec, startVec, endVec);
        }

        // Bullet hole and ignition effects (disabled)
//        if (this.level() instanceof ServerLevel serverLevel) {
//            BulletHoleOption bulletHoleOption = new BulletHoleOption(result.getDirection(), result.getBlockPos(), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
//            serverLevel.sendParticles(bulletHoleOption, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
//        }
        this.discard();
    }

    /**
     * Handles armor-piercing explosion mechanics.
     * Destroys weak blocks and calculates explosion position after penetration.
     */
    private void handleArmorPiercingExplosion(BlockPos pos, Vec3 hitVec, Vec3 startVec, Vec3 endVec) {
        Level level = level();
        BlockState state = level.getBlockState(pos);
        float destroySpeed = state.getDestroySpeed(level, pos);
        
        // Destroy weak blocks if configured
        if (!state.isAir() && destroySpeed > 0 && destroySpeed < 50 
                && explosion.destroyBlock && AllConfigs.common.explosionDestroyBlocks.get()) {
            level().destroyBlock(pos, false);
        }
        
        // Calculate explosion position after penetration
        Vec3 explosionAtPos = hitVec.add(endVec.subtract(startVec).normalize().scale(getDeltaMovement().length()));
        BlockHitResult resultAfterPenetrate = BlockRayTrace.rayTraceBlocks(
            this.level(), 
            new ClipContext(hitVec, explosionAtPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)
        );
        
        if (resultAfterPenetrate.getType() != HitResult.Type.MISS) {
            explosionAtPos = resultAfterPenetrate.getLocation();
        }
        
        createExplosion(explosionAtPos);
    }

    // TODO: Implement distance-based damage falloff system
    /**
     * Calculates damage at hit location with distance-based falloff.
     * Uses the weapon's damage falloff configuration to reduce damage over distance.
     * 
     * @param hitVec Hit location in world coordinates
     * @return Final damage value after applying falloff multiplier
     */
    public float getDamage(Vec3 hitVec) {
        double travelDistance = hitVec.distanceTo(this.startPos);
        
        // Use new damage falloff system if weapon data is available
        if (weaponData != null && weaponData.getDamageFalloff() != null) {
            float multiplier = weaponData.getDamageFalloff().calculateMultiplier(travelDistance);
            return damage * multiplier;
        }
        
        // Fallback to legacy function-based system
        float multiplier = this.distanceDamageFunction.apply(travelDistance);
        return damage * multiplier;
    }


    /**
     * Performs a split damage attack with armor penetration mechanics.
     * Damage is split between normal and armor-piercing based on armorIgnore value.
     */
    private void performAttack(Entity target, float damage, Pair<DamageSource, DamageSource> sources) {
        var normalSource = sources.getLeft();
        var armorPiercingSource = sources.getRight();
        
        // Calculate damage split between normal and armor-piercing
        float armorDamagePercent = Mth.clamp(this.armorIgnore, 0.0F, 1.0F);
        float normalDamagePercent = 1 - armorDamagePercent;

        // Apply normal damage
        resetInvulnerability(target);
        target.hurt(normalSource, damage * normalDamagePercent);

        // Apply armor-piercing damage
        resetInvulnerability(target);
        target.hurt(armorPiercingSource, damage * armorDamagePercent);
    }

    /**
     * Resets entity invulnerability time to allow consecutive damage application.
     */
    private void resetInvulnerability(Entity entity) {
        if (entity instanceof PartEntity<?> part) {
            part.getParent().invulnerableTime = 0;
        } else {
            entity.invulnerableTime = 0;
        }
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
        buffer.writeResourceLocation(this.weaponId);
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
        this.startPos = this.position();
        this.gravity = additionalData.readFloat();
        this.life = additionalData.readInt();
        this.speed = additionalData.readFloat();
        this.friction = additionalData.readFloat();
        this.weaponId = additionalData.readResourceLocation();
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

    public void setHeadShot(float headShot) {
        this.headShot = headShot;
    }

    public float getHeadShot() {
        return headShot;
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

    public void setDistanceDamageFunction(Function<Double, Float> distanceDamageFunction) {
        this.distanceDamageFunction = distanceDamageFunction;
    }

    /**
     * Sets weapon data for advanced damage falloff calculations.
     * This enables the new distance-based damage falloff system.
     * 
     * @param weaponData Weapon configuration data
     */
    public void setWeaponData(VehicleCannonWeaponData weaponData) {
        this.weaponData = weaponData;
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
