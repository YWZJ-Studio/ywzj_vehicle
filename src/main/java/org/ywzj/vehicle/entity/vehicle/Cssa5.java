package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class Cssa5 extends WheeledVehicle {

    public int partRotateTick;

    public Cssa5(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (hasPower()) {
                partRotateTick += 1;
            }
        }
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
//        double velocity = Math.abs(entityData.get(FORWARD_SPEED)) * 20 + Math.abs(entityData.get(TURN_SPEED)) * 5;
//        if ((!this.getPassengers().isEmpty() && velocity > 0 || tickCount % 10 == 0) && hasPower()) {
//            Vec3 v1 = this.getLookAngle();
//            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
//            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(-4.2)).add(v2.scale(0.2)).add(0, 1.7, 0);
//            for (int count = 0; count < velocity / 32 + 1; count++) {
//                Vec3 engineSmokeVelocity = this.getLookAngle().normalize().scale(-0.1);
//                level().addParticle(ParticleTypes.LARGE_SMOKE, true,
//                        engineSmokePos.x, engineSmokePos.y, engineSmokePos.z,
//                        engineSmokeVelocity.x, engineSmokeVelocity.y, engineSmokeVelocity.z);
//            }
//        }
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(weaponIndex, aimContexts, operator);
        }
    }

}
