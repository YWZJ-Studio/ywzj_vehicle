package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.client.gui.VehicleOverlay;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
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
        missileWeaponUnit = missileWeaponUnit.getRootParentWeaponUnit();
        if (missileWeaponUnit.getXRot() < data.getXRotMin()
                || missileWeaponUnit.getXRot() > data.getXRotMax()
                || missileWeaponUnit.getYRot() < data.getYRotMin()
                || missileWeaponUnit.getYRot() > data.getYRotMax()) {
            VehicleOverlay.tips.put(System.currentTimeMillis(), Component.translatable("ui.out_of_launch_limits"));
            return false;
        }
        if (data.getGuidance() == VehicleMissileWeaponData.Guidance.HOMING) {
            if (missileWeaponUnit.getLockedEntity() == null
                    && missileWeaponUnit.getFireControlSensorType() != WeaponUnitData.FireControlSensorType.EO) {
                LocalVehiclePlayer.instance.sendMessage("ui.need_lock_entity");
                return false;
            }
        }
        return super.doClientShoot();
    }

    @OnlyIn(Dist.CLIENT)
    public void onClientFire() {
        int coldLaunchTimeTick = getWeaponUnit().getRootParentWeaponUnit().getColdLaunchTimeTick();
        if (coldLaunchTimeTick > 0) {
            new Thread(() -> {
                try {
                    Thread.sleep(coldLaunchTimeTick * 50L);
                } catch (InterruptedException e) {}
                Minecraft.getInstance().execute(super::onClientFire);
            }).start();
        } else {
            super.onClientFire();
        }
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

        WeaponUnit weaponUnit = getWeaponUnit();
        WeaponUnit rootWeaponUnit = weaponUnit.getRootParentWeaponUnit();
        for (AimContext aimContext : aimContexts) {
            MissileEntity missileEntity = new MissileEntity(AllEntities.MISSILE.get(), vehicle.level(), data, rootWeaponUnit);
            missileEntity.initColdLaunch(weaponUnit);
            if (data.getGuidance() == VehicleMissileWeaponData.Guidance.PRESET
                    || data.getGuidance() == VehicleMissileWeaponData.Guidance.HOMING) {
                missileEntity.targetPos = aimContext.position;
                AimContext currentAimContext = rootWeaponUnit.aimContext();
                missileEntity.targetVec = VectorUtil.rotToVec(currentAimContext.direction.x, currentAimContext.direction.y);
                if (data.getGuidance() == VehicleMissileWeaponData.Guidance.HOMING) {
                    missileEntity.targetEntity = rootWeaponUnit.getLockedEntity();
                }
            }
            missileEntity.shoot(vehicle, this.getDisplayName(),
                    aimContext.from, aimContext.direction.x, aimContext.direction.y,
                    this.weaponUnit.getOwner());
            vehicle.level().addFreshEntity(missileEntity);
            vehicle.physicsEngine.recoil(weaponUnit, data.getRecoil());
        }
        return true;
    }

    @Override
    public boolean withSeeker() {
        return true;
    }

}
