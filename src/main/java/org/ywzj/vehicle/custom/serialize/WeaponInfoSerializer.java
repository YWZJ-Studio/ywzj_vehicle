package org.ywzj.vehicle.custom.serialize;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.ywzj.vehicle.vehicle.pojo.WeaponInfo;

import java.lang.reflect.Type;

public class WeaponInfoSerializer implements JsonDeserializer<WeaponInfo> {

    public WeaponInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
        if (GsonHelper.isStringValue(json)) {
            String str = json.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(str);
            if (id == null) {
                throw new JsonParseException("Invalid weapon info string: " + str);
            }
            String saveId = id.toString();
            return new WeaponInfo(id, null, null, false, saveId);
        } else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            String id = GsonHelper.getAsString(obj, "id", null);
            String partUnitId = GsonHelper.getAsString(obj, "part_unit_id", null);
            String weaponBayUnitId = GsonHelper.getAsString(obj, "weapon_bay_unit_id", null);
            boolean secondary = GsonHelper.getAsBoolean(obj, "secondary", false);
            String saveId = GsonHelper.getAsString(obj, "save_id", partUnitId);
            return new WeaponInfo(id == null ? null : ResourceLocation.tryParse(id), partUnitId, weaponBayUnitId, secondary, saveId);
        } else {
            throw new JsonParseException("Invalid weapon info format: " + json);
        }
    }

}
