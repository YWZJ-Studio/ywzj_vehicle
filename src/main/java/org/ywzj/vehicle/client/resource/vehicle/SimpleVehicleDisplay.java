package org.ywzj.vehicle.client.resource.vehicle;

import org.ywzj.vehicle.client.render.animation.context.VehicleContext;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

// 拿来规避神秘泛型检查的，白板display
public class SimpleVehicleDisplay extends VehicleDisplay<AbstractVehicle, VehicleContext<AbstractVehicle>> {

    public SimpleVehicleDisplay(VehicleDisplayPojo pojo) {
        super(pojo);
    }

}
