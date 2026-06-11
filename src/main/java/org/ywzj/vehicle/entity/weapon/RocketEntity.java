package org.ywzj.vehicle.entity.weapon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.custom.weapon.data.VehicleRocketWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.particle.SmokeCloudOption;
import org.ywzj.vehicle.vehicle.PhysicsEngine;

public class RocketEntity extends AmmoEntity {

    public float mass;
    public float thrust;
    public float motorBurnTime;
    public float dragCoefficient;
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
        this.setRot((float) (ammoYRot + inaccuracy * 16 * (0.5 - this.random.nextFloat())),
                (float) (ammoXRot + inaccuracy * 16 * (0.5 - this.random.nextFloat())));
        this.setOwner(shooter);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        super.readSpawnData(additionalData);
        VehicleWeaponIndex<?, ?> vehicleWeaponIndex = CommonAssetsManager.vehicleWeaponManager().getIndex(getWeaponId()).orElse(null);
        if (vehicleWeaponIndex != null && vehicleWeaponIndex.data() instanceof VehicleRocketWeaponData data) {
            initRocket(data);
        }
    }

    private void initRocket(VehicleRocketWeaponData data) {
        this.mass = data.getMass();
        this.thrust = data.getThrust();
        this.motorBurnTime = data.getMotorBurnTime();
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
        if (tickCount <= motorBurnTime) {
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
        if (tickCount <= motorBurnTime) {
            Vec3 pos = this.position().add(this.getLookAngle().scale(-1));
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

}
