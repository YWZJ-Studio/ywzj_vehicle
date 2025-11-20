package org.ywzj.vehicle.api.scripts.bedrock;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import org.joml.Quaternionf;

public class BedrockBoneHandler implements IBoneHandler {
    private final BedrockBone bone;

    public BedrockBoneHandler(BedrockBone bone) {
        this.bone = bone;
    }

    @Override
    public void rotate(float xRot, float yRot, float zRot) {
        Quaternionf q = new Quaternionf().rotateZYX(
                (float) Math.toRadians(zRot),
                (float) Math.toRadians(yRot),
                (float) Math.toRadians(xRot)
        );
        bone.rotation.mul(q);
    }
}
