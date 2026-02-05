package org.ywzj.vehicle.api.scripts;

import org.mozillaa.javascript.*;

import java.util.Set;
import java.util.function.Consumer;

public final class ScriptContextFactory extends ContextFactory {
    private static final ScriptContextFactory INSTANCE;
    private static ScriptExtensionEvent extensionEvent;
    private static boolean eventFired = false;
    
    static {
        // Create class shutter with lazy event initialization
        ClassShutter shutter = className -> {
            ensureEventFired();
            return extensionEvent.isClassAllowed(className);
        };
        INSTANCE = new ScriptContextFactory(null, shutter);
    }

    private final Scriptable globalScope;
    private final WrapFactory wrapFactory;
    private final ClassShutter classShutter;

    public ScriptContextFactory(WrapFactory wrapFactory, ClassShutter classShutter) {
        this.wrapFactory = wrapFactory;
        this.classShutter = classShutter;
        try (Context ctx = this.enterContext()) {
            Scriptable scope = ctx.initSafeStandardObjects();
            ScriptUtils.inject(scope);
            
            // Apply scope initializers from extension event (lazy)
            ensureEventFired();
            if (extensionEvent != null) {
                for (Consumer<Scriptable> initializer : extensionEvent.getScopeInitializers()) {
                    initializer.accept(scope);
                }
            }
            
            if (scope instanceof ScriptableObject so) {
                so.sealObject();
            }
            this.globalScope = scope;
        }
    }

    /**
     * Ensures the extension event has been fired.
     * Called lazily to avoid issues with mod loading context timing.
     */
    private static synchronized void ensureEventFired() {
        if (!eventFired) {
            extensionEvent = new ScriptExtensionEvent();
            // Event is now just a data holder - addons can register via API
            // No need to post to event bus since it's called too early
            eventFired = true;
        }
    }

    public static ScriptContextFactory get() {
        return INSTANCE;
    }

    /**
     * Gets the script extension event for inspection or modification.
     * Useful for addons to register their classes/packages.
     * 
     * Example usage in addon mod:
     * <pre>
     * ScriptContextFactory.getExtensionEvent().allowPackage("com.example.addon");
     * </pre>
     */
    public static ScriptExtensionEvent getExtensionEvent() {
        ensureEventFired();
        return extensionEvent;
    }

    public Scriptable createScope(Context ctx) {
        Scriptable scope = ctx.newObject(globalScope);
        scope.setPrototype(globalScope);
        scope.setParentScope(null);
        return scope;
    }

    @Override
    public Context makeContext() {
        Context ctx = super.makeContext();
        ctx.setInterpretedMode(false);
        if (wrapFactory != null) {
            ctx.setWrapFactory(wrapFactory);
        }
        if (classShutter != null) {
            ctx.setClassShutter(classShutter);
        }
        return ctx;
    }
}
