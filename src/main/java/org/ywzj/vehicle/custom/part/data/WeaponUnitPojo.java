package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.custom.pojo.Bolt;
import org.ywzj.vehicle.custom.pojo.WeaponInfo;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

public class WeaponUnitPojo extends RotatableUnitPojo {
    @SerializedName("base")
    public String base = null;

    @SerializedName("bolts")
    public List<Bolt> bolts = null;

    @SerializedName("firing_mode")
    public WeaponUnit.FiringMode firingMode = WeaponUnit.FiringMode.RIPPLE;

    @SerializedName("parent_weapon_unit_aim")
    public boolean parentWeaponUnitAim = false;

    @SerializedName("optical_sight_offset")
    public Vec3 opticalSightOffset = Vec3.ZERO;

    @SerializedName("operator_view_offset")
    public Vec3 operatorViewOffset = Vec3.ZERO;

    @SerializedName("operator_on_weapon_unit")
    public boolean operatorOnWeaponUnit = true;

    @SerializedName("optical_sight_type")
    public WeaponUnit.OpticalSightType opticalSightType;

    @SerializedName("zoom_max")
    public float zoomMax;

    @SerializedName("weapons")
    public List<WeaponInfo> weapons = List.of();
}
