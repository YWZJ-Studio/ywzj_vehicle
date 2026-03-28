package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class FixedWingVehicleDataPojo extends BaseVehicleDataPojo {

    @SerializedName("attributes")
    public FixedWingAttributes attributes;

    @SerializedName("landing_gear_part_id")
    public String landingGearPartId;

    public static class FixedWingAttributes {

        @SerializedName("thrust")
        public float thrust = 0.02f;

        @SerializedName("thrust_k")
        public float thrustK = 1.5f;

        @SerializedName("ceiling")
        public float ceiling = 512;

        @SerializedName("x_rot_input_step")
        public float xRotInputStep = 0.2f;

        @SerializedName("y_rot_input_step")
        public float yRotInputStep = 0.5f;

        @SerializedName("z_rot_input_step")
        public float zRotInputStep = 0.2f;

        @SerializedName("air_drag_k_min")
        public float airDragKMin = 1f / 500;

        @SerializedName("air_drag_k_max")
        public float airDragKMax = 4f / 500;

        @SerializedName("lift_to_drag_k")
        public float liftToDragK = 6;

        @SerializedName("angle_of_attack_min")
        public float angleOfAttackMin = -10f;

        @SerializedName("angle_of_attack_max")
        public float angleOfAttackMax = 25f;

        @SerializedName("x_rot_input_drag_k")
        public float xRotInputDragK = 1f;

        @SerializedName("y_rot_input_drag_k")
        public float yRotInputDragK = 1f / 4;

        @SerializedName("z_rot_input_drag_k")
        public float zRotInputDragK = 1f / 8;

        @SerializedName("landing_gear_drag_k")
        public float landingGearDragK = 1f / 2;

        @SerializedName("turn_rate_by_speed")
        public float turnRateBySpeed = 1f / 2.5f;

        @SerializedName("x_turn_rate")
        public float xTurnRate = 2;

        @SerializedName("y_turn_rate")
        public float yTurnRate = 3;

        @SerializedName("z_turn_rate")
        public float zTurnRate = 8;

        @SerializedName("vortex_offsets")
        public List<Vec3> vortexOffsets = new ArrayList<>();

    }

}
