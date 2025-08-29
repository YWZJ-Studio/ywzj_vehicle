package org.ywzj.vehicle.bedrock.animation;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.BedrockAnimationFile;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.resource.GsonUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.function.Function;

public class BedrockAnimationSet extends SimplePreparableReloadListener<Void> {
    private Map<ResourceLocation, Map<String, BedrockAnimation>> animations = ImmutableMap.of();
    private Map<ResourceLocation, Function<BedrockAnimationFile, Map<String, BedrockAnimation>>> knowLocations = new Object2ObjectArrayMap<>();

    void addAnimation(ResourceLocation location, Function<BedrockAnimationFile, Map<String, BedrockAnimation>> function) {
        this.knowLocations.put(location, function);
    }

    void immutableKnowLocations() {
        this.knowLocations = ImmutableMap.copyOf(knowLocations);
    }

    @Override
    protected Void prepare(ResourceManager manager, ProfilerFiller profilerFiller) {
        animations = Maps.newHashMap();
        this.knowLocations.forEach((animationLocation, function) -> {
            // 将 ID 转换成实际动画文件路径，默认是 <namespace>:animations/<path>.json
            ResourceLocation path = new ResourceLocation(animationLocation.getNamespace(), "animations/" + animationLocation.getPath() + ".json");
            manager.getResource(path).ifPresentOrElse(resource -> {
                SimpleBedrockModel.LOGGER.info("Loading bedrock animation file: {}", path);
                try (InputStream stream = resource.open()) {
                    BedrockAnimationFile pojo = GsonUtil.GSON.fromJson(new InputStreamReader(stream), BedrockAnimationFile.class);
                    this.animations.put(animationLocation, function.apply(pojo));
                } catch (IOException e) {
                    SimpleBedrockModel.LOGGER.error("Failed to load animation file: {}", path, e);
                }
            }, () -> SimpleBedrockModel.LOGGER.error("Not found animation file: {}", path));
        });
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        this.animations = ImmutableMap.copyOf(animations);
    }

    Map<ResourceLocation, Map<String, BedrockAnimation>> getAnimations() {
        return animations;
    }
}
