package org.ywzj.vehicle.vehicle.scripts;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;

public class RotaryWingVehicleScriptContext extends VehicleScriptContext<RotaryWingVehicle> {

    public RotaryWingVehicleScriptContext(RotaryWingVehicle vehicle, BedrockModel model) {
        super(vehicle, model);
    }

    public float getCollectivePitch() {
        return entity.getCollectivePitch();
    }

}
