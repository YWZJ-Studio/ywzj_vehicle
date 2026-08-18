package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.render.Drawer;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientDecorationAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DecorationSelectScreen extends ApricityScreen {

    private static final String TEMPLATE = "screens/decoration_select.html";

    private final List<ResourceLocation> decorationDisplayIds;
    private final List<DecorationEntry> decorationEntries = new ArrayList<>();

    private Document document;
    private Element searchInput;
    private Element decorationCount;
    private Element decorationGrid;
    private Element decorationGridEmpty;
    private Element selectedDecorationName;
    private Element selectedDecorationId;
    private Element previewAnchor;
    private Element description;
    private Element closeButton;

    private String filter = "";
    private String appliedQuery;
    private ResourceLocation selectedDisplayId;
    private BaseDisplay selectedDisplay;
    private double previewZoom = 1.0;
    private double previewPanX;
    private double previewPanY;
    private boolean previewDragging;
    private int gridSlotSize = -1;

    public DecorationSelectScreen() {
        super(TEMPLATE);
        this.decorationDisplayIds = ClientAssetsManager.INSTANCE.getDecorationDisplays().stream()
                .map(BaseDisplay::getDisplayId)
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        setPauseGame(false);
        setShowDefaultBackground(false);
    }

    @Override
    protected void init() {
        appliedQuery = null;
        gridSlotSize = -1;
        super.init();
        document = getLinkedDocument();
        if (document == null) {
            return;
        }

        searchInput = element("decoration-search");
        decorationCount = element("decoration-count");
        decorationGrid = element("decoration-grid");
        decorationGridEmpty = element("decoration-grid-empty");
        selectedDecorationName = element("selected-decoration-name");
        selectedDecorationId = element("selected-decoration-id");
        previewAnchor = element("decoration-preview");
        description = element("decoration-description");
        closeButton = element("close-button");

        buildDecorationCatalog();
        if (searchInput != null) {
            searchInput.setValue(filter);
            searchInput.addEventListener("input", event -> applyFilter(searchInput.getValue()));
            searchInput.addEventListener("change", event -> applyFilter(searchInput.getValue()));
        }
        if (closeButton != null) {
            closeButton.addEventListener("click", event -> onClose());
        }

        applyFilter(filter);
        updateGridSlotSize();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        appliedQuery = null;
        gridSlotSize = -1;
        super.resize(minecraft, width, height);
        gridSlotSize = -1;
        updateGridSlotSize();
    }

    private Element element(String id) {
        return document.getElementById(id);
    }

    private void buildDecorationCatalog() {
        if (decorationGrid == null) {
            return;
        }

        decorationGrid.setTextContent("");
        decorationEntries.clear();
        for (ResourceLocation displayId : decorationDisplayIds) {
            BaseDisplay display = ClientAssetsManager.INSTANCE.getDecorationDisplay(displayId).orElse(null);
            Element entry = document.createElement("button");
            entry.setClassName("decoration-entry");
            entry.setAttribute("type", "button");
            entry.setAttribute("aria-label", displayId.getPath());
            entry.setAttribute("title", displayId.toString());

            Element thumbnail = document.createElement("div");
            thumbnail.setClassName("decoration-thumbnail");
            boolean rendersModel = display != null && display.getSlotTexture() == null
                    && display.getModel() != null && display.getTexture() != null;
            Element texture = null;
            if (display != null && display.getSlotTexture() != null) {
                texture = document.createElement("texture");
                texture.setClassName("decoration-texture");
                texture.setAttribute("src", display.getSlotTexture().toString());
                thumbnail.appendChild(texture);
            }

            entry.appendChild(thumbnail);
            entry.addEventListener("click", event -> selectDisplay(displayId, true));
            decorationGrid.appendChild(entry);
            decorationEntries.add(new DecorationEntry(displayId, display, entry, thumbnail, texture, rendersModel));
        }
    }

    private void applyFilter(String text) {
        filter = text == null ? "" : text;
        String query = filter.strip().toLowerCase(Locale.ROOT);
        if (query.equals(appliedQuery)) {
            return;
        }
        appliedQuery = query;

        int visibleCount = 0;
        for (DecorationEntry decorationEntry : decorationEntries) {
            boolean visible = query.isEmpty()
                    || decorationEntry.displayId.toString().toLowerCase(Locale.ROOT).contains(query);
            decorationEntry.visible = visible;
            decorationEntry.element.setInlineStyleProperty("display", visible ? "flex" : "none");
            document.markDirty(decorationEntry.element, Drawer.RELAYOUT | Drawer.REPAINT);
            if (visible) {
                visibleCount++;
            }
        }

        setText(decorationCount, visibleCount + "/" + decorationEntries.size());
        setEmptyStateHidden(visibleCount > 0);
        clearSelection();
    }

    private void selectDisplay(ResourceLocation displayId, boolean playSound) {
        BaseDisplay display = ClientAssetsManager.INSTANCE.getDecorationDisplay(displayId).orElse(null);
        if (display == null) {
            clearSelection();
            return;
        }

        selectedDisplayId = displayId;
        selectedDisplay = display;
        resetPreviewTransform();
        refreshEntrySelection();
        setText(selectedDecorationName, displayId.getPath());
        setText(selectedDecorationId, displayId.getNamespace());

        String descriptionKey = display.getDescription();
        setDescriptionText(Component.translatable(descriptionKey == null || descriptionKey.isBlank()
                ? "screen.no_description"
                : descriptionKey).getString());

        ClientDecorationAction action = new ClientDecorationAction();
        action.action = ClientDecorationAction.Action.UPDATE_ITEM;
        action.displayId = displayId.toString();
        Channel.CHANNEL.sendToServer(action);
        if (playSound) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void clearSelection() {
        selectedDisplayId = null;
        selectedDisplay = null;
        resetPreviewTransform();
        refreshEntrySelection();
        setText(selectedDecorationName, Component.translatable("screen.decoration_select.preview").getString());
        setText(selectedDecorationId, "--");
        setDescriptionText(Component.translatable("screen.decoration_select.select_decoration").getString());
    }

    private void refreshEntrySelection() {
        for (DecorationEntry decorationEntry : decorationEntries) {
            boolean selected = decorationEntry.displayId.equals(selectedDisplayId);
            decorationEntry.element.setClassName(selected
                    ? "decoration-entry active"
                    : "decoration-entry");
            if (document != null) {
                document.markDirty(decorationEntry.element, Drawer.RELAYOUT | Drawer.REPAINT);
            }
        }
    }

    private void setEmptyStateHidden(boolean hidden) {
        if (decorationGridEmpty != null) {
            decorationGridEmpty.setClassName(hidden ? "empty-state hidden" : "empty-state");
        }
    }

    private void setText(Element element, String text) {
        if (element != null) {
            element.setTextContent(text);
        }
    }

    private void setDescriptionText(String text) {
        if (description != null) {
            description.setTextContent(text);
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateGridSlotSize();
        if (searchInput != null) {
            String currentFilter = searchInput.getValue() == null ? "" : searchInput.getValue();
            if (!filter.equals(currentFilter)) {
                applyFilter(currentFilter);
            }
        }
    }

    private void updateGridSlotSize() {
        if (decorationGrid == null || document == null) {
            return;
        }
        Element.DOMRect rect = decorationGrid.getBoundingClientRect();
        if (rect.width <= 0) {
            return;
        }

        // Grid chrome: 2px border + 4px padding on both sides, plus five 4px gaps.
        int slotSize = Math.max(28, (int) Math.floor((rect.width - 12 - 20) / 6.0));
        if (slotSize == gridSlotSize) {
            return;
        }
        gridSlotSize = slotSize;
        int thumbnailSize = Math.max(20, slotSize - 8);
        String slotPixels = slotSize + "px";
        String thumbnailPixels = thumbnailSize + "px";

        decorationGrid.setInlineStyleProperty("grid-template-columns", "repeat(6, " + slotPixels + ")");
        decorationGrid.setInlineStyleProperty("grid-auto-rows", slotPixels);
        for (DecorationEntry decorationEntry : decorationEntries) {
            decorationEntry.element.setInlineStyleProperty("width", slotPixels);
            decorationEntry.element.setInlineStyleProperty("height", slotPixels);
            decorationEntry.element.setInlineStyleProperty("min-width", slotPixels);
            decorationEntry.element.setInlineStyleProperty("min-height", slotPixels);
            decorationEntry.element.setInlineStyleProperty("max-width", slotPixels);
            decorationEntry.element.setInlineStyleProperty("max-height", slotPixels);
            decorationEntry.thumbnail.setInlineStyleProperty("width", thumbnailPixels);
            decorationEntry.thumbnail.setInlineStyleProperty("height", thumbnailPixels);
            decorationEntry.thumbnail.setInlineStyleProperty("min-width", thumbnailPixels);
            decorationEntry.thumbnail.setInlineStyleProperty("min-height", thumbnailPixels);
            decorationEntry.thumbnail.setInlineStyleProperty("max-width", thumbnailPixels);
            decorationEntry.thumbnail.setInlineStyleProperty("max-height", thumbnailPixels);
            decorationEntry.thumbnail.setInlineStyleProperty("flex", "0 0 " + thumbnailPixels);
            if (decorationEntry.texture != null) {
                decorationEntry.texture.setInlineStyleProperty("width", thumbnailPixels);
                decorationEntry.texture.setInlineStyleProperty("height", thumbnailPixels);
                decorationEntry.texture.setInlineStyleProperty("min-width", thumbnailPixels);
                decorationEntry.texture.setInlineStyleProperty("min-height", thumbnailPixels);
                decorationEntry.texture.setInlineStyleProperty("max-width", thumbnailPixels);
                decorationEntry.texture.setInlineStyleProperty("max-height", thumbnailPixels);
                decorationEntry.texture.setInlineStyleProperty("flex", "0 0 " + thumbnailPixels);
            }
        }
        document.markDirty(decorationGrid, Drawer.RELAYOUT | Drawer.REPAINT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Bounds bounds = getBounds(previewAnchor);
        if (!hasPreviewModel() || bounds == null || !bounds.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        double oldZoom = previewZoom;
        previewZoom = Mth.clamp(previewZoom * Math.pow(1.15, delta), 0.35, 4.0);
        double ratio = previewZoom / oldZoom;
        double modelCenterX = bounds.centerX() + previewPanX;
        double modelCenterY = bounds.modelCenterY() + previewPanY;
        previewPanX = mouseX + (modelCenterX - mouseX) * ratio - bounds.centerX();
        previewPanY = mouseY + (modelCenterY - mouseY) * ratio - bounds.modelCenterY();
        clampPreviewPan(bounds);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Bounds bounds = getBounds(previewAnchor);
        if (button == 0 && hasPreviewModel() && bounds != null && bounds.contains(mouseX, mouseY)) {
            setPreviewDragging(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && previewDragging) {
            previewPanX += dragX;
            previewPanY += dragY;
            Bounds bounds = getBounds(previewAnchor);
            if (bounds != null) {
                clampPreviewPan(bounds);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && previewDragging) {
            setPreviewDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean hasPreviewModel() {
        return selectedDisplay != null && selectedDisplay.getModel() != null && selectedDisplay.getTexture() != null;
    }

    private Bounds getBounds(Element element) {
        if (element == null || document == null) {
            return null;
        }
        Element.DOMRect rect = element.getBoundingClientRect();
        if (rect.width <= 0 || rect.height <= 0) {
            return null;
        }
        double viewportScale = document.getViewport().renderScale();
        return new Bounds(
                rect.x * viewportScale,
                rect.y * viewportScale,
                (rect.x + rect.width) * viewportScale,
                (rect.y + rect.height) * viewportScale
        );
    }

    private void clampPreviewPan(Bounds bounds) {
        previewPanX = Mth.clamp(previewPanX, -bounds.width() * 0.45, bounds.width() * 0.45);
        previewPanY = Mth.clamp(previewPanY, -bounds.height() * 0.45, bounds.height() * 0.45);
    }

    private void resetPreviewTransform() {
        previewZoom = 1.0;
        previewPanX = 0;
        previewPanY = 0;
        setPreviewDragging(false);
    }

    private void setPreviewDragging(boolean dragging) {
        previewDragging = dragging;
        if (previewAnchor != null) {
            previewAnchor.setClassName(dragging ? "dragging" : "");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFallbackThumbnails(graphics);
        renderDecorationPreview(graphics);
    }

    private void renderFallbackThumbnails(GuiGraphics graphics) {
        Bounds gridBounds = getBounds(decorationGrid);
        if (gridBounds == null) {
            return;
        }

        graphics.enableScissor((int) Math.floor(gridBounds.left), (int) Math.floor(gridBounds.top),
                (int) Math.ceil(gridBounds.right), (int) Math.ceil(gridBounds.bottom));
        try {
            Lighting.setupForEntityInInventory();
            for (DecorationEntry decorationEntry : decorationEntries) {
                if (!decorationEntry.visible || !decorationEntry.rendersModel || decorationEntry.display == null) {
                    continue;
                }
                Bounds slotBounds = getBounds(decorationEntry.element);
                if (slotBounds == null || !slotBounds.intersects(gridBounds)) {
                    continue;
                }
                double inset = Math.min(slotBounds.width(), slotBounds.height()) * 0.125;
                Bounds modelBounds = slotBounds.inset(inset);
                renderModel(graphics, decorationEntry.display, modelBounds.centerX(), modelBounds.centerY(),
                        Math.min(modelBounds.width(), modelBounds.height()), 0, 180);
            }
            // Keep all thumbnails in one buffer batch. Flushing once per model made
            // every drag event wait for the GPU dozens of times.
            graphics.flush();
        } finally {
            Lighting.setupFor3DItems();
            graphics.disableScissor();
        }
    }

    private void renderDecorationPreview(GuiGraphics graphics) {
        if (!hasPreviewModel()) {
            return;
        }
        Bounds bounds = getBounds(previewAnchor);
        if (bounds == null) {
            return;
        }

        double centerX = bounds.centerX() + previewPanX;
        double centerY = bounds.modelCenterY() + previewPanY;
        double scale = Math.min(bounds.width(), bounds.height()) * 0.38 * previewZoom;
        float rotation = (System.currentTimeMillis() % 10000L) / 10000.0F * 360.0F;
        graphics.enableScissor((int) Math.floor(bounds.left), (int) Math.floor(bounds.top),
                (int) Math.ceil(bounds.right), (int) Math.ceil(bounds.bottom));
        try {
            Lighting.setupForEntityInInventory();
            renderModel(graphics, selectedDisplay, centerX, centerY, scale, 165, rotation);
            graphics.flush();
        } finally {
            Lighting.setupFor3DItems();
            graphics.disableScissor();
        }
    }

    private void renderModel(GuiGraphics graphics, BaseDisplay display, double centerX, double centerY,
                             double scale, float xRotation, float yRotation) {
        VehicleBedrockModel model = display.getModel();
        ResourceLocation texture = display.getTexture();
        if (model == null || texture == null) {
            return;
        }

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate((float) centerX, (float) centerY, 512);
            if (xRotation != 0) {
                poseStack.mulPose(Axis.XP.rotationDegrees(xRotation));
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
            if (xRotation == 0) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
            poseStack.mulPoseMatrix(new Matrix4f().scaling((float) scale, (float) scale, (float) -scale));

            model.renderToBuffer(poseStack, graphics.bufferSource(), texture, 15728880);
            model.renderSpecialBones(poseStack, graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY);
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class DecorationEntry {
        private final ResourceLocation displayId;
        private final BaseDisplay display;
        private final Element element;
        private final Element thumbnail;
        private final Element texture;
        private final boolean rendersModel;
        private boolean visible = true;

        private DecorationEntry(ResourceLocation displayId, BaseDisplay display, Element element,
                                Element thumbnail, Element texture, boolean rendersModel) {
            this.displayId = displayId;
            this.display = display;
            this.element = element;
            this.thumbnail = thumbnail;
            this.texture = texture;
            this.rendersModel = rendersModel;
        }
    }

    private record Bounds(double left, double top, double right, double bottom) {
        private boolean contains(double x, double y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }

        private boolean intersects(Bounds other) {
            return right > other.left && left < other.right && bottom > other.top && top < other.bottom;
        }

        private Bounds inset(double amount) {
            return new Bounds(left + amount, top + amount, right - amount, bottom - amount);
        }

        private double width() {
            return right - left;
        }

        private double height() {
            return bottom - top;
        }

        private double centerX() {
            return (left + right) / 2;
        }

        private double centerY() {
            return (top + bottom) / 2;
        }

        private double modelCenterY() {
            return centerY() + height() * 0.08;
        }
    }
}
