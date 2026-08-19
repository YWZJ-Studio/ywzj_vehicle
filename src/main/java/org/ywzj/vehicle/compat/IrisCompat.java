package org.ywzj.vehicle.compat;

import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

@OnlyIn(Dist.CLIENT)
public final class IrisCompat {

    private static final boolean LOADED = ModList.get().isLoaded("iris");

    private IrisCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static boolean isRenderingShadowPass() {
        return LOADED && Backend.isRenderingShadowPass();
    }

    private static final class Backend {

        private Backend() {
        }

        static boolean isRenderingShadowPass() {
            try {
                return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

}
