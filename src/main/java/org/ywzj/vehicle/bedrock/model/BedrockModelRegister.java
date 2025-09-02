package org.ywzj.vehicle.bedrock.model;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;

import java.util.Set;

@Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BedrockModelRegister {

    public static BedrockModelRegister INSTANCE = null;
    private final BedrockModelSet modelSet;

    private BedrockModelRegister(BedrockModelSet modelSet) {
        this.modelSet = modelSet;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRegisterClientReloadListenersEvent(RegisterClientReloadListenersEvent event) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager instanceof ReloadableResourceManager manager) {
            INSTANCE = new BedrockModelRegister(new BedrockModelSet());
            ModLoader.get().postEvent(new BedrockModelRegisterEvent(INSTANCE.modelSet));
            // 将注册冻结
            INSTANCE.modelSet.immutableKnowLocations();
            // 添加到最前面，避免实体读取模型时模型还没加载完成
            manager.listeners.add(0, INSTANCE.modelSet);
        }
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    @SubscribeEvent
    public static void onAddPackFindersEvent(AddPackFindersEvent event) {
        INSTANCE = new BedrockModelRegister(new BedrockModelSet());
        ModLoader.get().postEvent(new BedrockModelRegisterEvent(INSTANCE.modelSet));
        // 将注册冻结
        INSTANCE.modelSet.immutableKnowLocations();
        // 服务端直接加载
        INSTANCE.modelSet.prepareServer();
    }

    public BedrockModel getModel(ResourceLocation location) {
        return modelSet.getModels().get(location);
    }

    public Set<ResourceLocation> getAllModelKeys() {
        return modelSet.getModels().keySet();
    }

}
