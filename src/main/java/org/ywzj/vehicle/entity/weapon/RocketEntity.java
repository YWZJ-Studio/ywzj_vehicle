package org.ywzj.vehicle.entity.weapon;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.PlayingState;
import com.maydaymemory.mae.control.runner.StopState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.InternalAssets;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.custom.weapon.data.VehicleRocketWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.particle.SmokeCloudOption;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.PhysicsEngine;

public class RocketEntity extends AmmoEntity {

    public float mass;
    public float thrust;
    public float motorBurnTime;
    public Vec3 engineNozzleOffset;
    public float dragCoefficient;
    public AnimationRunner animationRunner;
    private VehicleSound sound;
    private Vec3 particlePosO;

    public RocketEntity(EntityType<? extends Projectile> entityType, Level level, VehicleRocketWeaponData data) {
        super(entityType, level, data.getWeaponId());
        initRocket(data);
        this.keepChunkLoaded = true;
    }

    public RocketEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level, null);
    }

    public RocketEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.ROCKET.get(), level, null);
    }

    public void shoot(AbstractVehicle vehicle, Component name, Vec3 spawnPos, float ammoXRot, float ammoYRot, float inaccuracy, LivingEntity shooter) {
        this.vehicle = vehicle;
        this.name = name;
        this.setPos(spawnPos);
        Vec3 direction = VectorUtil.rotToVec(ammoXRot, ammoYRot);
        direction = direction.add(
                this.random.triangle(0.0D, 0.0172275D * inaccuracy),
                this.random.triangle(0.0D, 0.0172275D * inaccuracy),
                this.random.triangle(0.0D, 0.0172275D * inaccuracy))
                .normalize();
        Vec2 rotation = VectorUtil.vecToRot(direction);
        this.setRot(rotation.y, rotation.x);
        this.setOwner(shooter);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        super.readSpawnData(additionalData);
        VehicleWeaponIndex<?, ?> vehicleWeaponIndex = CommonAssetsManager.vehicleWeaponManager().getIndex(getWeaponId()).orElse(null);
        if (vehicleWeaponIndex != null && vehicleWeaponIndex.data() instanceof VehicleRocketWeaponData data) {
            initRocket(data);
        }
        InternalAssets assets = ClientAssetsManager.INSTANCE.getInternalAssets();
        BedrockAnimation flameAnimation = assets.getRocketMotorFlameAnimation();
        AnimationContext animContext = new AnimationContext(flameAnimation.getSpecifiedEndTimeS());
        animationRunner = new AnimationRunner(flameAnimation, animContext);
        animationRunner.setState(new PlayingState(System::nanoTime, StopState::new));
        if (triggered) {
            animContext.setProgress(flameAnimation.getSpecifiedEndTimeS());
        }
    }

    private void initRocket(VehicleRocketWeaponData data) {
        this.mass = data.getMass();
        this.thrust = data.getThrust();
        this.motorBurnTime = data.getMotorBurnTime();
        this.engineNozzleOffset = data.getEngineNozzleOffset();
        this.dragCoefficient = data.getDragCoefficient();
        this.damage = data.getDamage();
        this.explosion = data.getExplosion();
        this.life = data.getLife();
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
            life -= 1;
            if (life < 0 && !isRemoved()) {
                this.discard();
            }
        }
    }

    private void tickMove() {
        Vec3 velocity = this.getDeltaMovement();
        Vec3 lookDir = this.getLookAngle();
        if (isMotorBurning()) {
            double acceleration = (this.thrust / this.mass);
            velocity = velocity.add(lookDir.scale(acceleration));
        }
        double speedSqr = velocity.lengthSqr();
        if (speedSqr > 0) {
            Vec3 drag = velocity.normalize().scale(-dragCoefficient * speedSqr);
            velocity = velocity.add(drag);
        }
        velocity = velocity.subtract(0, PhysicsEngine.G, 0);
        this.setDeltaMovement(velocity);
        double dx = this.getX() + velocity.x;
        double dy = this.getY() + velocity.y;
        double dz = this.getZ() + velocity.z;
        if (!((ServerLevel) level()).isPositionEntityTicking(BlockPos.containing(dx, dy, dz))) {
            EntityUtil.keepChunkLoaded(this, new Vec3(dx, dy, dz));
            return;
        }
        this.setPos(dx, dy, dz);
        if (tickCount > motorBurnTime && velocity.lengthSqr() > 0.01) {
            Vec3 normVel = velocity.normalize();
            double pitch = Math.toDegrees(-Math.asin(normVel.y));
            double yaw = Math.toDegrees(Math.atan2(normVel.z, normVel.x)) - 90.0;
            this.setXRot((float) Mth.lerp(0.2, this.getXRot(), pitch));
            this.setYRot((float) Mth.lerp(0.2, this.getYRot(), yaw));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tickParticle() {
        if (isMotorBurning() && engineNozzleOffset != null) {
            Vec3 previousPosition = new Vec3(this.xo, this.yo, this.zo);
            Vec3 rotatedOffset = engineNozzleOffset
                    .xRot(-this.xRotO * Mth.DEG_TO_RAD)
                    .yRot(-this.yRotO * Mth.DEG_TO_RAD);
            Vec3 pos = previousPosition.add(rotatedOffset);
            Vec3 posO = particlePosO == null ? pos : particlePosO;
            Vec3 step = pos.subtract(posO);
            double dist = step.length();
            int segments = (int) (dist / 0.5);
            Vec3 dir = step.normalize();
            Level level = level();
            for (int i = 0; i <= segments; i++) {
                Vec3 particlePos = posO.add(dir.scale(i * 0.5));
                double dx = (level.random.nextDouble() - 0.5) * 0.2;
                double dy = (level.random.nextDouble() - 0.5) * 0.2;
                double dz = (level.random.nextDouble() - 0.5) * 0.2;
                level.addParticle(new SmokeCloudOption(0.7f, 0.7f, 0.7f,
                                0.66f, 0.66f, 0.66f,
                                40, 0.2f, 0.005f), true,
                        particlePos.x + dx, particlePos.y + dy, particlePos.z + dz,
                        0.015, 0.015, 0.015);
            }
            particlePosO = pos;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tickSound() {
        if (sound == null) {
            sound = new VehicleSound(AllSounds.ROCKET_FLYING.get(), 1f, 1f, 1f, false, 50, true, true, this.getId());
            sound.play();
        }
    }

    @Override
    public float getCaliber() {
        return 40f;
    }

    public boolean isMotorBurning() {
        return tickCount <= motorBurnTime;
    }

}
