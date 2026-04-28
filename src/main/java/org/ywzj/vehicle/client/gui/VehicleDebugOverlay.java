package org.ywzj.vehicle.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.command.sub.DebugCommand;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.UUID;

public class VehicleDebugOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!LocalVehiclePlayer.instance.onVehicle() || !DebugCommand.DEBUG) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer()) {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server != null && mc.player != null) {
                UUID uuid = mc.player.getUUID();
                ServerPlayer serverPlayer = server.getPlayerList().getPlayer(uuid);
                int leftX = 0;
                int leftY = 0;
                Entity entity = serverPlayer.level().getEntity(LocalVehiclePlayer.instance.getVehicle().getId());
                if (!(entity instanceof AbstractVehicle vehicle)) {
                    return;
                }

                guiGraphics.drawString(Minecraft.getInstance().font, "载具: " + vehicle.getDisplayName(), leftX, leftY, Color.WHITE);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "整备质量: " + vehicle.curbWeight, leftX, leftY, Color.WHITE);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "物理质量: " + vehicle.physicsEngine.mass, leftX, leftY, Color.WHITE);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "功率: " + vehicle.getPower() + "%", leftX, leftY, Color.WHITE);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "油量: " + vehicle.getEnergy() + "/" + vehicle.energyInfo.energyCapacity, leftX, leftY, Color.WHITE);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "耗油: " + vehicle.energyInfo.energyConsumptionPerTick + "/t", leftX, leftY, Color.WHITE);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "速度: " + new Vec3(vehicle.physicsEngine.velocity), leftX, leftY, Color.WHITE);
            }
        }
    }

}
