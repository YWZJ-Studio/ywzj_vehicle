package org.ywzj.vehicle.client.resource;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.ywzj.vehicle.api.scripts.ScriptContextFactory;
import org.ywzj.vehicle.client.render.animation.compiler.*;
import org.ywzj.vehicle.client.render.animation.controller.AnimationController;
import org.ywzj.vehicle.client.render.animation.graph.BoneMask;
import org.ywzj.vehicle.client.resource.animation.AnimationControllerDefinition;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resource reload listener for animation controllers.
 * Compiles controllers with inline state machines during resource loading.
 * Scripts are loaded at controller level and shared by all state machines.
 */
public class AnimationControllerManager extends SimplePreparableReloadListener<Map<ResourceLocation, AnimationController<?>>> {

    private Map<ResourceLocation, AnimationController<?>> controllers = Map.of();
    private final AnimationControllerDefinitionManager definitionManager;
    private final ScriptContextFactory scriptContextFactory;

    public AnimationControllerManager(AnimationControllerDefinitionManager definitionManager,
                                     ScriptContextFactory scriptContextFactory) {
        this.definitionManager = definitionManager;
        this.scriptContextFactory = scriptContextFactory;
    }

    @Override
    protected Map<ResourceLocation, AnimationController<?>> prepare(
            ResourceManager manager, ProfilerFiller profiler
    ) {
        Map<ResourceLocation, AnimationController<?>> result = new HashMap<>();

        // Register bone masks (TODO: load from config or resource packs)
        Map<String, BoneMask> boneMasks = createDefaultBoneMasks();

        // Get all controller definitions
        Map<ResourceLocation, AnimationControllerDefinition> definitions = definitionManager.getData();

        for (Map.Entry<ResourceLocation, AnimationControllerDefinition> entry : definitions.entrySet()) {
            ResourceLocation controllerId = entry.getKey();
            AnimationControllerDefinition definition = entry.getValue();

            try {
                // Load external script if specified
                String scriptContent = null;
                if (definition.getScript() != null) {
                    scriptContent = loadScript(manager, definition.getScript());
                }

                // Create compilers with script context
                ScriptCompiler scriptCompiler = new ScriptCompiler(scriptContextFactory);

                // Pre-load the external script into the compiler if present
                if (scriptContent != null && !scriptContent.isEmpty()) {
                    scriptCompiler.loadExternalScript(scriptContent);
                }

                ActionCompiler actionCompiler = new ActionCompiler(scriptCompiler);
                ConditionCompiler conditionCompiler = new ConditionCompiler(scriptCompiler);
                StateMachineCompiler stateMachineCompiler = new StateMachineCompiler(
                        scriptCompiler, actionCompiler, conditionCompiler);
                AnimationControllerCompiler controllerCompiler = new AnimationControllerCompiler(
                        stateMachineCompiler, boneMasks);

                // Compile the controller with inline state machines
                AnimationController<?> controller = controllerCompiler.compile(definition);
                result.put(controllerId, controller);

            } catch (Exception e) {
                System.err.println("Failed to compile animation controller: " + controllerId);
                e.printStackTrace();
            }
        }

        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, AnimationController<?>> prepared,
                        ResourceManager manager, ProfilerFiller profiler) {
        this.controllers = ImmutableMap.copyOf(prepared);
    }

    /**
     * Load external script file
     */
    private String loadScript(ResourceManager manager, ResourceLocation scriptLocation) {
        try {
            Optional<Resource> resource = manager.getResource(scriptLocation);
            if (resource.isPresent()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load script: " + scriptLocation);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create default bone masks
     * TODO: Load from config or resource packs
     */
    private Map<String, BoneMask> createDefaultBoneMasks() {
        Map<String, BoneMask> masks = new HashMap<>();
        // Add default masks here
        // Example: masks.put("upper_body", new BoneMask("upper_body", Set.of("spine", "chest", "head", "arm_left", "arm_right")));
        return masks;
    }

    /**
     * Get a compiled animation controller by ID
     */
    public Optional<AnimationController<?>> getController(ResourceLocation id) {
        return Optional.ofNullable(controllers.get(id));
    }
}
