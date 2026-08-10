package org.ywzj.vehicle.client.render.animation.context;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import org.ywzj.vehicle.client.render.animation.util.AnimationHandler;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.control.ControlUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.RotatableUnit;
import org.ywzj.vehicle.vehicle.part.SwitchableUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.weapon.VehicleWeaponAgent;

import java.util.List;
import java.util.Optional;

public class VehicleContext<E extends AbstractVehicle> extends EntityContext<E> {

    private AnimationHandler eventAnimationHandler;

    public VehicleContext(E entity) {
        super(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (eventAnimationHandler != null) {
            eventAnimationHandler.tick(this.events);
        }
    }

    public void setEventAnimationHandler(AnimationHandler eventAnimationHandler) {
        this.eventAnimationHandler = eventAnimationHandler;
        this.eventAnimationHandler.setSoundProcessor(this::processSounds);
    }

    public Pose getEventPose() {
        if (eventAnimationHandler != null) {
            return eventAnimationHandler.evaluate();
        }
        return DummyPose.INSTANCE;
    }

    public void setBoneVisible(String boneName, boolean visible) {
        BakedModelInstance modelInstance = entity.getModelInstance();
        BoneState boneState = modelInstance.getBone(boneName);
        if (boneState != null) {
            boneState.visible = visible;
        }
    }

    public void setBoneIlluminated(String boneName, boolean illuminated) {
        BakedModelInstance modelInstance = entity.getModelInstance();
        BoneState boneState = modelInstance.getBone(boneName);
        if (boneState != null) {
            boneState.illuminated = illuminated;
        }
    }

    public float getXRot() {
        return getEntity().getXRot();
    }

    public float getYRot() {
        return getEntity().getYRot();
    }

    public float getZRot() {
        return getEntity().getZRot();
    }

    public boolean hasOwner(String id) {
        return getEntity().getPartUnit(id).map(part -> part.getOwner() != null).orElse(false);
    }

    public double getSpeed() {
        return getEntity().getDeltaMovement().length();
    }

    public ControlUnit getControlUnit() {
        return getEntity().controlUnit;
    }

    public float getPower() {
        return getEntity().getPower();
    }

    public float getEngineSpeed() {
        return getEntity().getEngineSpeed();
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

    public boolean isPartDestroyed(String id) {
        return getEntity().getPartUnit(id).map(PartUnit::isDestroyed).orElse(false);
    }

    public boolean isPartDetached(String id) {
        return getEntity().getPartUnit(id).map(PartUnit::isDetached).orElse(false);
    }

    public boolean isPartOn(String id) {
        return getEntity().getPartUnit(id).map(part -> {
            if (part instanceof SwitchableUnit<?> switchableUnit) {
                return switchableUnit.isOn();
            }
            return false;
        }).orElse(false);
    }

    public int getWeaponRemainAmmo(String id, int weaponIndex) {
        Optional<PartUnit<?>> partUnitOptional = getEntity().getPartUnit(id);
        if (partUnitOptional.isEmpty()) {
            return 0;
        }
        if (partUnitOptional.get() instanceof WeaponUnit weaponUnit) {
            List<AbstractVehicleWeapon<?>> indexedWeapons = weaponUnit.getIndexedWeapons();
            if (weaponIndex >= 0 && weaponIndex < indexedWeapons.size()) {
                AbstractVehicleWeapon<?> weapon = indexedWeapons.get(weaponIndex);
                if (weapon instanceof VehicleWeaponAgent weaponAgent) {
                    return weaponAgent.getWeaponUnit().getCurrentWeapon().get().getRemainAmmo();
                }
                return weapon.getRemainAmmo();
            }
        }
        return 0;
    }

    public String getWeaponName(String id, int weaponIndex) {
        Optional<PartUnit<?>> partUnitOptional = getEntity().getPartUnit(id);
        if (partUnitOptional.isEmpty()) {
            return "";
        }
        if (partUnitOptional.get() instanceof WeaponUnit weaponUnit) {
            List<AbstractVehicleWeapon<?>> indexedWeapons = weaponUnit.getIndexedWeapons();
            if (weaponIndex >= 0 && weaponIndex < indexedWeapons.size()) {
                AbstractVehicleWeapon<?> weapon = indexedWeapons.get(weaponIndex);
                if (weapon instanceof VehicleWeaponAgent weaponAgent) {
                    return weaponAgent.getWeaponUnit().getCurrentWeapon().get().getDisplayName().getString();
                }
                return weapon.getDisplayName().getString();
            }
        }
        return "";
    }

    public int tickCount() {
        return getEntity().tickCount;
    }

    public long lastRenderTime() {
        return getEntity().lastRenderTime;
    }

}
