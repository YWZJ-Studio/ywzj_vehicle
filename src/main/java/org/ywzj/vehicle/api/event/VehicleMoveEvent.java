package org.ywzj.vehicle.api.event;

import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class VehicleMoveEvent extends VehicleEvent {

    public VehicleMoveEvent(AbstractVehicle vehicle) {
        super(vehicle);
    }

}
