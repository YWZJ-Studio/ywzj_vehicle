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
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import java.util.Optional;

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
        PartUnit<?> operatorUnit = vehicle.getOwnOperatorUnit(instance.getPlayer());
        Font font = Minecraft.getInstance().font;
        int y = -20;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.scale(0.75F, 0.75F, 0.75F);
            if (operatorUnit instanceof WeaponUnit weaponUnit) {
                Optional<AbstractVehicleWeapon<?>> mainWeaponOptional = weaponUnit.getCurrentWeapon();
                if (mainWeaponOptional.isPresent()) {
                    AbstractVehicleWeapon<?> mainWeapon = mainWeaponOptional.get();
                    int reloadTime = mainWeapon.getReloadTime();
                    int remainAmmo = mainWeapon.getRemainAmmo();
                    int maxAmmo = mainWeapon.getMaxCapacity();
                    poseStack.pushPose();
                    {
                        guiGraphics.drawString(font, "Ammo: " + remainAmmo + " / " + maxAmmo, 10, screenHeight / 2 + y, 0xFFFFFF);
                        if (reloadTime > 0) {
                            guiGraphics.drawString(font, mainWeapon.getDisplayName().getString() + String.format(" Reloading: %.2fs", (float) reloadTime / 20), 10, screenHeight / 2 + y - 15, 0xFF0000);
                        } else {
                            guiGraphics.drawString(font, mainWeapon.getDisplayName(), 10, screenHeight / 2 + y - 15, 0xFFFFFF);
                        }
                    }
                    poseStack.popPose();
                }
                if (!weaponUnit.secondaryWeapons.isEmpty()) {
                    y += 35;
                    Optional<AbstractVehicleWeapon<?>> secondaryWeaponOptional = weaponUnit.getCurrentSecondaryWeapon();
                    if (secondaryWeaponOptional.isPresent()) {
                        AbstractVehicleWeapon<?> secondaryWeapon = secondaryWeaponOptional.get();
                        int reloadTime = secondaryWeapon.getReloadTime();
                        int remainAmmo = secondaryWeapon.getRemainAmmo();
                        int maxAmmo = secondaryWeapon.getMaxCapacity();
                        poseStack.pushPose();
                        {
                            guiGraphics.drawString(font, "Ammo: " + remainAmmo + " / " + maxAmmo, 10, screenHeight / 2 + y, 0xFFFFFF);
                            if (reloadTime > 0) {
                                guiGraphics.drawString(font, secondaryWeapon.getDisplayName().getString() + String.format(" Reloading: %.2fs", (float) reloadTime / 20), 10, screenHeight / 2 + y - 15, 0xFF0000);
                            } else {
                                guiGraphics.drawString(font, secondaryWeapon.getDisplayName(), 10, screenHeight / 2 + y - 15, 0xFFFFFF);
                            }
                        }
                        poseStack.popPose();
                    }
                }
                if (!weaponUnit.independentWeapons.isEmpty()) {
                    y += 35;
                    for (AbstractVehicleWeapon<?> independentWeapon : weaponUnit.independentWeapons) {
                        int reloadTime = independentWeapon.getReloadTime();
                        int remainAmmo = independentWeapon.getRemainAmmo();
                        int maxAmmo = independentWeapon.getMaxCapacity();
                        guiGraphics.drawString(font, "Ammo: " + remainAmmo + " / " + maxAmmo, 10, screenHeight / 2 + y, 0xFFFFFF);
                        if (reloadTime > 0) {
                            guiGraphics.drawString(font, independentWeapon.getDisplayName().getString() + String.format(" Reloading: %.2fs", (float) reloadTime / 20), 10, screenHeight / 2 + y - 15, 0xFF0000);
                        } else {
                            guiGraphics.drawString(font, independentWeapon.getDisplayName(), 10, screenHeight / 2 + y - 15, 0xFFFFFF);
                        }
                        y += 35;
                    }
                }
            }
        }
        poseStack.popPose();
    }

}
