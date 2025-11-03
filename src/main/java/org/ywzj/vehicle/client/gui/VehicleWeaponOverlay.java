package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class VehicleWeaponOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        //todo 待优化
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        if (!instance.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = instance.getVehicle();
        PartUnit operatorUnit = vehicle.getOwnOperatorUnit(instance.getPlayer());
        if (operatorUnit instanceof WeaponUnit weaponUnit) {
            weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon -> {
                PoseStack poseStack = guiGraphics.pose();
                int reloadTime = vehicleWeapon.getReloadTime();
                int remainAmmo = vehicleWeapon.getRemainAmmo();
                int maxAmmo = vehicleWeapon.getMaxCapacity();
                Font font = Minecraft.getInstance().font;
                poseStack.pushPose();
                {
                    guiGraphics.drawString(font, "Ammo: " + remainAmmo + " / " + maxAmmo, 10, screenHeight / 2 - 20, 0xFFFFFF);
                    guiGraphics.drawString(font, vehicleWeapon.getName(), 10, screenHeight / 2 - 35, 0xFFFFFF);
                    if (reloadTime > 0) {
                        guiGraphics.drawString(font, "Reloading: " + reloadTime + " ticks", 10, screenHeight / 2 - 50, 0xFF0000);
                    }
                }
                poseStack.popPose();
            });
        }
    }

}
