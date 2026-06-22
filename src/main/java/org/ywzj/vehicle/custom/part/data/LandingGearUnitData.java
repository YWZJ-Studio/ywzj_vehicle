package org.ywzj.vehicle.custom.part.data;

public class LandingGearUnitData extends PartUnitData {

    protected float dragK;

    public LandingGearUnitData(String id) {
        super(id);
    }

    public LandingGearUnitData(LandingGearUnitPojo pojo) {
        super(pojo);
        this.dragK = pojo.dragK;
    }

    public float getDragK() {
        return dragK;
    }

    public void setDragK(float dragK) {
        this.dragK = dragK;
    }

}
