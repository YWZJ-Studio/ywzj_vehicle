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
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllBlockEntities;
import org.ywzj.vehicle.all.AllBlocks;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllParticleTypes;
import org.ywzj.vehicle.client.gui.*;
import org.ywzj.vehicle.client.particle.BulletHoleParticle;
import org.ywzj.vehicle.client.particle.DustSmokeParticle;
import org.ywzj.vehicle.client.particle.DustStoneParticle;
import org.ywzj.vehicle.client.particle.SmokeCloudParticle;
import org.ywzj.vehicle.client.render.entity.block.FigureBoxBlockRenderer;
import org.ywzj.vehicle.client.render.entity.block.MachineMaxBlockRenderer;
import org.ywzj.vehicle.client.render.entity.misc.FakePlayerRenderer;
import org.ywzj.vehicle.client.render.entity.vehicle.DumpTruckRenderer;
import org.ywzj.vehicle.client.render.entity.vehicle.VehicleRender;
import org.ywzj.vehicle.client.render.entity.vehicle.Ztl11Renderer;
import org.ywzj.vehicle.client.render.entity.weapon.AmmoEntityRenderer;
import org.ywzj.vehicle.client.render.entity.weapon.BulletEntityRenderer;
import org.ywzj.vehicle.client.render.entity.weapon.DecoyFlareEntityRenderer;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.weapon.AmmoEntity;
import org.ywzj.vehicle.entity.weapon.GrenadeEntity;
import org.ywzj.vehicle.particle.TrackParticle;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID)
public class ClientSetupHandler {

    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.NONE_VEHICLE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.WHEELED_VEHICLE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.TRACKED_VEHICLE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.ROTARY_WING_VEHICLE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.FIXED_WING_VEHICLE.get(), VehicleRender::new));

//        event.enqueueWork(() -> EntityRenderers.register(AllEntities.LAV150.get(), Lav150Renderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.ZTL11.get(), Ztl11Renderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.ZTZ99A.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.Z10.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.MOTORCYCLE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.DUMP_TRUCK.get(), DumpTruckRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.HIACE.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.QUADCOPTER.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.M1A2.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.CSSA5.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.AH64D.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.LAV_AD.get(), VehicleRender::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.BGM_71_TOW.get(), VehicleRender::new));

        event.enqueueWork(() -> EntityRenderers.register(AllEntities.BULLET.get(), BulletEntityRenderer::new));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.ROCKET.get(),
                ctx -> new AmmoEntityRenderer<>(ctx, AmmoEntity::getWeaponId, "entity/rocket_57mm", "textures/entity/rocket_57mm.png")));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.AERIAL_BOMB.get(),
                ctx -> new AmmoEntityRenderer<>(ctx, AmmoEntity::getWeaponId, "entity/aerial_bomb", "textures/entity/aerial_bomb.png")));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.MISSILE.get(),
                ctx -> new AmmoEntityRenderer<>(ctx, AmmoEntity::getWeaponId, "entity/missile_akd10", "textures/entity/missile_akd10.png")));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.SMOKE_GRENADE.get(),
                ctx -> new AmmoEntityRenderer<>(ctx, GrenadeEntity::getWeaponId, "entity/grenade_40mm", "textures/entity/grenade_40mm.png")));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.APS_GRENADE.get(),
                ctx -> new AmmoEntityRenderer<>(ctx, GrenadeEntity::getWeaponId, "entity/grenade_40mm", "textures/entity/grenade_40mm.png")));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.FRAG_GRENADE.get(),
                ctx -> new AmmoEntityRenderer<>(ctx, GrenadeEntity::getWeaponId, "entity/grenade_40mm", "textures/entity/grenade_40mm.png")));
        event.enqueueWork(() -> EntityRenderers.register(AllEntities.DECOY_FLARE.get(), DecoyFlareEntityRenderer::new));

        event.enqueueWork(() -> EntityRenderers.register(AllEntities.FAKE_PLAYER.get(), FakePlayerRenderer::new));

        ItemBlockRenderTypes.setRenderLayer(AllBlocks.FIGURE_BOX_BLOCK.get(), RenderType.cutout());
        BlockEntityRenderers.register(AllBlockEntities.FIGURE_BOX_BLOCK_ENTITY.get(), FigureBoxBlockRenderer::new);
        ItemBlockRenderTypes.setRenderLayer(AllBlocks.MACHINE_MAX_BLOCK.get(), RenderType.cutout());
        BlockEntityRenderers.register(AllBlockEntities.MACHINE_MAX_BLOCK_ENTITY.get(), MachineMaxBlockRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterHud(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vehicle", new VehicleOverlay());
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vehicle_rotary_wing", new RotaryWingVehicleOverlay());
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vehicle_fixed_wing", new FixedWingVehicleOverlay());
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vehicle_aim_at", new VehicleAimAtOverlay());
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vehicle_scope", new VehicleScopeOverlay());
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vehicle_weapon", new VehicleWeaponOverlay());
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vehicle_hit_indicator", new VehicleHitIndicatorOverlay());
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vehicle_radar", new VehicleRadarOverlay());
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vehicle_debug", new VehicleDebugOverlay());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AllParticleTypes.DUST_SMOKE.get(), DustSmokeParticle::provider);
        event.registerSpriteSet(AllParticleTypes.DUST_STONE.get(), DustStoneParticle::provider);
        event.registerSpriteSet(AllParticleTypes.TRACK.get(), TrackParticle.Factory::new);
        event.registerSpriteSet(AllParticleTypes.CHANGING_CLOUD.get(), SmokeCloudParticle::provider);
        event.registerSpriteSet(AllParticleTypes.FIXED_CLOUD.get(), SmokeCloudParticle::provider);
        event.registerSpecial(AllParticleTypes.BULLET_HOLE.get(), new BulletHoleParticle.Provider());
    }

    @SubscribeEvent
    public static void onRegisterResourceListener(RegisterClientReloadListenersEvent event) {
        ClientAssetsManager.INSTANCE.registerListeners(event::registerReloadListener);
    }

}
