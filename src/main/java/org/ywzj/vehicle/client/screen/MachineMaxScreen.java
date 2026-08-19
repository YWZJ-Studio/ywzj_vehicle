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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientMachineMaxAction;
import org.ywzj.vehicle.recipe.VehiclePrintingIngredient;
import org.ywzj.vehicle.recipe.VehiclePrintingRecipe;

import java.util.*;

public class MachineMaxScreen extends ApricityScreen {

    private static final String TEMPLATE = "screens/machine_max.html";
    private final MachineMaxBlockEntity machine;
    private final List<BaseVehicleData<?>> allVehicles;
    private final List<BaseVehicleData<?>> visibleVehicles = new ArrayList<>();
    private final List<Element> vehicleEntries = new ArrayList<>();
    private final List<MaterialRequirementView> materialRequirementViews = new ArrayList<>();
    private Document document;
    private Element searchInput;
    private Element vehicleCount;
    private Element vehicleList;
    private Element vehicleListEmpty;
    private Element selectedVehicleName;
    private Element selectedVehicleId;
    private Element previewAnchor;
    private Element description;
    private Element requirements;
    private Element requirementsEmpty;
    private Element progressBar;
    private Element progressLabel;
    private Element progressValue;
    private Element productNote;
    private Element craftButton;
    private Element closeButton;

    private String filter = "";
    private String appliedQuery;
    private int selectedIndex = -1;
    private BaseVehicleData<?> selectedVehicleData;
    private AbstractVehicle previewVehicle;
    private boolean selectedVehicleHasRecipe;
    private double previewZoom = 1.0;
    private double previewPanX;
    private double previewPanY;
    private float previewRotationX = 165.0F;
    private float previewRotationY;
    private boolean previewDragging;
    private int previewDragButton = -1;
    private int lastProgress = -1;
    private MachineState lastMachineState;
    private boolean vehicleDetailsDirty;

    public MachineMaxScreen(MachineMaxBlockEntity machine) {
        super(TEMPLATE);
        this.machine = machine;
        this.allVehicles = new ArrayList<>();
        CommonAssetsManager.vehicleDataManager().getVehicleData().values().forEach(vehicle -> {
            BaseVehicleData<?> vehicleData = (BaseVehicleData<?>) vehicle;
            if (!vehicleData.isExperimental()) {
                allVehicles.add(vehicleData);
            }
        });
        allVehicles.sort((left, right) -> left.getName().getString().compareToIgnoreCase(right.getName().getString()));
        setPauseGame(false);
        setShowDefaultBackground(false);
    }

    @Override
    protected void init() {
        appliedQuery = null;
        vehicleDetailsDirty = true;
        super.init();
        document = getLinkedDocument();
        if (document == null) {
            return;
        }

        searchInput = element("vehicle-search");
        vehicleCount = element("vehicle-count");
        vehicleList = element("vehicle-list");
        vehicleListEmpty = element("vehicle-list-empty");
        selectedVehicleName = element("selected-vehicle-name");
        selectedVehicleId = element("selected-vehicle-id");
        previewAnchor = element("vehicle-preview");
        description = element("vehicle-description");
        requirements = element("vehicle-requirements");
        requirementsEmpty = element("requirements-empty");
        progressBar = element("vehicle-progress");
        progressLabel = element("progress-label");
        progressValue = element("progress-value");
        productNote = element("product-note");
        craftButton = element("craft-button");
        closeButton = element("close-button");

        buildVehicleCatalog();
        if (searchInput != null) {
            searchInput.setValue(filter);
            searchInput.addEventListener("input", event -> applyFilter(searchInput.getValue()));
            searchInput.addEventListener("change", event -> applyFilter(searchInput.getValue()));
        }
        if (craftButton != null) {
            craftButton.addEventListener("click", event -> onCraft());
        }
        if (closeButton != null) {
            closeButton.addEventListener("click", event -> onClose());
        }

        applyFilter(filter);
        updateMachineState(true);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        appliedQuery = null;
        vehicleDetailsDirty = true;
        super.resize(minecraft, width, height);
    }

    private Element element(String id) {
        return document.getElementById(id);
    }

