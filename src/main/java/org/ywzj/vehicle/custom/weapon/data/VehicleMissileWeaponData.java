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

    @SerializedName("preset_cruise_altitude")
    private float presetCruiseAltitude = 512f;

    @SerializedName("preset_max_ascent_lead")
    private float presetMaxAscentLead = 64f;

    @SerializedName("preset_ascent_radius")
    private float presetAscentRadius = 24f;

    @SerializedName("preset_dive_radius")
    private float presetDiveRadius = 24f;

    @SerializedName("preset_dive_altitude_factor")
    private float presetDiveAltitudeFactor = 0.75f;

    @SerializedName("preset_dive_lead_factor")
    private float presetDiveLeadFactor = 1.5f;

    @SerializedName("preset_cruise_altitude_gain")
    private float presetCruiseAltitudeGain = 0.002f;

    @SerializedName("preset_cruise_vertical_damping")
    private float presetCruiseVerticalDamping = 0.05f;

    @SerializedName("preset_cruise_max_vertical_component")
    private float presetCruiseMaxVerticalComponent = 0.5f;

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

    public float getPresetCruiseAltitude() {
        return presetCruiseAltitude;
    }

    public float getPresetMaxAscentLead() {
        return presetMaxAscentLead;
    }

    public float getPresetAscentRadius() {
        return presetAscentRadius;
    }

    public float getPresetDiveRadius() {
        return presetDiveRadius;
    }

    public float getPresetDiveAltitudeFactor() {
        return presetDiveAltitudeFactor;
    }

    public float getPresetDiveLeadFactor() {
        return presetDiveLeadFactor;
    }

    public float getPresetCruiseAltitudeGain() {
        return presetCruiseAltitudeGain;
    }

    public float getPresetCruiseVerticalDamping() {
        return presetCruiseVerticalDamping;
    }

    public float getPresetCruiseMaxVerticalComponent() {
        return presetCruiseMaxVerticalComponent;
    }

    public enum Guidance {
        SACLOS, HOMING, PRESET
    }

    public enum HomingMode {
        INFRARED, ELECTRO_OPTICAL, SEMI_ACTIVE_RADAR, ACTIVE_RADAR
    }

}
