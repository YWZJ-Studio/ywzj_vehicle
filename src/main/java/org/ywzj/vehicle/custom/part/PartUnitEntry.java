package org.ywzj.vehicle.custom.part;

import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.PartUnit;

public record PartUnitEntry<T extends PartUnit<D>, D extends PartUnitData>(
        PartUnitType<T, D> type,
        D data
) {
    public T create(int index, AbstractVehicle vehicle) {
        return type.factory().create(index, vehicle, data);
    }
}
