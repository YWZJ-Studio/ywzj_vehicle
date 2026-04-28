package org.ywzj.vehicle.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.render.util.GuiHelper;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.weapon.VehicleWeaponAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleWeaponOverlay implements LayeredDraw.Layer {

    private static final int CARD_W        = 130;
    private static final int CARD_H        = 58;
    private static final int MARGIN_RIGHT  = 4;
    private static final int MARGIN_BOTTOM = -12;
    private static final int RING_R        = 14;
    private static final int TAB_H         = 14;
    private static final int TAB_EXTRA     = 5;

    private static final int COL_BG         = 0xCC0A1408;
    private static final int COL_BG_ACTIVE  = 0xCC0E2A10;
    private static final int COL_BORDER     = 0xFF2A7A3A;
    private static final int COL_TEXT       = 0xFFD0F0D8;
    private static final int COL_AMMO       = 0xFF60E880;
    private static final int COL_RELOAD     = 0xFFFF6040;
    private static final int COL_RING_FG    = 0xFF30D050;
    private static final int COL_TAB_TEXT   = 0xFF5A9060;
    private static final int COL_TAB_ACTIVE = 0xFFD8F8E0;

    private record WeaponEntry(AbstractVehicleWeapon<?> weapon, String abbr) {}

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        if (!instance.onVehicle()) return;

        AbstractVehicle vehicle = instance.getVehicle();
        PartUnit<?> operatorUnit = vehicle.getOwnOperatorUnit(instance.getPlayer());
        if (!(operatorUnit instanceof WeaponUnit weaponUnit)) return;

        List<WeaponEntry> entries = buildEntries(weaponUnit);
        if (entries.isEmpty()) return;

        int activeIndex = findActiveIndex(weaponUnit, entries);

        Font font = Minecraft.getInstance().font;
        guiGraphics.pose().pushPose();
        {
            int tabRowH    = TAB_H + TAB_EXTRA;
            int cardRight  = screenWidth  - MARGIN_RIGHT;
            int cardBottom = screenHeight - MARGIN_BOTTOM - tabRowH;
            guiGraphics.pose().translate(cardRight, cardBottom, 0);
            guiGraphics.pose().scale(0.75f, 0.75f, 0.75f);
            int cardLeft = -CARD_W;
            int cardTop  = -CARD_H;

            renderTabs(guiGraphics, font, entries, activeIndex, cardLeft);
            if (activeIndex >= 0 && activeIndex < entries.size()) {
                renderCard(guiGraphics, font, entries.get(activeIndex), cardLeft, cardTop);
            }
        }
        guiGraphics.pose().popPose();
    }

    private List<WeaponEntry> buildEntries(WeaponUnit weaponUnit) {
        List<WeaponEntry> list = new ArrayList<>();
        List<AbstractVehicleWeapon<?>> weapons = new ArrayList<>();
        weapons.addAll(weaponUnit.weapons);
        weapons.addAll(weaponUnit.secondaryWeapons);
        weapons.addAll(weaponUnit.independentWeapons);
        for (AbstractVehicleWeapon<?> weapon : weapons) {
            if (weapon instanceof VehicleWeaponAgent weaponAgent) {
                Optional<AbstractVehicleWeapon<?>> weaponOptional = weaponAgent.getWeaponUnit().getCurrentWeapon();
                if (weaponOptional.isPresent()) {
                    weapon = weaponOptional.get();
                }
            }
            list.add(new WeaponEntry(weapon, weapon.getDisplayName().getString().trim()));
        }
        return list;
    }

    private int findActiveIndex(WeaponUnit weaponUnit, List<WeaponEntry> entries) {
        Optional<AbstractVehicleWeapon<?>> main = weaponUnit.getCurrentWeapon();
        if (main.isPresent()) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).weapon() == main.get()) return i;
            }
        }
        Optional<AbstractVehicleWeapon<?>> sec = weaponUnit.getCurrentSecondaryWeapon();
        if (sec.isPresent()) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).weapon() == sec.get()) return i;
            }
        }
        return entries.isEmpty() ? -1 : 0;
    }

    private void renderTabs(GuiGraphics gg, Font font, List<WeaponEntry> entries, int activeIndex, int cardLeft) {
        int tabCount = entries.size();
        if (tabCount == 0) return;

        int totalW = -cardLeft;
        int baseW  = totalW / tabCount;
        int rem    = totalW % tabCount;

        int[] tabX = new int[tabCount + 1];
        tabX[0] = cardLeft;
        for (int i = 0; i < tabCount; i++) {
            tabX[i + 1] = tabX[i] + baseW + (i < rem ? 1 : 0);
        }

        for (int i = 0; i < tabCount; i++) {
            boolean active = (i == activeIndex);
            WeaponEntry entry = entries.get(i);

            int tx  = tabX[i];
            int tx2 = tabX[i + 1];

            int tabH   = active ? (TAB_H + TAB_EXTRA) : TAB_H;
            int tabTop = -tabH;
            int tabBot = 0;

            int bgCol = active ? COL_BG_ACTIVE : COL_BG;
            RenderHelper.fill(gg, RenderType.guiOverlay(), tx, tabTop, tx2, tabBot, 0, bgCol);

            gg.hLine(tx, tx2 - 1, tabTop, Color.GREEN);
            if (active) {
                gg.hLine(tx + 1, tx2 - 2, tabTop + 1, Color.GREEN);
            }

            if (i > 0) {
                gg.vLine(tx, tabTop, tabBot - 1, Color.GREEN);
            }

            int textCol = active ? COL_TAB_ACTIVE : COL_TAB_TEXT;
            int lengthShow = baseW / 7;
            int lengthText = entry.abbr().length();
            String abbr = entry.abbr().substring(0, Math.min(lengthShow, lengthText));
            if (lengthShow < lengthText) {
                abbr += "..";
            }
            int textX = tx + 3;
            int textY = tabTop + (tabH - font.lineHeight) / 2;
            gg.drawString(font, abbr, textX, textY, textCol, true);
        }
    }

    private void renderCard(GuiGraphics gg, Font font, WeaponEntry entry, int cardLeft, int cardTop) {
        AbstractVehicleWeapon<?> weapon = entry.weapon();
        int remainAmmo = weapon.getRemainAmmo();
        int maxAmmo    = weapon.getMaxCapacity();
        int reloadTime = weapon.getReloadTime();

        RenderHelper.fill(gg, RenderType.guiOverlay(),
                cardLeft, cardTop, cardLeft + VehicleWeaponOverlay.CARD_W, cardTop + VehicleWeaponOverlay.CARD_H, 0, COL_BG);

        gg.hLine(cardLeft, cardLeft + VehicleWeaponOverlay.CARD_W - 1, cardTop,             COL_BORDER);
        gg.hLine(cardLeft, cardLeft + VehicleWeaponOverlay.CARD_W - 1, cardTop + VehicleWeaponOverlay.CARD_H - 1, COL_BORDER);
        gg.vLine(cardLeft,             cardTop, cardTop + VehicleWeaponOverlay.CARD_H - 1,  COL_BORDER);
        gg.vLine(cardLeft + VehicleWeaponOverlay.CARD_W - 1, cardTop, cardTop + VehicleWeaponOverlay.CARD_H - 1,  COL_BORDER);

        int padding     = 5;
        int contentLeft = cardLeft + padding;
        int contentTop  = cardTop  + padding;
        int textAreaW   = VehicleWeaponOverlay.CARD_W - padding * 2 - RING_R * 2 - 6;

        // 主武器名
        String weaponName = GuiHelper.truncateText(font, weapon.getDisplayName().getString(), textAreaW);
        gg.drawString(font, Component.literal(weaponName), contentLeft, contentTop, COL_TEXT, false);
        // 主武器弹药数
        boolean isReloading = reloadTime > 0;
        int ammoY = contentTop + font.lineHeight + 4;
        if (isReloading) {
            gg.drawString(font, "RELOADING", contentLeft, ammoY, COL_RELOAD, false);
        } else {
            String ammoStr = remainAmmo + " / " + maxAmmo;
            int ammoCol = remainAmmo == 0 ? COL_RELOAD
                        : remainAmmo <= maxAmmo / 4 ? Color.AMMO_WARNING
                        : COL_AMMO;
            gg.drawString(font, ammoStr, contentLeft, ammoY, ammoCol, false);
        }
        // 主武器弹量条
        int barY = ammoY + font.lineHeight + 4;
        int barH = 3;
        float ammoFrac = maxAmmo > 0 ? (float) remainAmmo / maxAmmo : 0f;
        RenderHelper.fill(gg, RenderType.guiOverlay(),
                contentLeft, barY, contentLeft + textAreaW * 0.8f, barY + barH, 0, Color.AMMO_BAR_BG);
        int filledW = (int) (textAreaW * 0.8f * ammoFrac);
        if (filledW > 0) {
            int barFgCol = remainAmmo == 0 ? COL_RELOAD
                         : remainAmmo <= maxAmmo / 4 ? Color.AMMO_WARNING
                         : COL_RING_FG;
            RenderHelper.fill(gg, RenderType.guiOverlay(),
                    contentLeft, barY, contentLeft + filledW, barY + barH, 0, barFgCol);
        }

        int infoAreaX = cardLeft + VehicleWeaponOverlay.CARD_W - padding * 2 - RING_R * 2 - 2;
        int infoAreaW = RING_R * 2 + padding;
        WeaponUnit weaponUnit = weapon.getWeaponUnit().getRootParentWeaponUnit();
        // 副武器
        AbstractVehicleWeapon<?> secWeapon = weaponUnit.getCurrentSecondaryWeapon().orElse(null);
        if (secWeapon != null) {
            String secName = GuiHelper.truncateText(font, secWeapon.getDisplayName().getString(), infoAreaW);
            String secAmmo = secWeapon.getRemainAmmo() + "/" + secWeapon.getMaxCapacity();
            int secAmmoCol = secWeapon.getReloadTime() > 0 ? COL_RELOAD
                    : secWeapon.getRemainAmmo() == 0 ? COL_RELOAD
                    : secWeapon.getRemainAmmo() <= secWeapon.getMaxCapacity() / 4 ? Color.AMMO_WARNING
                    : COL_AMMO;
            int secNameY = cardTop + padding;
            int secAmmoY = secNameY + font.lineHeight + 1;
            gg.drawString(font, Component.literal(secName),
                    infoAreaX + (infoAreaW - font.width(secName)) / 2, secNameY, COL_TEXT, false);
            gg.drawString(font, secAmmo,
                    infoAreaX + (infoAreaW - font.width(secAmmo)) / 2, secAmmoY, secAmmoCol, false);
        }
        // 单武器
        List<AbstractVehicleWeapon<?>> indepWeapons = weaponUnit.independentWeapons;
        if (!indepWeapons.isEmpty()) {
            AbstractVehicleWeapon<?> indep = indepWeapons.get(0);
            String indepName = GuiHelper.truncateText(font, indep.getDisplayName().getString(), infoAreaW);
            String indepAmmo = indep.getRemainAmmo() + "/" + indep.getMaxCapacity();
            int indepAmmoCol = indep.getReloadTime() > 0 ? COL_RELOAD
                    : indep.getRemainAmmo() == 0 ? COL_RELOAD
                    : indep.getRemainAmmo() <= indep.getMaxCapacity() / 4 ? Color.AMMO_WARNING
                    : COL_AMMO;
            int indepNameY = cardTop + VehicleWeaponOverlay.CARD_H / 2 - 4;
            int indepAmmoY = indepNameY + font.lineHeight + 1;
            gg.drawString(font, Component.literal(indepName),
                    infoAreaX + (infoAreaW - font.width(indepName)) / 2, indepNameY, COL_TEXT, false);
            gg.drawString(font, indepAmmo,
                    infoAreaX + (infoAreaW - font.width(indepAmmo)) / 2, indepAmmoY, indepAmmoCol, false);
        }
    }

}
