package org.ywzj.vehicle.bedrock.animation;

import com.maydaymemory.mae.basic.Animation;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.CubicHermiteInterpolatorBlender;
import com.maydaymemory.mae.blend.InterpolatorBlender;
import com.maydaymemory.mae.blend.SimpleInterpolatorBlender;
import com.maydaymemory.mae.control.Tickable;
import com.maydaymemory.mae.control.misc.AnimationVelocityEstimatorNode;
import com.maydaymemory.mae.control.misc.RealtimeVelocityEstimatorNode;
import com.maydaymemory.mae.control.runner.AnimationRunner;

public class TestAnimationContext implements Tickable {
    public static final CubicHermiteInterpolatorBlender blender = new CubicHermiteInterpolatorBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    public static final InterpolatorBlender blender2 = new SimpleInterpolatorBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    private static final String[] ANIMATIONS = new String[]{
            "治疗魔法", "待机初始帧", "跑步", "抓取", "抓取 未命中", "跑步—>冲刺轻击",
            "走路", "待机", "待机—>防空技能", "待机—>蹲击", "待机—>推击", "待机—>轻击",
            "待机—>重击", "待机—>暗能量球", "待机—>格林爆破", "待机—>治疗魔法蓄力",
            "治疗魔法蓄力ing", "治疗魔法蓄力—>被打断（待机）", "治疗魔法蓄力—>治疗魔法释放"
    };
    private static final Animation[] ANIMATIONS_CACHE = new Animation[ANIMATIONS.length];

    private int currentAnimationIndex = 0;
    public boolean needTransition = false;

    public final RealtimeVelocityEstimatorNode velocityEstimatorNode;
    public final AnimationVelocityEstimatorNode targetVelocityEstimatorNode;

    private AnimationRunner runner;
    private Pose velocitySnapshot;

    public TestAnimationContext(RealtimeVelocityEstimatorNode velocityEstimatorNode) {
        this.velocityEstimatorNode = velocityEstimatorNode;
        this.targetVelocityEstimatorNode = new AnimationVelocityEstimatorNode(ArrayPoseBuilder::new);
        targetVelocityEstimatorNode.getAnimationSlot().connect(this::currentAnimation);
        targetVelocityEstimatorNode.getTimeSlot().connect(() -> getRunner().getProgressInSecond());
    }

    public Animation nextAnimation() {
        currentAnimationIndex = (currentAnimationIndex + 1) % ANIMATIONS.length;
        return fromIndex(currentAnimationIndex);
    }

    public Animation currentAnimation() {
        return fromIndex(currentAnimationIndex);
    }

    private Animation fromIndex(int index) {
        if(ANIMATIONS_CACHE[index] == null) {
            ANIMATIONS_CACHE[index] = BedrockAnimationLoader.getAnimations(BedrockAnimationLoader.TEST_ANIMATION).get(ANIMATIONS[index]);
        }
        return ANIMATIONS_CACHE[index];
    }

    public void snapshotVelocity() {
        velocitySnapshot = velocityEstimatorNode.getVelocityPose();
    }

    public Pose getVelocitySnapshot() {
        return velocitySnapshot;
    }

    public AnimationRunner getRunner() {
        return runner;
    }

    public void setRunner(AnimationRunner runner) {
        this.runner = runner;
    }

    @Override
    public void tick() {
        if (runner != null) {
            runner.tick();
        }
    }
}
