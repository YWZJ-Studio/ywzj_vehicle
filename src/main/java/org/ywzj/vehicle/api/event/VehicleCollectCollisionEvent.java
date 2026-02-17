package org.ywzj.vehicle.api.event;

import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.List;

public class VehicleCollectCollisionEvent extends VehicleEvent {

    private final List<VehicleCubeOBB.CubePoint> touchPoints;

    public VehicleCollectCollisionEvent(AbstractVehicle vehicle,  List<VehicleCubeOBB.CubePoint> touchPoints) {
        super(vehicle);
        this.touchPoints = touchPoints;
    }

    public List<VehicleCubeOBB.CubePoint> getTouchPoints() {
        return touchPoints;
    }

}
