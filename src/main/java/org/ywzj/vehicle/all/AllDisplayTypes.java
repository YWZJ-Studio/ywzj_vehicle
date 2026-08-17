package org.ywzj.vehicle.all;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.render.animation.context.AnimationContextFactory;
import org.ywzj.vehicle.client.resource.vehicle.*;
import org.ywzj.vehicle.custom.serialize.GsonUtil;

public class AllDisplayTypes {

    public static final DeferredRegister<VehicleDisplayType<?>> VEHICLE_DISPLAY_TYPES = DeferredRegister.create(ModRegistries.VEHICLE_DISPLAY_TYPE_KEY, YwzjVehicle.MOD_ID);

    public static final DeferredHolder<VehicleDisplayType<?>, VehicleDisplayType<SimpleVehicleDisplay>> GENERIC_VEHICLE = register(
            "generic",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, VehicleDisplayPojo.class);
                return new SimpleVehicleDisplay(pojo);
            },
            AnimationContextFactory.vehicle()
    );

    public static final DeferredHolder<VehicleDisplayType<?>, VehicleDisplayType<BaseDisplay>> WEAPON = register(
            "weapon",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, BaseDisplayPojo.class);
                return new BaseDisplay(pojo);
            },
            null
    );

    public static final DeferredHolder<VehicleDisplayType<?>, VehicleDisplayType<WheeledVehicleDisplay>> WHEELED_VEHICLE = register(
            "wheeled_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, VehicleDisplayPojo.class);
                return new WheeledVehicleDisplay(pojo);
            },
            AnimationContextFactory.wheeledVehicle()
    );

    public static final DeferredHolder<VehicleDisplayType<?>, VehicleDisplayType<TrackedVehicleDisplay>> TRACKED_VEHICLE = register(
            "tracked_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, TrackedVehicleDisplayPojo.class);
                return new TrackedVehicleDisplay(pojo);
            },
            AnimationContextFactory.trackedVehicle()
    );

    public static final DeferredHolder<VehicleDisplayType<?>, VehicleDisplayType<RotaryWingVehicleDisplay>> ROTARY_WING_VEHICLE = register(
            "rotary_wing_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, VehicleDisplayPojo.class);
                return new RotaryWingVehicleDisplay(pojo);
            },
            AnimationContextFactory.rotaryWingVehicle()
    );

    public static final DeferredHolder<VehicleDisplayType<?>, VehicleDisplayType<FixedWingVehicleDisplay>> FIXED_WING_VEHICLE = register(
            "fixed_wing_vehicle",
            json -> {
                var pojo = GsonUtil.GSON.fromJson(json, FixedWingVehicleDisplayPojo.class);
                return new FixedWingVehicleDisplay(pojo);
            },
            AnimationContextFactory.fixedWingVehicle()
    );

    private static <D extends BaseDisplay> DeferredHolder<VehicleDisplayType<?>, VehicleDisplayType<D>> register(
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
