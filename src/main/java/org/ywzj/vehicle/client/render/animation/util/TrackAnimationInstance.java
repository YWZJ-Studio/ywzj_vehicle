package org.ywzj.vehicle.client.render.animation.util;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;

import java.util.List;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.MERGE_BLENDER;

/**
 * 一个简单的履带动画实例，包含左右履带动画的进度与累计位移计算
 */
public class TrackAnimationInstance {
    private final AnimationRunner leftTrackRunner;
    private final AnimationRunner rightTrackRunner;

    public float leftAnimProgress;
    public float rightAnimProgress;

    public float leftCumulativeDisplacement = 0f;
    public float rightCumulativeDisplacement = 0f;

    private float moduleLength = 0.25f;
    private float trackWidth = 3f;

    public TrackAnimationInstance(BedrockAnimation left, BedrockAnimation right) {
        leftTrackRunner = new AnimationRunner(left, new AnimationContext(left.getSpecifiedEndTimeS()));
        rightTrackRunner = new AnimationRunner(right, new AnimationContext(right.getSpecifiedEndTimeS()));
    }

    /**
     * 根据左右履带线速度（米/秒）推进动画进度与累计位移。
     * animProgress 保持为模 1 的相位，用于循环动画。
     * cumulativeDisplacement 累加真实位移，用于计算轮子绝对旋转角度。
     */
    public void advanceProgress(float leftLinearSpeed, float rightLinearSpeed, float deltaSeconds) {
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
        return MERGE_BLENDER.blend(List.of(leftPose, rightPose));
    }

    public float getTrackWidth() {
        return trackWidth;
    }

    public float getModuleLength() {
        return moduleLength;
    }

    public void setTrackWidth(float trackWidth) {
        this.trackWidth = trackWidth;
    }

    public void setModuleLength(float moduleLength) {
        this.moduleLength = moduleLength;
    }
}