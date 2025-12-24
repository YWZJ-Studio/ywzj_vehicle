package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class Ka50 extends RotaryWingVehicle {

    public Ka50(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
        float engineSpeed = getPower();
        int collectivePitch = getCollectivePitch();
        if ((!this.getPassengers().isEmpty() && engineSpeed > 0 && tickCount % Mth.clamp(10 - collectivePitch / 10, 3, 10) == 0) && hasPower()) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePosLeft = this.position().add(this.getLookAngle().normalize().scale(-1f)).add(v2.scale(-0.9)).add(0, 2.5, 0);
            Vec3 engineSmokePosRight = this.position().add(this.getLookAngle().normalize().scale(-1f)).add(v2.scale(0.9)).add(0, 2.5, 0);
            Vec3 vSmoke = v1.scale(-0.3);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosLeft.x, engineSmokePosLeft.y, engineSmokePosLeft.z, vSmoke.x, vSmoke.y, vSmoke.z);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosRight.x, engineSmokePosRight.y, engineSmokePosRight.z, vSmoke.x, vSmoke.y, vSmoke.z);
        }
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(weaponIndex, aimContexts, operator);
        }
    }

}
