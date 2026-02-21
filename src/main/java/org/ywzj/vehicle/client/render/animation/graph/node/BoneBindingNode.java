package org.ywzj.vehicle.client.render.animation.graph.node;

import com.maydaymemory.mae.basic.*;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 骨骼绑定节点，直接绑定骨骼到车辆数据（轮子转动、部件旋转等），通过缓存绑定配置和骨骼索引来优化性能。
 * 适用于大量骨骼绑定的情况，避免每帧进行字符串查找和排序。
 */
public class BoneBindingNode implements PoseNode {
    
    private final List<CachedSpecialBinding> cachedSpecialBindings;
    private final List<CachedPartBinding> cachedPartBindings;
    private final int[] sortedBoneIndices;
    private final int[] indexToArrayPos;

    public BoneBindingNode(List<CachedSpecialBinding> cachedSpecialBindings,
                           List<CachedPartBinding> cachedPartBindings) {
        this.cachedSpecialBindings = cachedSpecialBindings != null ? cachedSpecialBindings : List.of();
        this.cachedPartBindings = cachedPartBindings != null ? cachedPartBindings : List.of();
        
        // Collect all unique bone indices and sort them
        Set<Integer> boneIndexSet = new HashSet<>();
        for (CachedSpecialBinding binding : this.cachedSpecialBindings) {
            for (int idx : binding.boneIndices) {
                boneIndexSet.add(idx);
            }
        }
        for (CachedPartBinding binding : this.cachedPartBindings) {
            boneIndexSet.add(binding.boneIndex);
        }
        
        // Convert to sorted array for efficient lookup
        this.sortedBoneIndices = boneIndexSet.stream()
            .mapToInt(Integer::intValue)
            .sorted()
            .toArray();
        
        // Build index mapping once at construction time
        if (sortedBoneIndices.length > 0) {
            int maxIndex = sortedBoneIndices[sortedBoneIndices.length - 1];
            this.indexToArrayPos = new int[maxIndex + 1];
            Arrays.fill(indexToArrayPos, -1);
            
            for (int i = 0; i < sortedBoneIndices.length; i++) {
                indexToArrayPos[sortedBoneIndices[i]] = i;
            }
        } else {
            this.indexToArrayPos = new int[0];
        }
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        OptimizedPoseBuilder builder = new OptimizedPoseBuilder(sortedBoneIndices, indexToArrayPos);
        
        // Apply special bindings
        for (CachedSpecialBinding binding : cachedSpecialBindings) {
            applySpecialBinding(builder, context, binding);
        }

        if (context.getContext() instanceof VehicleContext<?> vehicleContext) {
            // Apply part bindings
            for (CachedPartBinding binding : cachedPartBindings) {
                applyPartBinding(builder, vehicleContext, binding);
            }
        }

        return builder.build();
    }

    private void applySpecialBinding(OptimizedPoseBuilder builder, IAnimationInstance<?> context, CachedSpecialBinding binding) {
        if (binding.source == null || binding.source.isEmpty()) {
            return;
        }

        float value = context.getContext().getBindingValue(binding.source, binding.param);

        value *= binding.multiplier;

        if (binding.min != null) {
            value = Math.max(binding.min, value);
        }
        if (binding.max != null) {
            value = Math.min(binding.max, value);
        }

        if (binding.axis >= 0) {
            for (int boneIndex : binding.boneIndices) {
                builder.setRotation(boneIndex, binding.axis, value);
            }
        }
    }

    private void applyPartBinding(OptimizedPoseBuilder builder, VehicleContext<?> context, CachedPartBinding binding) {
        float rotation = switch (binding.rotationType) {
            case "x" -> context.getPartXRot(binding.part);
            case "y" -> context.getPartYRot(binding.part);
            default -> 0f;
        };
        
        if (binding.invert) {
            rotation = -rotation;
        }
        
        builder.setRotation(binding.boneIndex, binding.axis, rotation);
    }

    public record CachedSpecialBinding(
        int[] boneIndices,
        String source,
        int axis, // 0=x, 1=y, 2=z, -1=none
        float multiplier,
        Float min,
        Float max,
        Float param
    ) {}

    public record CachedPartBinding(
        int boneIndex,
        String part,
        String rotationType,
        int axis, // 0=x, 1=y, 2=z
        boolean invert
    ) {}

    private static class OptimizedPoseBuilder {
        private static final ZYXBoneTransformFactory TRANSFORM_FACTORY = new ZYXBoneTransformFactory();

        private final int[] indexToArrayPos;
        private final BoneTransformData[] transforms;

        OptimizedPoseBuilder(int[] sortedBoneIndices, int[] indexToArrayPos) {
            this.indexToArrayPos = indexToArrayPos;
            this.transforms = new BoneTransformData[sortedBoneIndices.length];
        }

        void setRotation(int boneIndex, int axis, float angle) {
            if (boneIndex < 0 || boneIndex >= indexToArrayPos.length) {
                return;
            }
            
            int arrayPos = indexToArrayPos[boneIndex];
            if (arrayPos < 0) {
                return;
            }
            
            BoneTransformData data = transforms[arrayPos];
            if (data == null) {
                data = new BoneTransformData(boneIndex);
                transforms[arrayPos] = data;
            }

            switch (axis) {
                case 0 -> data.rotX = angle;
                case 1 -> data.rotY = angle;
                case 2 -> data.rotZ = angle;
            }
        }

        Pose build() {
            if (transforms.length == 0) {
                return DummyPose.INSTANCE;
            }

            ArrayPoseBuilder builder = new ArrayPoseBuilder();

            for (BoneTransformData data : transforms) {
                if (data != null) {
                    float rotXRad = (float) Math.toRadians(data.rotX);
                    float rotYRad = (float) Math.toRadians(data.rotY);
                    float rotZRad = (float) Math.toRadians(data.rotZ);
                    
                    BoneTransform transform = TRANSFORM_FACTORY.createBoneTransform(
                        data.boneIndex,
                        new Vector3f(0, 0, 0),
                        new Vector3f(rotXRad, rotYRad, rotZRad),
                        new Vector3f(1, 1, 1)
                    );
                    
                    builder.addBoneTransform(transform);
                }
            }
            
            return builder.toPose();
        }

        private static class BoneTransformData {
            final int boneIndex;
            float rotX, rotY, rotZ;

            BoneTransformData(int boneIndex) {
                this.boneIndex = boneIndex;
            }
        }
    }

}
