package org.ywzj.vehicle.client.render.animation.compiler;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.BoneIndexProvider;
import org.ywzj.vehicle.client.render.animation.graph.*;
import org.ywzj.vehicle.client.resource.animation.PoseNodeDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compiler for pose graph definitions.
 * Compiles JSON pose node definitions into runtime PoseNode objects.
 */
public class PoseGraphCompiler {

    /**
     * Resolver for script functions.
     * Takes a function name and returns a ScriptPoseNode, or null if not found.
     */
    @FunctionalInterface
    public interface ScriptNodeResolver {
        ScriptPoseNode resolve(String functionName);
    }

    private final ScriptNodeResolver scriptNodeResolver;

    private final BoneIndexProvider boneIndexProvider;

    public PoseGraphCompiler(ScriptNodeResolver scriptNodeResolver, BoneIndexProvider boneIndexProvider) {
        this.scriptNodeResolver = scriptNodeResolver;
        this.boneIndexProvider = boneIndexProvider;
    }

    /**
     * Compile a pose graph from root node definition
     */
    public PoseGraph compile(PoseNodeDefinition rootDefinition) {
        if (rootDefinition == null) {
            throw new IllegalArgumentException("Root node definition cannot be null");
        }

        PoseNode rootNode = compileNode(rootDefinition);
        return new PoseGraph(rootNode);
    }

    /**
     * Compile a single pose node
     */
    private PoseNode compileNode(PoseNodeDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Node definition cannot be null");
        }

        String type = definition.getType();
        if (type == null) {
            throw new IllegalArgumentException("Node type cannot be null");
        }

        return switch (type.toLowerCase()) {
            case "state_machine" -> compileStateMachineNode(definition);
            case "blend" -> compileBlendNode(definition);
            case "layered_blend" -> compileLayeredBlendNode(definition);
            case "additive" -> compileAdditiveNode(definition);
            case "script" -> compileScriptNode(definition);
            case "bone_binding" -> compileBoneBindingNode(definition, boneIndexProvider);
            case "track_animation" -> compileTrackAnimationNode();
            default -> throw new IllegalArgumentException("Unknown node type: " + type);
        };
    }

    /**
     * Compile state_machine node
     */
    private PoseNode compileStateMachineNode(PoseNodeDefinition definition) {
        String ref = definition.getRef();
        if (ref == null || ref.isEmpty()) {
            throw new IllegalArgumentException("state_machine node requires 'ref' field");
        }
        return new StateMachineNode(ref);
    }

    /**
     * Compile blend node
     */
    private PoseNode compileBlendNode(PoseNodeDefinition definition) {
        PoseNodeDefinition aDef = definition.getA();
        PoseNodeDefinition bDef = definition.getB();

        if (aDef == null || bDef == null) {
            throw new IllegalArgumentException("blend node requires 'a' and 'b' fields");
        }

        PoseNode nodeA = compileNode(aDef);
        PoseNode nodeB = compileNode(bDef);
        WeightSource weightSource = compileWeightSource(definition.getWeight());

        return new BlendNode(nodeA, nodeB, weightSource);
    }

    /**
     * Compile layered_blend node
     */
    private PoseNode compileLayeredBlendNode(PoseNodeDefinition definition) {
        PoseNodeDefinition baseDef = definition.getBase();
        if (baseDef == null) {
            throw new IllegalArgumentException("layered_blend node requires 'base' field");
        }

        PoseNode baseNode = compileNode(baseDef);
        List<LayeredBlendNode.Layer> layers = new ArrayList<>();

        if (definition.getLayers() != null) {
            for (PoseNodeDefinition.LayerDefinition layerDef : definition.getLayers()) {
                PoseNode layerPose = compileNode(layerDef.getPose());
                WeightSource weightSource = compileWeightSource(layerDef.getWeight());
                layers.add(new LayeredBlendNode.Layer(layerPose, weightSource));
            }
        }

        return new LayeredBlendNode(baseNode, layers);
    }

    /**
     * Compile additive node
     */
    private PoseNode compileAdditiveNode(PoseNodeDefinition definition) {
        PoseNodeDefinition baseDef = definition.getBase();
        PoseNodeDefinition addDef = definition.getAdd();

        if (baseDef == null || addDef == null) {
            throw new IllegalArgumentException("additive node requires 'base' and 'add' fields");
        }

        PoseNode baseNode = compileNode(baseDef);
        PoseNode addNode = compileNode(addDef);

        return new AdditiveNode(baseNode, addNode);
    }

    /**
     * Compile script node
     */
    private PoseNode compileScriptNode(PoseNodeDefinition definition) {
        String functionName = definition.getFunction();
        if (functionName == null || functionName.isEmpty()) {
            throw new IllegalArgumentException("script node requires 'function' field");
        }

        if (scriptNodeResolver == null) {
            throw new IllegalStateException("Script node resolver not available");
        }

        ScriptPoseNode node = scriptNodeResolver.resolve(functionName);
        if (node == null) {
            throw new IllegalArgumentException("Script function not found: " + functionName);
        }

        return node;
    }

    /**
     * Compile bone_binding node
     */
    private PoseNode compileBoneBindingNode(PoseNodeDefinition definition, BoneIndexProvider boneIndexProvider) {
        List<BoneBindingNode.WheelBinding> wheelBindings = new ArrayList<>();
        List<BoneBindingNode.PartBinding> partBindings = new ArrayList<>();

        // Compile wheel bindings
        if (definition.getWheelBindings() != null) {
            for (PoseNodeDefinition.WheelBindingDefinition def : definition.getWheelBindings()) {
                BoneBindingNode.WheelBinding binding = new BoneBindingNode.WheelBinding();
                binding.bones = def.getBones();
                binding.side = def.getSide();
                binding.radius = def.getRadius();
                binding.axis = def.getAxis();
                wheelBindings.add(binding);
            }
        }

        // Compile part bindings
        if (definition.getPartBindings() != null) {
            for (PoseNodeDefinition.PartBindingDefinition def : definition.getPartBindings()) {
                BoneBindingNode.PartBinding binding = new BoneBindingNode.PartBinding();
                binding.bone = def.getBone();
                binding.part = def.getPart();
                binding.rotationType = def.getRotationType();
                binding.axis = def.getAxis();
                binding.invert = def.isInvert();
                partBindings.add(binding);
            }
        }

        return new BoneBindingNode(wheelBindings, partBindings, boneIndexProvider);
    }

    /**
     * Compile track_animation node
     */
    private PoseNode compileTrackAnimationNode() {
        return new TrackAnimationNode();
    }

    /**
     * Compile weight source from JSON value
     */
    private WeightSource compileWeightSource(Object weightValue) {
        if (weightValue == null) {
            return new WeightSource.Static(1.0f);
        }

        // Static float value
        if (weightValue instanceof Number) {
            return new WeightSource.Static(((Number) weightValue).floatValue());
        }

        // Parameter reference
        if (weightValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> weightMap = (Map<String, Object>) weightValue;
            String param = (String) weightMap.get("param");
            if (param != null) {
                return new WeightSource.Parameter(param, 0.0f);
            }
        }

        throw new IllegalArgumentException("Invalid weight value: " + weightValue);
    }
}
