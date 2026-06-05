package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.client.render.animation.context.FixedWingVehicleContext;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixedWingVehicleDisplay extends VehicleDisplay<FixedWingVehicle, FixedWingVehicleContext> {

    private final VehicleBedrockModel afterburnerModel;
    private final ResourceLocation afterburnerTexture;
    private final Map<String, BedrockAnimation> afterburnerAnimations;

    public FixedWingVehicleDisplay(FixedWingVehicleDisplayPojo pojo) {
        super(pojo);

        if (pojo.afterburnerModel != null) {
            var modelPojo = ClientAssetsManager.INSTANCE.getModel(pojo.afterburnerModel);
            this.afterburnerModel = modelPojo
                    .map(bedrockModelPOJO -> new VehicleBedrockModel(bedrockModelPOJO, List.of()))
                    .orElse(null);
            if (this.afterburnerModel != null) {
                this.afterburnerModel.getBoneMap().values().forEach(bone -> bone.illuminated = true);
            }
        } else {
            this.afterburnerModel = null;
        }

        this.afterburnerTexture = pojo.afterburnerTexture;

        if (pojo.afterburnerAnimations != null && this.afterburnerModel != null) {
            var animPojo = ClientAssetsManager.INSTANCE.getAnimation(pojo.afterburnerAnimations);
            var anims = animPojo
                    .map(animationPOJO -> BedrockAnimation.createAnimation(animationPOJO, this.afterburnerModel))
                    .orElse(List.of());
            var map = new HashMap<String, BedrockAnimation>();
            for (var anim : anims) {
                map.put(anim.getName(), anim);
            }
            this.afterburnerAnimations = map;
        } else {
            this.afterburnerAnimations = Map.of();
        }
    }

    public VehicleBedrockModel getAfterburnerModel() {
        return afterburnerModel;
    }

    public ResourceLocation getAfterburnerTexture() {
        return afterburnerTexture;
    }

    public Map<String, BedrockAnimation> getAfterburnerAnimations() {
        return afterburnerAnimations;
    }

}
