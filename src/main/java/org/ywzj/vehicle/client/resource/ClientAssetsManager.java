package org.ywzj.vehicle.client.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationFile;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.ywzj.vehicle.client.resource.vehicle.BaseVehicleDisplay;
import org.ywzj.vehicle.custom.serialize.GsonUtil;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public enum ClientAssetsManager {
    INSTANCE;
    private JsonDataManager<BedrockModelPOJO> models;
    private JsonDataManager<BedrockAnimationFile> animations;
    private VehicleDisplayManager vehicleDisplayManager;

    public void registerListeners(Consumer<PreparableReloadListener> consumer) {
        models = this.create(BedrockModelPOJO.class, "models/bedrock", "BedrockModelPojo", consumer);
        animations = this.create(BedrockAnimationFile.class, "animations/bedrock", "BedrockAnimationPojo", consumer);
        vehicleDisplayManager = new VehicleDisplayManager();
        consumer.accept(vehicleDisplayManager);

        // 完成加载后清理临时数据
        consumer.accept(new SimplePreparableReloadListener<Void>() {
            @Override
            protected @NotNull Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                return null;
            }

            @Override
            protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                models.clearData();
                animations.clearData();
            }
        });
    }

    public <T> JsonDataManager<T> create(Class<T> clazz, String folder, String marker, Consumer<PreparableReloadListener> consumer) {
        JsonDataManager<T> manager = new JsonDataManager<>(clazz, GsonUtil.GSON, folder, marker);
        consumer.accept(manager);
        return manager;
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
    public Map<ResourceLocation, BaseVehicleDisplay> getVehicleDisplays() {
        return vehicleDisplayManager != null ? vehicleDisplayManager.getDisplayMap() : null;
    }

    @NotNull
    public Optional<BaseVehicleDisplay> getVehicleDisplay(ResourceLocation id) {
        if (vehicleDisplayManager == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(vehicleDisplayManager.getDisplayMap().get(id));
    }
}
