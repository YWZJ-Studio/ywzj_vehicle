package org.ywzj.vehicle.client.render.animation.compiler;

import org.mozillaa.javascript.Script;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;
import org.ywzj.vehicle.client.resource.animation.ConditionDefinition;

import java.util.List;
import java.util.function.Predicate;

/**
 * Compiles condition definitions into Predicate functions.
 * Supports script, and, or, not condition types.
 */
public class ConditionCompiler {
    private final ScriptCompiler scriptCompiler;

    public ConditionCompiler(ScriptCompiler scriptCompiler) {
        this.scriptCompiler = scriptCompiler;
    }

    /**
     * Compile a condition definition into a Predicate
     */
    public <T extends BaseAnimationContext> Predicate<T> compileCondition(ConditionDefinition def) {
        if (def == null) {
            return context -> true; // No condition = always true
        }

        String type = def.getType();
        if (type == null) {
            type = "script"; // Default to script type
        }

        return switch (type) {
            case "script" -> compileScriptCondition(def.getScript());
            case "and" -> compileAndCondition(def.getConditions());
            case "or" -> compileOrCondition(def.getConditions());
            case "not" -> compileNotCondition(def.getCondition());
            default -> throw new IllegalArgumentException("Unknown condition type: " + type);
        };
    }

    /**
     * Compile a script-based condition
     */
    private <T extends BaseAnimationContext> Predicate<T> compileScriptCondition(String scriptCode) {
        if (scriptCode == null || scriptCode.isEmpty()) {
            return context -> true;
        }

        Script compiledScript = scriptCompiler.compile(scriptCode);
        return context -> scriptCompiler.evaluateBoolean(compiledScript, context);
    }

    /**
     * Compile an AND condition (all must be true)
     */
    private <T extends BaseAnimationContext> Predicate<T> compileAndCondition(List<ConditionDefinition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return context -> true;
        }

        List<Predicate<T>> predicates = conditions.stream()
                .map(this::<T>compileCondition)
                .toList();

        return context -> predicates.stream().allMatch(p -> p.test(context));
    }

    /**
     * Compile an OR condition (at least one must be true)
     */
    private <T extends BaseAnimationContext> Predicate<T> compileOrCondition(List<ConditionDefinition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return context -> false;
        }

        List<Predicate<T>> predicates = conditions.stream()
                .map(this::<T>compileCondition)
                .toList();

        return context -> predicates.stream().anyMatch(p -> p.test(context));
    }

    /**
     * Compile a NOT condition (negate the result)
     */
    private <T extends BaseAnimationContext> Predicate<T> compileNotCondition(ConditionDefinition condition) {
        if (condition == null) {
            return context -> false;
        }

        Predicate<T> predicate = compileCondition(condition);
        return predicate.negate();
    }
}
