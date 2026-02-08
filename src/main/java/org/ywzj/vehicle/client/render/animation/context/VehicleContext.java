package org.ywzj.vehicle.client.render.animation.context;

import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import java.util.List;
import java.util.Optional;

public class VehicleContext<E extends AbstractVehicle> extends EntityContext<E> {

    public VehicleContext(E entity) {
        super(entity);
    }

    public boolean hasOwner(String id) {
        return getEntity().getPartUnit(id).map(part -> part.getOwner() != null).orElse(false);
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

    // ========== Tracked Vehicle Methods ==========

    public float getForwardSpeed() {
        if (getEntity() instanceof TrackedVehicle tracked) {
            return tracked.getForwardSpeed();
        }
        return 0f;
    }

    public float getTurnSpeed() {
        if (getEntity() instanceof TrackedVehicle tracked) {
            return tracked.getTurnSpeed();
        }
        return 0f;
    }

    // ========== Utility Methods ==========

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long lastRenderTime() {
        return getEntity().lastRenderTime;
    }
}
