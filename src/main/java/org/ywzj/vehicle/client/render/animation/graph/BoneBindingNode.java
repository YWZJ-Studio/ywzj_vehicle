package org.ywzj.vehicle.client.render.animation.graph;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.BoneIndexProvider;
import com.maydaymemory.mae.basic.*;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.TrackedVehicleContext;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;
import org.ywzj.vehicle.client.render.animation.context.WheeledVehicleContext;

import java.util.*;

/**
 * 骨骼绑定节点，直接绑定骨骼到车辆数据（轮子转动、部件旋转等），通过缓存绑定配置和骨骼索引来优化性能。
 * 适用于大量骨骼绑定的情况，避免每帧进行字符串查找和排序。
 */
public class BoneBindingNode implements PoseNode {
    
    private final List<CachedSpecialBinding> cachedSpecialBindings;
    private final List<CachedPartBinding> cachedPartBindings;
    private final int[] sortedBoneIndices;
    private final int[] indexToArrayPos;

    public BoneBindingNode(List<SpecialBinding> specialBindings, List<PartBinding> partBindings, BoneIndexProvider boneIndexProvider) {
        // Pre-compile special bindings with bone indices
        this.cachedSpecialBindings = compileSpecialBindings(
            specialBindings != null ? specialBindings : List.of(), 
            boneIndexProvider
        );
        
        // Pre-compile part bindings with bone indices
        this.cachedPartBindings = compilePartBindings(
            partBindings != null ? partBindings : List.of(), 
            boneIndexProvider
        );
        
        // Collect all unique bone indices and sort them
        Set<Integer> boneIndexSet = new HashSet<>();
        for (CachedSpecialBinding binding : cachedSpecialBindings) {
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
     * Compile special bindings: resolve bone names to indices once at construction time
     */
    private static List<CachedSpecialBinding> compileSpecialBindings(
            List<SpecialBinding> bindings, 
            BoneIndexProvider provider) {
        return bindings.stream()
            .map(binding -> {
                int[] indices = binding.bones.stream()
                    .mapToInt(provider::getIndex)
                    .filter(idx -> idx >= 0)
                    .toArray();
                return new CachedSpecialBinding(
                    indices,
                    binding.source,
                    parseAxis(binding.axis),
                    binding.multiplier,
                    binding.min,
                    binding.max,
                    binding.param
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
        if (axis == null) {
            return -1;
        }
        return switch (axis) {
            case "x" -> 0;
            case "y" -> 1;
            case "z" -> 2;
            default -> -1;
        };
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
        
        float value = getValueFromSource(context, binding.source, binding.param);
        
        // Apply multiplier
        value *= binding.multiplier;
        
        // Apply constraints
        if (binding.min != null) {
            value = Math.max(binding.min, value);
        }
        if (binding.max != null) {
            value = Math.min(binding.max, value);
        }
        
        // Apply to all bones
        if (binding.axis >= 0) {
            for (int boneIndex : binding.boneIndices) {
                builder.setRotation(boneIndex, binding.axis, value);
            }
        }
    }
    
    /**
     * Get value from data source based on context type
     */
    private float getValueFromSource(IAnimationInstance<?> context, String source, Float param) {
        Object ctx = context.getContext();
        float paramValue = param != null ? param : 0f;
        
        return switch (source) {
            // Wheeled vehicle sources
            case "wheel_rotation" -> {
                if (ctx instanceof WheeledVehicleContext wheeledCtx) {
                    yield wheeledCtx.getWheelDegrees(paramValue > 0 ? paramValue : 0.35f);
                }
                yield 0f;
            }
            case "steering_angle" -> {
                if (ctx instanceof WheeledVehicleContext wheeledCtx) {
                    yield wheeledCtx.getSteeringAngle();
                }
                yield 0f;
            }
            
            // Tracked vehicle sources
            case "left_wheel_rotation" -> {
                if (ctx instanceof TrackedVehicleContext trackedCtx) {
                    yield trackedCtx.getLeftWheelDegrees(paramValue > 0 ? paramValue : 0.35f);
                }
                yield 0f;
            }
            case "right_wheel_rotation" -> {
                if (ctx instanceof TrackedVehicleContext trackedCtx) {
                    yield trackedCtx.getRightWheelDegrees(paramValue > 0 ? paramValue : 0.35f);
                }
                yield 0f;
            }
            
            default -> 0f;
        };
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

    public static class SpecialBinding {
        public List<String> bones;
        public String source; // Data source: "wheel_rotation", "steering_angle", "left_wheel_rotation", "right_wheel_rotation"
        public String axis; // "x", "y", or "z" - rotation axis
        public float multiplier = 1.0f; // Multiplier for the value
        public Float min; // Minimum constraint (optional)
        public Float max; // Maximum constraint (optional)
        public Float param; // Parameter for the source (e.g., wheel radius)
    }

    public static class PartBinding {
        public String bone;
        public String part;
        public String rotationType; // "x", "y", or "z" - which rotation to read from part
        public String axis; // "x", "y", or "z" - which axis to apply to bone
        public boolean invert;
    }

    private record CachedSpecialBinding(
        int[] boneIndices,
        String source,
        int axis, // 0=x, 1=y, 2=z, -1=none
        float multiplier,
        Float min,
        Float max,
        Float param
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
