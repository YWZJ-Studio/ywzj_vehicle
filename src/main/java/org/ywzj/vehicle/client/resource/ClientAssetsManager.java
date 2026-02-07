package org.ywzj.vehicle.client.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationFile;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.mozillaa.javascript.Script;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.custom.serialize.GsonUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public enum ClientAssetsManager {

    INSTANCE;
    private JsonDataManager<BedrockModelPOJO> models;
    private JsonDataManager<BedrockAnimationFile> animations;
    private AnimationControllerDefinitionManager animationControllerDefinitionManager;
    private DisplayManager vehicleDisplayManager;
    private DisplayManager weaponDisplayManager;
    private ScriptManager scriptManager;
    private InternalAssets internalAssets;

    public void registerListeners(Consumer<PreparableReloadListener> consumer) {
        models = new JsonDataManager<>(BedrockModelPOJO.class, GsonUtil.GSON, "models/bedrock", "BedrockModelPojo");
        animations = new JsonDataManager<>(BedrockAnimationFile.class, GsonUtil.GSON, "animations/bedrock", "BedrockAnimationPojo");
        animationControllerDefinitionManager = new AnimationControllerDefinitionManager();
        vehicleDisplayManager = new DisplayManager("vehicle");
        weaponDisplayManager = new DisplayManager("weapon");
        scriptManager = new ScriptManager();
        internalAssets = new InternalAssets();

        consumer.accept(models);
        consumer.accept(animations);
        consumer.accept(animationControllerDefinitionManager);
        consumer.accept(scriptManager);
        consumer.accept(vehicleDisplayManager);
        consumer.accept(weaponDisplayManager);
        consumer.accept(internalAssets);

        // 完成加载后清理临时数据
        consumer.accept(new SimplePreparableReloadListener<Void>() {
            @Override
            @ParametersAreNonnullByDefault
            protected @NotNull Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                return null;
            }

            @Override
            @ParametersAreNonnullByDefault
            protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                models.clearData();
                animations.clearData();
            }
        });
    }

    public void reload(ResourceManager resourceManager) {
        models.apply(models.prepare(resourceManager, null), null, null);
        scriptManager.apply(scriptManager.prepare(resourceManager, null), null, null);
        animations.apply(animations.prepare(resourceManager, null), null, null);
        vehicleDisplayManager.apply(vehicleDisplayManager.prepare(resourceManager, null), null, null);
        vehicleDisplayManager.getDisplayMap().values().forEach(vehicleDisplay -> {
            try {
                Minecraft.getInstance().textureManager.register(vehicleDisplay.getTexture(), new SimpleTexture(vehicleDisplay.getTexture()));
                Minecraft.getInstance().textureManager.register(vehicleDisplay.getSlotTexture(), new SimpleTexture(vehicleDisplay.getSlotTexture()));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @UnmodifiableView
    @Nullable
    public Map<ResourceLocation, BedrockModelPOJO> getModels() {
        return models != null ? models.getAllData() : null;
    }

    @NotNull
    public Optional<BedrockModelPOJO> getModel(ResourceLocation id) {
        if (models == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(models.getAllData().get(id));
    }

    @UnmodifiableView
    @Nullable
    public Map<ResourceLocation, BedrockAnimationFile> getAnimations() {
        return animations != null ? animations.getAllData() : null;
    }

    @NotNull
    public Optional<BedrockAnimationFile> getAnimation(ResourceLocation id) {
        if (animations == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(animations.getAllData().get(id));
    }

    @UnmodifiableView
    @Nullable
    public Map<ResourceLocation, BaseDisplay> getVehicleDisplays() {
        return vehicleDisplayManager != null ? vehicleDisplayManager.getDisplayMap() : null;
    }

    @NotNull
    public List<BaseDisplay> getVariableDisplay(ResourceLocation model) {
        return vehicleDisplayManager.getModelWithVariableDisplay().get(model);
    }

    @NotNull
    public Optional<BaseDisplay> getVehicleDisplay(ResourceLocation id) {
        if (vehicleDisplayManager == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(vehicleDisplayManager.getDisplayMap().get(id));
    }

    @NotNull
    public Optional<BaseDisplay> getWeaponDisplay(ResourceLocation id) {
        if (weaponDisplayManager == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(weaponDisplayManager.getDisplayMap().get(id));
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public Optional<Script> getScript(ResourceLocation script) {
        return scriptManager.getScript(script);
    }

    public InternalAssets getInternalAssets() {
        return internalAssets;
    }
}
