package org.ywzj.vehicle.custom.vehicle;

import net.minecraft.world.level.Level;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;

public class FixedWingVehicleData extends BaseVehicleData<FixedWingVehicle> {

    @Override
    public AbstractVehicle fromCustom(Level level) {
       return new FixedWingVehicle(AllEntities.FIXED_WING_VEHICLE.get(), level);
    }

    public void build(FixedWingVehicleDataPojo pojo) {
        super.build(pojo);
    }

    public void inject(FixedWingVehicle vehicle) {

    }

}
