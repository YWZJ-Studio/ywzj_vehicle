package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class DumpTruck extends WheeledVehicle {

    private VehicleSound bedTurnSoundInstance;

    public DumpTruck(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (getDriver() != null) {
                if (controlUnit.leftYaw || controlUnit.rightYaw) {
                    RotatableUnit bed = (RotatableUnit) partUnits.get(1);
                    if (controlUnit.leftYaw) {
                        bed.setXAimRot(bed.getXAimRot() - 5);
                    } else {
                        bed.setXAimRot(bed.getXAimRot() + 5);
                    }
                    bed.setXAimRot(Mth.clamp(bed.getXAimRot(), bed.xRotMin, bed.xRotMax));
                    ClientVehicleAction control = new ClientVehicleAction();
                    control.vehicleEntityId = this.getId();
                    control.partUnitIndex = bed.getIndex();
                    control.xAimRot = bed.getXAimRot();
                    control.yAimRot = 0;
                    Channel.CHANNEL.sendToServer(control);
                }
            }
        }
    }

    @Override
    protected void tickSound() {
        super.tickSound();
        RotatableUnit bed = (RotatableUnit) partUnits.get(1);
        if (Math.abs(bed.getXAimRot() - bed.getXRot()) > 1 && bed.getXRot() < bed.xRotMax && bed.getXRot() > bed.xRotMin) {
            if (bedTurnSoundInstance == null) {
                bedTurnSoundInstance = new VehicleSound(AllSounds.TURRET_TURN_SERVO_V.get(), 1f, 1f, 1f, true, 10, true, true, this.getId());
                bedTurnSoundInstance.play();
            }
        } else {
            if (bedTurnSoundInstance != null) {
                bedTurnSoundInstance.stop();
                bedTurnSoundInstance = null;
            }
        }
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
        if (hasPower() && tickCount % 10 == 0) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(2f)).add(v2.scale(-1.6)).add(0, 3, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
        }
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {}

}
