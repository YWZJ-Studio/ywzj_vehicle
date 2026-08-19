package org.ywzj.vehicle.api.event;

import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.List;

/**
 * Fired after a vehicle's contact points have been sampled, before the impact solve consumes
 * them. Listeners may inspect or mutate the list.
 */
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
