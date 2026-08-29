package org.ywzj.vehicle.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockModelResourceSet;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.RawResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.ywzj.vehicle.client.resource.InternalAssets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@EventBusSubscriber
public class BedrockModelLoader {

    public static final RawResourceLoader COMMON_LOADER = new RawResourceLoader() {
        @Override
        public <T> T load(InputStream inputStream, Class<T> clazz) {
            try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                return GsonUtil.CLIENT_GSON.fromJson(reader, clazz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

    @SubscribeEvent
    public static void onRegisterBedrockModelRenderers(RegisterBedrockModelEvent event) {
        // 通用模型直接走sbm自带的加载器
        event.register(InternalAssets.BASIC_BULLET_MODEL, COMMON_LOADER);
        event.register(InternalAssets.GRENADE_40MM_MODEL, COMMON_LOADER);
        event.register(InternalAssets.ROCKET_57MM_MODEL, COMMON_LOADER);
        event.register(InternalAssets.AERIAL_BOMB_MODEL, COMMON_LOADER);
        event.register(InternalAssets.MISSILE_AKD10_MODEL, COMMON_LOADER);
        event.register(InternalAssets.MACHINE_MAX_BLOCK_MODEL, COMMON_LOADER);
    }

    public static BedrockModel getModel(ResourceLocation location) {
        return BedrockModelResourceSet.getInstance().getModel(location);
    }

}
