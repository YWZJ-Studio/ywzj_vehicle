package org.ywzj.vehicle.client.render.animation.context;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.client.render.animation.util.SimpleFireAnimationHandler;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import java.util.List;
import java.util.Optional;

public class VehicleContext<E extends AbstractVehicle> extends EntityContext<E> {

    private SimpleFireAnimationHandler fireAnimationHandler;

    public VehicleContext(E entity) {
        super(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (fireAnimationHandler != null) {
            fireAnimationHandler.tick(this.events);
        }
    }

    public void setFireAnimationHandler(SimpleFireAnimationHandler fireAnimationHandler) {
        this.fireAnimationHandler = fireAnimationHandler;
        this.fireAnimationHandler.setSoundProcessor(this::processSounds);
    }

    public Pose getFirePose() {
        if (fireAnimationHandler != null) {
            return fireAnimationHandler.evaluate();
        }
        return DummyPose.INSTANCE;
    }

    public boolean hasOwner(String id) {
        return getEntity().getPartUnit(id).map(part -> part.getOwner() != null).orElse(false);
    }

    public float getPower() {
        return getEntity().getPower();
    }

    public float getPartXRot(String id) {
        return getEntity().getPartUnit(id).map(part -> {
            if (part instanceof RotatableUnit<?> rotatable) {
                return rotatable.getViewXRot(partialTick);
            }
            return 0f;
        }).orElse(0f);
    }

    public float getPartYRot(String id) {
        return getEntity().getPartUnit(id).map(part -> {
            if (part instanceof RotatableUnit<?> rotatable) {
                return rotatable.getViewYRot(partialTick);
            }
            return 0f;
        }).orElse(0f);
    }

    public int getWeaponRemainAmmo(String id, int weaponIndex) {
        Optional<PartUnit<?>> partUnitOptional = getEntity().getPartUnit(id);
        if (partUnitOptional.isEmpty()) {
            return 0;
        }
        if (partUnitOptional.get() instanceof WeaponUnit weaponUnit) {
            List<AbstractVehicleWeapon<?>> indexedWeapons = weaponUnit.getIndexedWeapons();
            if (weaponIndex >= 0 && weaponIndex < indexedWeapons.size()) {
                return indexedWeapons.get(weaponIndex).getRemainAmmo();
            }
        }
        return 0;
    }

    // ========== Utility Methods ==========

    public long lastRenderTime() {
        return getEntity().lastRenderTime;
    }
}
