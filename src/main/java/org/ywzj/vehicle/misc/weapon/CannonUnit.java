package org.ywzj.vehicle.misc.weapon;

import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.custom.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;

public class CannonUnit extends AbstractTurretUnit<WeaponUnitData> {
    public CannonUnit(AbstractVehicle vehicle, int index, WeaponUnitData data) {
        super(vehicle, index, data);
    }

    @Override
    public void shoot(Vec3 origin, float ammoXRot, float ammoYRot) {
        var vehicle = getVehicle();
        BulletEntity bulletEntity = new BulletEntity(vehicle.level(), this.getOperator(), origin);
        bulletEntity.shootFromRotation(vehicle, ammoXRot, ammoYRot, 0, 10.0f, 1f);
        bulletEntity.setDamage(this.getData().getDamage());
        bulletEntity.setHeadShot(1.5f);
        vehicle.level().addFreshEntity(bulletEntity);
    }
}
