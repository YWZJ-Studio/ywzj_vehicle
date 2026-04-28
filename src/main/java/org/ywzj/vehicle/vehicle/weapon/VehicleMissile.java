package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
        if (missileWeaponUnit.isParentWeaponUnitAim()) {
            missileWeaponUnit = missileWeaponUnit.getRootParentWeaponUnit();
        }
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

        WeaponUnit weaponUnit = getWeaponUnit().getRootParentWeaponUnit();
        for (AimContext aimContext : aimContexts) {
            MissileEntity missileEntity = new MissileEntity(AllEntities.MISSILE.get(), vehicle.level(), data, weaponUnit);
            if (data.getGuidance() == VehicleMissileWeaponData.Guidance.PRESET
                    || data.getGuidance() == VehicleMissileWeaponData.Guidance.HOMING) {
                List<Vec3> positions = weaponUnit.aimContexts().stream().map(context -> context.position).toList();
                double x = positions.stream().mapToDouble(pos -> pos.x).average().orElse(0);
                double y = positions.stream().mapToDouble(pos -> pos.y).average().orElse(0);
                double z = positions.stream().mapToDouble(pos -> pos.z).average().orElse(0);
                AimContext currentAimContext = weaponUnit.aimContext();
                Vec3 targetVec = VectorUtil.rotToVec(currentAimContext.direction.x, currentAimContext.direction.y);
                Vec3 start = new Vec3(x, y, z);
                Vec3 end = start.add(targetVec.normalize().scale(1024));
                missileEntity.targetPos = VectorUtil.hitPosition(vehicle, start, end);
                missileEntity.targetVec = targetVec;
                if (data.getGuidance() == VehicleMissileWeaponData.Guidance.HOMING) {
                    missileEntity.targetEntity = weaponUnit.getLockedEntity();
                }
            }
            missileEntity.shoot(this.getVehicle(), this.getDisplayName(),
                    aimContext.position, aimContext.direction.x, aimContext.direction.y,
                    this.getWeaponUnit().getOwner());
            missileEntity.setDeltaMovement(missileEntity.getDeltaMovement().add(vehicle.getDeltaMovement()));
            vehicle.level().addFreshEntity(missileEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
        }
        return true;
    }

    @Override
    public boolean withSeeker() {
        return true;
    }

}
