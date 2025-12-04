package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;

public class WheeledVehicleDataPojo extends BaseVehicleDataPojo {

    @SerializedName("attributes")
    public WheeledVehicleDataPojo.WheeledVehicleAttributes attributes;

    public static class WheeledVehicleAttributes {

        @SerializedName("brake_force")
        public float brakeForce;

        @SerializedName("forward_force")
        public float forwardForce;

        @SerializedName("backward_force")
        public float backwardForce;

        @SerializedName("max_speed_forward")
        public float maxSpeedForward;

        @SerializedName("max_speed_backward")
        public float maxSpeedBackward;

        @SerializedName("turn_step")
        public float turnStep;

        @SerializedName("max_turn")
        public float maxTurn;

    }

}
