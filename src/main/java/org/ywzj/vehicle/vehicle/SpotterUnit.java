package org.ywzj.vehicle.vehicle;

import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class SpotterUnit extends WeaponUnit {

    public SpotterUnit(AbstractVehicle vehicle, Vec3 boltOffset, Vec3 operatorViewOffset, Vec3 seatOffset, WeaponUnit baseWeaponUnit) {
        super("spotter", -1, vehicle, boltOffset, 1f, null, operatorViewOffset, seatOffset, baseWeaponUnit);
        this.setXRotMax(90);
        this.setXRotMin(-90);
        this.setXRotSpeed(120);
        this.setYRotSpeed(120);
    }

}
