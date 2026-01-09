package org.ywzj.vehicle.client.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.HandedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.BasicAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.stream.Collectors;

public class InternalAssets extends SimplePreparableReloadListener<Void> {

    public static final ResourceLocation REPAIR_TOOL_MODEL = YwzjVehicle.modLocation("item/repair_tool");
    public static final ResourceLocation REPAIR_TOOL_TEXTURE = YwzjVehicle.modLocation("textures/bedrock/item/repair_tool.png");
    public static final ResourceLocation REPAIR_TOOL_ANIMATION = YwzjVehicle.modLocation("item/repair_tool.animation");
    public static final ResourceLocation REPAIR_TOOL_SLOT_TEXTURE = YwzjVehicle.modLocation("textures/item/repair_tool.png");

    private HandedBedrockModel repairToolModel;
    private Map<String, BedrockAnimation> repairToolAnimations;

    @NotNull
    @Override
    @ParametersAreNonnullByDefault
    protected Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return null;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        repairToolModel = ClientAssetsManager.INSTANCE.getModel(REPAIR_TOOL_MODEL)
                .map(modelPojo -> {
                    var model = new HandedBedrockModel(modelPojo, null);
                    model.getBone("fire").illuminated = true;
                    return model;
                })
                .orElseThrow(()-> new IllegalStateException("Failed to load repair tool model. The mod may be corrupted."));

        repairToolAnimations = ClientAssetsManager.INSTANCE.getAnimation(REPAIR_TOOL_ANIMATION)
                .map(pojo -> {
                    return BedrockAnimation.createAnimation(pojo, repairToolModel)
                            .stream()
                            .collect(Collectors.toMap(
                                    BasicAnimation::getName, anim -> anim
                            ));
                })
                .orElseThrow(()-> new IllegalStateException("Failed to load repair tool animation. The mod may be corrupted."));
    }

    @NotNull
    public HandedBedrockModel getRepairToolModel() {
        return repairToolModel;
    }

    @NotNull
    public Map<String, BedrockAnimation> getRepairToolAnimations() {
        return repairToolAnimations;
    }
}
