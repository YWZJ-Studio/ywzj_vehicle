package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;
import org.ywzj.vehicle.client.render.entity.block.FigureBoxBlockRenderer;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientFigureBoxUpdate;

public class FigureBoxScreen extends Screen {

    private final FigureBoxBlockEntity figureBoxBlockEntity;

    public FigureBoxScreen(FigureBoxBlockEntity figureBoxBlockEntity) {
        super(Component.literal("Figure Box Settings"));
        this.figureBoxBlockEntity = figureBoxBlockEntity;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 10;
        int spacing = 24;
        addRenderableWidget(new Checkbox(centerX + 60, startY + 6 * spacing, 100, 20,
                Component.translatable("check_box.figure_box.open"), figureBoxBlockEntity.open) {
            @Override
            public void onPress() {
                super.onPress();
                figureBoxBlockEntity.open = this.selected();
                Minecraft.getInstance().level.setBlock(figureBoxBlockEntity.getBlockPos(),
                        figureBoxBlockEntity.getBlockState().setValue(FigureBoxBlock.OPEN, figureBoxBlockEntity.open),
                        3);
                updateServer();
            }
        });
        createEditBox(centerX, startY + spacing, String.valueOf(figureBoxBlockEntity.scale),
                val -> figureBoxBlockEntity.scale = parseSafe(val, figureBoxBlockEntity.scale));
        createEditBox(centerX, startY + spacing * 2, String.valueOf(figureBoxBlockEntity.xShift),
                val -> figureBoxBlockEntity.xShift = parseSafe(val, figureBoxBlockEntity.xShift));
        createEditBox(centerX, startY + spacing * 3, String.valueOf(figureBoxBlockEntity.yShift),
                val -> figureBoxBlockEntity.yShift = parseSafe(val, figureBoxBlockEntity.yShift));
        createEditBox(centerX, startY + spacing * 4, String.valueOf(figureBoxBlockEntity.zShift),
                val -> figureBoxBlockEntity.zShift = parseSafe(val, figureBoxBlockEntity.zShift));
        createEditBox(centerX, startY + spacing * 5, String.valueOf(figureBoxBlockEntity.xRot),
                val -> figureBoxBlockEntity.xRot = parseSafe(val, figureBoxBlockEntity.xRot));
        createEditBox(centerX, startY + spacing * 6, String.valueOf(figureBoxBlockEntity.yRot),
                val -> figureBoxBlockEntity.yRot = parseSafe(val, figureBoxBlockEntity.yRot));
        addRenderableWidget(Button.builder(Component.translatable("button.figure_box.done"), (btn) -> {
            this.onClose();
        }).bounds(centerX - 50, startY + spacing * 7 + 10, 100, 20).build());
    }

    private EditBox createEditBox(int x, int y, String defaultValue, java.util.function.Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x - 50, y, 100, 20, Component.empty());
        editBox.setValue(defaultValue);
        editBox.setResponder(val -> {
            responder.accept(val);
            updateServer();
        });
        return addRenderableWidget(editBox);
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
        Channel.CHANNEL.sendToServer(clientFigureBoxUpdate);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int leftShift = 105;
        int startY = 10;
        int spacing = 24;
        guiGraphics.drawString(font, Component.translatable("edit_box.figure_box.scale"), centerX - leftShift, startY + spacing + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("edit_box.figure_box.x_shift"), centerX - leftShift, startY + spacing * 2 + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("edit_box.figure_box.y_shift"), centerX - leftShift, startY + spacing * 3 + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("edit_box.figure_box.z_shift"), centerX - leftShift, startY + spacing * 4 + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("edit_box.figure_box.x_rot"), centerX - leftShift, startY + spacing * 5 + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("edit_box.figure_box.y_rot"), centerX - leftShift, startY + spacing * 6 + 5, 0xFFFFFF);
        renderFigureBox(guiGraphics);
    }

    private void renderFigureBox(GuiGraphics guiGraphics) {
        BlockRenderDispatcher blockDispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState state = figureBoxBlockEntity.getBlockState();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            Lighting.setupForEntityInInventory();
            poseStack.translate((float) width / 2 + 115, (float) height / 2 - 25, 512);
            float scale = 64 / figureBoxBlockEntity.scale;
            poseStack.scale(scale, -scale, scale);
            poseStack.mulPose(Axis.XP.rotationDegrees(30));
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

}
