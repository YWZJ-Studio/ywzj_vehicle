package org.ywzj.vehicle.client.manager;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.resource.GsonUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.function.Function;

public class BedrockModelSet extends SimplePreparableReloadListener<Void> {
    private Map<ResourceLocation, BedrockModel> models = ImmutableMap.of();
    private Map<ResourceLocation, Function<BedrockModelPOJO, ? extends BedrockModel>> knowLocations = Maps.newHashMap();

    void addModel(ResourceLocation location, Function<BedrockModelPOJO, ? extends BedrockModel> function) {
        this.knowLocations.put(location, function);
    }

    void immutableKnowLocations() {
        this.knowLocations = ImmutableMap.copyOf(knowLocations);
    }

    @Override
    protected Void prepare(ResourceManager manager, ProfilerFiller filler) {
        this.models = Maps.newHashMap();
        this.knowLocations.keySet().forEach(location -> {
            // 将 ID 转换成实际模型文件路径，默认是 <namespace>:models/<path>.json
            ResourceLocation path = new ResourceLocation(location.getNamespace(), "models/" + location.getPath() + ".json");
            Function<BedrockModelPOJO, ? extends BedrockModel> modelFunction = knowLocations.get(location);
            manager.getResource(path).ifPresentOrElse(model -> {
                SimpleBedrockModel.LOGGER.info("Loading bedrock model file: {}", path);
                try (InputStream stream = model.open()) {
                    BedrockModelPOJO pojo = GsonUtil.GSON.fromJson(new InputStreamReader(stream), BedrockModelPOJO.class);
                    this.models.put(location, modelFunction.apply(pojo));
                } catch (IOException e) {
                    SimpleBedrockModel.LOGGER.error("Failed to load model file: {}", path, e);
                }
            }, () -> SimpleBedrockModel.LOGGER.error("Not found model file: {}", path));
        });
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager manager, ProfilerFiller filler) {
        this.models = ImmutableMap.copyOf(models);
    }

    Map<ResourceLocation, BedrockModel> getModels() {
        return models;
    }
}
