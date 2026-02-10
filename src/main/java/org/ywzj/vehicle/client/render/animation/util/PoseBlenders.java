package org.ywzj.vehicle.client.render.animation.util;

import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleInterpolatorBlender;

public class PoseBlenders {
    public static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    public static final SimpleInterpolatorBlender INTERPOLATOR_BLENDER = new SimpleInterpolatorBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    public static final NoAllocMergeBlender MERGE_BLENDER = new NoAllocMergeBlender();

}
