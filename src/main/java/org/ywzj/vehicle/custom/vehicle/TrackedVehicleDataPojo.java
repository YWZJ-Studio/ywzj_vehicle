package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;

public class TrackedVehicleDataPojo extends BaseVehicleDataPojo {

    @SerializedName("attributes")
    public TrackedVehicleAttributes attributes;

    public static class TrackedVehicleAttributes {

        @SerializedName("brake_acceleration")
        public float brakeAcceleration;

        @SerializedName("forward_acceleration")
        public float forwardAcceleration;

        @SerializedName("backward_acceleration")
        public float backwardAcceleration;

        @SerializedName("max_speed_forward")
        public float maxSpeedForward;

        @SerializedName("max_speed_backward")
        public float maxSpeedBackward;

        @SerializedName("turn_acceleration")
        public float turnAcceleration;

        @SerializedName("max_turn")
        public float maxTurn;

    }

}
