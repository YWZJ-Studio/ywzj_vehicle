package org.ywzj.vehicle.entity.vehicle.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;

public class Quadcopter extends RotaryWingVehicle {

    public Quadcopter(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

}
