package org.ywzj.vehicle.vehicle;

import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class SpotterUnit extends WeaponUnit {

    public SpotterUnit(AbstractVehicle vehicle, Vec3 boltOffset, Vec3 operatorOffset, Vec3 seatOffset, WeaponUnit baseWeaponUnit) {
        super("spotter", -1, vehicle, boltOffset, 1f, operatorOffset, seatOffset, baseWeaponUnit);
        this.xRotMax = 90;
        this.xRotMin = -90;
        this.xRotSpeed = 120;
        this.yRotSpeed = 120;
    }

}
