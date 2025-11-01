package org.ywzj.vehicle.all;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.part.PartUnitType;
import org.ywzj.vehicle.custom.part.PartUnitTypes;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.part.data.RotatableUnitData;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class AllPartUnitType {
    public static final DeferredRegister<PartUnitType<?, ?>> WEAPON_TYPES = DeferredRegister.create(ModRegistries.PART_UNIT_TYPE, YwzjVehicle.MOD_ID);

    public static final RegistryObject<PartUnitType<PartUnit<PartUnitData>, PartUnitData>> GENERIC = register(PartUnitTypes.GENERIC);

    public static final RegistryObject<PartUnitType<RotatableUnit<RotatableUnitData>, RotatableUnitData>> ROTATABLE = register(PartUnitTypes.ROTATABLE);

    public static final RegistryObject<PartUnitType<WeaponUnit, WeaponUnitData>> WEAPON = register(PartUnitTypes.WEAPON);

    private static <T extends PartUnit<D>, D extends PartUnitData> RegistryObject<PartUnitType<T, D>> register(
            PartUnitType<T, D> type
    ) {
        return WEAPON_TYPES.register(type.id().getPath(), () -> type);
    }

    public static void register(IEventBus eventBus) {
        WEAPON_TYPES.register(eventBus);
    }
}
