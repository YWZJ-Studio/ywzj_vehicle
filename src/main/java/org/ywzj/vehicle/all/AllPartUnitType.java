package org.ywzj.vehicle.all;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.part.PartUnitType;
import org.ywzj.vehicle.custom.part.PartUnitTypes;
import org.ywzj.vehicle.custom.part.data.*;
import org.ywzj.vehicle.vehicle.part.*;

public class AllPartUnitType {

    public static final DeferredRegister<PartUnitType<?, ?>> PART_UNIT_TYPE = DeferredRegister.create(ModRegistries.PART_UNIT_TYPE_KEY, YwzjVehicle.MOD_ID);

    public static final DeferredHolder<PartUnitType<?, ?>, PartUnitType<PartUnit<PartUnitData>, PartUnitData>> GENERIC = register(PartUnitTypes.GENERIC);

    public static final DeferredHolder<PartUnitType<?, ?>, PartUnitType<SwitchableUnit<PartUnitData>, PartUnitData>> SWITCHABLE = register(PartUnitTypes.SWITCHABLE);

    public static final DeferredHolder<PartUnitType<?, ?>, PartUnitType<LandingGearUnit, PartUnitData>> LANDING_GEAR = register(PartUnitTypes.LANDING_GEAR);

    public static final DeferredHolder<PartUnitType<?, ?>, PartUnitType<RotatableUnit<RotatableUnitData>, RotatableUnitData>> ROTATABLE = register(PartUnitTypes.ROTATABLE);

    public static final DeferredHolder<PartUnitType<?, ?>, PartUnitType<DoorUnit, DoorUnitData>> DOOR = register(PartUnitTypes.DOOR);

    public static final DeferredHolder<PartUnitType<?, ?>, PartUnitType<WeaponUnit, WeaponUnitData>> WEAPON = register(PartUnitTypes.WEAPON);

    public static final DeferredHolder<PartUnitType<?, ?>, PartUnitType<AutoWeaponUnit, WeaponUnitData>> AUTO_WEAPON = register(PartUnitTypes.AUTO_WEAPON);

    public static final DeferredHolder<PartUnitType<?, ?>, PartUnitType<RadarUnit, RadarUnitData>> RADAR = register(PartUnitTypes.RADAR);

    public static final DeferredHolder<PartUnitType<?, ?>, PartUnitType<WeaponBayUnit, PartUnitData>> WEAPON_BAY = register(PartUnitTypes.WEAPON_BAY);

    private static <T extends PartUnit<D>, D extends PartUnitData> DeferredHolder<PartUnitType<?, ?>, PartUnitType<T, D>> register(
            PartUnitType<T, D> type
    ) {
        return PART_UNIT_TYPE.register(type.id().getPath(), () -> type);
    }

    public static void register(IEventBus eventBus) {
        PART_UNIT_TYPE.register(eventBus);
    }

}
