package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.custom.vehicle.*;

public class AllVehicleDataTypes {

    public static final DeferredRegister<VehicleDataType<?>> VEHICLE_TYPES = DeferredRegister.create(ModRegistries.VEHICLE_DATA_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<VehicleDataType<BaseVehicleData>> GENERIC_VEHICLE = register(
            "generic",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, BaseVehicleDataPojo.class);
                BaseVehicleData baseVehicleData = new BaseVehicleData();
                baseVehicleData.build(pojo);
                return baseVehicleData;
            }
    );

    public static final RegistryObject<VehicleDataType<WheeledVehicleData>> WHEELED_VEHICLE = register(
            "wheeled_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, WheeledVehicleDataPojo.class);
                WheeledVehicleData wheeledVehicleData = new WheeledVehicleData();
                wheeledVehicleData.build(pojo);
                return wheeledVehicleData;
            }
    );

    public static final RegistryObject<VehicleDataType<TrackedVehicleData>> TRACKED_VEHICLE = register(
            "tracked_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, TrackedVehicleDataPojo.class);
                TrackedVehicleData trackedVehicleData = new TrackedVehicleData();
                trackedVehicleData.build(pojo);
                return trackedVehicleData;
            }
    );

    public static final RegistryObject<VehicleDataType<RotaryWingVehicleData>> ROTARY_WING_VEHICLE = register(
            "rotary_wing_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, RotaryWingVehicleDataPojo.class);
                RotaryWingVehicleData rotaryWingVehicleData = new RotaryWingVehicleData();
                rotaryWingVehicleData.build(pojo);
                return rotaryWingVehicleData;
            }
    );

    public static final RegistryObject<VehicleDataType<FixedWingVehicleData>> FIXED_WING_VEHICLE = register(
            "fixed_wing_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, FixedWingVehicleDataPojo.class);
                FixedWingVehicleData fixedWingVehicleData = new FixedWingVehicleData();
                fixedWingVehicleData.build(pojo);
                return fixedWingVehicleData;
            }
    );

    private static <D extends BaseVehicleData> RegistryObject<VehicleDataType<D>> register(
            String name,
            VehicleDataType.DataSerializer<D> dataSerializer
    ) {
        return VEHICLE_TYPES.register(name,
                () -> VehicleDataType.Builder.<D>of(YwzjVehicle.modLocation(name))
                        .setDataSerializer(dataSerializer)
                        .build()
        );
    }

    public static void register(IEventBus eventBus) {
        VEHICLE_TYPES.register(eventBus);
    }

}
