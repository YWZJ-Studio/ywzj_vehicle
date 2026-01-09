package org.ywzj.vehicle.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockModelResourceSet;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.RawResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
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
        event.register(YwzjVehicle.modLocation("entity/basic_bullet"), COMMON_LOADER);
        event.register(YwzjVehicle.modLocation("entity/grenade_40mm"), COMMON_LOADER);
        event.register(YwzjVehicle.modLocation("entity/rocket_57mm"), COMMON_LOADER);
        event.register(YwzjVehicle.modLocation("entity/aerial_bomb"), COMMON_LOADER);
        event.register(YwzjVehicle.modLocation("entity/missile_akd10"), COMMON_LOADER);
    }

    public static BedrockModel getModel(ResourceLocation location) {
        return BedrockModelResourceSet.getInstance().getModel(location);
    }

}
