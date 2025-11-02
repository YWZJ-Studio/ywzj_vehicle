package org.ywzj.vehicle.client.render.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;

import java.util.List;

public class TrackAnimationInstance {
    private static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    private AnimationRunner leftTrackRunner;
    private AnimationRunner rightTrackRunner;

    public float leftAnimProgress;
    public float rightAnimProgress;

    public TrackAnimationInstance(List<BedrockAnimation> animations) {
        leftTrackRunner = new AnimationRunner(animations.get(0), new AnimationContext(animations.get(0).getSpecifiedEndTimeS()));
        rightTrackRunner = new AnimationRunner(animations.get(1), new AnimationContext(animations.get(1).getSpecifiedEndTimeS()));
    }

    public Pose evaluate() {
        leftTrackRunner.setProgress(leftAnimProgress);
        rightTrackRunner.setProgress(rightAnimProgress);
        Pose leftPose = leftTrackRunner.evaluate();
        Pose rightPose = rightTrackRunner.evaluate();
        return BLENDER.blend(leftPose, rightPose);
    }
}