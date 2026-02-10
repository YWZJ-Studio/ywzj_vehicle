package org.ywzj.vehicle.client.render.animation.graph;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.BoneIndexProvider;
import com.maydaymemory.mae.basic.*;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.TrackedVehicleContext;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;

import java.util.*;

/**
 * 骨骼绑定节点，直接绑定骨骼到车辆数据（轮子转动、部件旋转等），通过缓存绑定配置和骨骼索引来优化性能。
 * 适用于大量骨骼绑定的情况，避免每帧进行字符串查找和排序。
 */
public class BoneBindingNode implements PoseNode {
    
    private final List<CachedWheelBinding> cachedWheelBindings;
    private final List<CachedPartBinding> cachedPartBindings;
    private final int[] sortedBoneIndices;
    private final int[] indexToArrayPos;

    public BoneBindingNode(List<WheelBinding> wheelBindings, List<PartBinding> partBindings, BoneIndexProvider boneIndexProvider) {
        // Pre-compile wheel bindings with bone indices
        this.cachedWheelBindings = compileWheelBindings(
            wheelBindings != null ? wheelBindings : List.of(), 
            boneIndexProvider
        );
        
        // Pre-compile part bindings with bone indices
        this.cachedPartBindings = compilePartBindings(
            partBindings != null ? partBindings : List.of(), 
            boneIndexProvider
        );
        
        // Collect all unique bone indices and sort them
        Set<Integer> boneIndexSet = new HashSet<>();
        for (CachedWheelBinding binding : cachedWheelBindings) {
            for (int idx : binding.boneIndices) {
                boneIndexSet.add(idx);
            }
        }
        for (CachedPartBinding binding : cachedPartBindings) {
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

    /**
     * Compile wheel bindings: resolve bone names to indices once at construction time
     */
    private static List<CachedWheelBinding> compileWheelBindings(
            List<WheelBinding> bindings, 
            BoneIndexProvider provider) {
        return bindings.stream()
            .map(binding -> {
                int[] indices = binding.bones.stream()
                    .mapToInt(provider::getIndex)
                    .filter(idx -> idx >= 0)
                    .toArray();
                return new CachedWheelBinding(
                    indices,
                    binding.side,
                    binding.radius,
                    parseAxis(binding.axis)
                );
            })
            .toList();
    }

    /**
     * Compile part bindings: resolve bone names to indices once at construction time
     */
    private static List<CachedPartBinding> compilePartBindings(
            List<PartBinding> bindings, 
            BoneIndexProvider provider) {
        return bindings.stream()
            .map(binding -> {
                int boneIndex = provider.getIndex(binding.bone);
                if (boneIndex < 0) {
                    return null; // Skip invalid bones
                }
                return new CachedPartBinding(
                    boneIndex,
                    binding.part,
                    binding.rotationType,
                    parseAxis(binding.axis),
                    binding.invert
                );
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private static int parseAxis(String axis) {
        return switch (axis) {
            case "x" -> 0;
            case "y" -> 1;
            case "z" -> 2;
            default -> 0;
        };
    }

    @Override
    public Pose evaluate(IAnimationInstance<?> context) {
        OptimizedPoseBuilder builder = new OptimizedPoseBuilder(sortedBoneIndices, indexToArrayPos);
        
        // Apply wheel bindings
        if (context.getContext() instanceof TrackedVehicleContext trackedContext) {
            for (CachedWheelBinding binding : cachedWheelBindings) {
                applyWheelBinding(builder, trackedContext, binding);
            }
        }

        if (context.getContext() instanceof VehicleContext<?> vehicleContext) {
            // Apply part bindings
            for (CachedPartBinding binding : cachedPartBindings) {
                applyPartBinding(builder, vehicleContext, binding);
            }
        }

        return builder.build();
    }

    private void applyWheelBinding(OptimizedPoseBuilder builder, TrackedVehicleContext context, CachedWheelBinding binding) {
        float angle = switch (binding.side) {
            case "left" -> context.getLeftWheelDegrees(binding.radius);
            case "right" -> context.getRightWheelDegrees(binding.radius);
            default -> 0f;
        };
        
        for (int boneIndex : binding.boneIndices) {
            builder.setRotation(boneIndex, binding.axis, angle);
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

    public static class WheelBinding {
        public List<String> bones;
        public String side; // "left" or "right"
        public float radius;
        public String axis; // "x", "y", or "z"
    }

    public static class PartBinding {
        public String bone;
        public String part;
        public String rotationType; // "x", "y", or "z" - which rotation to read from part
        public String axis; // "x", "y", or "z" - which axis to apply to bone
        public boolean invert;
    }

    private record CachedWheelBinding(
        int[] boneIndices,
        String side,
        float radius,
        int axis // 0=x, 1=y, 2=z
    ) {}

    private record CachedPartBinding(
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
