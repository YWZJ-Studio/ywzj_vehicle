package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientDecorationAction;
import org.ywzj.vehicle.vehicle.parts.DecorationUnit;

import java.util.function.Consumer;

public class DecorationSettingsScreen extends Screen {

    private final DecorationUnit decorationUnit;
    private float viewShiftX;
    private float viewShiftY;
    private float viewScale = 1;
    private float viewRotX;
    private float viewRotY;

    public DecorationSettingsScreen(DecorationUnit decorationUnit) {
        super(Component.literal("Decoration Settings"));
        this.decorationUnit = decorationUnit;
        decorationUnit.setting = true;
        Vector3f rot = new Vector3f();
        decorationUnit.rotation.getEulerAnglesYXZ(rot);
        this.viewRotX = (float) (180 - Math.toDegrees(rot.x));
        this.viewRotY = (float) (decorationUnit.getVehicle().getYRot() - Math.toDegrees(rot.y));
    }

    @Override
    protected void init() {
        int centerX = this.width / 4;
        int startY = this.height / 2 - 128;
        int spacing = 24;
        addValueEditor(centerX, startY + spacing, String.valueOf(decorationUnit.scale),
                val -> decorationUnit.scale = parseSafe(val, decorationUnit.scale));

        addValueEditor(centerX, startY + spacing * 2, String.valueOf(decorationUnit.offsetFromBone.x),
                val -> decorationUnit.offsetFromBone = new Vec3(parseSafe(val, (float) decorationUnit.offsetFromBone.x), decorationUnit.offsetFromBone.y, decorationUnit.offsetFromBone.z));

        addValueEditor(centerX, startY + spacing * 3, String.valueOf(decorationUnit.offsetFromBone.y),
                val -> decorationUnit.offsetFromBone = new Vec3(decorationUnit.offsetFromBone.x, parseSafe(val, (float) decorationUnit.offsetFromBone.y), decorationUnit.offsetFromBone.z));

        addValueEditor(centerX, startY + spacing * 4, String.valueOf(decorationUnit.offsetFromBone.z),
                val -> decorationUnit.offsetFromBone = new Vec3(decorationUnit.offsetFromBone.x, decorationUnit.offsetFromBone.y, parseSafe(val, (float) decorationUnit.offsetFromBone.z)));
        addRenderableWidget(new RotationSlider(centerX - 50, startY + spacing * 5, 100, 20, decorationUnit.selfXRot, val -> {
            decorationUnit.selfXRot = val;
            updateServer();
        }));
        addRenderableWidget(new RotationSlider(centerX - 50, startY + spacing * 6, 100, 20, decorationUnit.selfYRot, val -> {
            decorationUnit.selfYRot = val;
            updateServer();
        }));
        addRenderableWidget(new RotationSlider(centerX - 50, startY + spacing * 7, 100, 20, decorationUnit.selfZRot, val -> {
            decorationUnit.selfZRot = val;
            updateServer();
        }));
        addRenderableWidget(Button.builder(Component.translatable("button.done"), (btn) -> {
            onClose();
        }).bounds(centerX - 50, startY + spacing * 8 + 10, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("button.delete"), (btn) -> {
            removeDecoration();
            onClose();
        }).bounds(centerX + 54, startY + spacing * 8 + 10, 40, 20).build());
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
        ClientDecorationAction clientDecorationAction = new ClientDecorationAction();
        clientDecorationAction.action = ClientDecorationAction.Action.SET;
        clientDecorationAction.decorationDisplayId = decorationUnit.decorationDisplayId;
        if (StringUtils.isEmpty(clientDecorationAction.decorationDisplayId)) {
            return;
        }
        clientDecorationAction.vehicleId = decorationUnit.getVehicle().getId();
        clientDecorationAction.decorationUnitId = decorationUnit.getId();
        clientDecorationAction.baseBoneName = decorationUnit.baseBoneName;
        clientDecorationAction.scale = decorationUnit.scale;
        clientDecorationAction.selfXRot = decorationUnit.selfXRot;
        clientDecorationAction.selfYRot = decorationUnit.selfYRot;
        clientDecorationAction.selfZRot = decorationUnit.selfZRot;
        clientDecorationAction.offsetFromBone = decorationUnit.offsetFromBone;
        Channel.CHANNEL.sendToServer(clientDecorationAction);
    }

    private void removeDecoration() {
        ClientDecorationAction clientDecorationAction = new ClientDecorationAction();
        clientDecorationAction.action = ClientDecorationAction.Action.REMOVE;
        clientDecorationAction.decorationDisplayId = decorationUnit.decorationDisplayId;
        if (StringUtils.isEmpty(clientDecorationAction.decorationDisplayId)) {
            return;
        }
        clientDecorationAction.vehicleId = decorationUnit.getVehicle().getId();
        clientDecorationAction.decorationUnitId = decorationUnit.getId();
        Channel.CHANNEL.sendToServer(clientDecorationAction);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int centerX = this.width / 4;
        int startY = this.height / 2 - 128;
        int leftShift = 105;
        int spacing = 24;
        guiGraphics.drawString(font, Component.translatable("edit_box.scale"), centerX - leftShift, startY + spacing + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("edit_box.x_shift"), centerX - leftShift, startY + spacing * 2 + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("edit_box.y_shift"), centerX - leftShift, startY + spacing * 3 + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("edit_box.z_shift"), centerX - leftShift, startY + spacing * 4 + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("slider.x_rot"), centerX - leftShift, startY + spacing * 5 + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("slider.y_rot"), centerX - leftShift, startY + spacing * 6 + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("slider.z_rot"), centerX - leftShift, startY + spacing * 7 + 5, 0xFFFFFF);
        drawPreview(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawPreview(GuiGraphics guiGraphics) {
        AbstractVehicle vehicle = decorationUnit.getVehicle();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate((float) width / 2 + 80 + viewShiftX, (float) height / 2 + viewShiftY, 512);
            double length = vehicle.getStructureLength();
            float scale = 300 * (float) (1 / length / 1.2) * this.viewScale;
            poseStack.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
            poseStack.mulPose(Axis.XP.rotationDegrees(viewRotX));
            poseStack.mulPose(Axis.YP.rotationDegrees(viewRotY));
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            Lighting.setupForEntityInInventory();
            RenderSystem.runAsFancy(() ->
                    dispatcher.render(
                            vehicle,
                            0, 0, 0,
                            0,
                            1.0F,
                            poseStack,
                            guiGraphics.bufferSource(),
                            15728880
                    )
            );
        }
        poseStack.popPose();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean isHandled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (!isHandled) {
            if (button == 0) {
                this.viewRotY += (float) dragX;
                this.viewRotX += (float) dragY;
                return true;
            }
            else if (button == 1) {
                this.viewShiftX += (float) dragX;
                this.viewShiftY += (float) dragY;
                return true;
            }
        }
        return isHandled;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        boolean isHandled = super.mouseScrolled(pMouseX, pMouseY, pDelta);
        if (!isHandled) {
            if (pDelta > 0) {
                this.viewScale *= 1.1f;
            } else if (pDelta < 0) {
                this.viewScale *= 0.9f;
            }
            this.viewScale = Math.max(0.1f, Math.min(this.viewScale, 10.0f));
            return true;
        }
        return isHandled;
    }

    @Override
    public void onClose() {
        super.onClose();
        decorationUnit.setting = false;
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

        public RotationSlider(int x, int y, int width, int height, float initialValue, java.util.function.Consumer<Float> responder) {
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
            float rot = (int) (this.value * 360.0 - 180.0);
            this.responder.accept(rot);
        }

    }

}
