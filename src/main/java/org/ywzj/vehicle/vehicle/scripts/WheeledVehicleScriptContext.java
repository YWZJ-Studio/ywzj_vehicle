package org.ywzj.vehicle.vehicle.scripts;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;

public class WheeledVehicleScriptContext extends VehicleScriptContext<WheeledVehicle> {

    public WheeledVehicleScriptContext(WheeledVehicle vehicle, BedrockModel model) {
        super(vehicle, model);
    }

    public float getForwardSpeed() {
        return entity.getForwardSpeed();
    }

    public float getTurnAngle() {
        return entity.getTurnAngle();
    }

}
