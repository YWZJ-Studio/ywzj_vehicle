package org.ywzj.vehicle.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.UUID;

public class VehicleDebugOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!LocalVehiclePlayer.instance.onVehicle()) {
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
                AbstractVehicle vehicle = (AbstractVehicle) serverPlayer.level().getEntity(LocalVehiclePlayer.instance.getVehicle().getId());
                guiGraphics.drawString(Minecraft.getInstance().font, "载具: " + vehicle.getVehicleType().getName(), leftX, leftY, 0xFFFFFF);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "整备质量: " + vehicle.curbWeight, leftX, leftY, 0xFFFFFF);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "物理质量: " + vehicle.physicsEngine.mass, leftX, leftY, 0xFFFFFF);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "功率: " + vehicle.getPower() + "%", leftX, leftY, 0xFFFFFF);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "油量: " + vehicle.getFuel() + "/" + vehicle.fuelCapacity, leftX, leftY, 0xFFFFFF);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "耗油: " + vehicle.fuelConsumptionPerTick + "/t", leftX, leftY, 0xFFFFFF);
                leftY += 10;
                guiGraphics.drawString(Minecraft.getInstance().font, "速度: " + new Vec3(vehicle.physicsEngine.velocity), leftX, leftY, 0xFFFFFF);
                leftY += 10;
            }
        }
    }

}
