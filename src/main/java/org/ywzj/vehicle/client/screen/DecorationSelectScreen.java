package org.ywzj.vehicle.client.screen;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.network.Channel;
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
        searchBox.setTextColor(0xFFFFFF);
        this.addRenderableWidget(searchBox);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        drawDisplayList(guiGraphics, mouseX, mouseY);
        drawPreview(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateFilteredList(String filter) {
        this.filteredDecorationDisplayIds = decorationDisplayIds.stream()
                .filter(loc -> loc.getPath().toLowerCase().contains(filter.toLowerCase()))
                .toList();
        this.scrollOffset = 0;
        this.selectedIndex = -1;
    }

    private void drawDisplayList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos - 2, topPos - 2, leftPos + GRID_WIDTH + 2, topPos + GRID_HEIGHT + 2, 0xFF252525);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scrollOffset + row) * COLUMNS + col;
                if (index >= filteredDecorationDisplayIds.size()) break;

                int slotX = leftPos + col * SLOT_SIZE;
                int slotY = topPos + row * SLOT_SIZE;

                boolean isHovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
                boolean isSelected = (index == selectedIndex);

                int bgColor = isSelected ? 0xFF3F6DB5 : (isHovered ? 0xFF5A5A5A : 0xFF3A3A3A);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, bgColor);

                ResourceLocation displayId = filteredDecorationDisplayIds.get(index);
                ClientAssetsManager.INSTANCE.getDecorationDisplay(displayId).ifPresent(display -> {
                    ResourceLocation slotTexture = display.getSlotTexture();
                    if (slotTexture != null) {
                        guiGraphics.blit(slotTexture, slotX + 4, slotY + 4, 0, 0, 24, 24, 24, 24);
                    } else {
                        BedrockModel model = display.getModel();
                        ResourceLocation texture = display.getTexture();
                        if (model != null && texture != null) {
                            PoseStack poseStack = guiGraphics.pose();
                            poseStack.pushPose();
                            {
                                float scale = 24;
                                poseStack.translate(slotX + 16, slotY + 16, 512);
                                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                                poseStack.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
                                VertexConsumer builder = guiGraphics.bufferSource().getBuffer(RenderType.entityCutout(texture));
                                model.renderToBuffer(poseStack, builder, 15728880, OverlayTexture.NO_OVERLAY);
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

        guiGraphics.fill(x, y, x + barWidth, y + GRID_HEIGHT, 0xFF3A3A3A);

        if (totalRows <= VISIBLE_ROWS) return;

        int knobHeight = Math.max(12, GRID_HEIGHT * VISIBLE_ROWS / totalRows);
        int maxScrollOffset = totalRows - VISIBLE_ROWS;
        int movable = GRID_HEIGHT - knobHeight;
        int knobY = y + (int) (movable * (scrollOffset / (float) maxScrollOffset));

        guiGraphics.fill(x, knobY, x + barWidth, knobY + knobHeight, 0xFFAAAAAA);
    }

    private void drawPreview(GuiGraphics guiGraphics) {
        if (selectedDisplayId == null) {
            return;
        }
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getDecorationDisplay(selectedDisplayId);
        if (displayOptional.isEmpty() || displayOptional.get().getModel() == null) {
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
            poseStack.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
            // 装饰模型
            VertexConsumer builder = guiGraphics.bufferSource().getBuffer(RenderType.entityCutout(display.getTexture()));
            display.getModel().renderToBuffer(poseStack, builder, 15728880, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
        poseStack.pushPose();
        {
            // 装饰名
            guiGraphics.drawCenteredString(this.font, selectedDisplayId.getPath(), previewX, previewY + 40, 0xFFFFFF);
            // 介绍
            int padding = 10;
            int x1 = width / 2;
            int x2 = width - 20;
            int maxWidth = (int) ((x2 - x1 + 2 * padding) / 1.03f);
            poseStack.translate((float) width / 2, (float) height / 2, 0);
            if (display.getDescription() != null) {
                var lines = font.split(Component.literal(display.getDescription()), maxWidth);
                for (int i = 0; i < lines.size(); i++) {
                    guiGraphics.drawString(font, lines.get(i), 0, i * 9, 0xFFFFFFFF);
                }
            } else {
                guiGraphics.drawString(font, Component.translatable("screen.no_description"), 0, 0, 0xFFFFFFFF);
            }
        }
        poseStack.popPose();
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalRows = (int) Math.ceil((double) filteredDecorationDisplayIds.size() / COLUMNS);
        if (totalRows > VISIBLE_ROWS) {
            int maxScrollOffset = totalRows - VISIBLE_ROWS;
            scrollOffset = (int) Mth.clamp(scrollOffset - Math.signum(delta), 0, maxScrollOffset);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void onDisplaySelected(ResourceLocation displayId) {
        this.selectedDisplayId = displayId;
        ClientDecorationAction clientDecorationAction = new ClientDecorationAction();
        clientDecorationAction.action = ClientDecorationAction.Action.UPDATE_ITEM;
        clientDecorationAction.decorationDisplayId = displayId.toString();
        Channel.CHANNEL.sendToServer(clientDecorationAction);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean isPauseScreen() { return false; }

}
