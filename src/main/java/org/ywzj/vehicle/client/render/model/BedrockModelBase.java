package org.ywzj.vehicle.client.render.model;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

public class BedrockModelBase extends BedrockModel implements PositionableModel {
    private static final String FIXED_ORIGIN_NAME = "fixed";
    private static final String GROUND_ORIGIN_NAME = "ground";
    private static final String THIRD_PERSON_HAND_ORIGIN_NAME = "thirdperson_hand";
    private static final String FIRST_PERSON_HAND_ORIGIN_NAME = "firstperson_hand";
    private static final Vector3f DEFAULT_SCALE = new Vector3f(1, 1, 1);
    private final @Nullable PositionPointTransform fixedTransform;
    private final @Nullable PositionPointTransform groundTransform;
    private final @Nullable PositionPointTransform thirdPersonHandTransform;
    private final @Nullable PositionPointTransform firstPersonHandTransform;
    private final @Nullable TransformScale scales;

    public BedrockModelBase(BedrockModelPOJO pojo, @Nullable TransformScale scales) {
        super(pojo);
        fixedTransform = getTransform(getBone(FIXED_ORIGIN_NAME));
        groundTransform = getTransform(getBone(GROUND_ORIGIN_NAME));
        thirdPersonHandTransform = getTransform(getBone(THIRD_PERSON_HAND_ORIGIN_NAME));
        firstPersonHandTransform = getTransform(getBone(FIRST_PERSON_HAND_ORIGIN_NAME));
        this.scales = scales;
    }

    public void applyTransform(PoseStack poseStack, ItemDisplayContext ctx) {
        Vector3f scale = scales == null ? DEFAULT_SCALE : scales.fromTransformType(ctx);
        switch (ctx) {
            case FIXED -> applyOriginTransform(fixedTransform, scale, poseStack);
            case GROUND -> applyOriginTransform(groundTransform, scale, poseStack);
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                    applyOriginTransform(thirdPersonHandTransform, scale, poseStack);
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
                    applyOriginTransform(firstPersonHandTransform, scale, poseStack);
        }
    }

    private void applyOriginTransform(@Nullable PositionPointTransform transform, @Nullable Vector3f scaleVector, PoseStack poseStack) {
        if (transform != null) {
            Vector3f translation = transform.translation.mul(scaleVector, new Vector3f());
            poseStack.translate(translation.x, translation.y, translation.z);
            poseStack.mulPose(transform.rotation);
        }
        if (scaleVector != null) {
            poseStack.scale(scaleVector.x, scaleVector.y, scaleVector.z);
        }
    }

    private @Nullable PositionPointTransform getTransform(BedrockBone bone) {
        return Optional.ofNullable(bone)
                .map(origin -> origin.getGlobalTransform().invert())
                .map(matrix -> {
                    Vector3f translation = matrix.getTranslation(new Vector3f());
                    Quaternionf rotation = matrix.getNormalizedRotation(new Quaternionf());
                    return new PositionPointTransform(translation, rotation);
                })
                .orElse(null);
    }

    private static class PositionPointTransform {
        Vector3f translation;
        Quaternionf rotation;

        public PositionPointTransform(Vector3f translation, Quaternionf rotation) {
            this.translation = translation;
            this.rotation = rotation;
        }
    }
}
