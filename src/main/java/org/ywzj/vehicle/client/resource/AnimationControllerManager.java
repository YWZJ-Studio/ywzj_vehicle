package org.ywzj.vehicle.client.resource;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.ywzj.vehicle.client.resource.animation.AnimationControllerDefinition;

import java.util.Map;
import java.util.Optional;

/**
 * Resource reload listener for animation controller definitions.
 * Only loads definitions, compilation happens during display loading.
 */
public class AnimationControllerManager extends SimplePreparableReloadListener<Map<ResourceLocation, AnimationControllerDefinition>> {

    private Map<ResourceLocation, AnimationControllerDefinition> definitions = Map.of();
    private final AnimationControllerDefinitionManager definitionManager;

    public AnimationControllerManager(AnimationControllerDefinitionManager definitionManager) {
        this.definitionManager = definitionManager;
    }

    @Override
    protected Map<ResourceLocation, AnimationControllerDefinition> prepare(
            ResourceManager manager, ProfilerFiller profiler
    ) {
        return definitionManager.getAllData();
    }

    @Override
    protected void apply(Map<ResourceLocation, AnimationControllerDefinition> prepared,
                        ResourceManager manager, ProfilerFiller profiler) {
        this.definitions = ImmutableMap.copyOf(prepared);
    }

    /**
     * Get animation controller definition by ID
     */
    public Optional<AnimationControllerDefinition> getDefinition(ResourceLocation id) {
        return Optional.ofNullable(definitions.get(id));
    }

    /**
     * Get all definitions
     */
    public Map<ResourceLocation, AnimationControllerDefinition> getAllDefinitions() {
        return definitions;
    }
}
