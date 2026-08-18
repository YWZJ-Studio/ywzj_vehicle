package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.render.Drawer;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleSelectPartWeapon;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleWeaponSelectScreen extends ApricityScreen {

    private static final String TEMPLATE = "screens/vehicle_weapon_select.html";

    private final AbstractVehicle vehicle;
    private final Screen parent;
    private final List<WeaponSelectionEntry> weaponSelections = new ArrayList<>();
    private Document document;
    private Element selectionList;
    private Element selectionEmpty;
    private Element weaponList;
    private Element weaponEmpty;
    private Element selectedPartName;
    private Element selectedWeaponDescription;
    private Element previewAnchor;
    private Element closeButton;
    private int selectedWeaponSelectionIndex;
    private int selectedWeaponIndex = -1;
    private float viewShiftX;
    private float viewShiftY;
    private float viewScale = 1;
    private float viewRotX;
    private float viewRotY;
    private boolean previewDragging;

    public VehicleWeaponSelectScreen(AbstractVehicle vehicle, Screen parent) {
        super(TEMPLATE);
        this.vehicle = vehicle;
        this.parent = parent;
        this.viewRotX = 180 - vehicle.getXRot();
        this.viewRotY = 180 + LocalVehiclePlayer.instance.getPlayer().getYRot();
        refreshWeaponSelections();
        this.selectedWeaponSelectionIndex = weaponSelections.isEmpty() ? -1 : 0;
        setPauseGame(false);
        setShowDefaultBackground(false);
    }

    @Override
    protected void init() {
        super.init();
        document = getLinkedDocument();
        if (document == null) {
            return;
        }
        selectionList = element("weapon-selection-list");
        selectionEmpty = element("weapon-selection-empty");
        weaponList = element("weapon-list");
        weaponEmpty = element("weapon-list-empty");
        selectedPartName = element("selected-part-name");
        selectedWeaponDescription = element("selected-weapon-description");
        previewAnchor = element("weapon-vehicle-preview");
        closeButton = element("close-button");
        if (closeButton != null) {
            closeButton.addEventListener("click", event -> onClose());
        }
        buildSelectionList();
        refreshWeaponPanel();
    }

    private Element element(String id) {
        return document.getElementById(id);
    }

    private void refreshWeaponSelections() {
        weaponSelections.clear();
        for (PartUnit<?> partUnit : vehicle.getPartUnits()) {
            if (partUnit instanceof WeaponUnit weaponUnit && weaponUnit.isInteractive() && weaponUnit.weapons.size() > 1) {
                weaponSelections.add(new WeaponSelectionEntry(partUnit.getIndex(), weaponUnit));
            }
        }
    }

    private void buildSelectionList() {
        if (selectionList == null) {
            return;
        }
        selectionList.setTextContent("");
        for (int index = 0; index < weaponSelections.size(); index++) {
            WeaponSelectionEntry selection = weaponSelections.get(index);
            Element entry = document.createElement("div");
            entry.setClassName("weapon-entry");
            entry.setAttribute("role", "button");
            entry.setAttribute("tabindex", "0");
            Element name = document.createElement("span");
            name.setClassName("weapon-entry-name");
            name.setTextContent(selection.weaponUnit.getName().getString());
            Element count = document.createElement("span");
            count.setClassName("weapon-entry-count");
            count.setTextContent((selection.weaponUnit.getCurrentWeaponIndex() + 1) + "/" + selection.weaponUnit.weapons.size());
            entry.appendChild(name);
            entry.appendChild(count);
            int selectionIndex = index;
            entry.addEventListener("click", event -> selectWeaponSelection(selectionIndex, true));
            selectionList.appendChild(entry);
        }
        setHidden(selectionEmpty, !weaponSelections.isEmpty());
    }

    private void selectWeaponSelection(int index, boolean playSound) {
        if (index < 0 || index >= weaponSelections.size()) {
            return;
        }
        selectedWeaponSelectionIndex = index;
        selectedWeaponIndex = weaponSelections.get(index).weaponUnit.getCurrentWeaponIndex();
        refreshWeaponPanel();
        if (playSound) {
            playClickSound();
        }
    }

    private void refreshWeaponPanel() {
        refreshSelectionClasses();
        if (selectedWeaponSelection() == null) {
            setText(selectedPartName, Component.translatable("screen.vehicle_weapon_select.select_part").getString());
            setDescriptionText(Component.translatable("screen.no_description").getString());
            if (weaponList != null) {
                weaponList.setTextContent("");
            }
            setHidden(weaponEmpty, false);
            return;
        }
        WeaponSelectionEntry selection = selectedWeaponSelection();
        selectedWeaponIndex = selection.weaponUnit.getCurrentWeaponIndex();
        setText(selectedPartName, selection.weaponUnit.getName().getString());
        buildWeaponList(selection);
        updateWeaponDescription(selection);
    }

    private void refreshSelectionClasses() {
        if (selectionList == null) {
            return;
        }
        List<Element> entries = selectionList.getChildren();
        for (int index = 0; index < entries.size(); index++) {
            entries.get(index).setClassName(index == selectedWeaponSelectionIndex
                    ? "weapon-entry active" : "weapon-entry");
            document.markDirty(entries.get(index), Drawer.RELAYOUT | Drawer.REPAINT);
        }
    }

    private void buildWeaponList(WeaponSelectionEntry selection) {
        if (weaponList == null) {
            return;
        }
        weaponList.setTextContent("");
        for (int index = 0; index < selection.weaponUnit.weapons.size(); index++) {
            AbstractVehicleWeapon<?> weapon = selection.weaponUnit.proxyWeapon(selection.weaponUnit.weapons.get(index));
            Element entry = document.createElement("div");
            entry.setClassName(index == selectedWeaponIndex
                    ? "weapon-entry active" : "weapon-entry");
            entry.setAttribute("role", "button");
            entry.setTextContent(weapon.getDisplayName().getString());
            int weaponIndex = index;
            entry.addEventListener("click", event -> onWeaponSelected(selection, weaponIndex));
            weaponList.appendChild(entry);
        }
        setHidden(weaponEmpty, !selection.weaponUnit.weapons.isEmpty());
    }

    private void onWeaponSelected(WeaponSelectionEntry selection, int weaponIndex) {
        selectedWeaponIndex = weaponIndex;
        buildWeaponList(selection);
        updateWeaponDescription(selection);
        Channel.CHANNEL.sendToServer(new ClientVehicleSelectPartWeapon(vehicle.getId(), selection.partUnitIndex, weaponIndex));
        playClickSound();
    }

    private void updateWeaponDescription(WeaponSelectionEntry selection) {
        int currentIndex = selectedWeaponIndex >= 0 && selectedWeaponIndex < selection.weaponUnit.weapons.size()
                ? selectedWeaponIndex : selection.weaponUnit.getCurrentWeaponIndex();
        if (currentIndex < 0 || currentIndex >= selection.weaponUnit.weapons.size()) {
            setDescriptionText(Component.translatable("screen.no_description").getString());
            return;
        }
        AbstractVehicleWeapon<?> weapon = selection.weaponUnit.proxyWeapon(selection.weaponUnit.weapons.get(currentIndex));
        Optional<String> description = ClientAssetsManager.INSTANCE.getWeaponDisplay(weapon.getData().getWeaponId())
                .map(BaseDisplay::getDescription)
                .filter(text -> text != null && !text.isBlank());
        setDescriptionText(Component.translatable(description.orElse("screen.no_description")).getString());
    }

    private WeaponSelectionEntry selectedWeaponSelection() {
        return selectedWeaponSelectionIndex >= 0 && selectedWeaponSelectionIndex < weaponSelections.size()
                ? weaponSelections.get(selectedWeaponSelectionIndex) : null;
    }

    private void setText(Element element, String text) {
        if (element != null) {
            element.setTextContent(text);
        }
    }

    private void setDescriptionText(String text) {
        if (selectedWeaponDescription != null) {
            selectedWeaponDescription.setTextContent(text);
        }
    }

    private void setHidden(Element element, boolean hidden) {
        if (element != null) {
            element.setClassName(hidden ? "empty-state hidden" : "empty-state");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderVehiclePreview(graphics);
    }

    private void renderVehiclePreview(GuiGraphics graphics) {
        Bounds bounds = getPreviewBounds();
        if (bounds == null) {
            return;
        }
        double scale = Math.min(bounds.width(), bounds.height()) * 1.5 / Math.max(vehicle.getStructureLength(), 3) * viewScale;
        graphics.enableScissor((int) bounds.left, (int) bounds.top, (int) Math.ceil(bounds.right), (int) Math.ceil(bounds.bottom));
        boolean scissorEnabled = true;
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate((float) bounds.centerX() + viewShiftX, (float) bounds.centerY() + viewShiftY, 512);
            poseStack.mulPoseMatrix(new Matrix4f().scaling((float) scale, (float) scale, (float) -scale));
            poseStack.mulPose(Axis.XP.rotationDegrees(viewRotX));
            poseStack.mulPose(Axis.YP.rotationDegrees(viewRotY));
            Vec3 centerOffset = vehicle.getBoundingBox().getCenter().subtract(vehicle.position());
            poseStack.translate((float) -centerOffset.x, (float) -centerOffset.y, (float) -centerOffset.z);
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            Lighting.setupForEntityInInventory();
            RenderSystem.runAsFancy(() ->
                    dispatcher.render(vehicle, 0, 0, 0, 0, 1.0F, poseStack, graphics.bufferSource(), 15728880));
            graphics.flush();
            graphics.disableScissor();
            scissorEnabled = false;
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, false);
            renderSelectedWeaponArrow(poseStack, graphics);
            graphics.flush();
        } finally {
            dispatcherResetShadow();
            Lighting.setupFor3DItems();
            poseStack.popPose();
            if (scissorEnabled) {
                graphics.disableScissor();
            }
        }
    }

    private void renderSelectedWeaponArrow(PoseStack poseStack, GuiGraphics graphics) {
        WeaponSelectionEntry selection = selectedWeaponSelection();
        if (selection == null) {
            return;
        }
        Vec3 arrowPosition = selection.weaponUnit.aimContext().from.subtract(vehicle.position());
        poseStack.pushPose();
        poseStack.translate(arrowPosition.x, arrowPosition.y, arrowPosition.z);
        org.ywzj.vehicle.util.RenderHelper.renderArrow3D(
                poseStack, graphics.bufferSource(), 0.15f, 0.3f, 0, 255, 0, 255);
        poseStack.popPose();
    }

    private void dispatcherResetShadow() {
        Minecraft.getInstance().getEntityRenderDispatcher().setRenderShadow(true);
    }

    private Bounds getPreviewBounds() {
        if (previewAnchor == null || document == null) {
            return null;
        }
        Element.DOMRect rect = previewAnchor.getBoundingClientRect();
        if (rect.width <= 0 || rect.height <= 0) {
            return null;
        }
        double scale = document.getViewport().renderScale();
        return new Bounds(rect.x * scale, rect.y * scale, (rect.x + rect.width) * scale, (rect.y + rect.height) * scale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Bounds bounds = getPreviewBounds();
        if (bounds != null && bounds.contains(mouseX, mouseY)) {
            viewScale = Mth.clamp(viewScale * (float) Math.pow(1.1, delta), 0.1f, 10.0f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Bounds bounds = getPreviewBounds();
        if ((button == 0 || button == 1) && bounds != null && bounds.contains(mouseX, mouseY)) {
            previewDragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (previewDragging && button == 0) {
            viewRotY += (float) dragX;
            viewRotX += (float) dragY;
            return true;
        }
        if (previewDragging && button == 1) {
            viewShiftX += (float) dragX;
            viewShiftY += (float) dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (previewDragging && (button == 0 || button == 1)) {
            previewDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean isPauseScreen() { return false; }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private record WeaponSelectionEntry(int partUnitIndex, WeaponUnit weaponUnit) {}

    private record Bounds(double left, double top, double right, double bottom) {
        private boolean contains(double x, double y) { return x >= left && x <= right && y >= top && y <= bottom; }
        private double width() { return right - left; }
        private double height() { return bottom - top; }
        private double centerX() { return (left + right) / 2; }
        private double centerY() { return (top + bottom) / 2; }
    }
}
