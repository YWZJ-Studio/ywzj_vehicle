package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.pojo.Bolt;
import org.ywzj.vehicle.vehicle.pojo.WeaponInfo;

import java.util.List;

public class WeaponUnitPojo extends RotatableUnitPojo {

    @SerializedName("bolts")
    public List<Bolt> bolts = null;

    @SerializedName("ammo_capacity")
    public int ammoCapacity = -1;

    @SerializedName("firing_mode")
    public WeaponUnitData.FiringMode firingMode = WeaponUnitData.FiringMode.RIPPLE;

    @SerializedName("parent_weapon_unit_aim")
    public boolean parentWeaponUnitAim = false;

    @SerializedName("optical_sight_offset")
    public Vec3 opticalSightOffset = Vec3.ZERO;

    @SerializedName("operator_view_offset")
    public Vec3 operatorViewOffset = Vec3.ZERO;

    @SerializedName("operator_on_weapon_unit")
    public boolean operatorOnWeaponUnit = true;

    @SerializedName("fire_control_sensor_type")
    public WeaponUnitData.FireControlSensorType fireControlSensorType = WeaponUnitData.FireControlSensorType.NONE;

    @SerializedName("optical_sight_type")
    public WeaponUnitData.OpticalSightType opticalSightType;

    @SerializedName("with_stabilizer")
    public boolean withStabilizer = false;

    @SerializedName("zoom_min")
    public float zoomMin = 1;

    @SerializedName("zoom_max")
    public float zoomMax = 8;

    @SerializedName("crosshair_style")
    public WeaponUnitData.CrosshairStyle crosshairStyle = WeaponUnitData.CrosshairStyle.CIRCLE;

    @SerializedName("weapons")
    public List<WeaponInfo> weapons = List.of();

}
