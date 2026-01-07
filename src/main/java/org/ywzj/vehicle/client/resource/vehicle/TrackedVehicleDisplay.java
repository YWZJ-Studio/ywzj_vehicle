package org.ywzj.vehicle.client.resource.vehicle;

import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;
import org.ywzj.vehicle.vehicle.scripts.TrackedVehicleScriptContext;
import org.ywzj.vehicle.vehicle.scripts.VehicleScriptContext;

public class TrackedVehicleDisplay extends BaseVehicleDisplay {

    public TrackedVehicleDisplay(BaseVehicleDisplayPojo pojo) {
        super(pojo);
    }

    @Override
    public VehicleScriptContext<TrackedVehicle> buildVehicleScriptContext() {
        return new TrackedVehicleScriptContext(null, model);
    }

}
