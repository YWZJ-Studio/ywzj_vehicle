package org.ywzj.vehicle.vehicle.scripts;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.ywzj.vehicle.api.scripts.EntityContextProvider;
import org.ywzj.vehicle.api.scripts.ParticleUtil;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.control.ControlUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;

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
