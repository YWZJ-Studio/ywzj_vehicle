package org.ywzj.vehicle.entity.vehicle.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.vehicle.part.WeaponBayUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class HTF5980 extends WheeledVehicle {

    private WeaponBayUnit pipe;
    private WeaponUnit missile;

    public HTF5980(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void initData() {
        super.initData();
        if (partUnitMap.get("pipe") instanceof WeaponBayUnit pipe) {
            this.pipe = pipe;
        }
        if (partUnitMap.get("missile") instanceof WeaponUnit missile) {
            this.missile = missile;
        }
        if (level().isClientSide()) {
            missile.rotByAim = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            missile.setXAimRot(pipe.isOn() ? -90 : 0);
        }
    }

    @Override
    public Vec3 tickMove() {
        if (pipe.isOn()) {
            controlUnit.reset();
        }
        return super.tickMove();
    }

    @Override
    protected void tickEngineSpeed() {
        if (pipe.isOn()) {
            controlUnit.reset();
        }
        super.tickEngineSpeed();
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (missile.getXRot() > -90) {
            return;
        }
        super.shoot(partUnitIndex, weaponIndex, aimContexts, operator);
    }

}
