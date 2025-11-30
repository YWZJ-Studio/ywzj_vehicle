package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.client.gui.VehicleScopeOverlay;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

public class VehicleMissile extends AbstractVehicleWeapon<VehicleMissileWeaponData> {

    public VehicleMissile(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleMissileWeaponData data, String serializeId) {
        super(vehicle, unit, index, data, serializeId);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean doClientShoot() {
        VehicleMissileWeaponData data = this.getData();
        WeaponUnit missileWeaponUnit = getWeaponUnit();
        if (missileWeaponUnit.isParentWeaponUnitAim()) {
            missileWeaponUnit = missileWeaponUnit.getParentWeaponUnit();
        }
        if (missileWeaponUnit.getXRot() < data.getXRotMin()
                || missileWeaponUnit.getXRot() > data.getXRotMax()
                || missileWeaponUnit.getYRot() < data.getYRotMin()
                || missileWeaponUnit.getYRot() > data.getYRotMax()) {
            VehicleScopeOverlay.tips.put(System.currentTimeMillis(), "超出导弹射界");
            return false;
        }
        return super.doClientShoot();
    }

    @Override
    public void shoot(List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        if (!check(ammoSpawnPositions, ammoXRot, ammoYRot, shooter)) {
            return;
        }
        if (isCoolingDown() || isReloading() || !consumeAmmo(ammoSpawnPositions.size())) {
            return;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        for (Vec3 ammoSpawnPosition : ammoSpawnPositions) {
            MissileEntity missileEntity = new MissileEntity(AllEntities.MISSILE.get(), vehicle.level());
            missileEntity.damage = data.getDamage();
            missileEntity.maxSpeed = data.getMaxSpeed();
            missileEntity.shoot(this.getVehicle(), this.getDisplayName(), ammoSpawnPosition, ammoXRot, ammoYRot, this.getWeaponUnit().getOwner());
            vehicle.level().addFreshEntity(missileEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
        }
    }

}
