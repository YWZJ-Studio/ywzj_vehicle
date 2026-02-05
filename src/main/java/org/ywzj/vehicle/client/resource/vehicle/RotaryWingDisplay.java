package org.ywzj.vehicle.client.resource.vehicle;

import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.vehicle.scripts.RotaryWingVehicleScriptContext;
import org.ywzj.vehicle.vehicle.scripts.VehicleScriptContext;

public class RotaryWingDisplay extends BaseDisplay {

    public RotaryWingDisplay(BaseDisplayPojo pojo) {
        super(pojo);
    }

    @Override
    public VehicleScriptContext<RotaryWingVehicle> buildVehicleScriptContext() {
        return new RotaryWingVehicleScriptContext(null, model);
    }

}
