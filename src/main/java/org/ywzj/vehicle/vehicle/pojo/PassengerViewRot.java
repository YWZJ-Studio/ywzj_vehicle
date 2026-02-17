package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;

/**
 * 乘客视角是否随载具旋转
 */
public class PassengerViewRot {

    @SerializedName("rot_by_vehicle_in_third_person")
    public boolean rotByVehicleInThirdPerson;

    @SerializedName("rot_by_vehicle_in_operator")
    public boolean rotByVehicleInOperator;

    public PassengerViewRot() {}

    public PassengerViewRot(PassengerViewRot passengerViewRot) {
        if (passengerViewRot == null) {
            return;
        }
        this.rotByVehicleInThirdPerson = passengerViewRot.rotByVehicleInThirdPerson;
        this.rotByVehicleInOperator = passengerViewRot.rotByVehicleInOperator;
    }

}
