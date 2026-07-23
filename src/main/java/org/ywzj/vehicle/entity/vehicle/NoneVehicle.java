package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.SimpleVehicleDisplay;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class NoneVehicle extends AbstractVehicle implements IAnimationEntity<AbstractVehicle, VehicleContext<AbstractVehicle>> {
    private IAnimationInstance<VehicleContext<AbstractVehicle>> animationInstance;

    public NoneVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(weaponIndex, aimContexts, operator);
        }
    }

    @Override
    protected Vec3 tickMove() {
        return Vec3.ZERO;
    }

    @Override
    public IAnimationInstance<VehicleContext<AbstractVehicle>> getAnimationInstance() {
        return animationInstance;
    }

    @Override
    public void initDisplayData(BaseDisplay display) {
        super.initDisplayData(display);
        if (display instanceof SimpleVehicleDisplay simpleVehicleDisplay) {
            this.animationInstance = simpleVehicleDisplay.createAnimationInstance(this);
        }
    }

}
