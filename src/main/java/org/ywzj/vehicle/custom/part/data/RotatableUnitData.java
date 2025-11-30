package org.ywzj.vehicle.custom.part.data;

import org.ywzj.vehicle.custom.pojo.RotInfo;

public class RotatableUnitData extends PartUnitData {

    protected RotInfo rotInfo;

    public RotatableUnitData(String id) {
        super(id);
    }

    public RotatableUnitData(RotatableUnitPojo pojo) {
        super(pojo);
        this.rotInfo = pojo.rotInfo;
    }

    public RotInfo getRotInfo() {
        return rotInfo;
    }

}
