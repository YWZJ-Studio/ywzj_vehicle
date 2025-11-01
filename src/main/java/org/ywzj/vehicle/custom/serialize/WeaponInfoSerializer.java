package org.ywzj.vehicle.custom.serialize;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.ywzj.vehicle.custom.pojo.WeaponInfo;

import java.lang.reflect.Type;

public class WeaponInfoSerializer implements JsonDeserializer<WeaponInfo> {
    public WeaponInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
        if (GsonHelper.isStringValue(json)) {
            String str = json.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(str);

            if (id == null) {
                throw new JsonParseException("Invalid weapon info string: " + str);
            }

            return new WeaponInfo(id, null);
        } else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "id"));
            String partUnit = GsonHelper.getAsString(obj, "part_unit", null);

            if (id == null) {
                throw new JsonParseException("Invalid weapon info id in object: " + obj);
            }

            return new WeaponInfo(id, partUnit);
        } else {
            throw new JsonParseException("Invalid weapon info format: " + json);
        }

    }
}