package org.ywzj.vehicle.entity.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

public class AerialBombEntity extends AmmoEntity {

    private float fuseDelay = 60;
    private VehicleSound soundWhistle;
    private VehicleSound soundIncoming;

    public AerialBombEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public AerialBombEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(AllEntities.AERIAL_BOMB.get(), level);
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
            motion = motion.add(0, -0.3, 0);
            this.setDeltaMovement(motion);
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void tickHit() {
        if (onGround()) {
            if (fuseDelay > 0) {
                fuseDelay -= 1;
            } else {
                VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), this.getOwner(), position(), 16, 20);
                vehicleExplosion.explode();
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
            if (soundWhistle == null) {
                soundWhistle = new VehicleSound(AllSounds.BOMB_WHISTLE.get(), 1f, 2f, 1f, false, 50, true, true, this.getId());
                soundWhistle.play();
            }
            if (LocalVehiclePlayer.instance.getPlayer().distanceTo(this) < 8) {
                if (soundIncoming == null) {
                    soundIncoming = new VehicleSound(AllSounds.BOMBS_INCOMING.get(), 1f, 2f, 1f, false, 50, true, true, this.getId());
                    soundIncoming.play();
                }
            }
        }
    }

}
