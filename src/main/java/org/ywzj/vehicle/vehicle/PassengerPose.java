package org.ywzj.vehicle.vehicle;

public class PassengerPose {

    public Float leftArmX;
    public Float leftArmY;
    public Float leftArmZ;
    public Float leftArmRotX;
    public Float leftArmRotY;
    public Float leftArmRotZ;
    public Float rightArmX;
    public Float rightArmY;
    public Float rightArmZ;
    public Float rightArmRotX;
    public Float rightArmRotY;
    public Float rightArmRotZ;

    public PassengerPose() {};

    public PassengerPose(Float leftArmX, Float leftArmY, Float leftArmZ, Float leftArmRotX, Float leftArmRotY, Float leftArmRotZ, Float rightArmX, Float rightArmY, Float rightArmZ, Float rightArmRotX, Float rightArmRotY, Float rightArmRotZ) {
        this.leftArmX = leftArmX;
        this.leftArmY = leftArmY;
        this.leftArmZ = leftArmZ;
        this.leftArmRotX = leftArmRotX;
        this.leftArmRotY = leftArmRotY;
        this.leftArmRotZ = leftArmRotZ;
        this.rightArmX = rightArmX;
        this.rightArmY = rightArmY;
        this.rightArmZ = rightArmZ;
        this.rightArmRotX = rightArmRotX;
        this.rightArmRotY = rightArmRotY;
        this.rightArmRotZ = rightArmRotZ;
    }

}
