package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.part.PartUnitType;
import org.ywzj.vehicle.custom.part.PartUnitTypes;
import org.ywzj.vehicle.custom.part.data.*;
import org.ywzj.vehicle.vehicle.part.*;

public class AllPartUnitType {

    public static final DeferredRegister<PartUnitType<?, ?>> PART_UNIT_TYPE = DeferredRegister.create(ModRegistries.PART_UNIT_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<PartUnitType<PartUnit<PartUnitData>, PartUnitData>> GENERIC = register(PartUnitTypes.GENERIC);

    public static final RegistryObject<PartUnitType<SwitchableUnit<PartUnitData>, PartUnitData>> SWITCHABLE = register(PartUnitTypes.SWITCHABLE);

    public static final RegistryObject<PartUnitType<LandingGearUnit, PartUnitData>> LANDING_GEAR = register(PartUnitTypes.LANDING_GEAR);

    public static final RegistryObject<PartUnitType<RotatableUnit<RotatableUnitData>, RotatableUnitData>> ROTATABLE = register(PartUnitTypes.ROTATABLE);

    public static final RegistryObject<PartUnitType<DoorUnit, DoorUnitData>> DOOR = register(PartUnitTypes.DOOR);

    public static final RegistryObject<PartUnitType<WeaponUnit, WeaponUnitData>> WEAPON = register(PartUnitTypes.WEAPON);

    public static final RegistryObject<PartUnitType<AutoWeaponUnit, WeaponUnitData>> AUTO_WEAPON = register(PartUnitTypes.AUTO_WEAPON);

    public static final RegistryObject<PartUnitType<RadarUnit, RadarUnitData>> RADAR = register(PartUnitTypes.RADAR);

    public static final RegistryObject<PartUnitType<WeaponBayUnit, PartUnitData>> WEAPON_BAY = register(PartUnitTypes.WEAPON_BAY);

    private static <T extends PartUnit<D>, D extends PartUnitData> RegistryObject<PartUnitType<T, D>> register(
            PartUnitType<T, D> type
    ) {
        return PART_UNIT_TYPE.register(type.id().getPath(), () -> type);
    }

    public static void register(IEventBus eventBus) {
        PART_UNIT_TYPE.register(eventBus);
    }

}
