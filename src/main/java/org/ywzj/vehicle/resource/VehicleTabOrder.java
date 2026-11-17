package org.ywzj.vehicle.resource;

import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class VehicleTabOrder {

    public static Comparator<BaseVehicleData> comparator(Function<ResourceLocation, String> tabName,
                                                         ToIntFunction<ResourceLocation> tabIndex) {
        return Comparator.comparing((BaseVehicleData data) -> normalizedTabName(tabName.apply(data.getVehicleId())))
                .thenComparingInt(data -> tabIndex.applyAsInt(data.getVehicleId())
                        + (data.isExperimental() && normalizedTabName(tabName.apply(data.getVehicleId())).isEmpty()
                        ? 1_000_000 : 0))
                .thenComparing(BaseVehicleData::getVehicleId);
    }

    private static String normalizedTabName(String name) {
        return name == null || name.isBlank() ? "" : name;
    }

}
