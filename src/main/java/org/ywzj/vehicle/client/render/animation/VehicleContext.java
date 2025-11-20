package org.ywzj.vehicle.client.render.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.Map;

public class VehicleContext extends EntityContext<AbstractVehicle> {
    private boolean shouldShoot = false;

    public VehicleContext(AbstractVehicle entity, Map<String, BedrockAnimation> animations) {
        super(entity, animations);
    }

    public void setShouldShoot(boolean shouldShoot) {
        this.shouldShoot = shouldShoot;
    }

    public boolean consumeShoot() {
        if (shouldShoot) {
            shouldShoot = false;
            return true;
        }
        return false;
    }
}
