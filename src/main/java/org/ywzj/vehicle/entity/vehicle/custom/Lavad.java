package org.ywzj.vehicle.entity.vehicle.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;

public class Lavad extends WheeledVehicle {

    public int partRotateTick;

    public Lavad(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
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

}
