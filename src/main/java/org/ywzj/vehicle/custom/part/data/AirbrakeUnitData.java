package org.ywzj.vehicle.custom.part.data;

public class AirbrakeUnitData extends PartUnitData {

    protected float dragK;

    public AirbrakeUnitData(String id) {
        super(id);
    }

    public AirbrakeUnitData(AirbrakeUnitPojo pojo) {
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
