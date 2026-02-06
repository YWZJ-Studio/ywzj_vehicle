package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;

public class RotaryWingVehicleDataPojo extends BaseVehicleDataPojo {

    @SerializedName("attributes")
    public RotaryWingAttributes attributes;

    public static class RotaryWingAttributes {

        @SerializedName("main_rotor_force")
        public float mainRotorForce;

        @SerializedName("x_rot_speed_acceleration")
        public float xRotSpeedAcceleration;

        @SerializedName("x_rot_speed_max")
        public float xRotSpeedMax;

        @SerializedName("y_rot_speed_acceleration")
        public float yRotSpeedAcceleration;

        @SerializedName("y_rot_speed_max")
        public float yRotSpeedMax;

        @SerializedName("z_rot_speed_acceleration")
        public float zRotSpeedAcceleration;

        @SerializedName("z_rot_speed_max")
        public float zRotSpeedMax;

        @SerializedName("max_air_speed")
        public float maxAirSpeed;

        @SerializedName("fast_roping")
        public boolean fastRoping;

        @SerializedName("fast_roping_door_id")
        public String fastRopingDoorId;
    }

}
