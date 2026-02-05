package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Animation state machine for managing complex animation transitions and blending.
 * Supports state-based animation control with automatic transitions, blending, and priorities.
 */
public class AnimationStateMachine {

    private final Map<String, AnimationState> states = new HashMap<>();
    private final Map<String, BedrockAnimation> animations;
    private AnimationState currentState;
    private AnimationState previousState;
    private float transitionProgress = 1.0f;
    private float transitionDuration = 0.0f;
    private long lastUpdateTime = System.currentTimeMillis();

    /**
     * Creates a new animation state machine.
     * 
     * @param animations Map of animation name to BedrockAnimation
     */
    public AnimationStateMachine(Map<String, BedrockAnimation> animations) {
        this.animations = animations;
    }

    /**
     * Defines a new animation state.
     * 
     * @param stateName Unique state identifier
     * @return Builder for configuring the state
     */
    public StateBuilder defineState(String stateName) {
        return new StateBuilder(stateName);
    }

    /**
     * Transitions to a new state.
     * 
     * @param stateName Target state name
     * @param transitionTime Transition duration in seconds
     * @return true if transition was successful
     */
    public boolean transitionTo(String stateName, float transitionTime) {
        AnimationState targetState = states.get(stateName);
        if (targetState == null) {
            return false;
        }

        // Check if transition is allowed
        if (currentState != null && !currentState.canTransitionTo(targetState)) {
            return false;
        }

        previousState = currentState;
        currentState = targetState;
        transitionDuration = transitionTime;
        transitionProgress = 0.0f;
        
        if (currentState != null) {
            currentState.onEnter();
        }
        
        return true;
    }

    /**
     * Immediately switches to a state without transition.
     * 
     * @param stateName Target state name
     * @return true if switch was successful
     */
    public boolean switchTo(String stateName) {
        return transitionTo(stateName, 0.0f);
    }

    /**
     * Updates the state machine and returns active animations with blend weights.
     * 
     * @return Map of animation name to blend weight (0.0 to 1.0)
     */
    public Map<String, Float> update() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        Map<String, Float> activeAnimations = new HashMap<>();

        // Update transition progress
        if (transitionProgress < 1.0f && transitionDuration > 0) {
            transitionProgress += deltaTime / transitionDuration;
            transitionProgress = Math.min(1.0f, transitionProgress);
        }

        // Blend between previous and current state
        if (previousState != null && transitionProgress < 1.0f) {
            float previousWeight = 1.0f - transitionProgress;
            float currentWeight = transitionProgress;

            // Add previous state animations
            for (String animName : previousState.animationNames) {
                activeAnimations.merge(animName, previousWeight * previousState.blendWeight, Float::sum);
            }

            // Add current state animations
            if (currentState != null) {
                for (String animName : currentState.animationNames) {
                    activeAnimations.merge(animName, currentWeight * currentState.blendWeight, Float::sum);
                }
            }
        } else if (currentState != null) {
            // Only current state is active
            for (String animName : currentState.animationNames) {
                activeAnimations.put(animName, currentState.blendWeight);
            }
        }

        // Check for automatic transitions
        if (currentState != null && transitionProgress >= 1.0f) {
            for (AnimationTransition transition : currentState.transitions) {
                if (transition.condition.test(this)) {
                    transitionTo(transition.targetState, transition.transitionTime);
                    break;
                }
            }
        }

