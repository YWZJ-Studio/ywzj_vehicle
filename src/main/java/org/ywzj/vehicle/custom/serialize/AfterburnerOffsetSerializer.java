package org.ywzj.vehicle.custom.serialize;

import com.google.gson.*;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.part.AfterburnerOffset;

import java.lang.reflect.Type;

public class AfterburnerOffsetSerializer implements JsonDeserializer<AfterburnerOffset> {

    public AfterburnerOffset deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray array = json.getAsJsonArray();
        double x = GsonHelper.convertToDouble(array.get(0), "(array i=0)");
        double y = GsonHelper.convertToDouble(array.get(1), "(array i=1)");
        double z = GsonHelper.convertToDouble(array.get(2), "(array i=2)");
        float scale = array.size() > 3 ? GsonHelper.convertToFloat(array.get(3), "(array i=3)") : 1.0f;
        return new AfterburnerOffset(new Vec3(x, y, z), scale);
    }

}
