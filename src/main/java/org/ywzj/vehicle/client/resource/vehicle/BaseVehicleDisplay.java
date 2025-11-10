package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 基础载具效果配置实例
 */
public class BaseVehicleDisplay {

    protected BedrockModel model;
    protected ResourceLocation texture;
    // todo 临时存这，实际使用需要封装成状态机
    protected Map<String, BedrockAnimation> animations;

    protected Map<String, SoundEvent> soundEvents;

    protected BaseVehicleDisplay() {
    }

    /**
     * 需要在资源包重载期间构造，否则无法正常获取模型等资源
     * @param pojo 配置数据
     */
    public BaseVehicleDisplay(BaseVehicleDisplayPojo pojo) {
        var modelPojo = ClientAssetsManager.INSTANCE.getModel(pojo.model);
        this.model = modelPojo.map(BedrockModel::new).orElseThrow();

        this.texture = pojo.texture;

        var animationPojo = ClientAssetsManager.INSTANCE.getAnimation(pojo.animations);
        var animations = animationPojo.map(animationPOJO -> {
            return BedrockAnimation.createAnimation(animationPOJO, model);
        }).orElseThrow();

        var map = new HashMap<String, BedrockAnimation>();
        for (var anim : animations) {
            map.put(anim.getName(), anim);
        }
        this.animations = map;
    }
}
