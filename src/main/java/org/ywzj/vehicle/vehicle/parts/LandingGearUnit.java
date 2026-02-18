package org.ywzj.vehicle.vehicle.parts;

import org.jetbrains.annotations.UnmodifiableView;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.Map;
import java.util.function.BiConsumer;

public class LandingGearUnit extends SwitchableUnit<PartUnitData> {

    private BiConsumer<LandingGearUnit, Boolean> onStateChange;
    private double maxHeight;

    public LandingGearUnit(int index, AbstractVehicle vehicle, PartUnitData data) {
        super(index, vehicle, data);
    }

    @Override
    public void combineAndInit(@UnmodifiableView Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        super.combineAndInit(partUnitsView, vehicle);
        this.maxHeight = this.getOBBs().stream()
                .mapToDouble(obb -> obb.extents().y * 2)
                .max()
                .orElse(0);
    }

    @Override
    public void setOn(boolean on) {
        super.setOn(on);
        if (onStateChange != null) {
            onStateChange.accept(this, on);
        }
    }

    public void setOnStateChange(BiConsumer<LandingGearUnit, Boolean> onStateChange) {
        this.onStateChange = onStateChange;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    @Override
    public boolean defaultOpen() {
        return true;
    }
}
