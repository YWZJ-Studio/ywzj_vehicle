package org.ywzj.vehicle.api.event;

import net.minecraftforge.event.entity.EntityEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class VehicleEvent extends EntityEvent {

    private final AbstractVehicle vehicle;

    public VehicleEvent(AbstractVehicle vehicle) {
        super(vehicle);
        this.vehicle = vehicle;
    }

    public AbstractVehicle getVehicle() {
        return vehicle;
    }

}
