package org.ywzj.vehicle.vehicle.parts;

import org.jetbrains.annotations.UnmodifiableView;
import org.ywzj.vehicle.custom.part.data.DoorUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.Map;

public class DoorUnit extends SwitchableUnit<DoorUnitData> {

    private PartUnit<?> seatUnitOfDoor;

    public DoorUnit(int index, AbstractVehicle vehicle, DoorUnitData data) {
        super(index, vehicle, data);
    }

    public void combineAndInit(@UnmodifiableView Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        this.seatUnitOfDoor = partUnitsView.get(data.getDoorForSeatId());
    }

    public PartUnit<?> getSeatUnitOfDoor() {
        return seatUnitOfDoor;
    }

}
