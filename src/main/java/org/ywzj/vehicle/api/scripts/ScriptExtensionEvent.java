package org.ywzj.vehicle.api.scripts;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.mozillaa.javascript.Scriptable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Event fired during script context initialization to allow addons to extend
 * available classes and functionality in vehicle scripts.
 * 
 * This event is fired on the MOD event bus during mod initialization.
 */
public class ScriptExtensionEvent extends Event implements IModBusEvent {

    private final Set<String> allowedPackages = new HashSet<>();
    private final Set<String> allowedClasses = new HashSet<>();
    private final Set<Consumer<Scriptable>> scopeInitializers = new HashSet<>();

    public ScriptExtensionEvent() {
        // Add default allowed packages
        allowedPackages.add("org.ywzj.vehicle");
    }

    /**
     * Allows scripts to access all classes in a package.
     * 
     * @param packageName Package name (e.g., "com.example.addon")
     */
    public void allowPackage(String packageName) {
        allowedPackages.add(packageName);
    }

    /**
     * Allows scripts to access a specific class.
     * 
     * @param className Fully qualified class name (e.g., "com.example.addon.MyClass")
     */
    public void allowClass(String className) {
        allowedClasses.add(className);
    }

    /**
     * Registers a scope initializer to inject custom objects/functions into scripts.
     * 
     * Example:
     * <pre>
     * event.addScopeInitializer(scope -> {
     *     ScriptableObject.putProperty(scope, "myAPI", Context.javaToJS(new MyAPI(), scope));
     * });
     * </pre>
     * 
     * @param initializer Consumer that receives the script scope
     */
    public void addScopeInitializer(Consumer<Scriptable> initializer) {
        scopeInitializers.add(initializer);
    }

    /**
     * Checks if a class name is allowed based on registered packages and classes.
     * 
     * @param className Fully qualified class name
     * @return true if the class is allowed
     */
    public boolean isClassAllowed(String className) {
        // Check exact class match
        if (allowedClasses.contains(className)) {
            return true;
        }

        // Check package prefixes
        for (String packageName : allowedPackages) {
            if (className.startsWith(packageName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets all registered scope initializers.
     */
    public Set<Consumer<Scriptable>> getScopeInitializers() {
        return scopeInitializers;
    }

    /**
     * Gets all allowed packages.
     */
    public Set<String> getAllowedPackages() {
        return allowedPackages;
    }

    /**
     * Gets all allowed classes.
     */
    public Set<String> getAllowedClasses() {
        return allowedClasses;
    }
}
