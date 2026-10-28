package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.render.Drawer;
import com.sighs.apricityui.render.GeometryQueryScope;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ClientMachineMaxAction;
import org.ywzj.vehicle.recipe.VehiclePrintingIngredient;
import org.ywzj.vehicle.recipe.VehiclePrintingRecipe;
import org.ywzj.vehicle.util.AuiTextHelper;

import java.util.*;

@EventBusSubscriber(modid = YwzjVehicle.MOD_ID, value = Dist.CLIENT)
public class MachineMaxScreen extends ApricityScreen {

    private static final String TEMPLATE = "screens/machine_max.html";
    private static final int CATALOG_INDEX_BATCH_SIZE = 128;
    private static final long CATALOG_BATCH_BUDGET_NS = 2_000_000L;
    private final MachineMaxBlockEntity machine;
    private VehicleCatalogCache.Catalog catalog;
    private List<BaseVehicleData<?>> allVehicles;
    private final List<BaseVehicleData<?>> visibleVehicles = new ArrayList<>();
    private final List<Element> vehicleEntries = new ArrayList<>();
    private Element selectedVehicleEntry;
    private boolean pageRendered;
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
    private float previewRotationX = 165;
    private float previewRotationY = 30;
    private boolean previewDragging;
    private int previewDragButton = -1;
    private int lastProgress = -1;
    private MachineState lastMachineState;
    private boolean vehicleDetailsDirty;

    public MachineMaxScreen(MachineMaxBlockEntity machine) {
        super(TEMPLATE);
        this.machine = machine;
        this.catalog = VehicleCatalogCache.current();
        this.allVehicles = catalog.vehicles();
        this.selectedVehicleData = catalog.vehicle(VehicleCatalogCache.lastSelectedVehicleId);
        setPauseGame(false);
        setShowDefaultBackground(false);
    }

