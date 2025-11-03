package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.custom.part.PartUnitEntry;

import java.util.List;

public class BaseVehicleDataPojo {

    @SerializedName("assets")
    public Assets assets;

    public static class Assets {

        @SerializedName("models")
        public Models models = null;

        @SerializedName("sounds")
        public Sounds sounds = null;

    }

    public static class Models {

        @SerializedName("visual_model")
        public ResourceLocation visualModel = null;

        @SerializedName("visual_texture")
        public ResourceLocation visualTexture = null;

        @SerializedName("structure_model")
        public ResourceLocation structureModel = null;

    }

    public static class Sounds {

        @SerializedName("engine_start")
        public ResourceLocation engineStart = null;

        @SerializedName("engine_stop")
        public ResourceLocation engineStop = null;

        @SerializedName("engine_idle")
        public ResourceLocation engineIdle = null;

        @SerializedName("engine_run")
        public ResourceLocation engineRun = null;

    }

    @SerializedName("parts")
    public List<PartUnitEntry<?, ?>> parts = List.of();

}
