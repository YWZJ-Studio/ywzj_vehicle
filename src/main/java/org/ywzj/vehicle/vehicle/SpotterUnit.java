package org.ywzj.vehicle.vehicle;

import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class SpotterUnit extends WeaponUnit {

    public SpotterUnit(AbstractVehicle vehicle) {
        super("spotter", -1, vehicle, vehicle.getCameraOffset().add(new Vec3(0d, 2.5d, 0d)), 1f, null, null);
        this.xRotMax = 90;
        this.xRotMin = -90;
        this.xRotSpeed = 120;
        this.yRotSpeed = 120;
    }

}
