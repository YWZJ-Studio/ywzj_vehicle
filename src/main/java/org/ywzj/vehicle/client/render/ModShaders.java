package org.ywzj.vehicle.client.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;

import java.io.IOException;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModShaders {

    private static final ResourceLocation CIRCLE_SHADER_LOCATION = new ResourceLocation(YwzjVehicle.MOD_ID, "circle");
    private static ShaderInstance circleShader;

    public static final VertexFormat HUD_CIRCLE = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                    .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                    .put("Color", DefaultVertexFormat.ELEMENT_COLOR)
                    .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
                    .put("Normal", DefaultVertexFormat.ELEMENT_NORMAL)
                    .build()
    );

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), CIRCLE_SHADER_LOCATION, HUD_CIRCLE),
                shaderInstance -> circleShader = shaderInstance
        );
    }

    public static ShaderInstance getCircleShader() {
        return circleShader;
    }
}
