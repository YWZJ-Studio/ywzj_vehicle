package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class RocketEntity extends AmmoEntity {

    private VehicleSound sound;

    public RocketEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level, null);
    }

    public RocketEntity(EntityType<? extends Projectile> entityType, Level level, ResourceLocation weaponId) {
        super(entityType, level, weaponId);
    }

    public RocketEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.ROCKET.get(), level, null);
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
        Vec3 velocity = this.getDeltaMovement();
        double dx = this.getX() + velocity.x;
        double dy = this.getY() + velocity.y;
        double dz = this.getZ() + velocity.z;
        this.setPos(dx, dy, dz);
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
            sound = new VehicleSound(AllSounds.ROCKET_FLYING.get(), 1f, 1f, 1f, false, 50, true, true, this.getId());
            sound.play();
        }
    }

}
