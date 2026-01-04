package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.custom.part.PartUnitEntry;
import org.ywzj.vehicle.vehicle.pojo.EnergyInfo;
import org.ywzj.vehicle.vehicle.pojo.PhysicsInfo;
import org.ywzj.vehicle.vehicle.pojo.ViewInfo;

import java.util.List;

public class BaseVehicleDataPojo {

    @SerializedName("max_health")
    public float maxHealth = 100f;

    @SerializedName("view_info")
    public ViewInfo viewInfo = new ViewInfo();

    @SerializedName("energy_info")
    public EnergyInfo energyInfo = new EnergyInfo();

    @SerializedName("physics_info")
    public PhysicsInfo physicsInfo = new PhysicsInfo();

    @SerializedName("with_warning_receiver")
    public boolean withWarningReceiver = false;

    @SerializedName("protect_passenger")
    public boolean protectPassenger = true;

    @SerializedName("structure_model")
    public ResourceLocation structureModel = null;

    @SerializedName("parts")
    public List<PartUnitEntry<?, ?>> parts = List.of();

}
