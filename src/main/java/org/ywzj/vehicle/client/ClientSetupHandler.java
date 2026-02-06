package org.ywzj.vehicle.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllBlockEntities;
import org.ywzj.vehicle.all.AllBlocks;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllParticleTypes;
import org.ywzj.vehicle.client.gui.*;
import org.ywzj.vehicle.client.particle.DustSmokeParticle;
import org.ywzj.vehicle.client.particle.DustStoneParticle;
import org.ywzj.vehicle.client.render.entity.block.FigureBoxBlockRenderer;
import org.ywzj.vehicle.client.render.entity.block.MachineMaxBlockRenderer;
import org.ywzj.vehicle.client.render.entity.misc.FakePlayerRenderer;
import org.ywzj.vehicle.client.render.entity.misc.RopeRenderer;
import org.ywzj.vehicle.client.render.entity.vehicle.DumpTruckRenderer;
import org.ywzj.vehicle.client.render.entity.vehicle.QuadcopterRenderer;
import org.ywzj.vehicle.client.render.entity.vehicle.VehicleRender;
import org.ywzj.vehicle.client.render.entity.vehicle.Ztl11Renderer;
import org.ywzj.vehicle.client.render.entity.weapon.*;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.particle.SmokeCloudParticle;
import org.ywzj.vehicle.particle.TrackParticle;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID)
public class ClientSetupHandler {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.NONE_VEHICLE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.WHEELED_VEHICLE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.TRACKED_VEHICLE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.ROTARY_WING_VEHICLE.get(), VehicleRender::new));

//        event.enqueueWork(() -> EntityRenderers.register(AllEntities.LAV150.get(), Lav150Renderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.ZTL11.get(), Ztl11Renderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.ZTZ99A.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.Z10.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.MOTORCYCLE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.DUMP_TRUCK.get(), DumpTruckRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.HIACE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.QUADCOPTER.get(), QuadcopterRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.M1A2.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.CSSA5.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.AH64D.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.LAV_AD.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.BGM_71_TOW.get(), VehicleRender::new));

//        event.enqueueWork(() -> EntityRenderers.register(AllEntities.WHEELED_VEHICLE.get(), CommonWheeledVehicleRender::new));

        event.enqueueWork(() -> EntityRenderers.register(AllEntities.BULLET.get(), BulletEntityRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.ROCKET.get(), RocketEntityRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.AERIAL_BOMB.get(), AerialBombEntityRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.MISSILE.get(), MissileEntityRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.SMOKE_GRENADE.get(), GrenadeEntityRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.APS_GRENADE.get(), GrenadeEntityRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.DECOY_FLARE.get(), DecoyFlareEntityRenderer::new));

        event.enqueueWork(() -> EntityRenderers.register(AllEntities.FAKE_PLAYER.get(), FakePlayerRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.ROPE.get(), RopeRenderer::new));

        ItemBlockRenderTypes.setRenderLayer(AllBlocks.FIGURE_BOX_BLOCK.get(), RenderType.cutout());
        BlockEntityRenderers.register(AllBlockEntities.FIGURE_BOX_BLOCK_ENTITY.get(), FigureBoxBlockRenderer::new);
        ItemBlockRenderTypes.setRenderLayer(AllBlocks.MACHINE_MAX_BLOCK.get(), RenderType.cutout());
        BlockEntityRenderers.register(AllBlockEntities.MACHINE_MAX_BLOCK_ENTITY.get(), MachineMaxBlockRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterHud(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("vehicle", new VehicleOverlay());
        event.registerAboveAll("vehicle_rotary_wing", new RotaryWingVehicleOverlay());
        event.registerAboveAll("vehicle_crosshair", new VehicleCrossHairOverlay());
        event.registerAboveAll("vehicle_scope", new VehicleScopeOverlay());
        event.registerAboveAll("vehicle_weapon", new VehicleWeaponOverlay());
        event.registerAboveAll("vehicle_hit_indicator", new VehicleHitIndicatorOverlay());
        event.registerAboveAll("vehicle_debug", new VehicleDebugOverlay());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AllParticleTypes.DUST_SMOKE.get(), DustSmokeParticle::provider);
        event.registerSpriteSet(AllParticleTypes.DUST_STONE.get(), DustStoneParticle::provider);
        event.registerSpriteSet(AllParticleTypes.TRACK.get(), TrackParticle.Factory::new);
        event.registerSpriteSet(AllParticleTypes.SMOKE_CLOUD.get(), SmokeCloudParticle::provider);
    }

    @SubscribeEvent
    public static void onRegisterResourceListener(RegisterClientReloadListenersEvent event) {
        ClientAssetsManager.INSTANCE.registerListeners(event::registerReloadListener);
    }

}
