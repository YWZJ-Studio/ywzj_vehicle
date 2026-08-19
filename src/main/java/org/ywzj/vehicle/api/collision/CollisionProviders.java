package org.ywzj.vehicle.api.collision;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of collision providers consulted during vehicle hull sampling. Register during mod
 * construction. Uses copy-on-write since the list is read every tick per vehicle and written
 * essentially never.
 */
public final class CollisionProviders {

    private static final List<CollisionProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private CollisionProviders() {}

    public static void register(CollisionProvider provider) {
        PROVIDERS.add(provider);
    }

    public static void unregister(CollisionProvider provider) {
        PROVIDERS.remove(provider);
    }

    /** Live view; safe to iterate without copying. */
    public static List<CollisionProvider> providers() {
        return PROVIDERS;
    }

    public static boolean isEmpty() {
        return PROVIDERS.isEmpty();
    }

}
