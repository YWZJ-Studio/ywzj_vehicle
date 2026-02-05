package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

/**
 * Optimized weapon status overlay for vehicles.
 * Features caching, improved rendering, and enhanced visual design.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class VehicleWeaponOverlay implements IGuiOverlay {

    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 15;
    private static final int WEAPON_SPACING = 35;
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 4;
    
    // Colors
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SHADOW = 0xFF000000;
    private static final int COLOR_RELOADING = 0xFFFF5555;
    private static final int COLOR_LOW_AMMO = 0xFFFFAA00;
    private static final int COLOR_AMMO_BAR_BG = 0x88000000;
    private static final int COLOR_AMMO_BAR_FULL = 0xFF55FF55;
    private static final int COLOR_AMMO_BAR_LOW = 0xFFFFAA00;
    private static final int COLOR_AMMO_BAR_EMPTY = 0xFFFF5555;
    
    // Cache for formatted strings to reduce GC pressure
    private String cachedAmmoText = "";
    private String cachedReloadText = "";
    private int lastAmmo = -1;
    private int lastMaxAmmo = -1;
    private int lastReloadTime = -1;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        if (!instance.onVehicle()) {
            return;
        }
        
        AbstractVehicle vehicle = instance.getVehicle();
        PartUnit operatorUnit = vehicle.getOwnOperatorUnit(instance.getPlayer());
        
        if (!(operatorUnit instanceof WeaponUnit weaponUnit)) {
            return;
        }
        
        Font font = Minecraft.getInstance().font;
        PoseStack poseStack = guiGraphics.pose();
        int yOffset = screenHeight / 2 - 20;
        
        // Render main weapon
        final int mainWeaponY = yOffset;
        weaponUnit.getCurrentWeapon().ifPresent(weapon -> {
            renderWeapon(guiGraphics, font, weapon, PADDING, mainWeaponY, true);
        });
        
        // Render independent weapons
        yOffset += WEAPON_SPACING;
        for (AbstractVehicleWeapon<?> weapon : weaponUnit.independentWeapons) {
            renderWeapon(guiGraphics, font, weapon, PADDING, yOffset, false);
            yOffset += WEAPON_SPACING;
        }
    }

    /**
     * Renders a single weapon's status with optimized caching and visual enhancements.
     */
    private void renderWeapon(GuiGraphics guiGraphics, Font font, AbstractVehicleWeapon<?> weapon, 
                              int x, int y, boolean isMainWeapon) {
        int reloadTime = weapon.getReloadTime();
        int remainAmmo = weapon.getRemainAmmo();
        int maxAmmo = weapon.getMaxCapacity();
        
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        // Weapon name (always visible, no reload text)
        Component weaponName = weapon.getDisplayName();
        drawTextWithShadow(guiGraphics, font, weaponName, x, y, COLOR_TEXT);
        
        y += LINE_HEIGHT;
        
        // Reload progress bar (shows when reloading)
        if (reloadTime > 0) {
            int maxReloadTime = weapon.getData().getReload().getTime();
            if (maxReloadTime > 0) {
                float reloadPercent = 1.0f - ((float) reloadTime / maxReloadTime);
                renderReloadBar(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, reloadPercent);
                y += BAR_HEIGHT + 4;
            }
        }
        
        // Ammo count below the bar
        if (lastAmmo != remainAmmo || lastMaxAmmo != maxAmmo) {
            cachedAmmoText = remainAmmo + " / " + maxAmmo;
            lastAmmo = remainAmmo;
            lastMaxAmmo = maxAmmo;
        }
        
        float ammoPercent = maxAmmo > 0 ? (float) remainAmmo / maxAmmo : 0;
        int ammoColor = getAmmoColor(ammoPercent);
        
        drawTextWithShadow(guiGraphics, font, cachedAmmoText, x, y, ammoColor);
        
        poseStack.popPose();
    }

    /**
     * Renders a reload progress bar.
     */
    private void renderReloadBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float fillPercent) {
        // Background
        guiGraphics.fill(x, y, x + width, y + height, COLOR_AMMO_BAR_BG);
        
        // Fill bar (orange/yellow for reload)
        int fillWidth = (int) (width * fillPercent);
        if (fillWidth > 0) {
            guiGraphics.fill(x, y, x + fillWidth, y + height, COLOR_LOW_AMMO);
        }
        
        // Border
        drawBorder(guiGraphics, x, y, width, height, 0xFF444444);
    }

    /**
     * Draws a border around a rectangle.
     */
    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, color); // Top
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, color); // Bottom
        guiGraphics.fill(x - 1, y, x, y + height, color); // Left
        guiGraphics.fill(x + width, y, x + width + 1, y + height, color); // Right
    }

    /**
     * Draws text with shadow for better readability.
     */
    private void drawTextWithShadow(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x, y, color, true);
    }

    /**
     * Draws text with shadow for better readability.
     */
    private void drawTextWithShadow(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x, y, color, true);
    }

    /**
     * Returns color based on ammo percentage for text.
     */
    private int getAmmoColor(float percent) {
        if (percent <= 0.2f) {
            return COLOR_RELOADING;
        } else if (percent <= 0.4f) {
            return COLOR_LOW_AMMO;
        }
        return COLOR_TEXT;
    }

    /**
     * Returns color based on ammo percentage for bar with smooth transitions.
     */
    private int getAmmoBarColor(float percent) {
        if (percent <= 0.2f) {
            return COLOR_AMMO_BAR_EMPTY;
        } else if (percent <= 0.4f) {
            return COLOR_AMMO_BAR_LOW;
        }
        return COLOR_AMMO_BAR_FULL;
    }

}
