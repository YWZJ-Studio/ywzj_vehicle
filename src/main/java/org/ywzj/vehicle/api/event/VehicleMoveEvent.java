package org.ywzj.vehicle.api.event;

import net.minecraftforge.eventbus.api.Cancelable;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Cancelable
public class VehicleMoveEvent extends VehicleEvent {

    public VehicleMoveEvent(AbstractVehicle vehicle) {
        super(vehicle);
    }

}
