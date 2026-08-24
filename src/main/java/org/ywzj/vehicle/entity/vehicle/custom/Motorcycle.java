package org.ywzj.vehicle.entity.vehicle.custom;

import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.util.ParticleUtil;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.pojo.PassengerPose;

import java.util.List;

public class Motorcycle extends WheeledVehicle {

    public Motorcycle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        physicsEngine.physicsInfo.friction = 0.003f;
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
    @OnlyIn(Dist.CLIENT)
    protected void tickParticle() {
        trackLength += getDeltaMovement().length();
        if (trackLength >= 0.5) {
            trackLength = 0;
            Vec3 trackPos = relativeRotPos(position().add(0, 0, -mainCubeOBB.obb().extents().z), false);
            ParticleUtil.spawnTracks(level(), trackSize, getYRot(), trackPos);
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
        ParticleUtil.spawnMotorcycleDust(level(), random, new Vec3(wheel.x(), wheel.y(), wheel.z()), getDeltaMovement(), offset > 0);
    }

    public Matrix4f getWheelsTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        return transform;
    }

    public Vector4f transformPosition(Matrix4f transform, float x, float y, float z) {
        return transform.transform(new Vector4f(x, y, z, 1));
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        // 喇叭声
    }

}
