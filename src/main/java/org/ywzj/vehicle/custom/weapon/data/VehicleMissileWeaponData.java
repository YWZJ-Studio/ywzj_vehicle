package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.pojo.Explosion;

public class VehicleMissileWeaponData extends BaseVehicleWeaponData {

    @SerializedName("x_rot_max")
    private float xRotMax = 10f;

    @SerializedName("x_rot_min")
    private float xRotMin = -10f;

    @SerializedName("y_rot_max")
    private float yRotMax = 10f;

    @SerializedName("y_rot_min")
    private float yRotMin = -10f;

    @SerializedName("seeker_fov")
    private float seekerFov = 30f;

    @SerializedName("mass")
    private float mass = 0.01f;

    @SerializedName("thrust")
    private float thrust = 0.01f;

    @SerializedName("motor_burn_time")
    private float motorBurnTime = 300f;

    @SerializedName("engine_nozzle_offset")
    private Vec3 engineNozzleOffset = new Vec3(0, 0, -2);

    @SerializedName("drag_coefficient")
    private float dragCoefficient = 0.005f;

    @SerializedName("max_g")
    private float maxG = 30f;

    @SerializedName("reference_speed")
    private float referenceSpeed = 3f;

    @SerializedName("explosion")
    private Explosion explosion = new Explosion();

    @SerializedName("guidance")
    private Guidance guidance = Guidance.SACLOS;

    @SerializedName("homing_mode")
    private HomingMode homingMode = HomingMode.INFRARED;

    public float getXRotMax() {
        return xRotMax;
    }

    public float getXRotMin() {
        return xRotMin;
    }

    public float getYRotMax() {
        return yRotMax;
    }

    public float getYRotMin() {
        return yRotMin;
    }

    public float getSeekerFov() {
        return seekerFov;
    }

    public float getMass() {
        return mass;
    }

    public float getThrust() {
        return thrust;
    }

    public float getMotorBurnTime() {
        return motorBurnTime;
    }

    public Vec3 getEngineNozzleOffset() {
        return engineNozzleOffset;
    }

    public float getDragCoefficient() {
        return dragCoefficient;
    }

    public float getMaxG() {
        return maxG;
    }

    public float getReferenceSpeed() {
        return referenceSpeed;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    public Guidance getGuidance() {
        return guidance;
    }

    public HomingMode getHomingMode() {
        return homingMode;
    }

    public enum Guidance {
        SACLOS, HOMING, PRESET
    }

    public enum HomingMode {
        INFRARED, ELECTRO_OPTICAL, SEMI_ACTIVE_RADAR, ACTIVE_RADAR
    }

}
