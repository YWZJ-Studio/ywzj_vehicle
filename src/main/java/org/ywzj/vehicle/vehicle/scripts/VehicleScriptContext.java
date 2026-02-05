package org.ywzj.vehicle.vehicle.scripts;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.ywzj.vehicle.api.scripts.EntityContextProvider;
import org.ywzj.vehicle.api.scripts.ParticleUtil;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.control.ControlUnit;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import java.util.List;
import java.util.Optional;

public class VehicleScriptContext<T extends AbstractVehicle> extends EntityContextProvider<T> {

    protected float partialTick;
    protected BedrockModel model;

    public VehicleScriptContext(T vehicle, BedrockModel model) {
        super(vehicle);
        this.model = model;
    }

    @ApiStatus.Internal
    public void updateRenderer(float partialTick, T vehicle) {
        this.entity = vehicle;
        this.partialTick = partialTick;
    }

    @ApiStatus.Internal
    public void updateLogic(T vehicle) {
        this.entity = vehicle;
    }

    public boolean hasOwner(String id) {
        return entity.getPartUnit(id).map(part -> part.getOwner() != null).orElse(false);
    }

    public float getPartXRot(String id) {
        return entity.getPartUnit(id).map(part -> {
            if (part instanceof RotatableUnit<?> rotatable) {
                return rotatable.getViewXRot(partialTick);
            }
            return 0f;
        }).orElse(0f);
    }

    public float getPartYRot(String id) {
        return entity.getPartUnit(id).map(part -> {
            if (part instanceof RotatableUnit<?> rotatable) {
                return rotatable.getViewYRot(partialTick);
            }
            return 0f;
        }).orElse(0f);
    }

    public int getWeaponRemainAmmo(String id, int weaponIndex) {
        Optional<PartUnit<?>> partUnitOptional = entity.getPartUnit(id);
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

    public void rotateBone(String boneName, float xRot, float yRot, float zRot) {
        BedrockBone bone = model.getBone(boneName);
        if (bone == null) {
            return;
        }
        Quaternionf q = new Quaternionf().rotateZYX(
                (float) Math.toRadians(zRot),
                (float) Math.toRadians(yRot),
                (float) Math.toRadians(xRot)
        );
        bone.rotation.mul(q);
    }

    public void setBone(String boneName, float xRot, float yRot, float zRot) {
        BedrockBone bone = model.getBone(boneName);
        if (bone == null) {
            return;
        }
        Quaternionf q = new Quaternionf().rotateZYX(
                (float) Math.toRadians(zRot),
                (float) Math.toRadians(yRot),
                (float) Math.toRadians(xRot)
        );
        bone.rotation.set(q);
    }

    public void visibleBone(String boneName, boolean visible) {
        BedrockBone bone = model.getBone(boneName);
        if (bone == null) {
            return;
        }
        bone.visible = visible;
    }

    public float getXRot() {
        return entity.getXRot();
    }

    public float getYRot() {
        return entity.getYRot();
    }

    public float getZRot() {
        return entity.getZRot();
    }

    public ControlUnit getControlUnit() {
        return entity.controlUnit;
    }

    public boolean hasPower() {
        return entity.hasPower();
    }

    public float getPower() {
        return entity.getPower();
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long lastRenderTime() {
        return entity.lastRenderTime;
    }

    public void addParticle(ParticleUtil.ParticleOptionsWrapper particleOptions, double x, double y, double z,
                            double vx, double vy, double vz) {
        entity.level().addParticle(particleOptions.options(), true, x, y, z, vx, vy, vz);
    }

    public float getHealth() {
        return entity.getHealth();
    }

    public void saveCache(Object data) {
        entity.getScriptCache().set(data);
    }

    public Object loadCache() {
        return entity.getScriptCache().get();
    }

}
