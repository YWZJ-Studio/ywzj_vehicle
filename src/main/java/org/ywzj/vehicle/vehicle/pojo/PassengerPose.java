package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;

/**
 * 乘客姿势
 */
public class PassengerPose {

    @SerializedName("left_arm_x")
    public Float leftArmX;

    @SerializedName("left_arm_y")
    public Float leftArmY;

    @SerializedName("left_arm_z")
    public Float leftArmZ;

    @SerializedName("left_arm_rot_x")
    public Float leftArmRotX;

    @SerializedName("left_arm_rot_y")
    public Float leftArmRotY;

    @SerializedName("left_arm_rot_z")
    public Float leftArmRotZ;

    @SerializedName("right_arm_x")
    public Float rightArmX;

    @SerializedName("right_arm_y")
    public Float rightArmY;

    @SerializedName("right_arm_z")
    public Float rightArmZ;

    @SerializedName("right_arm_rot_x")
    public Float rightArmRotX;

    @SerializedName("right_arm_rot_y")
    public Float rightArmRotY;

    @SerializedName("right_arm_rot_z")
    public Float rightArmRotZ;

    public PassengerPose() {}

    public PassengerPose(PassengerPose passengerPose) {
        if (passengerPose == null) {
            return;
        }
        this.leftArmX = passengerPose.leftArmX;
        this.leftArmY = passengerPose.leftArmY;
        this.leftArmZ = passengerPose.leftArmZ;
        this.leftArmRotX = passengerPose.leftArmRotX;
        this.leftArmRotY = passengerPose.leftArmRotY;
        this.leftArmRotZ = passengerPose.leftArmRotZ;
        this.rightArmX = passengerPose.rightArmX;
        this.rightArmY = passengerPose.rightArmY;
        this.rightArmZ = passengerPose.rightArmZ;
        this.rightArmRotX = passengerPose.rightArmRotX;
        this.rightArmRotY = passengerPose.rightArmRotY;
        this.rightArmRotZ = passengerPose.rightArmRotZ;
    }

}
