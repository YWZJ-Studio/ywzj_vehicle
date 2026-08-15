package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.render.animation.context.AnimationContextFactory;
import org.ywzj.vehicle.client.resource.vehicle.*;
import org.ywzj.vehicle.custom.serialize.GsonUtil;

public class AllDisplayTypes {

    public static final DeferredRegister<VehicleDisplayType<?>> VEHICLE_DISPLAY_TYPES = DeferredRegister.create(ModRegistries.VEHICLE_DISPLAY_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<VehicleDisplayType<SimpleVehicleDisplay>> GENERIC_VEHICLE = register(
            "generic",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, VehicleDisplayPojo.class);
                return new SimpleVehicleDisplay(pojo);
            },
            AnimationContextFactory.vehicle()
    );

    public static final RegistryObject<VehicleDisplayType<BaseDisplay>> WEAPON = register(
            "weapon",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, BaseDisplayPojo.class);
                return new BaseDisplay(pojo);
            },
            null
    );

    public static final RegistryObject<VehicleDisplayType<WheeledVehicleDisplay>> WHEELED_VEHICLE = register(
            "wheeled_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, VehicleDisplayPojo.class);
                return new WheeledVehicleDisplay(pojo);
            },
            AnimationContextFactory.wheeledVehicle()
    );

    public static final RegistryObject<VehicleDisplayType<TrackedVehicleDisplay>> TRACKED_VEHICLE = register(
            "tracked_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, TrackedVehicleDisplayPojo.class);
                return new TrackedVehicleDisplay(pojo);
            },
            AnimationContextFactory.trackedVehicle()
    );

    public static final RegistryObject<VehicleDisplayType<RotaryWingVehicleDisplay>> ROTARY_WING_VEHICLE = register(
            "rotary_wing_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, VehicleDisplayPojo.class);
                return new RotaryWingVehicleDisplay(pojo);
            },
            AnimationContextFactory.rotaryWingVehicle()
    );

    public static final RegistryObject<VehicleDisplayType<FixedWingVehicleDisplay>> FIXED_WING_VEHICLE = register(
            "fixed_wing_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, FixedWingVehicleDisplayPojo.class);
                return new FixedWingVehicleDisplay(pojo);
            },
            AnimationContextFactory.fixedWingVehicle()
    );

    private static <D extends BaseDisplay> RegistryObject<VehicleDisplayType<D>> register(
            String name,
            VehicleDisplayType.DataSerializer<D> dataSerializer,
            AnimationContextFactory<?, ?> contextFactory
    ) {
        return VEHICLE_DISPLAY_TYPES.register(name,
                () -> VehicleDisplayType.Builder.<D>of(YwzjVehicle.modLocation(name))
                        .setDataSerializer(dataSerializer)
                        .setContextFactory(contextFactory)
                        .build()
        );
    }

    public static void register(IEventBus eventBus) {
        VEHICLE_DISPLAY_TYPES.register(eventBus);
    }

}
