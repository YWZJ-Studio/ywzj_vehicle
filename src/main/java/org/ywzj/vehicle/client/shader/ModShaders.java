package org.ywzj.vehicle.client.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.ywzj.vehicle.YwzjVehicle;

import java.io.IOException;

@EventBusSubscriber(value = Dist.CLIENT)
public class ModShaders {

    private static final ResourceLocation CIRCLE_SHADER_LOCATION = YwzjVehicle.modLocation("circle");
    private static ShaderInstance circleShader;
    private static final ResourceLocation OPTICAL_SCOPE_SHADER_LOCATION = YwzjVehicle.modLocation("optical_scope");
    private static ShaderInstance opticalScopeShader;
    public static final VertexFormat HUD_CIRCLE = DefaultVertexFormat.BLOCK;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), CIRCLE_SHADER_LOCATION, HUD_CIRCLE),
                shaderInstance -> circleShader = shaderInstance
        );
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), OPTICAL_SCOPE_SHADER_LOCATION, DefaultVertexFormat.POSITION_TEX),
                shaderInstance -> opticalScopeShader = shaderInstance
        );
    }

    public static ShaderInstance getCircleShader() {
        return circleShader;
    }

    public static ShaderInstance getOpticalScopeShader() {
        return opticalScopeShader;
    }

}