    @Override
    protected void init() {
        pageRendered = false;
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

        resetVehicleCatalog();
        if (searchInput != null) {
            searchInput.setAttribute("placeholder", Component.translatable("screen.search").getString());
            searchInput.setValue(filter);
        }
        if (craftButton != null) {
            craftButton.addEventListener("click", event -> onCraft());
        }
        if (closeButton != null) {
            closeButton.addEventListener("click", event -> onClose());
        }

        refreshCatalog();
        showCatalog();
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

    private void resetVehicleCatalog() {
        if (vehicleList == null) {
            return;
        }

        if (vehicleList.getScrollTop() != 0 || vehicleList.getTargetScrollTop() != 0) {
            try (GeometryQueryScope ignored = GeometryQueryScope.open()) {
                vehicleList.setScrollTop(0);
            }
        }
        vehicleList.setTextContent("");
        vehicleEntries.clear();
        selectedVehicleEntry = null;
    }

    private boolean refreshCatalog() {
        VehicleCatalogCache.Catalog current = VehicleCatalogCache.current();
        if (catalog == current) {
            return false;
        }
        ResourceLocation selectedId = selectedVehicleData == null
                ? VehicleCatalogCache.lastSelectedVehicleId : selectedVehicleData.getVehicleId();
        catalog = current;
        allVehicles = catalog.vehicles();
        appliedQuery = null;
        visibleVehicles.clear();
        resetVehicleCatalog();
        clearSelection();
        selectedVehicleData = catalog.vehicle(selectedId);
        vehicleDetailsDirty = selectedVehicleData != null;
        return true;
    }

    private void showCatalog() {
        if (catalog.isReady()) {
            applyFilter(filter);
        } else {
            setText(vehicleCount, "0/" + allVehicles.size());
            setText(vehicleListEmpty, Component.translatable("screen.machine_max.loading").getString());
            setHidden(vehicleListEmpty, false);
        }
    }

    private void indexVehicleCatalog() {
        if (catalog.indexBatch(CATALOG_INDEX_BATCH_SIZE, CATALOG_BATCH_BUDGET_NS)) {
            applyFilter(filter);
        }
    }

    private void createVehicleCatalogEntries() {
        if (vehicleList == null) {
            return;
        }
        while (vehicleEntries.size() < visibleVehicles.size()) {
            int visibleIndex = vehicleEntries.size();
            BaseVehicleData<?> vehicleData = visibleVehicles.get(visibleIndex);
            VehicleCatalogCache.Text text = catalog.text(vehicleData);
            Element entry = document.createElement("div");
            boolean selected = vehicleData == selectedVehicleData;
            entry.setClassName(selected ? "list-group-item vehicle-entry active" : "list-group-item vehicle-entry");

            Element name = document.createElement("div");
            name.setClassName("vehicle-entry-name");
            name.setTextContent(text.name());

            Element code = document.createElement("div");
            code.setClassName("vehicle-entry-code");
            code.setTextContent(text.id());

            entry.appendChild(name);
            entry.appendChild(code);
            entry.addEventListener("click", event -> {
                if (visibleIndex < vehicleEntries.size() && vehicleEntries.get(visibleIndex) == entry) {
                    selectVehicle(visibleIndex, true);
                }
            });
            vehicleList.appendChild(entry);
            vehicleEntries.add(entry);
            if (selected) {
                selectedVehicleEntry = entry;
            }
        }
    }

    private void applyFilter(String text) {
        filter = text == null ? "" : text;
        if (!catalog.isReady()) {
            return;
        }
        String query = filter.strip().toLowerCase(Locale.ROOT);
        if (query.equals(appliedQuery)) {
            return;
        }
        appliedQuery = query;
        resetVehicleCatalog();
        visibleVehicles.clear();

        for (BaseVehicleData<?> vehicleData : allVehicles) {
            if (matches(vehicleData, query)) {
                visibleVehicles.add(vehicleData);
            }
        }

        if (vehicleCount != null) {
            vehicleCount.setTextContent(visibleVehicles.size() + "/" + allVehicles.size());
        }
        setText(vehicleListEmpty, Component.translatable("screen.machine_max.no_results").getString());
        setHidden(vehicleListEmpty, !visibleVehicles.isEmpty());

        BaseVehicleData<?> retainedVehicle = selectedVehicleData == null
                ? catalog.vehicle(VehicleCatalogCache.lastSelectedVehicleId) : selectedVehicleData;
        int retainedIndex = visibleVehicles.indexOf(retainedVehicle);
        if (retainedIndex >= 0) {
            selectVehicle(retainedIndex, false);
        } else if (!visibleVehicles.isEmpty()) {
            selectVehicle(0, false);
        } else {
            clearSelection();
        }
        createVehicleCatalogEntries();
    }

    private boolean matches(BaseVehicleData<?> vehicleData, String query) {
        if (query.isEmpty()) {
            return true;
        }
        VehicleCatalogCache.Text text = catalog.text(vehicleData);
        return text.searchName().contains(query) || text.searchId().contains(query);
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
        VehicleCatalogCache.lastSelectedVehicleId = vehicleData.getVehicleId();
        refreshEntrySelection();

        if (changed) {
            vehicleDetailsDirty = true;
            previewVehicle = null;
            selectedVehicleHasRecipe = false;
            if (playSound) {
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }
        updateMachineState(true);
    }

    private void refreshEntrySelection() {
        Element nextEntry = selectedIndex >= 0 && selectedIndex < vehicleEntries.size()
                ? vehicleEntries.get(selectedIndex) : null;
        if (nextEntry == selectedVehicleEntry) {
            return;
        }
        if (selectedVehicleEntry != null) {
            selectedVehicleEntry.setClassName("list-group-item vehicle-entry");
        }
        selectedVehicleEntry = nextEntry;
        if (selectedVehicleEntry != null) {
            selectedVehicleEntry.setClassName("list-group-item vehicle-entry active");
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
        Optional<RecipeHolder<?>> recipe = minecraft.level.getRecipeManager().byKey(vehicleId);
        return recipe.map(RecipeHolder::value)
                .filter(VehiclePrintingRecipe.class::isInstance)
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
        vehicleDetailsDirty = false;
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
        AuiTextHelper.setDescription(description, text);
    }

    private void setHidden(Element element, boolean hidden) {
        if (element != null) {
            element.setClassName(hidden ? "empty-state hidden" : "empty-state");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!pageRendered || document == null || document.getLayout().needsInitialFullCommit()) {
            return;
        }
        if (refreshCatalog()) {
            showCatalog();
        }
        if (searchInput != null) {
            String currentFilter = searchInput.getValue() == null ? "" : searchInput.getValue();
            if (!filter.equals(currentFilter)) {
                applyFilter(currentFilter);
            }
        }
        if (!catalog.isReady()) {
            indexVehicleCatalog();
            updateMachineState(false);
            return;
        }
        if (vehicleDetailsDirty && selectedVehicleData != null) {
            vehicleDetailsDirty = false;
            updateVehicleDetails(selectedVehicleData);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        PreviewBounds bounds = getPreviewBounds();
        if (previewVehicle == null || bounds == null || !bounds.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }

        double oldZoom = previewZoom;
        previewZoom = Mth.clamp(previewZoom * Math.pow(1.15, deltaY), 0.35, 4.0);
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
        // AUI 可能分多帧完成初始布局；布局完成前，不能用尚未确定的边界绘制预览或处理交互。
        if (document.getLayout().needsInitialFullCommit()) {
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
        previewRotationX = 165;
        previewRotationY = 30;
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
        pageRendered = true;
        renderVehiclePreview(graphics);
    }

    private void renderVehiclePreview(GuiGraphics graphics) {
        if (previewVehicle == null) {
            return;
        }

        PreviewBounds bounds = getPreviewBounds();
        if (bounds == null) {
            return;
        }

        double centerX = bounds.centerX() + previewPanX;
        double centerY = bounds.modelCenterY() + previewPanY;
        double modelLength = Math.max(previewVehicle.getStructureLength(), 3);
        float modelScale = (float) (Math.min(bounds.width(), bounds.height()) * 0.78 / modelLength * previewZoom);

        graphics.enableScissor((int) Math.floor(bounds.left), (int) Math.floor(bounds.top),
                (int) Math.ceil(bounds.right), (int) Math.ceil(bounds.bottom));
        PoseStack poseStack = graphics.pose();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        poseStack.pushPose();
        try {
            poseStack.translate((float) centerX, (float) centerY, 512);
            poseStack.mulPose(Axis.XP.rotationDegrees(previewRotationX));
            poseStack.mulPose(Axis.YP.rotationDegrees(previewRotationY));
            poseStack.scale(modelScale, modelScale, -modelScale);

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
        PacketDistributor.sendToServer(action);
        machine.clearPrintingPreview();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        VehicleCatalogCache.cached = null;
        VehicleCatalogCache.lastSelectedVehicleId = null;
    }

    private static final class VehicleCatalogCache {

        private static Catalog cached;
        private static ResourceLocation lastSelectedVehicleId;

        record Text(String name, String id, String searchName, String searchId) {}

        private VehicleCatalogCache() {}

        static Catalog current() {
            Map<ResourceLocation, BaseVehicleData> source = CommonAssetsManager.vehicleDataManager().getVehicleData();
            Language language = Language.getInstance();
            if (cached == null || cached.source != source || cached.language != language) {
                cached = new Catalog(source, language);
            }
            return cached;
        }

        static final class Catalog {

            private final Map<ResourceLocation, BaseVehicleData> source;
            private final Language language;
            private final List<BaseVehicleData<?>> vehicles = new ArrayList<>();
            private final List<BaseVehicleData<?>> vehicleView = Collections.unmodifiableList(vehicles);
            private final Map<BaseVehicleData<?>, Text> texts = new IdentityHashMap<>();
            private int indexedVehicles;
            private boolean ready;

            private Catalog(Map<ResourceLocation, BaseVehicleData> source, Language language) {
                this.source = source;
                this.language = language;
                for (BaseVehicleData<?> vehicle : source.values()) {
                    if (!vehicle.isExperimental()) {
                        vehicles.add(vehicle);
                    }
                }
            }

            List<BaseVehicleData<?>> vehicles() {
                return vehicleView;
            }

            BaseVehicleData<?> vehicle(ResourceLocation id) {
                BaseVehicleData<?> vehicle = id == null ? null : source.get(id);
                return vehicle != null && !vehicle.isExperimental() ? vehicle : null;
            }

            Text text(BaseVehicleData<?> vehicle) {
                return texts.get(vehicle);
            }

            boolean isReady() {
                return ready;
            }

            boolean indexBatch(int batchSize, long budgetNs) {
                if (ready) {
                    return true;
                }
                long deadline = System.nanoTime() + budgetNs;
                int end = Math.min(indexedVehicles + batchSize, vehicles.size());
                while (indexedVehicles < end) {
                    BaseVehicleData<?> vehicle = vehicles.get(indexedVehicles++);
                    String name = vehicle.getName().getString();
                    String id = vehicle.getVehicleId().toString();
                    texts.put(vehicle, new Text(name, id,
                            name.toLowerCase(Locale.ROOT), id.toLowerCase(Locale.ROOT)));
                    if (System.nanoTime() >= deadline) {
                        break;
                    }
                }
                if (indexedVehicles == vehicles.size()) {
                    vehicles.sort(Comparator.comparing(vehicle -> texts.get(vehicle).name(),
                            String.CASE_INSENSITIVE_ORDER));
                    ready = true;
                }
                return ready;
            }
        }

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
