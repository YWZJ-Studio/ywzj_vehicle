package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class VehicleThrowerWeaponData extends BaseVehicleWeaponData {

    @SerializedName("entity_id")
    private ResourceLocation entityId;

    @SerializedName("muzzle_particle")
    private ResourceLocation muzzleParticle;

    public ResourceLocation getEntityId() {
        return entityId;
    }

    public ResourceLocation getMuzzleParticle() {
        return muzzleParticle;
    }

}
