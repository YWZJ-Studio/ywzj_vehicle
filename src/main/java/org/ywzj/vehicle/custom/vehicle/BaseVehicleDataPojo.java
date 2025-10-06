package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class BaseVehicleDataPojo {

    @SerializedName("structure_model")
    public ResourceLocation structureModel = null;

    @SerializedName("weapon_units")
    public List<WeaponUnitPojo> weaponUnitData = List.of();

}
