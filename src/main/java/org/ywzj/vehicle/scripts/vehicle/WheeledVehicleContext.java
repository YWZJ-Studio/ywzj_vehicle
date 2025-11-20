package org.ywzj.vehicle.scripts.vehicle;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;
import org.ywzj.vehicle.api.scripts.EntityContextProvider;
import org.ywzj.vehicle.api.scripts.ParticleUtil;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;

@SuppressWarnings("unused")
public class WheeledVehicleContext extends EntityContextProvider<WheeledVehicle> {
    private float partialTick;

    public WheeledVehicleContext(WheeledVehicle vehicle) {
        super(vehicle);
    }

    @ApiStatus.Internal
    public void updateRenderer(float partialTick, WheeledVehicle vehicle) {
        this.entity = vehicle;
        this.partialTick = partialTick;
    }

    @ApiStatus.Internal
    public void updateLogic(WheeledVehicle vehicle) {
        this.entity = vehicle;
    }

    public float getPartXRot(String id) {
        return entity.getPartUnit(id).map(part -> {
            if (part instanceof RotatableUnit<?> rotatable) {
                var xRot = rotatable.getXRot();
                var xRotO = rotatable.xRotO;
                return Mth.rotLerp(partialTick, xRotO, xRot);
            }
            return 0f;
        }).orElse(0f);
    }

    public float getPartYRot(String id) {
        return entity.getPartUnit(id).map(part -> {
            if (part instanceof RotatableUnit<?> rotatable) {
                var yRot = rotatable.getYRot();
                var yRotO = rotatable.yRotO;
                return Mth.rotLerp(partialTick, yRotO, yRot);
            }
            return 0f;
        }).orElse(0f);
    }

    public float getForwardSpeed() {
        return entity.getForwardSpeed();
    }

    public float getTurnAngle() {
        return entity.getTurnAngle();
    }

    public float getHealth() {
        return entity.getHealth();
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long lastRenderTime() {
        return entity.lastRenderTime;
    }

    public void saveCache(Object data) {
        entity.getScriptCache().set(data);
    }

    public Object loadCache() {
        return entity.getScriptCache().get();
    }

    public void addParticle(ParticleUtil.ParticleOptionsWrapper particleOptions, double x, double y, double z,
                            double vx, double vy, double vz) {
        entity.level().addParticle(particleOptions.options(), true, x, y, z, vx, vy, vz);
    }
}
