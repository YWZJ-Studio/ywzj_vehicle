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

    private final AnimationRunner leftTrackRunner;
    private final AnimationRunner rightTrackRunner;

    public float leftAnimProgress;
    public float rightAnimProgress;

    public float leftCumulativeDisplacement = 0f;
    public float rightCumulativeDisplacement = 0f;

    public TrackAnimationInstance(List<BedrockAnimation> animations) {
        leftTrackRunner = new AnimationRunner(animations.get(0), new AnimationContext(animations.get(0).getSpecifiedEndTimeS()));
        rightTrackRunner = new AnimationRunner(animations.get(1), new AnimationContext(animations.get(1).getSpecifiedEndTimeS()));
    }

    /**
     * 根据左右履带线速度（米/秒）推进动画进度与累计位移。
     * animProgress 保持为模 1 的相位，用于循环动画。
     * cumulativeDisplacement 累加真实位移，用于计算轮子绝对旋转角度。
     */
    public void advanceProgress(float leftLinearSpeed, float rightLinearSpeed, float deltaSeconds, float moduleLength) {
        // 累计位移（米）
        leftCumulativeDisplacement += leftLinearSpeed * deltaSeconds;
        rightCumulativeDisplacement += rightLinearSpeed * deltaSeconds;

        // anim progress 按 moduleLength 的位移转换为相位（1 表示前进一个 moduleLength）
        float leftInc = leftLinearSpeed * deltaSeconds / moduleLength;
        float rightInc = rightLinearSpeed * deltaSeconds / moduleLength;

        leftAnimProgress = mod01(leftAnimProgress + leftInc);
        rightAnimProgress = mod01(rightAnimProgress + rightInc);
    }

    private float mod01(float v) {
        v = v % 1f;
        if (v < 0f) v += 1f;
        return v;
    }

    /**
     * 由累计位移计算轮子旋转角度（度）。
     * displacement 单位为米，可以为负（表示反向旋转）。
     */
    public float wheelRotation(float displacement, float radius) {
        if (radius <= 0f) return 0f;
        float rotations = displacement / (2f * (float) Math.PI * radius);
        return rotations * 360f;
    }

    public float leftWheelDegrees(float leftDriveRadius) {
        return wheelRotation(leftCumulativeDisplacement, leftDriveRadius);
    }

    public float rightWheelDegrees(float rightDriveRadius) {
        return wheelRotation(rightCumulativeDisplacement, rightDriveRadius);
    }

    public Pose evaluate() {
        leftTrackRunner.setProgress(leftAnimProgress);
        rightTrackRunner.setProgress(rightAnimProgress);
        Pose leftPose = leftTrackRunner.evaluate();
        Pose rightPose = rightTrackRunner.evaluate();
        return BLENDER.blend(leftPose, rightPose);
    }
}