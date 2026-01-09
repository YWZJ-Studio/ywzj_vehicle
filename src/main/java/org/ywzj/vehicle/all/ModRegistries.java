package org.ywzj.vehicle.all;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.vehicle.VehicleDisplayType;
import org.ywzj.vehicle.custom.part.PartUnitType;
import org.ywzj.vehicle.custom.vehicle.VehicleDataType;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponType;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRegistries {

    public static final ResourceKey<Registry<VehicleWeaponType<?, ?>>> VEHICLE_WEAPON_TYPE = ResourceKey.createRegistryKey(
            YwzjVehicle.modLocation("vehicle_weapon_type")
    );
    public static Supplier<IForgeRegistry<VehicleWeaponType<?, ?>>> VEHICLE_WEAPON_TYPE_SUPPLIER;

    public static final ResourceKey<Registry<PartUnitType<?, ?>>> PART_UNIT_TYPE = ResourceKey.createRegistryKey(
            YwzjVehicle.modLocation("part_unit_type")
    );
    public static Supplier<IForgeRegistry<PartUnitType<?, ?>>> PART_UNIT_TYPE_SUPPLIER;

    public static final ResourceKey<Registry<VehicleDataType<?>>> VEHICLE_DATA_TYPE = ResourceKey.createRegistryKey(
            YwzjVehicle.modLocation("vehicle_data_type")
    );
    public static Supplier<IForgeRegistry<VehicleDataType<?>>> VEHICLE_DATA_TYPE_SUPPLIER;

    public static final ResourceKey<Registry<VehicleDisplayType<?>>> VEHICLE_DISPLAY_TYPE = ResourceKey.createRegistryKey(
            YwzjVehicle.modLocation("vehicle_display_type")
    );
    public static Supplier<IForgeRegistry<VehicleDisplayType<?>>> VEHICLE_DISPLAY_TYPE_SUPPLIER;

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        VEHICLE_WEAPON_TYPE_SUPPLIER = event.create(new RegistryBuilder<VehicleWeaponType<?, ?>>().setName(VEHICLE_WEAPON_TYPE.location()));
        PART_UNIT_TYPE_SUPPLIER = event.create(new RegistryBuilder<PartUnitType<?, ?>>().setName(PART_UNIT_TYPE.location()));
        VEHICLE_DATA_TYPE_SUPPLIER = event.create(new RegistryBuilder<VehicleDataType<?>>().setName(VEHICLE_DATA_TYPE.location()));
        VEHICLE_DISPLAY_TYPE_SUPPLIER = event.create(new RegistryBuilder<VehicleDisplayType<?>>()
                .disableSync().setName(VEHICLE_DISPLAY_TYPE.location()));
    }

}
