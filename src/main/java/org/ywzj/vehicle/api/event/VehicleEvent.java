package org.ywzj.vehicle.api.event;

import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public abstract class VehicleEvent extends EntityEvent implements ICancellableEvent {

    private final AbstractVehicle vehicle;

    public VehicleEvent(AbstractVehicle vehicle) {
        super(vehicle);
        this.vehicle = vehicle;
    }

    public AbstractVehicle getVehicle() {
        return vehicle;
    }

}
