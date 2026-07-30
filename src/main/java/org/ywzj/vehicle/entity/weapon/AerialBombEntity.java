package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PhysicsEngine;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

public class AerialBombEntity extends AmmoEntity {

    public int fuseDelayTick;
    public float penetrationDepth;
    public boolean homing;
    public float dragCoefficient;
    public float maxG;
    public float referenceSpeed;
    public WeaponUnit weaponUnit;
    public Entity targetEntity;
    public Vec3 targetPos;
    private float remainingPenetration = -1;
    private VehicleSound soundWhistle;
    private VehicleSound soundIncoming;

    public AerialBombEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(AllEntities.ROCKET.get(), level, null);
    }

    public AerialBombEntity(EntityType<? extends Projectile> entityType, Level level, ResourceLocation weaponId) {
        super(entityType, level, weaponId);
        this.keepChunkLoaded = true;
    }

    public AerialBombEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.AERIAL_BOMB.get(), level, null);
    }

    public void shoot(AbstractVehicle vehicle, Component name, Vec3 spawnPos, float ammoXRot, float ammoYRot, float inaccuracy, LivingEntity shooter) {
        this.vehicle = vehicle;
        this.name = name;
        this.setPos(spawnPos);
        this.setRot((float) (ammoYRot + inaccuracy * 16 * (0.5 - this.random.nextFloat())),
                (float) (ammoXRot + inaccuracy * 16 * (0.5 - this.random.nextFloat())));
        this.setDeltaMovement(vehicle.getDeltaMovement());
        this.setOwner(shooter);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickSound();
        } else {
            tickMove();
            tickHit();
        }
    }

    private void tickMove() {
        if (!this.isNoGravity()) {
            Vec3 motion = this.getDeltaMovement();
            motion = motion.add(0, -PhysicsEngine.G, 0);
            // 空气阻力
            double speedSqr = motion.lengthSqr();
            if (speedSqr > 0) {
                Vec3 drag = motion.normalize().scale(-dragCoefficient * speedSqr);
                motion = motion.add(drag);
            }
            this.setDeltaMovement(motion);
        }
        if (!onGround()) {
            tickGuidance();
        }
        Vec3 toPosition = position().add(getDeltaMovement());
        if (!((ServerLevel) level()).isPositionEntityTicking(BlockPos.containing(toPosition))) {
            EntityUtil.keepChunkLoaded(this, toPosition);
            return;
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void tickGuidance() {
        if (!homing) {
            return;
        }
        if (targetEntity != null && targetEntity.isAlive()) {
            intercept(targetEntity, null);
        } else if (targetPos != null) {
            intercept(null, targetPos);
        }
    }

    protected void tickHit() {
        if (onGround()) {
            if (remainingPenetration < 0 && penetrationDepth > 0) {
                remainingPenetration = penetrationDepth;
            }
            if (remainingPenetration > 0) {
                penetrateOneBlock();
                return;
            }
            if (fuseDelayTick > 0) {
                fuseDelayTick -= 1;
            } else {
                if (explosion != null && explosion.explode) {
                    VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), this, position(), explosion.radius, explosion.damage, explosion.destroyBlock);
                    vehicleExplosion.explode();
                }
                this.kill();
            }
        }
    }

    private void penetrateOneBlock() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        pos.set(this.blockPosition());
        pos.move(0, -1, 0);

        if (pos.getY() <= level().getMinBuildHeight()) {
            remainingPenetration = 0;
            return;
        }
        BlockState state = level().getBlockState(pos);
        if (state.isAir()) {
            this.setPos(this.getX(), pos.getY(), this.getZ());
            return;
        }
        float hardness = state.getDestroySpeed(level(), pos);
        if (hardness < 0) {
            remainingPenetration = 0;
            return;
        }
        float cost = Math.max(0.1f, hardness);
        if (cost > remainingPenetration) {
            remainingPenetration = 0;
            return;
        }
        remainingPenetration -= cost;
        level().destroyBlock(pos, false, this.getOwner());
        this.setPos(this.getX(), pos.getY(), this.getZ());
    }

    @OnlyIn(Dist.CLIENT)
    public void tickSound() {
        if (onGround()) {
            if (soundWhistle != null) {
                soundWhistle.stop();
            }
            if (soundIncoming != null) {
                soundIncoming.stop();
            }
        } else {
            Player player = LocalVehiclePlayer.instance.getPlayer();
            if (soundWhistle == null
                    && player.distanceTo(this) < 8
                    && !(player.getVehicle() instanceof AbstractVehicle)) {
                soundWhistle = new VehicleSound(AllSounds.BOMBS_INCOMING.get(), 1f, 2f, 1f, false, 50, true, true, this.getId());
                soundWhistle.play();
            }
            if (player.distanceTo(this) < 32) {
                if (soundIncoming == null) {
                    soundIncoming = new VehicleSound(AllSounds.BOMB_WHISTLE.get(), 1f, 2f, 1f, false, 50, true, true, this.getId());
                    soundIncoming.play();
                }
            }
        }
    }

    @Override
    public float getCaliber() {
        return 125f;
    }

    private void intercept(Entity target, Vec3 pos) {
        Vec3 bombPos = this.position();
        Vec3 bombVel = this.getDeltaMovement();
        double bombSpeed = bombVel.length();
        if (bombSpeed == 0) {
            return;
        }

        Vec3 targetPos;
        Vec3 targetVel;
        if (target != null) {
            targetVel = target.getDeltaMovement();
            targetPos = target.getBoundingBox().getCenter().add(targetVel);
        } else {
            targetVel = Vec3.ZERO;
            targetPos = pos;
        }

        double targetSpeedSq = targetVel.lengthSqr();
        Vec3 d = targetPos.subtract(bombPos);
        double a = targetSpeedSq - (bombSpeed * bombSpeed);
        double b = 2.0 * d.dot(targetVel);
        double c = d.lengthSqr();
        double t = -1.0;
        if (Math.abs(a) < 1e-5) {
            if (b < 0) t = -c / b;
        } else {
            double discriminant = b * b - 4 * a * c;
            if (discriminant >= 0) {
                double t1 = (-b + Math.sqrt(discriminant)) / (2 * a);
                double t2 = (-b - Math.sqrt(discriminant)) / (2 * a);
                if (t1 > 0 && t2 > 0) t = Math.min(t1, t2);
                else t = Math.max(t1, t2);
            }
        }
        if (t <= 0) {
            Vec3 relVel = targetVel.subtract(bombVel);
            double closingSpeed = bombSpeed - relVel.dot(d.normalize());
            t = d.length() / Math.max(closingSpeed, 0.1);
        }

        Vec3 interceptPos = targetPos.add(targetVel.scale(t));
        Vec3 targetDir = interceptPos.subtract(bombPos).normalize();

        Vec3 desiredDir;
        double dot = bombVel.dot(targetDir);
        double magSq = bombSpeed * bombSpeed;
        double discriminant = dot * dot - magSq;
        if (discriminant < 0) {
            desiredDir = targetDir.scale(dot * PhysicsEngine.MAGIC_NUMBER * 4).subtract(bombVel);
        } else {
            double k = dot + Math.sqrt(discriminant);
            desiredDir = targetDir.scale(k).subtract(bombVel);
        }

        Vec2 rot = VectorUtil.vecToRot(desiredDir);
        float targetXRot = rot.x;
        float targetYRot = rot.y;

        // 动压限制过载
        double dynamicPressureFactor = Math.min(1.0, (bombSpeed * bombSpeed) / (referenceSpeed * referenceSpeed));
        double availableG = this.maxG * dynamicPressureFactor;
        double maxAccelLimit = availableG * PhysicsEngine.G;
        double maxOmega = maxAccelLimit / bombSpeed;
        double maxAnglePerTick = Math.toDegrees(maxOmega);

        float curX = this.getXRot();
        float curY = this.getYRot();
        float deltaX = Mth.wrapDegrees(targetXRot - curX);
        float deltaY = Mth.wrapDegrees(targetYRot - curY);
        deltaX = Mth.clamp(deltaX, (float) -maxAnglePerTick, (float) maxAnglePerTick);
        deltaY = Mth.clamp(deltaY, (float) -maxAnglePerTick, (float) maxAnglePerTick);
        this.setXRot(curX + deltaX);
        this.setYRot(curY + deltaY);

        // 尾翼升力：将小部分速度重定向至弹体指向
        if (bombSpeed < referenceSpeed * 0.7) {
            return;
        }
        Vec3 lookDir = this.getLookAngle();
        double alignment = Mth.clamp(bombVel.normalize().dot(lookDir), 0, 1);
        double steeringBlend = 0.3 * dynamicPressureFactor * alignment;
        Vec3 newDir = bombVel.normalize().scale(1 - steeringBlend)
                .add(lookDir.scale(steeringBlend)).normalize();
        this.setDeltaMovement(newDir.scale(bombSpeed));
    }

}