    private void buildVehicleCatalog() {
        if (vehicleList == null) {
            return;
        }

        vehicleList.setTextContent("");
        vehicleEntries.clear();
        for (int index = 0; index < allVehicles.size(); index++) {
            BaseVehicleData<?> vehicleData = allVehicles.get(index);
            Element entry = document.createElement("div");
            entry.setClassName("list-group-item vehicle-entry");

            Element name = document.createElement("div");
            name.setClassName("vehicle-entry-name");
            name.setTextContent(vehicleData.getName().getString());

            Element code = document.createElement("div");
            code.setClassName("vehicle-entry-code");
            code.setTextContent(vehicleData.getVehicleId().toString());

            entry.appendChild(name);
            entry.appendChild(code);
            int catalogIndex = index;
            entry.addEventListener("click", event -> selectCatalogVehicle(catalogIndex));
            vehicleList.appendChild(entry);
            vehicleEntries.add(entry);
        }
    }

    private void applyFilter(String text) {
        filter = text == null ? "" : text;
        String query = filter.strip().toLowerCase(Locale.ROOT);
        if (query.equals(appliedQuery)) {
            return;
        }
        appliedQuery = query;
        visibleVehicles.clear();

        for (int index = 0; index < allVehicles.size(); index++) {
            BaseVehicleData<?> vehicleData = allVehicles.get(index);
            boolean visible = matches(vehicleData, query);
            setVehicleEntryVisible(vehicleEntries.get(index), visible);
            if (visible) {
                visibleVehicles.add(vehicleData);
            }
        }

        if (vehicleCount != null) {
            vehicleCount.setTextContent(visibleVehicles.size() + "/" + allVehicles.size());
        }
        setHidden(vehicleListEmpty, !visibleVehicles.isEmpty());

        int retainedIndex = selectedVehicleData == null ? -1 : visibleVehicles.indexOf(selectedVehicleData);
        if (retainedIndex >= 0) {
            selectedIndex = retainedIndex;
            refreshEntrySelection();
            if (vehicleDetailsDirty) {
                updateVehicleDetails(selectedVehicleData);
            }
        } else if (!visibleVehicles.isEmpty()) {
            selectVehicle(0, false);
        } else {
            clearSelection();
        }
        vehicleDetailsDirty = false;
    }

    private boolean matches(BaseVehicleData<?> vehicleData, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return vehicleData.getName().getString().toLowerCase(Locale.ROOT).contains(query)
                || vehicleData.getVehicleId().toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private void selectCatalogVehicle(int catalogIndex) {
        BaseVehicleData<?> vehicleData = allVehicles.get(catalogIndex);
        int visibleIndex = visibleVehicles.indexOf(vehicleData);
        if (visibleIndex >= 0) {
            selectVehicle(visibleIndex, true);
        }
    }

    private void selectVehicle(int visibleIndex, boolean playSound) {
        if (visibleIndex < 0 || visibleIndex >= visibleVehicles.size()) {
            clearSelection();
            return;
        }

        BaseVehicleData<?> vehicleData = visibleVehicles.get(visibleIndex);
        boolean changed = selectedVehicleData != vehicleData;
        selectedIndex = visibleIndex;
        selectedVehicleData = vehicleData;
        refreshEntrySelection();

        if (changed) {
            updateVehicleDetails(vehicleData);
            if (playSound) {
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }
        updateMachineState(true);
    }

    private void refreshEntrySelection() {
        for (int index = 0; index < allVehicles.size(); index++) {
            boolean selected = allVehicles.get(index) == selectedVehicleData;
            String className = selected
                    ? "list-group-item vehicle-entry active"
                    : "list-group-item vehicle-entry";
            setVehicleEntryClass(vehicleEntries.get(index), className);
        }
    }

    private void setVehicleEntryVisible(Element entry, boolean visible) {
        entry.setInlineStyleProperty("display", visible ? "flex" : "none");
        if (document != null) {
            document.markDirty(entry, Drawer.RELAYOUT | Drawer.REPAINT);
        }
    }

    private void setVehicleEntryClass(Element entry, String className) {
        entry.setClassName(className);
        if (document != null) {
            document.markDirty(entry, Drawer.RELAYOUT | Drawer.REPAINT);
        }
    }

    private void updateVehicleDetails(BaseVehicleData<?> vehicleData) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clearSelection();
            return;
        }

        resetPreviewTransform();
        previewVehicle = vehicleData.construct(minecraft.level, Vec3.ZERO, 0, 0);
        previewVehicle.initData();
        previewVehicle.initDisplayData();

        if (selectedVehicleName != null) {
            selectedVehicleName.setTextContent(vehicleData.getName().getString());
        }
        if (selectedVehicleId != null) {
            selectedVehicleId.setTextContent(vehicleData.getVehicleId().getPath());
        }
        if (description != null) {
            String descriptionKey = ClientAssetsManager.INSTANCE.getVehicleDisplay(previewVehicle.getDisplayId())
                    .map(BaseDisplay::getDescription)
                    .filter(value -> value != null && !value.isBlank())
                    .orElse("screen.no_description");
            setDescriptionText(Component.translatable(descriptionKey).getString());
        }

        selectedVehicleHasRecipe = updateRequirements(vehicleData.getVehicleId());
    }