        return activeAnimations;
    }

    /**
     * Gets the current state name.
     */
    @Nullable
    public String getCurrentStateName() {
        return currentState != null ? currentState.name : null;
    }

    /**
     * Gets the current state.
     */
    @Nullable
    public AnimationState getCurrentState() {
        return currentState;
    }

    /**
     * Checks if currently transitioning between states.
     */
    public boolean isTransitioning() {
        return transitionProgress < 1.0f;
    }

    /**
     * Gets the transition progress (0.0 to 1.0).
     */
    public float getTransitionProgress() {
        return transitionProgress;
    }

    /**
     * Sets a custom variable for use in transition conditions.
     */
    private final Map<String, Object> variables = new HashMap<>();

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public <T> T getVariable(String key, T defaultValue) {
        Object value = variables.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Animation state definition.
     */
    public static class AnimationState {
        private final String name;
        private final List<String> animationNames;
        private final float blendWeight;
        private final int priority;
        private final List<AnimationTransition> transitions = new ArrayList<>();
        private final Set<String> allowedTransitions = new HashSet<>();
        private Runnable onEnterCallback;
        private Runnable onExitCallback;

        private AnimationState(String name, List<String> animationNames, float blendWeight, int priority) {
            this.name = name;
            this.animationNames = animationNames;
            this.blendWeight = blendWeight;
            this.priority = priority;
        }

        public String getName() {
            return name;
        }

        public List<String> getAnimationNames() {
            return animationNames;
        }

        public float getBlendWeight() {
            return blendWeight;
        }

        public int getPriority() {
            return priority;
        }

        private void onEnter() {
            if (onEnterCallback != null) {
                onEnterCallback.run();
            }
        }

        private void onExit() {
            if (onExitCallback != null) {
                onExitCallback.run();
            }
        }

        private boolean canTransitionTo(AnimationState target) {
            // Can always transition if no restrictions
            if (allowedTransitions.isEmpty()) {
                return true;
            }
            return allowedTransitions.contains(target.name);
        }
    }

    /**
     * Automatic transition definition.
     */
    public static class AnimationTransition {
        private final String targetState;
        private final Predicate<AnimationStateMachine> condition;
        private final float transitionTime;

        private AnimationTransition(String targetState, Predicate<AnimationStateMachine> condition, float transitionTime) {
            this.targetState = targetState;
            this.condition = condition;
            this.transitionTime = transitionTime;
        }
    }

    /**
     * Builder for creating animation states.
     */
    public class StateBuilder {
        private final String stateName;
        private final List<String> animationNames = new ArrayList<>();
        private float blendWeight = 1.0f;
        private int priority = 0;
        private Runnable onEnterCallback;
        private Runnable onExitCallback;
        private final Set<String> allowedTransitions = new HashSet<>();

        private StateBuilder(String stateName) {
            this.stateName = stateName;
        }

        /**
         * Adds an animation to this state.
         */
        public StateBuilder withAnimation(String animationName) {
            this.animationNames.add(animationName);
            return this;
        }

        /**
         * Adds multiple animations to this state.
         */
        public StateBuilder withAnimations(String... animationNames) {
            this.animationNames.addAll(Arrays.asList(animationNames));
            return this;
        }

        /**
         * Sets the blend weight for this state's animations.
         */
        public StateBuilder withBlendWeight(float weight) {
            this.blendWeight = weight;
            return this;
        }

        /**
         * Sets the priority for this state (higher priority states can interrupt lower priority).
         */
        public StateBuilder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets a callback to run when entering this state.
         */
        public StateBuilder onEnter(Runnable callback) {
            this.onEnterCallback = callback;
            return this;
        }

        /**
         * Sets a callback to run when exiting this state.
         */
        public StateBuilder onExit(Runnable callback) {
            this.onExitCallback = callback;
            return this;
        }

        /**
         * Restricts transitions to only specified states.
         */
        public StateBuilder allowTransitionsTo(String... stateNames) {
            this.allowedTransitions.addAll(Arrays.asList(stateNames));
            return this;
        }

        /**
         * Adds an automatic transition to another state when condition is met.
         */
        public StateBuilder addTransition(String targetState, Predicate<AnimationStateMachine> condition, float transitionTime) {
            AnimationState state = states.computeIfAbsent(stateName, k -> 
                new AnimationState(stateName, animationNames, blendWeight, priority));
            state.transitions.add(new AnimationTransition(targetState, condition, transitionTime));
            return this;
        }

        /**
         * Builds and registers the state.
         */
        public AnimationStateMachine build() {
            AnimationState state = new AnimationState(stateName, animationNames, blendWeight, priority);
            state.onEnterCallback = this.onEnterCallback;
            state.onExitCallback = this.onExitCallback;
            state.allowedTransitions.addAll(this.allowedTransitions);
            states.put(stateName, state);
            
            // Set as initial state if none exists
            if (currentState == null) {
                currentState = state;
                transitionProgress = 1.0f;
            }
            
            return AnimationStateMachine.this;
        }
    }
}
