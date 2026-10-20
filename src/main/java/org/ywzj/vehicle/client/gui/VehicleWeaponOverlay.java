package org.ywzj.vehicle.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.ywzj.vehicle.all.AllKeys;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.weapon.VehicleMultiWeapons;
import org.ywzj.vehicle.vehicle.weapon.VehicleWeaponAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class VehicleWeaponOverlay implements LayeredDraw.Layer {

    private static final int PANEL_WIDTH = 168;
    private static final int MARGIN = 18;
    private static final int ROW_HEIGHT = 14;
    private static final int MAX_VISIBLE_ROWS = 10;
    private static final int DETAIL_HEIGHT = 42;
    private static final int AUXILIARY_HEIGHT = 21;
    private static final int GROUP_GAP = 2;
    private static final int SEPARATOR_SPACING = 5;
    private static final int READOUT_CONTENT_Y = 14;
    private static final int PADDING = 4;
    private static final float AMMO_SCALE = 2.8f;
    private static final float CAPACITY_SCALE = 0.9f;
    private static final float SECONDARY_SCALE = 0.8f;
    private static final float AMMUNITION_SCALE = 0.7f;
    private static final int WHITE = 0xFFF2F2F2;
    private static final int GRAY = 0xFFB8B8B8;
    private static final int DARK_TEXT = 0xFF161616;
    private static final int RELOAD_GRAY = 0xB0B0B0B0;
    private static final int AMMO_ORANGE = 0xFFFFA33D;
    private static final int AMMO_RED = 0xFFFF4A3D;
    private final Map<AbstractVehicleWeapon<?>, ReloadAnimation> reloadAnimations = new WeakHashMap<>();
    private long renderFrame;

    private static final class ReloadAnimation {
        int remainingTicks;
        int totalTicks;
        double sampleTick;
        float progress;
        long lastFrame;
    }

    private record WeaponEntry(AbstractVehicleWeapon<?> weapon, String name, int sourceIndex) {}

    private record WeaponGroup(List<WeaponEntry> entries, int activeIndex, float scale, String hint) {
        int visibleRows() {
            return Math.min(entries.size(), MAX_VISIBLE_ROWS);
        }

        int firstRow() {
            return Mth.clamp(Math.max(0, activeIndex) - visibleRows() / 2, 0, entries.size() - visibleRows());
        }

        float height() {
            if (entries.isEmpty()) {
                return 0;
            }
            int rows = visibleRows() + (hint.isEmpty() ? 0 : 1) + (entries.size() > visibleRows() ? 1 : 0);
            return rows * ROW_HEIGHT * scale;
        }

        WeaponEntry activeEntry() {
            return activeIndex >= 0 ? entries.get(activeIndex) : null;
        }
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        renderFrame++;
        LocalVehiclePlayer player = LocalVehiclePlayer.instance;
        if (!player.onVehicle()) {
            reloadAnimations.clear();
            return;
        }
        PartUnit<?> operator = player.vehicle.getOwnOperatorUnit(player.getPlayer());
        if (!(operator instanceof WeaponUnit weaponUnit)) {
            reloadAnimations.clear();
            return;
        }
        List<WeaponEntry> primaryEntries = buildEntries(weaponUnit, weaponUnit.weapons);
        List<WeaponEntry> secondaryEntries = buildEntries(weaponUnit, weaponUnit.secondaryWeapons);
        List<WeaponEntry> independentEntries = buildEntries(weaponUnit, weaponUnit.independentWeapons);
        if (primaryEntries.isEmpty() && secondaryEntries.isEmpty() && independentEntries.isEmpty()) {
            return;
        }
        String primaryHint = "[" + Component.translatable("ui.vehicle_hud.mouse_wheel").getString() + "] ↑↓";
        WeaponGroup primary = new WeaponGroup(primaryEntries,
                findActiveIndex(primaryEntries, weaponUnit.getCurrentWeaponIndex()), 1, primaryHint);
        String secondaryHint = "[" + AllKeys.SECONDARY_WEAPON_SWITCH.getTranslatedKeyMessage().getString() + "] ↑↓";
        WeaponGroup secondary = new WeaponGroup(secondaryEntries,
                findActiveIndex(secondaryEntries, weaponUnit.getCurrentSecondaryWeaponIndex()), SECONDARY_SCALE, secondaryHint);
        List<WeaponEntry> auxiliary = new ArrayList<>();
        if (secondary.activeEntry() != null) {
            auxiliary.add(secondary.activeEntry());
        }
        auxiliary.addAll(independentEntries);
        Font font = Minecraft.getInstance().font;
        try (VehicleHudLayout layout = VehicleHudLayout.begin(graphics)) {
            int availableWidth = Math.min(Math.max(96, (int) (layout.width() / 2 - 84)),
                    Math.max(48, (int) layout.width() - MARGIN * 2));
            int width = Math.min(PANEL_WIDTH, availableWidth);
            float left = layout.width() - MARGIN - width;
            float groupGap = primary.height() > 0 && secondary.height() > 0 ? GROUP_GAP : 0;
            float listHeight = primary.height() + groupGap + secondary.height();
            float listTop = VehicleHudLayout.HEIGHT - MARGIN - listHeight;
            float separatorY = listTop - (listHeight > 0 ? 3 : 0);
            renderReadouts(graphics, font, primary.activeEntry(), auxiliary, left,
                    listTop - (listHeight > 0 ? SEPARATOR_SPACING : 0), separatorY, width, partialTick);
            if (listHeight > 0) {
                drawSeparator(graphics, left, separatorY, width, false);
            }
            renderWeaponGroup(graphics, font, primary, left, listTop, width, true);
            renderWeaponGroup(graphics, font, secondary, left, listTop + primary.height() + groupGap, width, true);
        }
    }

    private List<WeaponEntry> buildEntries(WeaponUnit weaponUnit, List<AbstractVehicleWeapon<?>> weapons) {
        List<WeaponEntry> entries = new ArrayList<>();
        for (int i = 0; i < weapons.size(); i++) {
            AbstractVehicleWeapon<?> resolved = weaponUnit.proxyWeapon(weapons.get(i));
            if (!(resolved instanceof VehicleWeaponAgent)) {
                entries.add(new WeaponEntry(resolved, resolved.getDisplayName().getString().trim(), i));
            }
        }
        return entries;
    }

    private int findActiveIndex(List<WeaponEntry> entries, int sourceIndex) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).sourceIndex() == sourceIndex) {
                return i;
            }
        }
        return -1;
    }

    private void renderWeaponGroup(GuiGraphics graphics, Font font, WeaponGroup group, float left, float top,
                                   int panelWidth, boolean showAmmo) {
        if (group.entries().isEmpty()) {
            return;
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left, top, 0);
            graphics.pose().scale(group.scale(), group.scale(), 1);
            int width = (int) (panelWidth / group.scale());
            int listTop = group.hint().isEmpty() ? 0 : ROW_HEIGHT;
            if (!group.hint().isEmpty()) {
                graphics.drawString(font, truncate(font, group.hint(), width), PADDING, 1, GRAY, true);
            }
            int first = group.firstRow();
            for (int row = 0; row < group.visibleRows(); row++) {
                int index = first + row;
                WeaponEntry entry = group.entries().get(index);
                boolean active = index == group.activeIndex();
                int y = listTop + row * ROW_HEIGHT;
                if (active) {
                    graphics.fill(0, y, width, y + ROW_HEIGHT - 2, WHITE);
                }
                String prefix = (entry.sourceIndex() + 1) + ". ";
                String ammo = showAmmo ? ammoText(entry.weapon()) : "";
                int ammoWidth = font.width(ammo);
                int nameWidth = width - PADDING * 2 - font.width(prefix) - (showAmmo ? ammoWidth + 12 : 0);
                String name = truncate(font, entry.name(), nameWidth);
                graphics.drawString(font, prefix + name, PADDING, y + 2, active ? DARK_TEXT : WHITE, !active);
                if (showAmmo) {
                    graphics.drawString(font, ammo, width - PADDING - ammoWidth, y + 2,
                            active ? DARK_TEXT : GRAY, !active);
                }
            }
            if (group.entries().size() > group.visibleRows()) {
                String range = (first + 1) + "-" + (first + group.visibleRows()) + " / " + group.entries().size();
                graphics.drawString(font, range, width - font.width(range),
                        listTop + group.visibleRows() * ROW_HEIGHT + 2, GRAY, true);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private void renderReadouts(GuiGraphics graphics, Font font, WeaponEntry primary, List<WeaponEntry> auxiliary,
                                float left, float bottom, float separatorY, int width, float partialTick) {
        WeaponGroup ammunition = buildAmmunitionGroup(primary);
        int primaryHeight = primary == null ? 0 : Math.max(DETAIL_HEIGHT, READOUT_CONTENT_Y + (int) Math.ceil(ammunition.height()) + 2);
        boolean beside = primary != null && !auxiliary.isEmpty() && width >= 160;
        int primaryWidth = beside ? Math.round((width - 10) * 0.7f) : width;
        int auxiliaryWidth = beside ? width - primaryWidth - 10 : width;
        int columns = auxiliary.isEmpty() ? 1 : beside ? 1 : Math.max(1, Math.min(auxiliary.size(), (auxiliaryWidth + 8) / 84));
        int rows = (auxiliary.size() + columns - 1) / columns;
        int auxiliaryHeight = rows * AUXILIARY_HEIGHT;
        int detailHeight = beside ? Math.max(primaryHeight, auxiliaryHeight)
                : primaryHeight + auxiliaryHeight + (primary != null && !auxiliary.isEmpty() ? GROUP_GAP : 0);
        float top = bottom - detailHeight;
        if (primary != null) {
            float areaRight = left + primaryWidth + (beside ? 5 : 0);
            float areaBottom = beside || auxiliary.isEmpty() ? separatorY : top + primaryHeight + GROUP_GAP / 2f;
            renderReloadArea(graphics, primary.weapon(), left, top - 2, areaRight, areaBottom, partialTick);
            renderWeaponReadout(graphics, font, primary, left, top, primaryWidth, false, ammunition);
        }
        float auxiliaryLeft = beside ? left + primaryWidth + 10 : left;
        float auxiliaryTop = beside || primary == null ? top : top + primaryHeight + GROUP_GAP;
        if (primary != null && !auxiliary.isEmpty()) {
            if (beside) {
                drawSeparator(graphics, left + primaryWidth + 5, top, separatorY - top, true);
            } else {
                drawSeparator(graphics, left, top + primaryHeight + GROUP_GAP / 2f, width, false);
            }
        }
        int columnWidth = (auxiliaryWidth - (columns - 1) * 8) / columns;
        float separatorLeft = beside ? left + primaryWidth + 5 : auxiliaryLeft;
        for (int row = 1; row < rows; row++) {
            drawSeparator(graphics, separatorLeft, auxiliaryTop + row * AUXILIARY_HEIGHT - 1.5f,
                    auxiliaryLeft + auxiliaryWidth - separatorLeft, false);
        }
        float columnTop = primary != null && !beside ? top + primaryHeight + GROUP_GAP / 2f : auxiliaryTop;
        for (int column = 1; column < columns; column++) {
            drawSeparator(graphics, auxiliaryLeft + column * (columnWidth + 8) - 4,
                    columnTop, separatorY - columnTop, true);
        }
        for (int i = 0; i < auxiliary.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            float itemLeft = auxiliaryLeft + column * (columnWidth + 8);
            float itemTop = auxiliaryTop + row * AUXILIARY_HEIGHT;
            float areaLeft = column == 0 ? separatorLeft : itemLeft - 4;
            float areaTop = row == 0 ? (primary != null && !beside ? columnTop : itemTop - 2) : itemTop - 1.5f;
            float areaRight = column == columns - 1 ? auxiliaryLeft + auxiliaryWidth : itemLeft + columnWidth + 4;
            float areaBottom = row == rows - 1 ? separatorY : itemTop + AUXILIARY_HEIGHT - 1.5f;
            renderReloadArea(graphics, auxiliary.get(i).weapon(), areaLeft, areaTop, areaRight, areaBottom, partialTick);
            renderWeaponReadout(graphics, font, auxiliary.get(i), itemLeft, itemTop, columnWidth, true, null);
        }
    }

    private void renderReloadArea(GuiGraphics graphics, AbstractVehicleWeapon<?> weapon,
                                   float left, float top, float right, float bottom, float partialTick) {
        float progress = reloadProgress(weapon, partialTick);
        if (progress > 0) {
            RenderHelper.fill(graphics, RenderType.gui(), left, top,
                    left + (right - left) * progress, bottom, 0, RELOAD_GRAY);
        }
    }

    private void drawSeparator(GuiGraphics graphics, float x, float y, float length, boolean vertical) {
        float thickness = VehicleHudLayout.HEIGHT / Math.max(1, Minecraft.getInstance().getWindow().getHeight());
        float left = vertical ? x - thickness / 2 : x;
        float top = vertical ? y : y - thickness / 2;
        RenderHelper.fill(graphics, RenderType.gui(), left, top,
                left + (vertical ? thickness : length), top + (vertical ? length : thickness), 0, WHITE);
    }

    private WeaponGroup buildAmmunitionGroup(WeaponEntry primary) {
        if (primary != null && primary.weapon() instanceof VehicleMultiWeapons multi && multi.getSubWeapons().size() > 1) {
            List<WeaponEntry> entries = buildEntries(multi.getWeaponUnit(), multi.getSubWeapons());
            return new WeaponGroup(entries, findActiveIndex(entries, multi.getSelectedIndex()), AMMUNITION_SCALE, "");
        }
        return new WeaponGroup(List.of(), -1, AMMUNITION_SCALE, "");
    }

    private void renderWeaponReadout(GuiGraphics graphics, Font font, WeaponEntry entry,
                                     float left, float top, int width, boolean compact,
                                     WeaponGroup ammunition) {
        AbstractVehicleWeapon<?> weapon = entry.weapon();
        boolean withAmmunition = ammunition != null && !ammunition.entries().isEmpty();
        String switchHint = "[" + AllKeys.MULTI_WEAPON_SWITCH.getTranslatedKeyMessage().getString() + "] ↑↓";
        float hintScale = Math.min(0.65f, width * 0.3f / Math.max(1, font.width(switchHint)));
        float hintOffset = width - font.width(switchHint) * hintScale;
        float nameScale = compact ? 0.65f : 1.1f;
        float nameInset = width * 0.04f;
        int nameWidth = (int) (Math.max(0, hintOffset - nameInset - 2) / nameScale);
        String name = truncate(font, entry.name(), nameWidth);
        drawScaledText(graphics, font, name, left + nameInset, top, nameScale, WHITE);
        if (withAmmunition) {
            drawScaledText(graphics, font, switchHint, left + hintOffset, top + 1, hintScale, GRAY);
        }
        float ammoWidth = withAmmunition ? width * 0.4f : width;
        float ammoY = top + (compact ? 9 : READOUT_CONTENT_Y);
        renderAmmoReadout(graphics, font, weapon, left + width - ammoWidth, ammoY, ammoWidth, compact);
        if (withAmmunition) {
            renderWeaponGroup(graphics, font, ammunition, left, top + READOUT_CONTENT_Y, (int) (width - ammoWidth - 6), false);
        }
    }

    private void renderAmmoReadout(GuiGraphics graphics, Font font, AbstractVehicleWeapon<?> weapon,
                                   float left, float top, float width, boolean compact) {
        String maximum = String.valueOf(Math.max(0, weapon.getMaxCapacity()));
        String remaining = String.valueOf(Math.max(0, weapon.getRemainAmmo()));
        String capacity = "/" + maximum;
        float capacityScale = compact ? 0.6f : CAPACITY_SCALE;
        float capacityWidth = font.width(capacity) * capacityScale;
        int maximumWidth = Math.max(1, font.width(maximum));
        float ammoScale = Math.min(compact ? 1 : AMMO_SCALE, (width - capacityWidth - 3) / maximumWidth);
        float capacityLeft = left + width - capacityWidth;
        float remainingLeft = capacityLeft - 3 - font.width(remaining) * ammoScale;
        drawScaledText(graphics, font, remaining, remainingLeft, top, ammoScale, ammoColor(weapon));
        drawScaledText(graphics, font, capacity, capacityLeft, top + 1, capacityScale, GRAY);
    }

    private float reloadProgress(AbstractVehicleWeapon<?> weapon, float partialTick) {
        while (weapon instanceof VehicleMultiWeapons multi) {
            weapon = multi.getSelectedWeapon();
        }
        int remainingTicks = weapon.getReloadTime();
        int totalTicks = weapon.getData().getReload().getTime();
        ReloadAnimation animation = reloadAnimations.get(weapon);
        if (remainingTicks <= 0 || totalTicks <= 0) {
            reloadAnimations.remove(weapon);
            return totalTicks > 0 && animation != null && animation.lastFrame >= renderFrame - 1 ? 1 : -1;
        }
        double frameTick = (double) Minecraft.getInstance().level.getGameTime() + partialTick;
        if (animation == null) {
            animation = new ReloadAnimation();
            reloadAnimations.put(weapon, animation);
        }
        boolean reset = animation.totalTicks != totalTicks || remainingTicks > animation.remainingTicks
                || animation.lastFrame < renderFrame - 1 || frameTick < animation.sampleTick;
        if (reset || remainingTicks != animation.remainingTicks) {
            animation.remainingTicks = remainingTicks;
            animation.totalTicks = totalTicks;
            animation.sampleTick = frameTick;
        }
        if (reset) {
            animation.progress = 0;
        }
        double remaining = Math.max(0, remainingTicks - (frameTick - animation.sampleTick));
        animation.progress = Math.max(animation.progress, Mth.clamp((float) (1 - remaining / totalTicks), 0, 1));
        animation.lastFrame = renderFrame;
        return animation.progress;
    }

    private static String ammoText(AbstractVehicleWeapon<?> weapon) {
        return Math.max(0, weapon.getRemainAmmo()) + "/" + Math.max(0, weapon.getMaxCapacity());
    }

    private static int ammoColor(AbstractVehicleWeapon<?> weapon) {
        int capacity = weapon.getMaxCapacity();
        if (capacity <= 0) {
            return WHITE;
        }
        float ratio = Mth.clamp(weapon.getRemainAmmo() / (capacity * 0.1f), 0, 1);
        if (ratio >= 1) {
            return WHITE;
        }
        int from = ratio >= 0.5f ? AMMO_ORANGE : AMMO_RED;
        int to = ratio >= 0.5f ? WHITE : AMMO_ORANGE;
        float blend = ratio >= 0.5f ? (ratio - 0.5f) * 2 : ratio * 2;
        int r = Math.round(Mth.lerp(blend, (from >> 16) & 255, (to >> 16) & 255));
        int g = Math.round(Mth.lerp(blend, (from >> 8) & 255, (to >> 8) & 255));
        int b = Math.round(Mth.lerp(blend, from & 255, to & 255));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static String truncate(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        if (width < font.width("...")) {
            return "";
        }
        return font.plainSubstrByWidth(text, width - font.width("...")) + "...";
    }

    private static void drawScaledText(GuiGraphics graphics, Font font, String text,
                                       float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, text, 0, 0, color, true);
        } finally {
            graphics.pose().popPose();
        }
    }

}
