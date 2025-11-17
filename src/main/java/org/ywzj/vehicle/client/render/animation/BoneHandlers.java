package org.ywzj.vehicle.client.render.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class BoneHandlers {
    private final Map<String, IBoneHandler> boneHandlers = new HashMap<>();
    private final BedrockModel bedrockModel;

    public BoneHandlers(BedrockModel model) {
        this.bedrockModel = model;
    }

    public IBoneHandler getBone(String boneName) {
        return boneHandlers.get(boneName);
    }

    public void addSpecialBone(String boneName) {
        var bone = bedrockModel.getBone(boneName);
        if (bone == null) {
            return;
        }
        boneHandlers.put(boneName, new BedrockBoneHandler(bone));
    }
}
