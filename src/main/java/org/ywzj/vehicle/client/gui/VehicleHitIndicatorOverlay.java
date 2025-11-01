package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Math;
import org.joml.Matrix4f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ServerHitVehicleEvent;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.ArrayList;
import java.util.List;

public class VehicleHitIndicatorOverlay implements IGuiOverlay {

    public static List<ServerHitVehicleEvent> events = new ArrayList<>();
    public static long lastHitTime = System.currentTimeMillis();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!AllConfigs.common.hitIndicator.get()) {
            return;
        }
        if (System.currentTimeMillis() - lastHitTime > 3000) {
            events.clear();
        }
        if (events.isEmpty()) {
            return;
        }
        Player player = LocalVehiclePlayer.instance.getPlayer();
        if (player == null) {
            return;
        }
        Entity entity = player.level().getEntity(events.get(0).entityId);
        if (entity == null) {
            return;
        }
        Vec3 viewVec;
        float scale;
        ServerHitVehicleEvent event = events.get(0);
        if (entity instanceof AbstractVehicle vehicle) {
            viewVec = vehicle.relativeRotPos(event.hitRelativePosition.add(vehicle.position()), false).subtract(entity.position());
            scale = Math.min(10, 8 / (vehicle.getMainCubeOBB().obb().extents().z * 2) * 10);
        } else {
            viewVec = VectorUtil.relativeRotPos(entity, event.hitRelativePosition.add(entity.position()), false).subtract(entity.position());
            scale = Math.min(10, (float) (48 / entity.getBoundingBox().getSize()));
        }
        float pitch = (float) Math.toDegrees(Math.atan2(-viewVec.y, Math.sqrt(viewVec.x * viewVec.x + viewVec.z * viewVec.z)));
        float yaw = (float) Math.toDegrees(Math.atan2(viewVec.x, viewVec.z));
        guiGraphics.pose().pushPose();
        {
            double modelX = screenWidth - (double) screenWidth / 8;
            double modelY = (double) screenHeight / 2;
            guiGraphics.pose().translate(modelX, modelY + (double) screenHeight / 5, 0);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "Hit",  0, -55, 0xFFFFFFFF);

            Vec3 root = new Vec3(0, 0, 0);
            guiGraphics.pose().rotateAround(Axis.XP.rotationDegrees(pitch + 180), (float) root.x, (float) root.y, (float) root.z);
            guiGraphics.pose().rotateAround(Axis.YP.rotationDegrees(yaw), (float) root.x, (float) root.y, (float) root.z);

            guiGraphics.pose().mulPoseMatrix((new Matrix4f()).scaling(scale, scale, -scale));
            Lighting.setupForEntityInInventory();
            EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

            entityrenderdispatcher.setRenderShadow(false);
            RenderSystem.runAsFancy(() -> {
                entityrenderdispatcher.render(entity, 0, 0,0, entity.getYRot(), 1.0F, guiGraphics.pose(), guiGraphics.bufferSource(), 15728880);
                for (ServerHitVehicleEvent hitVehicleEvent : events) {
                    Vec3 start;
                    Vec3 end;
                    if (entity instanceof AbstractVehicle vehicle) {
                        start = vehicle.relativeRotPos(hitVehicleEvent.hitRelativePosition.add(vehicle.position()), false).subtract(entity.position());
                        end = start.subtract(vehicle.relativeRotDirection(hitVehicleEvent.hitRelativeVector, false).normalize().scale(3));
                    } else {
                        start = VectorUtil.relativeRotPos(entity, hitVehicleEvent.hitRelativePosition.add(entity.position()), false).subtract(entity.position());
                        end = start.subtract(VectorUtil.relativeRotDirection(entity, hitVehicleEvent.hitRelativeVector, false).normalize().scale(3));
                    }
                    renderRedLine(guiGraphics, start, end);
                }
            });
            guiGraphics.flush();
            entityrenderdispatcher.setRenderShadow(true);
        }
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    private void renderRedLine(GuiGraphics guiGraphics, Vec3 start, Vec3 end) {
        PoseStack pose = guiGraphics.pose();
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        float r = 1.0f, g = 0.0f, b = 0.0f, a = 1.0f;
        float offset = 0.01f;
        for (int i = -1; i <= 2; i++) {
            for (int j = -1; j <= 2; j++) {
                lineConsumer.vertex(matrix, (float) (start.x + i * offset), (float) start.y, (float) (start.z + j * offset))
                        .color(r, g, b, a)
                        .normal(0, 1, -100)
                        .endVertex();
                lineConsumer.vertex(matrix, (float) (end.x + i * offset), (float) end.y, (float) (end.z + j * offset))
                        .color(r, g, b, a)
                        .normal(0, 1, -100)
                        .endVertex();
            }
        }
    }

}
