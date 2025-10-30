package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.client.gui.ScopeOverlay;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class VehicleMissile extends AbstractVehicleWeapon<BaseVehicleWeaponData> {

    public VehicleMissile(AbstractVehicle vehicle, WeaponUnit unit, int index, BaseVehicleWeaponData data) {
        super(vehicle, unit, index, data);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean doClientShoot() {
        VehicleMissileWeaponData data = (VehicleMissileWeaponData) this.getData();
        WeaponUnit missileWeaponUnit = getWeaponUnit();
        if (missileWeaponUnit.parentWeaponUnitAim) {
            missileWeaponUnit = missileWeaponUnit.getParentWeaponUnit();
        }
        if (missileWeaponUnit.xRot < data.getXRotMin()
                || missileWeaponUnit.xRot > data.getXRotMax()
                || missileWeaponUnit.yRot < data.getYRotMin()
                || missileWeaponUnit.yRot > data.getYRotMax()) {
            ScopeOverlay.tips.put(System.currentTimeMillis(), "超出导弹射界");
            return false;
        }
        return super.doClientShoot();
    }

    @Override
    public void shoot(Vec3 origin, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        if (isCoolingDown() || isReloading() || !consumeAmmo()) {
            return;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        MissileEntity missileEntity = new MissileEntity(AllEntities.MISSILE.get(), vehicle.level());
        missileEntity.shoot(this.getVehicle(), this.getName(), origin, ammoXRot, ammoYRot, this.getWeaponUnit().getOwner());
        vehicle.level().playSound(null, vehicle, AllSounds.MISSILE_LAUNCH.get(), SoundSource.PLAYERS, 16f, 1f);
        vehicle.level().addFreshEntity(missileEntity);
    }

}
