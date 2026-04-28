package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;
import org.ywzj.vehicle.client.render.entity.block.FigureBoxBlockRenderer;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.network.message.ClientFigureBoxUpdate;

import java.util.function.Consumer;

public class FigureBoxScreen extends Screen {

    private final FigureBoxBlockEntity figureBoxBlockEntity;

    public FigureBoxScreen(FigureBoxBlockEntity figureBoxBlockEntity) {
        super(Component.literal("Figure Box Settings"));
        this.figureBoxBlockEntity = figureBoxBlockEntity;
    }

    @Override
    protected void init() {
        int centerX = this.width / 4;
        int startY = this.height / 2 - 128;
        int spacing = 24;
        this.addRenderableWidget(Checkbox.builder(Component.translatable("check_box.figure_box.open"), this.font)
                .pos(centerX + 60, startY + 8 * spacing)
                .selected(figureBoxBlockEntity.open)
                .onValueChange((checkbox, selected) -> {
                    figureBoxBlockEntity.open = selected;
                    if (this.minecraft.level != null) {
                        this.minecraft.level.setBlock(figureBoxBlockEntity.getBlockPos(),
                                figureBoxBlockEntity.getBlockState().setValue(FigureBoxBlock.OPEN, selected),
                                3);
                    }
                    updateServer();
                })
                .build());
        addValueEditor(centerX, startY + spacing, String.valueOf(figureBoxBlockEntity.scale),
                val -> figureBoxBlockEntity.scale = parseSafe(val, figureBoxBlockEntity.scale));
        addValueEditor(centerX, startY + spacing * 2, String.valueOf(figureBoxBlockEntity.xShift),
                val -> figureBoxBlockEntity.xShift = parseSafe(val, figureBoxBlockEntity.xShift));
        addValueEditor(centerX, startY + spacing * 3, String.valueOf(figureBoxBlockEntity.yShift),
                val -> figureBoxBlockEntity.yShift = parseSafe(val, figureBoxBlockEntity.yShift));
        addValueEditor(centerX, startY + spacing * 4, String.valueOf(figureBoxBlockEntity.zShift),
                val -> figureBoxBlockEntity.zShift = parseSafe(val, figureBoxBlockEntity.zShift));
        addRenderableWidget(new RotationSlider(centerX - 50, startY + spacing * 5, 100, 20, figureBoxBlockEntity.xRot, val -> {
            figureBoxBlockEntity.xRot = val;
            updateServer();
        }));
        addRenderableWidget(new RotationSlider(centerX - 50, startY + spacing * 6, 100, 20, figureBoxBlockEntity.yRot, val -> {
            figureBoxBlockEntity.yRot = val;
            updateServer();
        }));
        addRenderableWidget(new RotationSlider(centerX - 50, startY + spacing * 7, 100, 20, figureBoxBlockEntity.zRot, val -> {
            figureBoxBlockEntity.zRot = val;
            updateServer();
        }));
        addRenderableWidget(Button.builder(Component.translatable("button.done"), (btn) -> {
            this.onClose();
        }).bounds(centerX - 50, startY + spacing * 8 + 10, 100, 20).build());
    }

