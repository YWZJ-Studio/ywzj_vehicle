package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleDataPojo;
import org.ywzj.vehicle.custom.vehicle.VehicleDataType;

public class AllVehicleDataTypes {
    public static final DeferredRegister<VehicleDataType<?>> VEHICLE_TYPES = DeferredRegister.create(ModRegistries.VEHICLE_DATA_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<VehicleDataType<BaseVehicleData>> GENERIC_VEHICLE = register(
            "generic",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, BaseVehicleDataPojo.class);
                return BaseVehicleData.of(pojo);
            }
    );

    private static <T extends VehicleDataType<D>, D extends BaseVehicleData> RegistryObject<VehicleDataType<D>> register(
            String name,
            VehicleDataType.DataSerializer<D> dataSerializer
    ) {
        return VEHICLE_TYPES.register(name,
                () -> VehicleDataType.Builder.<D>of(YwzjVehicle.modLoc(name))
                        .setDataSerializer(dataSerializer)
                        .build()
        );
    }

    public static void register(IEventBus eventBus) {
        VEHICLE_TYPES.register(eventBus);
    }

}
