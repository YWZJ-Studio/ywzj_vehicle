package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.ywzj.vehicle.all.AllParticleTypes;
import org.ywzj.vehicle.particle.DustSmokeOption;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.pojo.PassengerPose;

import java.util.List;

public class Motorcycle extends WheeledVehicle {

    public Motorcycle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        physicsEngine.friction = 0.003f;
        physicsEngine.lockZRot = true;
    }

    @Override
    public void initData() {
        super.initData();
        PartUnit<?> passengerSeat = partUnits.get(0);
        PassengerPose passengerPose = new PassengerPose();
        passengerPose.leftArmRotX = -1.5f;
        passengerPose.rightArmRotX= -1.5f;
        passengerSeat.setPassengerPose(passengerPose);
    }

    @Override
    protected void tickParticle() {
        trackLength += getDeltaMovement().length();
        if (trackLength >= 0.5) {
            trackLength = 0;
            Vec3 trackPos = relativeRotPos(position().add(0, 0, -mainCubeOBB.obb().extents().z), false);
            if (EntityUtil.isOnBlockSurface(this, trackPos)) {
                this.level().addParticle(AllParticleTypes.TRACK.get(), true,
                        trackPos.x, trackPos.y, trackPos.z,  trackSize, this.getYRot(), 0
                );
            }
        }

        double speedSqr = this.getDeltaMovement().lengthSqr();
        if (speedSqr <= 0.255) return;

        int interval;
        if (speedSqr < 0.64) {
            interval = 4;
        } else if (speedSqr < 0.81) {
            interval = 3;
        } else if (speedSqr < 1) {
            interval = 2;
        } else {
            interval = 1;
        }
        if (tickCount % interval != 0) return;

        var matrix4f = this.getWheelsTransform(1f);
        spawnDustParticles(0.8f, matrix4f);
        spawnDustParticles(-0.8f, matrix4f);
    }

    private void spawnDustParticles(float offset, Matrix4f matrix4f) {
        var wheel = this.transformPosition(matrix4f, 0, 0, offset);
        this.level().addParticle(new DustSmokeOption(2.5f),
                wheel.x() - this.getDeltaMovement().x,
                wheel.y() + 0.1,
                wheel.z() - this.getDeltaMovement().z,
                this.getDeltaMovement().x * 0.1,
                0.02,
                this.getDeltaMovement().z * 0.1);
        double dx = (offset > 0 ? 0.1 : 0.25);
        double dy = (offset > 0 ? 0.02 : 0.1);
        if (this.random.nextFloat() < 0.5f) return;
        this.level().addParticle(AllParticleTypes.DUST_STONE.get(),
                wheel.x() + this.random.nextDouble() * 0.1 - this.getDeltaMovement().x,
                wheel.y() + 0.1,
                wheel.z() + this.random.nextDouble() * 0.1 - this.getDeltaMovement().z,
                this.getDeltaMovement().x * dx + this.random.nextDouble() * 0.1,
                dy,
                this.getDeltaMovement().z * dx + this.random.nextDouble() * 0.1
        );
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        // 喇叭声
    }

}
