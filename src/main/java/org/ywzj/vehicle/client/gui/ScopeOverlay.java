package org.ywzj.vehicle.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

public class ScopeOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
            return;
        }
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
    }

}
