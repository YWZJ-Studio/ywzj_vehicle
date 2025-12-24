package org.ywzj.vehicle.custom.part.data;

import org.ywzj.vehicle.vehicle.pojo.RotInfo;

public class RotatableUnitData extends PartUnitData {

    protected String base;
    protected RotInfo rotInfo;

    public RotatableUnitData(String id) {
        super(id);
    }

    public RotatableUnitData(RotatableUnitPojo pojo) {
        super(pojo);
        this.base = pojo.base;
        this.rotInfo = pojo.rotInfo;
    }

    public String getBase() {
        return base;
    }

    public RotInfo getRotInfo() {
        return rotInfo;
    }

}
