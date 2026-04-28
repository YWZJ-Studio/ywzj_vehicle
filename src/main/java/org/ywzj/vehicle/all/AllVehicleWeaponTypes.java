package org.ywzj.vehicle.all;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponType;
import org.ywzj.vehicle.custom.weapon.data.*;
import org.ywzj.vehicle.vehicle.weapon.*;

public class AllVehicleWeaponTypes {

    public static final DeferredRegister<VehicleWeaponType<?, ?>> WEAPON_TYPES = DeferredRegister.create(ModRegistries.VEHICLE_WEAPON_TYPE_KEY, YwzjVehicle.MOD_ID);

    public static final DeferredHolder<VehicleWeaponType<?, ?>, VehicleWeaponType<VehicleCannon, VehicleCannonWeaponData>> CANNON = register(
            "cannon", json -> GsonUtil.GSON.fromJson(json, VehicleCannonWeaponData.class), VehicleCannon::new
    );

    public static final DeferredHolder<VehicleWeaponType<?, ?>, VehicleWeaponType<VehicleGrenade, VehicleGrenadeWeaponData>> GRENADE = register(
            "grenade", json -> GsonUtil.GSON.fromJson(json, VehicleGrenadeWeaponData.class), VehicleGrenade::new
    );

    public static final DeferredHolder<VehicleWeaponType<?, ?>, VehicleWeaponType<VehicleRocket, VehicleRocketWeaponData>> ROCKET = register(
            "rocket", json -> GsonUtil.GSON.fromJson(json, VehicleRocketWeaponData.class), VehicleRocket::new
    );

    public static final DeferredHolder<VehicleWeaponType<?, ?>, VehicleWeaponType<VehicleAerialBomb, VehicleAerialBombWeaponData>> AERIAL_BOMB = register(
            "aerial_bomb", json -> GsonUtil.GSON.fromJson(json, VehicleAerialBombWeaponData.class), VehicleAerialBomb::new
    );

    public static final DeferredHolder<VehicleWeaponType<?, ?>, VehicleWeaponType<VehicleMissile, VehicleMissileWeaponData>> MISSILE = register(
            "missile", json -> GsonUtil.GSON.fromJson(json, VehicleMissileWeaponData.class), VehicleMissile::new
    );

    public static final DeferredHolder<VehicleWeaponType<?, ?>, VehicleWeaponType<VehicleDecoyFlare, BaseVehicleWeaponData>> DECOY_FLARE = register(
            "decoy_flare", json -> GsonUtil.GSON.fromJson(json, BaseVehicleWeaponData.class), VehicleDecoyFlare::new
    );

    private static <T extends AbstractVehicleWeapon<D>, D extends BaseVehicleWeaponData> DeferredHolder<VehicleWeaponType<?, ?>, VehicleWeaponType<T, D>> register(
            String name,
            VehicleWeaponType.DataSerializer<D> dataSerializer,
            VehicleWeaponType.WeaponUnitFactory<T, D> factory
    ) {
        return WEAPON_TYPES.register(name,
                () -> VehicleWeaponType.Builder.<T, D>of(YwzjVehicle.modLocation(name))
                        .setDataSerializer(dataSerializer)
                        .setFactory(factory)
                        .build()
        );
    }

    public static void register(IEventBus eventBus) {
        WEAPON_TYPES.register(eventBus);
    }

}
