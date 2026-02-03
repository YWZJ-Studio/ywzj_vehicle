package org.ywzj.vehicle.custom.part.data;

public class DoorUnitData extends PartUnitData {

    protected String doorForSeatId;

    public DoorUnitData(String id) {
        super(id);
    }

    public DoorUnitData(DoorUnitPojo pojo) {
        super(pojo);
        this.doorForSeatId = pojo.doorForSeatId;
    }

    public String getDoorForSeatId() {
        return doorForSeatId;
    }

    public void setDoorForSeatId(String doorForSeatId) {
        this.doorForSeatId = doorForSeatId;
    }

}
