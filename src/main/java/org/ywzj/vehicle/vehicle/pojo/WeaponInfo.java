package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class WeaponInfo {

    /**
     * 武器id
     */
    @Nullable
    @SerializedName("id")
    public ResourceLocation id;

    /**
     * 子武器站的载具部件ID
     */
    @Nullable
    @SerializedName("part_unit_id")
    public String partUnitId;

    /**
     * 弹仓的载具部件ID
     */
    @Nullable
    @SerializedName("weapon_bay_unit_id")
    public String weaponBayUnitId;

    @SerializedName("secondary")
    public boolean secondary;

    @SerializedName("save_id")
    public String saveId;

    public WeaponInfo(@Nullable ResourceLocation id, @Nullable String partUnitId, @Nullable String weaponBayUnitId, boolean secondary, String saveId) {
        this.id = id;
        this.partUnitId = partUnitId;
        this.weaponBayUnitId = weaponBayUnitId;
        this.secondary = secondary;
        this.saveId = saveId;
    }

}
