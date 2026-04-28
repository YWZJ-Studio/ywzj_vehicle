package org.ywzj.vehicle.all;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.vehicle.VehicleDisplayType;
import org.ywzj.vehicle.custom.part.PartUnitType;
import org.ywzj.vehicle.custom.vehicle.VehicleDataType;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponType;

@EventBusSubscriber
public class ModRegistries {

    public static final ResourceKey<Registry<VehicleWeaponType<?, ?>>> VEHICLE_WEAPON_TYPE_KEY = ResourceKey.createRegistryKey(
            YwzjVehicle.modLocation("vehicle_weapon_type")
    );
    public static Registry<VehicleWeaponType<?, ?>> VEHICLE_WEAPON_TYPE;

    public static final ResourceKey<Registry<PartUnitType<?, ?>>> PART_UNIT_TYPE_KEY = ResourceKey.createRegistryKey(
            YwzjVehicle.modLocation("part_unit_type")
    );
    public static Registry<PartUnitType<?, ?>> PART_UNIT_TYPE;

    public static final ResourceKey<Registry<VehicleDataType<?>>> VEHICLE_DATA_TYPE_KEY = ResourceKey.createRegistryKey(
            YwzjVehicle.modLocation("vehicle_data_type")
    );
    public static Registry<VehicleDataType<?>> VEHICLE_DATA_TYPE;

    public static final ResourceKey<Registry<VehicleDisplayType<?>>> VEHICLE_DISPLAY_TYPE_KEY = ResourceKey.createRegistryKey(
            YwzjVehicle.modLocation("vehicle_display_type")
    );
    public static Registry<VehicleDisplayType<?>> VEHICLE_DISPLAY_TYPE;

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void registerRegistries(NewRegistryEvent event) {
        VEHICLE_WEAPON_TYPE = event.create(new RegistryBuilder<>(VEHICLE_WEAPON_TYPE_KEY));
        PART_UNIT_TYPE = event.create(new RegistryBuilder<>(PART_UNIT_TYPE_KEY));
        VEHICLE_DATA_TYPE = event.create(new RegistryBuilder<>(VEHICLE_DATA_TYPE_KEY));
        VEHICLE_DISPLAY_TYPE = event.create(new RegistryBuilder<>(VEHICLE_DISPLAY_TYPE_KEY));
    }

}
