package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ClientVehicleSelectPartWeapon;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import java.util.ArrayList;
import java.util.List;

public class VehicleWeaponSelectScreen extends Screen {

    private static final int ITEM_HEIGHT = 20;
    private static final int WEAPON_SELECTION_LIST_WIDTH = 170;
    private static final int WEAPON_LIST_WIDTH = 220;
    private static final int WEAPON_LIST_VISIBLE_ITEMS = 4;
    private static final int DESCRIPTION_PADDING = 4;
    private static final int BOTTOM_PADDING = 18;
    private final AbstractVehicle vehicle;
    private final Screen parent;
    private final List<WeaponSelectionEntry> weaponSelections = new ArrayList<>();
    private int leftPos;
    private int topPos;
    private int weaponSelectionScrollOffset;
    private int weaponScrollOffset;
    private int selectedWeaponSelectionIndex;
    private float viewShiftX;
    private float viewShiftY;
    private float viewScale = 1;
    private float viewRotX;
    private float viewRotY;

    public VehicleWeaponSelectScreen(AbstractVehicle vehicle, Screen parent) {
        super(Component.literal("Vehicle Weapon Select"));
        this.vehicle = vehicle;
        this.parent = parent;
        this.viewRotX = 180 - vehicle.getXRot();
        this.viewRotY = 180 + LocalVehiclePlayer.instance.getPlayer().getYRot();
        refreshWeaponSelections();
        this.selectedWeaponSelectionIndex = weaponSelections.isEmpty() ? -1 : 0;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - WEAPON_SELECTION_LIST_WIDTH - WEAPON_LIST_WIDTH - 16) / 2;
        this.topPos = this.height / 2 - 32;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(0, 0, this.width, this.height, Color.BG_DARK);
        drawVehiclePreview(guiGraphics);
        if (weaponSelections.isEmpty()) {
            return;
        } else {
            drawWeaponSelectionList(guiGraphics, mouseX, mouseY);
            drawWeaponList(guiGraphics, mouseX, mouseY);
        }
    }

    private void refreshWeaponSelections() {
        weaponSelections.clear();
        for (PartUnit<?> partUnit : vehicle.getPartUnits()) {
            if (partUnit instanceof WeaponUnit weaponUnit && weaponUnit.isInteractive() && weaponUnit.weapons.size() > 1) {
                weaponSelections.add(new WeaponSelectionEntry(partUnit.getIndex(), weaponUnit));
            }
        }
    }

    private void drawWeaponSelectionList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("screen.vehicle_weapon_select.weapon_selections"), leftPos, topPos - 12, Color.GRAY);
        guiGraphics.fill(leftPos, topPos, leftPos + WEAPON_SELECTION_LIST_WIDTH, bottom(), Color.BG_LIST);
        int visibleItems = visibleItems();
        int end = Math.min(weaponSelectionScrollOffset + visibleItems, weaponSelections.size());
        for (int index = weaponSelectionScrollOffset; index < end; index++) {
            WeaponSelectionEntry entry = weaponSelections.get(index);
            int itemY = topPos + (index - weaponSelectionScrollOffset) * ITEM_HEIGHT;
            boolean hovered = mouseX >= leftPos + 2 && mouseX <= leftPos + WEAPON_SELECTION_LIST_WIDTH - 6 && mouseY >= itemY + 1 && mouseY <= itemY + ITEM_HEIGHT - 1;
            boolean selected = index == selectedWeaponSelectionIndex;
            int bgColor = selected ? Color.ITEM_SELECTED : (hovered ? Color.ITEM_HOVERED : Color.ITEM_NORMAL);
            guiGraphics.fill(leftPos + 2, itemY + 1, leftPos + WEAPON_SELECTION_LIST_WIDTH - 6, itemY + ITEM_HEIGHT - 1, bgColor);
            String progress = (entry.weaponUnit.getCurrentWeaponIndex() + 1) + "/" + entry.weaponUnit.weapons.size();
            guiGraphics.drawString(font, entry.weaponUnit.getName(), leftPos + 8, itemY + 6, Color.WHITE);
            guiGraphics.drawString(font, progress, leftPos + WEAPON_SELECTION_LIST_WIDTH - 8 - font.width(progress), itemY + 6, Color.WHITE);
        }
        drawScrollBar(guiGraphics, leftPos + WEAPON_SELECTION_LIST_WIDTH - 4, topPos, bottom(), weaponSelections.size(), weaponSelectionScrollOffset);
    }

    private void drawVehiclePreview(GuiGraphics guiGraphics) {
        double length = vehicle.getStructureLength();
        float scale = (float) (1 / Math.max(length, 3) * 196) * this.viewScale;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate((double) width / 2 + viewShiftX, Math.max(50, topPos / 2.0 + 20) + viewShiftY, 512);
            poseStack.last().pose().mul(new Matrix4f().scaling(scale, scale, -scale));
            poseStack.mulPose(Axis.XP.rotationDegrees(viewRotX));
            poseStack.mulPose(Axis.YP.rotationDegrees(viewRotY));
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            Lighting.setupForEntityInInventory();
            RenderSystem.runAsFancy(() -> {
                dispatcher.render(
                        vehicle,
                        0, 0, 0,
                        0,
                        1.0F,
                        poseStack,
                        guiGraphics.bufferSource(),
                        15728880
                );
                WeaponSelectionEntry weaponSelection = selectedWeaponSelection();
                if (weaponSelection != null) {
                    Vec3 arrowPosition = weaponSelection.weaponUnit.aimContext().from.subtract(vehicle.position());
                    poseStack.pushPose();
                    poseStack.translate(arrowPosition.x, arrowPosition.y, arrowPosition.z);
                    RenderHelper.renderArrow3D(poseStack, guiGraphics.bufferSource(), 0.15f, 0.3f, 0, 255, 0, 255);
                    poseStack.popPose();
                }
            });
        }
        poseStack.popPose();
    }

    private void drawWeaponList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        WeaponSelectionEntry weaponSelection = selectedWeaponSelection();
        if (weaponSelection == null) {
            return;
        }
        int x = leftPos + WEAPON_SELECTION_LIST_WIDTH + 16;
        int weaponListBottom = weaponListBottom();
        int descriptionTop = weaponListBottom + 4;
        guiGraphics.drawString(font, weaponSelection.weaponUnit.getName(), x, topPos - 12, Color.GRAY);
        guiGraphics.fill(x, topPos, x + WEAPON_LIST_WIDTH, bottom(), Color.BG_LIST);
        guiGraphics.fill(x, descriptionTop, x + WEAPON_LIST_WIDTH, bottom(), Color.BG_PREVIEW);
        int visibleItems = weaponListVisibleItems();
        int end = Math.min(weaponScrollOffset + visibleItems, weaponSelection.weaponUnit.weapons.size());
        for (int index = weaponScrollOffset; index < end; index++) {
            int itemY = topPos + (index - weaponScrollOffset) * ITEM_HEIGHT;
            boolean hovered = mouseX >= x + 2 && mouseX <= x + WEAPON_LIST_WIDTH - 6 && mouseY >= itemY + 1 && mouseY <= itemY + ITEM_HEIGHT - 1;
            boolean selected = index == weaponSelection.weaponUnit.getCurrentWeaponIndex();
            int bgColor = selected ? Color.ITEM_SELECTED : (hovered ? Color.ITEM_HOVERED : Color.ITEM_NORMAL);
            AbstractVehicleWeapon<?> weapon = weaponSelection.weaponUnit.proxyWeapon(weaponSelection.weaponUnit.weapons.get(index));
            guiGraphics.fill(x + 2, itemY + 1, x + WEAPON_LIST_WIDTH - 6, itemY + ITEM_HEIGHT - 1, bgColor);
            guiGraphics.drawString(font, weapon.getDisplayName(), x + 8, itemY + 6, Color.WHITE);
        }
        drawScrollBar(guiGraphics, x + WEAPON_LIST_WIDTH - 4, topPos, weaponListBottom + 4, weaponSelection.weaponUnit.weapons.size(), weaponScrollOffset);
        drawSelectedWeaponDescription(guiGraphics, weaponSelection, x, descriptionTop);
    }

    private int visibleItems() {
        return Math.max(1, (bottom() - topPos) / ITEM_HEIGHT);
    }

    private int weaponListVisibleItems() {
        return Math.max(1, Math.min(WEAPON_LIST_VISIBLE_ITEMS, (bottom() - topPos) / ITEM_HEIGHT));
    }

    private int weaponListBottom() {
        return topPos + weaponListVisibleItems() * ITEM_HEIGHT;
    }

    private void drawSelectedWeaponDescription(GuiGraphics guiGraphics, WeaponSelectionEntry weaponSelection, int x, int y) {
        int currentIndex = weaponSelection.weaponUnit.getCurrentWeaponIndex();
        if (currentIndex < 0 || currentIndex >= weaponSelection.weaponUnit.weapons.size()) {
            return;
        }
        AbstractVehicleWeapon<?> weapon = weaponSelection.weaponUnit.proxyWeapon(weaponSelection.weaponUnit.weapons.get(currentIndex));
        Component description = ClientAssetsManager.INSTANCE.getWeaponDisplay(weapon.getData().getWeaponId())
                .map(BaseDisplay::getDescription)
                .filter(text -> !text.isBlank())
                .map(Component::translatable)
                .orElse(Component.translatable("screen.no_description"));
        int textX = x + DESCRIPTION_PADDING;
        int textY = y + DESCRIPTION_PADDING;
        int maxWidth = WEAPON_LIST_WIDTH - DESCRIPTION_PADDING * 2;
        var lines = font.split(description, maxWidth);
        int maxLines = Math.max(0, (bottom() - textY - DESCRIPTION_PADDING) / 9);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            guiGraphics.drawString(font, lines.get(i), textX, textY + i * 9, Color.WHITE);
        }
    }

    private void drawScrollBar(GuiGraphics guiGraphics, int x, int y, int bottom, int size, int scrollOffset) {
        int barWidth = 6;
        int barHeight = bottom - y;
        int visibleItems = Math.max(1, barHeight / ITEM_HEIGHT);
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, Color.SCROLLBAR_TRACK);
        if (size <= visibleItems) {
            return;
        }
        int knobHeight = Math.max(12, barHeight * visibleItems / size);
        int movable = barHeight - knobHeight;
        int maxScroll = size - visibleItems;
        int knobY = y + (int) (movable * (scrollOffset / (float) maxScroll));
        guiGraphics.fill(x, knobY, x + barWidth, knobY + knobHeight, Color.SCROLLBAR_KNOB);
    }

    private int bottom() {
        return this.height - BOTTOM_PADDING;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && !weaponSelections.isEmpty()) {
            if (mouseX >= leftPos && mouseX <= leftPos + WEAPON_SELECTION_LIST_WIDTH - 5 && mouseY >= topPos && mouseY <= bottom()) {
                int clickedIndex = weaponSelectionScrollOffset + ((int) (mouseY - topPos) / ITEM_HEIGHT);
                if (clickedIndex >= 0 && clickedIndex < weaponSelections.size()) {
                    selectedWeaponSelectionIndex = clickedIndex;
                    weaponScrollOffset = 0;
                    playClickSound();
                    return true;
                }
            }
            WeaponSelectionEntry weaponSelection = selectedWeaponSelection();
            int weaponListX = leftPos + WEAPON_SELECTION_LIST_WIDTH + 16;
            if (weaponSelection != null && mouseX >= weaponListX && mouseX <= weaponListX + WEAPON_LIST_WIDTH - 5 && mouseY >= topPos && mouseY <= weaponListBottom()) {
                int clickedIndex = weaponScrollOffset + ((int) (mouseY - topPos) / ITEM_HEIGHT);
                if (clickedIndex >= 0 && clickedIndex < weaponSelection.weaponUnit.weapons.size()) {
                    PacketDistributor.sendToServer(new ClientVehicleSelectPartWeapon(vehicle.getId(), weaponSelection.partUnitIndex, clickedIndex));
                    playClickSound();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseY >= topPos && mouseY <= bottom()) {
            if (mouseX >= leftPos && mouseX <= leftPos + WEAPON_SELECTION_LIST_WIDTH) {
                if (weaponSelections.size() > visibleItems()) {
                    weaponSelectionScrollOffset = (int) Mth.clamp(weaponSelectionScrollOffset - Math.signum(deltaY), 0, weaponSelections.size() - visibleItems());
                    return true;
                }
            }
            WeaponSelectionEntry weaponSelection = selectedWeaponSelection();
            int weaponListX = leftPos + WEAPON_SELECTION_LIST_WIDTH + 16;
            int weaponListVisibleItems = weaponListVisibleItems();
            if (weaponSelection != null && mouseX >= weaponListX && mouseX <= weaponListX + WEAPON_LIST_WIDTH && mouseY <= weaponListBottom() && weaponSelection.weaponUnit.weapons.size() > weaponListVisibleItems) {
                weaponScrollOffset = (int) Mth.clamp(weaponScrollOffset - Math.signum(deltaY), 0, weaponSelection.weaponUnit.weapons.size() - weaponListVisibleItems);
                return true;
            }
        }
        if (deltaY > 0) {
            this.viewScale *= 1.1f;
        } else if (deltaY < 0) {
            this.viewScale *= 0.9f;
        }
        this.viewScale = Math.max(0.1f, Math.min(this.viewScale, 10.0f));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean handled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (!handled) {
            if (button == 0) {
                this.viewRotY += (float) dragX;
                this.viewRotX += (float) dragY;
                return true;
            } else if (button == 1) {
                this.viewShiftX += (float) dragX;
                this.viewShiftY += (float) dragY;
                return true;
            }
        }
        return handled;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private WeaponSelectionEntry selectedWeaponSelection() {
        if (selectedWeaponSelectionIndex >= 0 && selectedWeaponSelectionIndex < weaponSelections.size()) {
            return weaponSelections.get(selectedWeaponSelectionIndex);
        }
        return null;
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private record WeaponSelectionEntry(int partUnitIndex, WeaponUnit weaponUnit) {}

}
