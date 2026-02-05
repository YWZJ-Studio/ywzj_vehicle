package org.ywzj.vehicle.api.scripts;

import org.mozillaa.javascript.Context;
import org.mozillaa.javascript.ScriptableObject;

/**
 * Example of how addon mods can extend script functionality.
 * 
 * To use this in your addon:
 * 1. Call ScriptContextFactory.getExtensionEvent() in your mod constructor
 * 2. Register your packages/classes or add scope initializers
 * 3. Your classes will be accessible in vehicle scripts
 * 
 * IMPORTANT: This must be done BEFORE any scripts are loaded (in mod constructor).
 */
public class ScriptExtensionExample {

    /**
     * Example of how to extend scripts from your addon mod.
     * Call this from your mod's constructor.
     */
    public static void registerScriptExtensions() {
        var event = ScriptContextFactory.getExtensionEvent();
        
        // Example 1: Allow access to entire package
        // event.allowPackage("com.example.addon");
        
        // Example 2: Allow access to specific class
        // event.allowClass("com.example.addon.api.CustomAPI");
        
        // Example 3: Inject custom API object into scripts
        // event.addScopeInitializer(scope -> {
        //     CustomAPI api = new CustomAPI();
        //     Object jsAPI = Context.javaToJS(api, scope);
        //     ScriptableObject.putProperty(scope, "customAPI", jsAPI);
        // });
        
        // Example 4: Add utility functions
        // event.addScopeInitializer(scope -> {
        //     ScriptableObject.putProperty(scope, "log", (Function) (cx, s, thisObj, args) -> {
        //         System.out.println("[Script] " + args[0]);
        //         return null;
        //     });
        // });
    }

    /**
     * Example custom API class that can be exposed to scripts.
     */
    public static class CustomAPI {
        public String getMessage() {
            return "Hello from custom API!";
        }
        
        public int calculate(int a, int b) {
            return a + b;
        }
    }
}
