package org.ywzj.vehicle.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class ScopeOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!LocalVehiclePlayer.instance.onVehicle() || LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();

        // 屏幕中心点
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        // 十字线长度
        int size = 18;
        // 颜色 (ARGB) 红色不透明
        int color = 0xFFFF0000;
        // 画水平线
        guiGraphics.fill(centerX - size, centerY, centerX + size + 1, centerY + 1, color);
        // 画垂直线
        guiGraphics.fill(centerX, centerY - size, centerX + 1, centerY + size + 1, color);

        // 成员组
        int x = centerX - guiGraphics.guiWidth() / 3;
        int y = centerY + guiGraphics.guiHeight() / 8;
        for (int index = 0; index < vehicle.seats; index++) {
            Integer playerId = vehicle.passengerIdsBySeat.get(index);
            Entity entity = null;
            WeaponUnit weaponUnit = null;
            if (playerId != null) {
                entity = LocalVehiclePlayer.instance.getPlayer().level().getEntity(playerId);
                if (entity instanceof LivingEntity livingEntity) {
                    weaponUnit = vehicle.getOwnWeaponUnit(livingEntity);
                }
            }
            String info = "[]";
            if (entity != null) {
                info = "[" + entity.getDisplayName().getString() + "] " + (weaponUnit == null ? "" : weaponUnit.getName().getString());
            }
            guiGraphics.drawString(Minecraft.getInstance().font, info, x, y, color);
            y += 10;
        }

    }

}
