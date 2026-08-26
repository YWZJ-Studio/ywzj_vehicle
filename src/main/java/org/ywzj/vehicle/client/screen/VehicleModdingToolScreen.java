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
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleChangeDisplay;
import org.ywzj.vehicle.util.AuiTextHelper;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class VehicleModdingToolScreen extends ApricityScreen {

    private static final String TEMPLATE = "screens/vehicle_modding_tool.html";
    private final AbstractVehicle vehicle;
    private final List<ResourceLocation> variableDisplayIds = new ArrayList<>();
    private final List<DisplayEntry> displayEntries = new ArrayList<>();
    private Document document;
    private Element searchInput;
    private Element displayList;
    private Element displayEmpty;
    private Element selectedDisplayName;
    private Element selectedDisplayId;
    private Element description;
    private Element previewAnchor;
    private Element weaponButton;
    private Element smokeControls;
    private Element smokeRInput;
    private Element smokeGInput;
    private Element smokeBInput;
    private Element smokeSwatch;
    private Element closeButton;
    private ResourceLocation selectedDisplay;
    private String filter = "";
    private String appliedQuery;
    private float viewShiftX;
    private float viewShiftY;
    private float viewScale = 1;
    private float viewRotX;
    private float viewRotY;
    private boolean previewDragging;

    public VehicleModdingToolScreen(AbstractVehicle vehicle) {
        super(TEMPLATE);
        this.vehicle = vehicle;
        this.viewRotX = 180 - vehicle.getXRot();
        this.viewRotY = 180 + LocalVehiclePlayer.instance.getPlayer().getYRot();
        ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).ifPresent(display -> {
            List<BaseDisplay> displays = ClientAssetsManager.INSTANCE.getVariableDisplay(display.getModelPath());
            displays.stream().map(BaseDisplay::getDisplayId).distinct().forEach(variableDisplayIds::add);
        });
        variableDisplayIds.sort((left, right) -> left.toString().compareTo(right.toString()));
        setPauseGame(false);
        setShowDefaultBackground(false);
    }

    @Override
    protected void init() {
        appliedQuery = null;
        super.init();
        document = getLinkedDocument();
        if (document == null) {
            return;
        }
        searchInput = element("display-search");
        displayList = element("display-list");
        displayEmpty = element("display-empty");
        selectedDisplayName = element("selected-display-name");
        selectedDisplayId = element("selected-display-id");
        description = element("vehicle-description");
        previewAnchor = element("modding-preview");
        weaponButton = element("weapon-button");
        smokeControls = element("smoke-controls");
        smokeRInput = element("smoke-red");
        smokeGInput = element("smoke-green");
        smokeBInput = element("smoke-blue");
        smokeSwatch = element("smoke-swatch");
        closeButton = element("close-button");

        buildDisplayCatalog();
        if (searchInput != null) {
            searchInput.setValue(filter);
            searchInput.addEventListener("input", event -> applyFilter(searchInput.getValue()));
            searchInput.addEventListener("change", event -> applyFilter(searchInput.getValue()));
        }
        if (closeButton != null) {
            closeButton.addEventListener("click", event -> onClose());
        }
        if (weaponButton != null) {
            weaponButton.addEventListener("click", event -> Minecraft.getInstance().setScreen(new VehicleWeaponSelectScreen(vehicle, this)));
            if (!hasWeaponSelections()) {
                weaponButton.setDisabled(true);
            }
        }
        bindSmokeControls();
        applyFilter(filter);
        refreshVehicleProfile();
    }

    private Element element(String id) { return document.getElementById(id); }

    private boolean hasWeaponSelections() {
        return vehicle.getPartUnits().stream().anyMatch(partUnit ->
                partUnit instanceof WeaponUnit weaponUnit && weaponUnit.isInteractive() && weaponUnit.weapons.size() > 1);
    }

    private void buildDisplayCatalog() {
        if (displayList == null) {
            return;
        }
        displayList.setTextContent("");
        displayEntries.clear();
        for (ResourceLocation displayId : variableDisplayIds) {
            Element entry = document.createElement("div");
            entry.setClassName("display-entry");
            entry.setAttribute("role", "button");
            entry.setAttribute("tabindex", "0");
            entry.setTextContent(displayId.getPath());
            entry.setAttribute("title", displayId.toString());
            entry.addEventListener("click", event -> onDisplaySelected(displayId));
            displayList.appendChild(entry);
            displayEntries.add(new DisplayEntry(displayId, entry));
        }
    }

    private void applyFilter(String text) {
        filter = text == null ? "" : text;
        String query = filter.strip().toLowerCase(Locale.ROOT);
        if (query.equals(appliedQuery)) {
            return;
        }
        appliedQuery = query;
        int visible = 0;
        for (DisplayEntry displayEntry : displayEntries) {
            boolean matches = query.isEmpty() || displayEntry.displayId.toString().toLowerCase(Locale.ROOT).contains(query);
            displayEntry.visible = matches;
            displayEntry.element.setInlineStyleProperty("display", matches ? "flex" : "none");
            document.markDirty(displayEntry.element, Drawer.RELAYOUT | Drawer.REPAINT);
            if (matches) visible++;
        }
        setText(displayEmpty, visible == 0 ? Component.translatable("screen.vehicle_modding_tool.no_results").getString() : "");
        setHidden(displayEmpty, visible > 0);
        selectedDisplay = null;
        refreshDisplayClasses();
    }

    private void refreshDisplayClasses() {
        for (DisplayEntry displayEntry : displayEntries) {
            displayEntry.element.setClassName(displayEntry.displayId.equals(selectedDisplay)
                    ? "display-entry active" : "display-entry");
        }
    }

    private void onDisplaySelected(ResourceLocation displayId) {
        selectedDisplay = displayId;
        refreshDisplayClasses();
        Channel.CHANNEL.sendToServer(new ClientVehicleChangeDisplay(vehicle.getId(), displayId));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        refreshVehicleProfile(displayId);
    }

    private void refreshVehicleProfile() { refreshVehicleProfile(selectedDisplay); }

    private void refreshVehicleProfile(ResourceLocation displayId) {
        if (displayId == null) {
            setText(selectedDisplayName, Component.translatable("screen.vehicle_modding_tool.select_display").getString());
            setText(selectedDisplayId, "--");
            setDescriptionText(getVehicleDescription().map(Component::getString).orElse(Component.translatable("screen.no_description").getString()));
            return;
        }
        setText(selectedDisplayName, displayId.getPath());
        setText(selectedDisplayId, displayId.getNamespace());
        String descriptionKey = ClientAssetsManager.INSTANCE.getVehicleDisplay(displayId)
                .map(BaseDisplay::getDescription).filter(text -> text != null && !text.isBlank()).orElse("screen.no_description");
        setDescriptionText(Component.translatable(descriptionKey).getString());
    }

    private Optional<Component> getVehicleDescription() {
        return ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId())
                .map(BaseDisplay::getDescription).filter(text -> text != null && !text.isBlank()).map(Component::translatable);
    }

    private void bindSmokeControls() {
        if (smokeControls != null) {
            smokeControls.setClassName("smoke-controls");
        }
        AllConfigs.CommonConfig common = AllConfigs.common;
        bindSmoke(smokeRInput, common.aerobaticSmokeR.get(), value -> common.aerobaticSmokeR.set(value));
        bindSmoke(smokeGInput, common.aerobaticSmokeG.get(), value -> common.aerobaticSmokeG.set(value));
        bindSmoke(smokeBInput, common.aerobaticSmokeB.get(), value -> common.aerobaticSmokeB.set(value));
        refreshSmokeSwatch();
    }

    private void bindSmoke(Element input, int initial, IntConsumer consumer) {
        if (input == null) return;
        input.setValue(Integer.toString(initial));
        Element valueInput = element(input.getAttribute("id") + "-value");
        if (valueInput != null) {
            valueInput.setValue(Integer.toString(initial));
        }
        input.addEventListener("input", event -> applySmokeRange(input, valueInput, initial, consumer));
        input.addEventListener("change", event -> applySmokeRange(input, valueInput, initial, consumer));
        if (valueInput != null) {
            valueInput.addEventListener("input", event -> applySmokeValue(input, valueInput, consumer, false));
            valueInput.addEventListener("change", event -> applySmokeValue(input, valueInput, consumer, true));
        }
    }

    private void applySmokeRange(Element input, Element valueInput, int fallback, IntConsumer consumer) {
        int value = parseInt(input.getValue(), fallback);
        consumer.accept(value);
        if (valueInput != null) {
            String text = Integer.toString(value);
            if (!text.equals(valueInput.getValue())) {
                valueInput.setValue(text);
            }
        }
        refreshSmokeSwatch();
    }

    private void applySmokeValue(Element input, Element valueInput, IntConsumer consumer, boolean commit) {
        int value;
        try {
            value = Mth.clamp(Integer.parseInt(valueInput.getValue()), 0, 255);
        } catch (NumberFormatException exception) {
            return;
        }
        input.setValue(Integer.toString(value));
        consumer.accept(value);
        if (commit) {
            valueInput.setValue(Integer.toString(value));
        }
        refreshSmokeSwatch();
    }

    private void refreshSmokeSwatch() {
        if (smokeSwatch == null) return;
        AllConfigs.CommonConfig common = AllConfigs.common;
        int color = (common.aerobaticSmokeR.get() << 16) | (common.aerobaticSmokeG.get() << 8) | common.aerobaticSmokeB.get();
        smokeSwatch.setInlineStyleProperty("background-color", String.format(Locale.ROOT, "#%06x", color));
    }

    private int parseInt(String value, int fallback) {
        try { return Mth.clamp(Integer.parseInt(value), 0, 255); } catch (NumberFormatException exception) { return fallback; }
    }

    private void setText(Element element, String text) { if (element != null) element.setTextContent(text); }
    private void setDescriptionText(String text) { AuiTextHelper.setDescription(description, text); }
    private void setHidden(Element element, boolean hidden) { if (element != null) element.setClassName(hidden ? "empty-state hidden" : "empty-state"); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderVehiclePreview(graphics);
    }

    private void renderVehiclePreview(GuiGraphics graphics) {
        Bounds bounds = getPreviewBounds();
        if (bounds == null) return;
        double scale = Math.min(bounds.width(), bounds.height()) * 1.5 / Math.max(vehicle.getStructureLength(), 3) * viewScale;
        graphics.enableScissor((int) bounds.left, (int) bounds.top, (int) Math.ceil(bounds.right), (int) Math.ceil(bounds.bottom));
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
            RenderSystem.runAsFancy(() -> dispatcher.render(vehicle, 0, 0, 0, 0, 1.0F, poseStack, graphics.bufferSource(), 15728880));
            graphics.flush();
        } finally {
            Minecraft.getInstance().getEntityRenderDispatcher().setRenderShadow(true);
            Lighting.setupFor3DItems();
            poseStack.popPose();
            graphics.disableScissor();
        }
    }

    private Bounds getPreviewBounds() {
        if (previewAnchor == null || document == null) return null;
        Element.DOMRect rect = previewAnchor.getBoundingClientRect();
        if (rect.width <= 0 || rect.height <= 0) return null;
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
        if ((button == 0 || button == 1) && bounds != null && bounds.contains(mouseX, mouseY)) { previewDragging = true; return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (previewDragging && button == 0) { viewRotY += (float) dragX; viewRotX += (float) dragY; return true; }
        if (previewDragging && button == 1) { viewShiftX += (float) dragX; viewShiftY += (float) dragY; return true; }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (previewDragging && (button == 0 || button == 1)) { previewDragging = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public boolean isPauseScreen() { return false; }

    private static final class DisplayEntry {
        private final ResourceLocation displayId;
        private final Element element;
        private boolean visible;

        private DisplayEntry(ResourceLocation displayId, Element element) {
            this.displayId = displayId;
            this.element = element;
            this.visible = true;
        }
    }
    @FunctionalInterface private interface IntConsumer { void accept(int value); }
    private record Bounds(double left, double top, double right, double bottom) {
        private boolean contains(double x, double y) { return x >= left && x <= right && y >= top && y <= bottom; }
        private double width() { return right - left; }
        private double height() { return bottom - top; }
        private double centerX() { return (left + right) / 2; }
        private double centerY() { return (top + bottom) / 2; }
    }

}
