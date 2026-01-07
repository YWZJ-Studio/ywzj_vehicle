package org.ywzj.vehicle.client.resource.vehicle;

import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.vehicle.scripts.VehicleScriptContext;
import org.ywzj.vehicle.vehicle.scripts.WheeledVehicleScriptContext;

public class WheeledVehicleDisplay extends BaseVehicleDisplay {

    public WheeledVehicleDisplay(BaseVehicleDisplayPojo pojo) {
        super(pojo);
    }

    @Override
    public VehicleScriptContext<WheeledVehicle> buildVehicleScriptContext() {
        return new WheeledVehicleScriptContext(null, model);
    }

}
