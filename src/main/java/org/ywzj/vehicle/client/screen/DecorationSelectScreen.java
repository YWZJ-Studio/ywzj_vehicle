package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.component.ScrollableTextPanel;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.network.message.ClientDecorationAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DecorationSelectScreen extends Screen {

    private static final int COLUMNS = 6;
    private static final int VISIBLE_ROWS = 6;
    private static final int SLOT_SIZE = 32;
    private static final int GRID_WIDTH = COLUMNS * SLOT_SIZE;
    private static final int GRID_HEIGHT = VISIBLE_ROWS * SLOT_SIZE;
    private ResourceLocation selectedDisplayId;
    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private final ScrollableTextPanel descriptionPanel = new ScrollableTextPanel();
    private int leftPos;
    private int topPos;
    private final List<ResourceLocation> decorationDisplayIds;
    private List<ResourceLocation> filteredDecorationDisplayIds = new ArrayList<>();

    public DecorationSelectScreen() {
        super(Component.literal("Decoration Select"));
        this.decorationDisplayIds = ClientAssetsManager.INSTANCE.getDecorationDisplays().stream()
                .map(BaseDisplay::getDisplayId)
                .distinct()
                .sorted(Comparator.comparing(String::valueOf))
                .toList();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width / 2 - GRID_WIDTH) / 2;
        this.topPos = (this.height - GRID_HEIGHT) / 2 + 10;
        updateFilteredList("");
        EditBox searchBox = new EditBox(this.font, leftPos, topPos - 24, GRID_WIDTH, 16, Component.literal("Search"));
        searchBox.setResponder(this::updateFilteredList);
        searchBox.setBordered(true);
        searchBox.active = true;
        searchBox.setTextColor(Color.WHITE);
        this.addRenderableWidget(searchBox);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        drawDisplayList(guiGraphics, mouseX, mouseY);
        drawPreview(guiGraphics);
    }

    private void updateFilteredList(String filter) {
        this.filteredDecorationDisplayIds = decorationDisplayIds.stream()
                .filter(loc -> loc.getPath().toLowerCase().contains(filter.toLowerCase()))
                .toList();
        this.scrollOffset = 0;
        this.selectedIndex = -1;
        this.selectedDisplayId = null;
        this.descriptionPanel.clear();
    }

    private void drawDisplayList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos - 2, topPos - 2, leftPos + GRID_WIDTH + 2, topPos + GRID_HEIGHT + 2, Color.BG_LIST);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scrollOffset + row) * COLUMNS + col;
                if (index >= filteredDecorationDisplayIds.size()) break;

                int slotX = leftPos + col * SLOT_SIZE;
                int slotY = topPos + row * SLOT_SIZE;

                boolean isHovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
                boolean isSelected = (index == selectedIndex);

                int bgColor = isSelected ? Color.ITEM_SELECTED : (isHovered ? Color.ITEM_HOVERED : Color.SCROLLBAR_TRACK);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, bgColor);

                ResourceLocation displayId = filteredDecorationDisplayIds.get(index);
                ClientAssetsManager.INSTANCE.getDecorationDisplay(displayId).ifPresent(display -> {
                    ResourceLocation slotTexture = display.getSlotTexture();
                    if (slotTexture != null) {
                        guiGraphics.blit(slotTexture, slotX + 4, slotY + 4, 0, 0, 24, 24, 24, 24);
                    } else {
                        VehicleBedrockModel model = display.getModel();
                        ResourceLocation texture = display.getTexture();
                        if (model != null && texture != null) {
                            PoseStack poseStack = guiGraphics.pose();
                            poseStack.pushPose();
                            {
                                float scale = 24;
                                poseStack.translate(slotX + 16, slotY + 16, 512);
                                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                                poseStack.last().pose().mul(new Matrix4f().scaling(scale, scale, -scale));
                                model.renderToBuffer(poseStack, guiGraphics.bufferSource(), texture, 15728880);
                                model.renderSpecialBones(poseStack, guiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY);
                            }
                            poseStack.popPose();
                        }
                    }
                });
            }
        }
        drawScrollBar(guiGraphics, leftPos + GRID_WIDTH + 4, topPos);
    }

    private void drawScrollBar(GuiGraphics guiGraphics, int x, int y) {
        int barWidth = 6;
        int size = filteredDecorationDisplayIds.size();
        int totalRows = (int) Math.ceil((double) size / COLUMNS);

        guiGraphics.fill(x, y, x + barWidth, y + GRID_HEIGHT, Color.SCROLLBAR_TRACK);

        if (totalRows <= VISIBLE_ROWS) return;

        int knobHeight = Math.max(12, GRID_HEIGHT * VISIBLE_ROWS / totalRows);
        int maxScrollOffset = totalRows - VISIBLE_ROWS;
        int movable = GRID_HEIGHT - knobHeight;
        int knobY = y + (int) (movable * (scrollOffset / (float) maxScrollOffset));

        guiGraphics.fill(x, knobY, x + barWidth, knobY + knobHeight, Color.SCROLLBAR_KNOB);
    }

    private void drawPreview(GuiGraphics guiGraphics) {
        if (selectedDisplayId == null) {
            descriptionPanel.clear();
            return;
        }
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getDecorationDisplay(selectedDisplayId);
        if (displayOptional.isEmpty() || displayOptional.get().getModel() == null) {
            descriptionPanel.clear();
            return;
        }

        BaseDisplay display = displayOptional.get();
        float scale = 64;
        int previewX = this.width * 3 / 4;
        int previewY = this.height / 2 - 64;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(previewX, previewY, 512);
            float rotation = (System.currentTimeMillis() % 10000) / 10000f * 360f;
            poseStack.mulPose(Axis.XP.rotationDegrees(165));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            poseStack.last().pose().mul(new Matrix4f().scaling(scale, scale, -scale));
            // 装饰模型
            VehicleBedrockModel model = display.getModel();
            model.renderToBuffer(poseStack, guiGraphics.bufferSource(), display.getTexture(), 15728880);
            model.renderSpecialBones(poseStack, guiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
        guiGraphics.drawCenteredString(this.font, selectedDisplayId.getPath(), previewX, previewY + 40, Color.WHITE);
        int descriptionLeft = width / 2;
        int descriptionTop = height / 2;
        descriptionPanel.setBounds(descriptionLeft, descriptionTop, width - 20, height - 10);
        Component description = display.getDescription() == null
                ? Component.translatable("screen.no_description")
                : Component.translatable(display.getDescription());
        descriptionPanel.setContent(selectedDisplayId, description);
        descriptionPanel.render(guiGraphics, font, Color.WHITE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= leftPos && mouseX < leftPos + GRID_WIDTH && mouseY >= topPos && mouseY < topPos + GRID_HEIGHT) {
            int col = (int) ((mouseX - leftPos) / SLOT_SIZE);
            int row = (int) ((mouseY - topPos) / SLOT_SIZE);
            int clickedIndex = (scrollOffset + row) * COLUMNS + col;

            if (clickedIndex >= 0 && clickedIndex < filteredDecorationDisplayIds.size()) {
                this.selectedIndex = clickedIndex;
                onDisplaySelected(filteredDecorationDisplayIds.get(clickedIndex));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (descriptionPanel.mouseScrolled(mouseX, mouseY, scrollDeltaY, font)) {
            return true;
        }
        int totalRows = (int) Math.ceil((double) filteredDecorationDisplayIds.size() / COLUMNS);
        if (mouseX >= leftPos && mouseX < leftPos + GRID_WIDTH
                && mouseY >= topPos && mouseY < topPos + GRID_HEIGHT
                && totalRows > VISIBLE_ROWS) {
            int maxScrollOffset = totalRows - VISIBLE_ROWS;
            scrollOffset = (int) Mth.clamp(scrollOffset - Math.signum(scrollDeltaY), 0, maxScrollOffset);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    private void onDisplaySelected(ResourceLocation displayId) {
        this.selectedDisplayId = displayId;
        ClientDecorationAction clientDecorationAction = new ClientDecorationAction();
        clientDecorationAction.action = ClientDecorationAction.Action.UPDATE_ITEM;
        clientDecorationAction.displayId = displayId.toString();
        PacketDistributor.sendToServer(clientDecorationAction);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean isPauseScreen() { return false; }

}
