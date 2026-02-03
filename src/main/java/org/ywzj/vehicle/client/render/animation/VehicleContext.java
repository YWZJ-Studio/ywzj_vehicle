package org.ywzj.vehicle.client.render.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class VehicleContext extends EntityContext<AbstractVehicle> {

    private final Queue<String> animationPlayQueue = new LinkedList<>();

    public VehicleContext(AbstractVehicle entity, Map<String, BedrockAnimation> animations) {
        super(entity, animations);
    }

    public void offerAnimation(String animation) {
        if (getAnimation(animation) != null) {
            animationPlayQueue.add(animation);
        }
    }

    public void consumeAnimation() {
        while (!animationPlayQueue.isEmpty()) {
            playMultiAnimation(animationPlayQueue.poll());
        }
    }

}