    private boolean updateRequirements(ResourceLocation vehicleId) {
        if (requirements == null) {
            return false;
        }
        requirements.setTextContent("");
        materialRequirementViews.clear();

        Optional<VehiclePrintingRecipe> recipe = findPrintingRecipe(vehicleId);
        setHidden(requirementsEmpty, recipe.isPresent());
        if (recipe.isEmpty()) {
            return false;
        }

        for (VehiclePrintingIngredient input : recipe.get().getInputs()) {
            ItemStack[] choices = input.ingredient().getItems();
            if (choices.length > 0) {
                requirements.appendChild(createMaterialRow(input, choices));
            }
        }
        updateMaterialCounts();
        return true;
    }

    private Optional<VehiclePrintingRecipe> findPrintingRecipe(ResourceLocation vehicleId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Optional.empty();
        }
        Optional<? extends Recipe<?>> recipe = minecraft.level.getRecipeManager().byKey(vehicleId);
        return recipe.filter(VehiclePrintingRecipe.class::isInstance)
                .map(VehiclePrintingRecipe.class::cast);
    }

    private Element createMaterialRow(VehiclePrintingIngredient input, ItemStack[] choices) {
        Element row = document.createElement("div");
        row.setClassName("material-row");

        Element slot = document.createElement("slot");
        slot.setClassName("requirement-slot");
        slot.setAttribute("size", "36");

        Element ingredient = document.createElement("ingredient");
        ingredient.setAttribute("cycle-interval", "900");
        ingredient.setTextContent(joinItemIds(choices));
        slot.appendChild(ingredient);

        Element copy = document.createElement("div");
        copy.setClassName("material-copy");
        Element name = document.createElement("div");
        name.setClassName("material-name");
        name.setTextContent(choices[0].getHoverName().getString());
        copy.appendChild(name);

        if (choices.length > 1) {
            Element options = document.createElement("div");
            options.setClassName("material-options");
            options.setTextContent(Component.translatable("screen.machine_max.options", choices.length).getString());
            copy.appendChild(options);
        }

        Element count = document.createElement("div");
        count.setClassName("material-count");
        int owned = countOwnedMaterial(input);
        count.setTextContent(owned + "/" + input.count());

        row.appendChild(slot);
        row.appendChild(copy);
        row.appendChild(count);
        materialRequirementViews.add(new MaterialRequirementView(input, count, owned));
        return row;
    }

    private void updateMaterialCounts() {
        if (materialRequirementViews.isEmpty()) {
            return;
        }

        for (MaterialRequirementView view : materialRequirementViews) {
            int owned = countOwnedMaterial(view.input);
            if (owned != view.shownOwned) {
                view.count.setTextContent(owned + "/" + view.input.count());
                if (document != null) {
                    document.markDirty(view.count, Drawer.RELAYOUT | Drawer.REPAINT);
                }
                view.shownOwned = owned;
            }
        }
    }

    private int countOwnedMaterial(VehiclePrintingIngredient input) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }

        int owned = 0;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (!stack.isEmpty() && input.ingredient().test(stack)) {
                owned += stack.getCount();
            }
        }
        return owned;
    }

    private String joinItemIds(ItemStack[] choices) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (ItemStack choice : choices) {
            ids.add(BuiltInRegistries.ITEM.getKey(choice.getItem()));
        }
        return ids.stream().map(ResourceLocation::toString).reduce((left, right) -> left + "|" + right).orElse("");
    }

    private void clearSelection() {
        selectedIndex = -1;
        selectedVehicleData = null;
        previewVehicle = null;
        resetPreviewTransform();
        selectedVehicleHasRecipe = false;
        refreshEntrySelection();
        setText(selectedVehicleName, Component.translatable("screen.machine_max.preview").getString());
        setText(selectedVehicleId, "--");
        setDescriptionText(Component.translatable("screen.machine_max.select_vehicle").getString());
        if (requirements != null) {
            requirements.setTextContent("");
        }
        materialRequirementViews.clear();
        setHidden(requirementsEmpty, false);
        updateMachineState(true);
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

    private void setHidden(Element element, boolean hidden) {
        if (element != null) {
            element.setClassName(hidden ? "empty-state hidden" : "empty-state");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (searchInput != null) {
            String currentFilter = searchInput.getValue() == null ? "" : searchInput.getValue();
            if (!filter.equals(currentFilter)) {
                applyFilter(currentFilter);
            }
        }
        updateMaterialCounts();
        updateMachineState(false);
    }

    private void updateMachineState(boolean force) {
        int progress = Math.max(0, Math.min(100, Math.round(machine.progress * 100)));
        MachineState state = resolveMachineState();
        if (!force && progress == lastProgress && state == lastMachineState) {
            return;
        }
        lastProgress = progress;
        lastMachineState = state;

        if (progressBar != null) {
            progressBar.setInlineStyleProperty("width", progress + "%");
        }
        setText(progressLabel, state == MachineState.PRINTING
                ? Component.translatable("screen.machine_max.progress.crafting", getCraftingVehicleName()).getString()
                : Component.translatable("screen.machine_max.progress").getString());
        setText(progressValue, progress + "%");

        if (productNote != null) {
            productNote.setTextContent(machine.hasProduct()
                    ? Component.translatable("tips.machine_max_product").getString()
                    : "");
        }
        if (craftButton != null) {
            craftButton.setDisabled(state != MachineState.READY);
        }
    }

    private String getCraftingVehicleName() {
        ResourceLocation vehicleId = machine.craftingVehicleId;
        if (vehicleId == null) {
            return "--";
        }
        return CommonAssetsManager.vehicleDataManager().getVehicleData(vehicleId)
                .map(vehicleData -> vehicleData.getName().getString())
                .orElse(vehicleId.getPath());
    }

    private MachineState resolveMachineState() {
        if (machine.hasProduct()) {
            return MachineState.PRODUCT_READY;
        }
        if (machine.isCrafting()) {
            return MachineState.PRINTING;
        }
        if (selectedVehicleData == null) {
            return MachineState.NO_SELECTION;
        }
        if (!selectedVehicleHasRecipe) {
            return MachineState.NO_RECIPE;
        }
        return MachineState.READY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        PreviewBounds bounds = getPreviewBounds();
        if (previewVehicle == null || bounds == null || !bounds.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        double oldZoom = previewZoom;
        previewZoom = Mth.clamp(previewZoom * Math.pow(1.15, delta), 0.35, 4.0);
        double ratio = previewZoom / oldZoom;
        double baseX = bounds.centerX();
        double baseY = bounds.modelCenterY();
        double modelCenterX = baseX + previewPanX;
        double modelCenterY = baseY + previewPanY;
        previewPanX = mouseX + (modelCenterX - mouseX) * ratio - baseX;
        previewPanY = mouseY + (modelCenterY - mouseY) * ratio - baseY;
        clampPreviewPan(bounds);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PreviewBounds bounds = getPreviewBounds();
        if ((button == 0 || button == 1) && previewVehicle != null && bounds != null && bounds.contains(mouseX, mouseY)) {
            previewDragButton = button;
            setPreviewDragging(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (previewDragging && button == previewDragButton && button == 0) {
            previewRotationY -= (float) dragX;
            previewRotationX -= (float) dragY;
            return true;
        }
        if (previewDragging && button == previewDragButton && button == 1) {
            previewPanX += dragX;
            previewPanY += dragY;
            PreviewBounds bounds = getPreviewBounds();
            if (bounds != null) {
                clampPreviewPan(bounds);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (previewDragging && button == previewDragButton) {
            previewDragButton = -1;
            setPreviewDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private PreviewBounds getPreviewBounds() {
        if (previewAnchor == null || document == null) {
            return null;
        }
        Element.DOMRect rect = previewAnchor.getBoundingClientRect();
        if (rect.width <= 0 || rect.height <= 0) {
            return null;
        }
        double viewportScale = document.getViewport().renderScale();
        return new PreviewBounds(
                rect.x * viewportScale,
                rect.y * viewportScale,
                (rect.x + rect.width) * viewportScale,
                (rect.y + rect.height) * viewportScale
        );
    }

    private void clampPreviewPan(PreviewBounds bounds) {
        previewPanX = Mth.clamp(previewPanX, -bounds.width() * 0.45, bounds.width() * 0.45);
        previewPanY = Mth.clamp(previewPanY, -bounds.height() * 0.45, bounds.height() * 0.45);
    }

    private void resetPreviewTransform() {
        previewZoom = 1.0;
        previewPanX = 0;
        previewPanY = 0;
        previewRotationX = 165.0F;
        previewRotationY = 0;
        previewDragButton = -1;
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
        renderVehiclePreview(graphics);
    }

    private void renderVehiclePreview(GuiGraphics graphics) {
        if (previewVehicle == null || previewAnchor == null || document == null) {
            return;
        }

        Element.DOMRect rect = previewAnchor.getBoundingClientRect();
        if (rect.width <= 0 || rect.height <= 0) {
            return;
        }

        float viewportScale = (float) document.getViewport().renderScale();
        double left = rect.x * viewportScale;
        double top = rect.y * viewportScale;
        double right = (rect.x + rect.width) * viewportScale;
        double bottom = (rect.y + rect.height) * viewportScale;
        double centerX = (left + right) / 2 + previewPanX;
        double centerY = (top + bottom) / 2 + (bottom - top) * 0.08 + previewPanY;
        double modelLength = Math.max(previewVehicle.getStructureLength(), 3);
        float modelScale = (float) (Math.min(right - left, bottom - top) * 0.78 / modelLength * previewZoom);

        graphics.enableScissor((int) Math.floor(left), (int) Math.floor(top),
                (int) Math.ceil(right), (int) Math.ceil(bottom));
        PoseStack poseStack = graphics.pose();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        poseStack.pushPose();
        try {
            poseStack.translate((float) centerX, (float) centerY, 512);
            poseStack.mulPose(Axis.XP.rotationDegrees(previewRotationX));
            poseStack.mulPose(Axis.YP.rotationDegrees(previewRotationY));
            poseStack.mulPoseMatrix(new Matrix4f().scaling(modelScale, modelScale, -modelScale));

            dispatcher.setRenderShadow(false);
            Lighting.setupForEntityInInventory();
            RenderSystem.runAsFancy(() -> dispatcher.render(previewVehicle, 0, 0, 0,
                    previewVehicle.getYRot(), 1.0F, poseStack, graphics.bufferSource(), 15728880));
            graphics.flush();
        } finally {
            dispatcher.setRenderShadow(true);
            Lighting.setupFor3DItems();
            poseStack.popPose();
            graphics.disableScissor();
        }
    }

    private void onCraft() {
        if (resolveMachineState() != MachineState.READY || selectedIndex < 0 || selectedIndex >= visibleVehicles.size()) {
            return;
        }

        ClientMachineMaxAction action = new ClientMachineMaxAction();
        action.craftingVehicleId = visibleVehicles.get(selectedIndex).getVehicleId();
        action.blockPos = machine.getBlockPos();
        action.action = ClientMachineMaxAction.Action.CRAFT;
        Channel.CHANNEL.sendToServer(action);
        machine.clearPrintingPreview();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum MachineState {
        READY,
        PRINTING,
        PRODUCT_READY,
        NO_RECIPE,
        NO_SELECTION
    }

    private static final class MaterialRequirementView {
        private final VehiclePrintingIngredient input;
        private final Element count;
        private int shownOwned = -1;

        private MaterialRequirementView(VehiclePrintingIngredient input, Element count, int shownOwned) {
            this.input = input;
            this.count = count;
            this.shownOwned = shownOwned;
        }
    }

    private record PreviewBounds(double left, double top, double right, double bottom) {
        private boolean contains(double x, double y) {
            return x >= left && x <= right && y >= top && y <= bottom;
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

        private double modelCenterY() {
            return (top + bottom) / 2 + height() * 0.08;
        }
    }
}
