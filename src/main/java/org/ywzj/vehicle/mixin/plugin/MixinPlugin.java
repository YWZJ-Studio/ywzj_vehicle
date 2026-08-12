package org.ywzj.vehicle.mixin.plugin;

import net.neoforged.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    private static final Logger LOGGER = LogManager.getLogger("ywzj_vehicle|chunk-stream");
    private static final String[] SODIUM_LIKE = {"embeddium", "rubidium", "sodium"};

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("tacz")) return getClass("com.tacz.guns.GunMod");
        if (mixinClassName.contains("sable")) return getClass("dev.ryanhcode.sable.Sable");
        // Sodium-likes replace LevelRenderer's chunk bookkeeping wholesale and already build their
        // render list from the camera, so the render-origin redirect is both useless and unsafe there.
        if (mixinClassName.endsWith("LevelRendererOriginMixin")) {
            boolean sodium = sodiumPresent();
            LOGGER.info("detached body render origin redirect {} (sodium-family renderer {})",
                    sodium ? "skipped" : "applied", sodium ? "present" : "absent");
            return !sodium;
        }
        return true;
    }

    private boolean sodiumPresent() {
        try {
            LoadingModList mods = LoadingModList.get();
            if (mods == null) {
                return false;
            }
            for (String id : SODIUM_LIKE) {
                if (mods.getModFileById(id) != null) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Code based on Sona-Survival-101
     */
    private boolean getClass(String className) {
        try {
            Class.forName(className, false, this.getClass().getClassLoader());
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
