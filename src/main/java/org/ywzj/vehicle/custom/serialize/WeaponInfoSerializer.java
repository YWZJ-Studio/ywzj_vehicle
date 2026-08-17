package org.ywzj.vehicle.custom.serialize;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.ywzj.vehicle.vehicle.pojo.WeaponInfo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WeaponInfoSerializer implements JsonDeserializer<WeaponInfo> {

    public WeaponInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
        if (GsonHelper.isStringValue(json)) {
            String str = json.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(str);
            if (id == null) {
                throw new JsonParseException("Invalid weapon info string: " + str);
            }
            String saveId = id.toString();
            return new WeaponInfo(id, null, null, null, false, false, saveId);
        } else if (json.isJsonArray()) {
            List<ResourceLocation> ids = new ArrayList<>();
            for (JsonElement elem : json.getAsJsonArray()) {
                ResourceLocation rl = ResourceLocation.tryParse(elem.getAsString());
                if (rl != null) {
                    ids.add(rl);
                }
            }
            if (ids.isEmpty()) {
                throw new JsonParseException("Invalid weapon info array (empty): " + json);
            }
            String saveId = "multi_weapons";
            return new WeaponInfo(null, ids, null, null, false, false, saveId);
        } else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            String id = GsonHelper.getAsString(obj, "id", null);
            List<ResourceLocation> ids = null;
            if (obj.has("ids") && obj.get("ids").isJsonArray()) {
                ids = new ArrayList<>();
                for (JsonElement elem : obj.getAsJsonArray("ids")) {
                    ResourceLocation rl = ResourceLocation.tryParse(elem.getAsString());
                    if (rl != null) {
                        ids.add(rl);
                    }
                }
                if (ids.isEmpty()) {
                    ids = null;
                }
            }
            String partUnitId = GsonHelper.getAsString(obj, "part_unit_id", null);
            String weaponBayUnitId = GsonHelper.getAsString(obj, "weapon_bay_unit_id", null);
            boolean secondary = GsonHelper.getAsBoolean(obj, "secondary", false);
            boolean fullSalvo = GsonHelper.getAsBoolean(obj, "full_salvo", false);
            String saveId = GsonHelper.getAsString(obj, "save_id", partUnitId);
            return new WeaponInfo(id == null ? null : ResourceLocation.tryParse(id), ids, partUnitId, weaponBayUnitId, secondary, fullSalvo, saveId);
        } else {
            throw new JsonParseException("Invalid weapon info format: " + json);
        }
    }

}
