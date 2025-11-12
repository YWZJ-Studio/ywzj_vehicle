package org.ywzj.vehicle.api.custom;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public interface IStructureModelManager {
    Map<ResourceLocation, BedrockModel> getStructureModels();

    Optional<BedrockModel> getStructureModel(ResourceLocation location);
}
