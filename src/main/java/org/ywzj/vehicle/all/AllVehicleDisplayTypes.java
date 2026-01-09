package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.vehicle.*;
import org.ywzj.vehicle.custom.serialize.GsonUtil;

public class AllVehicleDisplayTypes {

    public static final DeferredRegister<VehicleDisplayType<?>> VEHICLE_DISPLAY_TYPES = DeferredRegister.create(ModRegistries.VEHICLE_DISPLAY_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<VehicleDisplayType<BaseVehicleDisplay>> GENERIC_VEHICLE = register(
            "generic",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, BaseVehicleDisplayPojo.class);
                return new BaseVehicleDisplay(pojo);
            }
    );

    public static final RegistryObject<VehicleDisplayType<WheeledVehicleDisplay>> WHEELED_VEHICLE = register(
            "wheeled_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, BaseVehicleDisplayPojo.class);
                return new WheeledVehicleDisplay(pojo);
            }
    );

    public static final RegistryObject<VehicleDisplayType<TrackedVehicleDisplay>> TRACKED_VEHICLE = register(
            "tracked_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, BaseVehicleDisplayPojo.class);
                return new TrackedVehicleDisplay(pojo);
            }
    );

    public static final RegistryObject<VehicleDisplayType<RotaryWingVehicleDisplay>> ROTARY_WING_VEHICLE = register(
            "rotary_wing_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, BaseVehicleDisplayPojo.class);
                return new RotaryWingVehicleDisplay(pojo);
            }
    );

    private static <D extends BaseVehicleDisplay> RegistryObject<VehicleDisplayType<D>> register(
            String name,
            VehicleDisplayType.DataSerializer<D> dataSerializer
    ) {
        return VEHICLE_DISPLAY_TYPES.register(name,
                () -> VehicleDisplayType.Builder.<D>of(YwzjVehicle.modLocation(name))
                        .setDataSerializer(dataSerializer)
                        .build()
        );
    }

    public static void register(IEventBus eventBus) {
        VEHICLE_DISPLAY_TYPES.register(eventBus);
    }

}
