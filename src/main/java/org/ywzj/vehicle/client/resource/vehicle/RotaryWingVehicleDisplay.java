package org.ywzj.vehicle.client.resource.vehicle;

import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.vehicle.scripts.RotaryWingVehicleScriptContext;
import org.ywzj.vehicle.vehicle.scripts.VehicleScriptContext;

public class RotaryWingVehicleDisplay extends BaseVehicleDisplay {

    public RotaryWingVehicleDisplay(BaseVehicleDisplayPojo pojo) {
        super(pojo);
    }

    @Override
    public VehicleScriptContext<RotaryWingVehicle> buildVehicleScriptContext() {
        return new RotaryWingVehicleScriptContext(null, model);
    }

}
