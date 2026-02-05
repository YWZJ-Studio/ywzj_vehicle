package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.client.gui.VehicleOverlay;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

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
            VehicleOverlay.tips.put(System.currentTimeMillis(), Component.translatable("ui.out_of_launch_limits"));
            return false;
        }
        return super.doClientShoot();
    }

    @Override
    public boolean shoot(List<AimContext> aimContexts, LivingEntity shooter) {
        if (!check(aimContexts, shooter)) {
            return false;
        }
        if (isCoolingDown() || isReloading() || !consumeAmmo(aimContexts)) {
            return false;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        WeaponUnit weaponUnit = getWeaponUnit().getParentWeaponUnit() != null ? getWeaponUnit().getParentWeaponUnit() : getWeaponUnit();

        for (AimContext aimContext : aimContexts) {
            MissileEntity missileEntity = new MissileEntity(AllEntities.MISSILE.get(), vehicle.level(), data.getGuidance(), weaponUnit, data.getWeaponId());
            missileEntity.damage = data.getDamage();
            missileEntity.explosion = data.getExplosion();
            missileEntity.maxSpeed = data.getMaxSpeed();
            missileEntity.maxG = data.getMaxG();
            missileEntity.shoot(this.getVehicle(), this.getDisplayName(), aimContext.position, aimContext.direction.x, aimContext.direction.y, this.getWeaponUnit().getOwner());
            vehicle.level().addFreshEntity(missileEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
        }
        return true;
    }

}
