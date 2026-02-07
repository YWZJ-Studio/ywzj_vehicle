package org.ywzj.vehicle.client.render.animation.context;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.Map;

public class VehicleContext extends EntityContext<AbstractVehicle> {

    public VehicleContext(AbstractVehicle entity, Map<String, BedrockAnimation> animations) {
        super(entity, animations);
    }

}
