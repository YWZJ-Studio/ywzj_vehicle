package org.ywzj.vehicle.client.manager;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.BedrockModelPOJO;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.function.Function;

public class BedrockModelRegisterEvent extends Event implements IModBusEvent {
    private final BedrockModelSet modelSet;

    public BedrockModelRegisterEvent(BedrockModelSet modelSet) {
        this.modelSet = modelSet;
    }

    public void register(ResourceLocation location, Function<BedrockModelPOJO, ? extends BedrockModel> function) {
        this.modelSet.addModel(location, function);
    }

    @Override
    public boolean isCancelable() {
        return false;
    }
}