    private void addValueEditor(int x, int y, String defaultValue, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x - 50, y, 100, 20, Component.empty());
        editBox.setValue(String.format("%.2f", Double.parseDouble(defaultValue)));
        editBox.setResponder(val -> {
            responder.accept(val);
            updateServer();
        });
        this.addRenderableWidget(editBox);
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            float currentVal = parseSafe(editBox.getValue(), 0.0f);
            editBox.setValue(String.format("%.2f", currentVal + 0.1f));
        }).bounds(x + 52, y, 10, 10).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            float currentVal = parseSafe(editBox.getValue(), 0.0f);
            editBox.setValue(String.format("%.2f", currentVal - 0.1f));
        }).bounds(x + 52, y + 10, 10, 10).build());
    }

    private float parseSafe(String val, float fallback) {
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void updateServer() {
        ClientFigureBoxUpdate clientFigureBoxUpdate = new ClientFigureBoxUpdate();
        clientFigureBoxUpdate.blockPos = figureBoxBlockEntity.getBlockPos();
        clientFigureBoxUpdate.open = figureBoxBlockEntity.open;
        clientFigureBoxUpdate.scale = figureBoxBlockEntity.scale;
        clientFigureBoxUpdate.xShift = figureBoxBlockEntity.xShift;
        clientFigureBoxUpdate.yShift = figureBoxBlockEntity.yShift;
        clientFigureBoxUpdate.zShift = figureBoxBlockEntity.zShift;
        clientFigureBoxUpdate.xRot = figureBoxBlockEntity.xRot;
        clientFigureBoxUpdate.yRot = figureBoxBlockEntity.yRot;
        clientFigureBoxUpdate.zRot = figureBoxBlockEntity.zRot;
        PacketDistributor.sendToServer(clientFigureBoxUpdate);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 4;
        int leftShift = 105;
        int startY = this.height / 2 - 128;
        int spacing = 24;
        guiGraphics.drawString(font, Component.translatable("edit_box.scale"), centerX - leftShift, startY + spacing + 5, Color.WHITE);
        guiGraphics.drawString(font, Component.translatable("edit_box.x_shift"), centerX - leftShift, startY + spacing * 2 + 5, Color.WHITE);
        guiGraphics.drawString(font, Component.translatable("edit_box.y_shift"), centerX - leftShift, startY + spacing * 3 + 5, Color.WHITE);
        guiGraphics.drawString(font, Component.translatable("edit_box.z_shift"), centerX - leftShift, startY + spacing * 4 + 5, Color.WHITE);
        guiGraphics.drawString(font, Component.translatable("slider.x_rot"), centerX - leftShift, startY + spacing * 5 + 5, Color.WHITE);
        guiGraphics.drawString(font, Component.translatable("slider.y_rot"), centerX - leftShift, startY + spacing * 6 + 5, Color.WHITE);
        guiGraphics.drawString(font, Component.translatable("slider.z_rot"), centerX - leftShift, startY + spacing * 7 + 5, Color.WHITE);
        drawFigureBox(guiGraphics);
    }

    private void drawFigureBox(GuiGraphics guiGraphics) {
        BlockRenderDispatcher blockDispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState state = figureBoxBlockEntity.getBlockState();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            Lighting.setupForEntityInInventory();
            poseStack.translate((float) width / 2 + 90, (float) height / 2, 512);
            float scale = 128 / Math.max(0.01f, figureBoxBlockEntity.scale);
            poseStack.last().pose().mul(new Matrix4f().scaling(scale, scale, -scale));
            poseStack.mulPose(Axis.XP.rotationDegrees(210));
            float yRot = 0f;
            if (!figureBoxBlockEntity.getBlockState().isAir()) {
                Direction facing = figureBoxBlockEntity.getBlockState().getValue(FigureBoxBlock.FACING);
                switch (facing) {
                    case NORTH -> yRot = 180f;
                    case SOUTH -> yRot = 0f;
                    case WEST -> yRot = -90f;
                    case EAST -> yRot = 90f;
                    default -> yRot = 0f;
                }
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(-15 - yRot));
            poseStack.translate(-0.5, -0.5, -0.5);
            blockDispatcher.renderSingleBlock(
                    state,
                    poseStack,
                    guiGraphics.bufferSource(),
                    15728880,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    null
            );
            FigureBoxBlockRenderer.renderEntity(poseStack, figureBoxBlockEntity, guiGraphics.bufferSource(), 15728880);
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class RotationSlider extends AbstractSliderButton {

        private final Consumer<Float> responder;

        public RotationSlider(int x, int y, int width, int height, float initialValue, Consumer<Float> responder) {
            super(x, y, width, height, Component.empty(), (initialValue + 180.0) / 360.0);
            this.responder = responder;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            int rot = (int) (this.value * 360.0 - 180.0);
            this.setMessage(Component.literal(String.format("%d°", rot)));
        }

        @Override
        protected void applyValue() {
            float rot = (float) (this.value * 360.0 - 180.0);
            this.responder.accept(rot);
        }

    }

}
