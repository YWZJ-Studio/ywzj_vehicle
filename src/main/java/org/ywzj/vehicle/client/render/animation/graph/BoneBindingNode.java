package org.ywzj.vehicle.client.render.animation.graph;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.BoneIndexProvider;
import com.maydaymemory.mae.basic.*;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.TrackedVehicleContext;
import org.ywzj.vehicle.client.render.animation.context.VehicleContext;

import java.util.List;
import java.util.Objects;

public class BoneBindingNode implements PoseNode {
    
    private final List<CompiledWheelBinding> compiledWheelBindings;
    private final List<CompiledPartBinding> compiledPartBindings;
    private final int maxBoneIndex;

    public BoneBindingNode(List<WheelBinding> wheelBindings, List<PartBinding> partBindings, BoneIndexProvider boneIndexProvider) {
        // Pre-compile wheel bindings with bone indices
        this.compiledWheelBindings = compileWheelBindings(
            wheelBindings != null ? wheelBindings : List.of(), 
            boneIndexProvider
        );
        
        // Pre-compile part bindings with bone indices
        this.compiledPartBindings = compilePartBindings(
            partBindings != null ? partBindings : List.of(), 
            boneIndexProvider
        );
        
        // Calculate max bone index for array sizing
        int max = -1;
        for (CompiledWheelBinding binding : compiledWheelBindings) {
            for (int idx : binding.boneIndices) {
                if (idx > max) max = idx;
            }
        }
        for (CompiledPartBinding binding : compiledPartBindings) {
            if (binding.boneIndex > max) max = binding.boneIndex;
        }
        this.maxBoneIndex = max;
    }

    /**
     * Compile wheel bindings: resolve bone names to indices once at construction time
     */
    private static List<CompiledWheelBinding> compileWheelBindings(
            List<WheelBinding> bindings, 
            BoneIndexProvider provider) {
        return bindings.stream()
            .map(binding -> {
                int[] indices = binding.bones.stream()
                    .mapToInt(provider::getIndex)
                    .filter(idx -> idx >= 0)
                    .toArray();
                return new CompiledWheelBinding(
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
    private static List<CompiledPartBinding> compilePartBindings(
            List<PartBinding> bindings, 
            BoneIndexProvider provider) {
        return bindings.stream()
            .map(binding -> {
                int boneIndex = provider.getIndex(binding.bone);
                if (boneIndex < 0) {
                    return null; // Skip invalid bones
                }
                return new CompiledPartBinding(
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
        OptimizedPoseBuilder builder = new OptimizedPoseBuilder(maxBoneIndex);
        
        // Apply wheel bindings
        if (context.getContext() instanceof TrackedVehicleContext trackedContext) {
            for (CompiledWheelBinding binding : compiledWheelBindings) {
                applyWheelBinding(builder, trackedContext, binding);
            }
        }

        if (context.getContext() instanceof VehicleContext<?> vehicleContext) {
            // Apply part bindings
            for (CompiledPartBinding binding : compiledPartBindings) {
                applyPartBinding(builder, vehicleContext, binding);
            }
        }

        return builder.build();
    }

    private void applyWheelBinding(OptimizedPoseBuilder builder, TrackedVehicleContext context, CompiledWheelBinding binding) {
        float angle = switch (binding.side) {
            case "left" -> context.getLeftWheelDegrees(binding.radius);
            case "right" -> context.getRightWheelDegrees(binding.radius);
            default -> 0f;
        };
        
        for (int boneIndex : binding.boneIndices) {
            builder.setRotation(boneIndex, binding.axis, angle);
        }
    }

    private void applyPartBinding(OptimizedPoseBuilder builder, VehicleContext<?> context, CompiledPartBinding binding) {
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

    /**
     * Configuration for wheel bone bindings
     */
    public static class WheelBinding {
        public List<String> bones;
        public String side; // "left" or "right"
        public float radius;
        public String axis; // "x", "y", or "z"
    }

    /**
     * Configuration for part rotation bindings
     */
    public static class PartBinding {
        public String bone;
        public String part;
        public String rotationType; // "x", "y", or "z" - which rotation to read from part
        public String axis; // "x", "y", or "z" - which axis to apply to bone
        public boolean invert;
    }

    /**
     * Compiled wheel binding with pre-resolved bone indices
     */
    private record CompiledWheelBinding(
        int[] boneIndices,
        String side,
        float radius,
        int axis // 0=x, 1=y, 2=z
    ) {}

    /**
     * Compiled part binding with pre-resolved bone index
     */
    private record CompiledPartBinding(
        int boneIndex,
        String part,
        String rotationType,
        int axis, // 0=x, 1=y, 2=z
        boolean invert
    ) {}

    /**
     * Optimized pose builder that uses pre-sorted array instead of TreeMap.
     * Eliminates per-frame sorting overhead and bone name lookup.
     */
    private static class OptimizedPoseBuilder {
        private static final ZYXBoneTransformFactory TRANSFORM_FACTORY = new ZYXBoneTransformFactory();
        
        private final BoneTransformData[] transforms;
        private int count = 0;

        OptimizedPoseBuilder(int maxBoneIndex) {
            // Pre-allocate array sized to max bone index
            this.transforms = new BoneTransformData[maxBoneIndex + 1];
        }

        void setRotation(int boneIndex, int axis, float angle) {
            if (boneIndex < 0 || boneIndex >= transforms.length) {
                return;
            }
            
            BoneTransformData data = transforms[boneIndex];
            if (data == null) {
                data = new BoneTransformData(boneIndex);
                transforms[boneIndex] = data;
                count++;
            }
            
            // Set rotation on the specified axis
            switch (axis) {
                case 0 -> data.rotX = angle;
                case 1 -> data.rotY = angle;
                case 2 -> data.rotZ = angle;
            }
        }

        Pose build() {
            if (count == 0) {
                return DummyPose.INSTANCE;
            }

            ArrayPoseBuilder builder = new ArrayPoseBuilder();
            
            // Iterate in index order (already sorted by array structure)
            for (BoneTransformData data : transforms) {
                if (data != null) {
                    // Convert degrees to radians
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
