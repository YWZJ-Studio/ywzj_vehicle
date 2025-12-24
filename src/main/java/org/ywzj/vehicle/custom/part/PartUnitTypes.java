package org.ywzj.vehicle.custom.part;

import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.part.data.*;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.RadarUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class PartUnitTypes {

    public static final PartUnitType<PartUnit<PartUnitData>, PartUnitData> GENERIC =
            PartUnitType.Builder.of(YwzjVehicle.modLoc("generic"))
                    .setFactory(PartUnit::new)
                    .setDataSerializer((json) -> {
                        var pojo = GsonUtil.GSON.fromJson(json, PartUnitPojo.class);
                        return new PartUnitData(pojo);
                    })
                    .build();

    public static final PartUnitType<RotatableUnit<RotatableUnitData>, RotatableUnitData> ROTATABLE =
            PartUnitType.Builder.<RotatableUnit<RotatableUnitData>, RotatableUnitData>of(YwzjVehicle.modLoc("rotatable"))
                    .setFactory(RotatableUnit::new)
                    .setDataSerializer((json) -> {
                        var pojo = GsonUtil.GSON.fromJson(json, RotatableUnitPojo.class);
                        return new RotatableUnitData(pojo);
                    })
                    .build();

    public static final PartUnitType<WeaponUnit, WeaponUnitData> WEAPON =
            PartUnitType.Builder.<WeaponUnit, WeaponUnitData>of(YwzjVehicle.modLoc("weapon"))
                    .setFactory(WeaponUnit::new)
                    .setDataSerializer((json) -> {
                        var pojo = GsonUtil.GSON.fromJson(json, WeaponUnitPojo.class);
                        return new WeaponUnitData(pojo);
                    })
                    .build();

    public static final PartUnitType<RadarUnit, RadarUnitData> RADAR =
            PartUnitType.Builder.<RadarUnit, RadarUnitData>of(YwzjVehicle.modLoc("radar"))
                    .setFactory(RadarUnit::new)
                    .setDataSerializer((json) -> {
                        var pojo = GsonUtil.GSON.fromJson(json, RadarUnitPojo.class);
                        return new RadarUnitData(pojo);
                    })
                    .build();

}
