package org.ywzj.vehicle.entity.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PhysicsEngine;

public class AerialBombEntity extends AmmoEntity {

    public int fuseDelayTick;
    private VehicleSound soundWhistle;
    private VehicleSound soundIncoming;

    public AerialBombEntity(EntityType<? extends Projectile> entityType, Level level, ResourceLocation weaponId) {
        super(entityType, level, weaponId);
    }

    public AerialBombEntity(EntityType<? extends AerialBombEntity> entityType, Level level) {
        super(entityType, level, null);
    }

    public void shoot(AbstractVehicle vehicle, Component name, Vec3 spawnPos, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        this.vehicle = vehicle;
        this.name = name;
        this.setPos(spawnPos);
        this.setRot(ammoYRot, ammoXRot);
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
            this.setDeltaMovement(motion);
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    protected void tickHit() {
        if (onGround()) {
            if (fuseDelayTick > 0) {
                fuseDelayTick -= 1;
            } else {
                if (explosion != null && explosion.explode) {
                    VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), this.vehicle, position(), explosion.radius, explosion.damage, explosion.destroyBlock);
                    vehicleExplosion.explode();
                }
                this.kill();
            }
        }
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

}
