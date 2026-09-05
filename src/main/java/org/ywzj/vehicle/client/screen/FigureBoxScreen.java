package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;
import org.ywzj.vehicle.client.render.entity.block.FigureBoxBlockRenderer;
import org.ywzj.vehicle.network.message.ClientFigureBoxUpdate;

import java.util.Locale;

public class FigureBoxScreen extends ApricityScreen {

    private static final String TEMPLATE = "screens/figure_box.html";
    private final FigureBoxBlockEntity figureBoxBlockEntity;
    private Document document;
    private Element openInput;
    private Element scaleInput;
    private Element xShiftInput;
    private Element yShiftInput;
    private Element zShiftInput;
    private Element xRotationInput;
    private Element yRotationInput;
    private Element zRotationInput;
    private Element previewAnchor;
    private Element doneButton;
    private boolean serverUpdatePending;
    private int serverUpdateCooldown;
    private boolean sliderDragging;
    private float scaleInputValue = 1;
    private float xShiftInputValue;
    private float yShiftInputValue;
    private float zShiftInputValue;
    private float xRotationInputValue;
    private float yRotationInputValue;
    private float zRotationInputValue;
    private Bounds previewBounds;
    private long lastRangeValueUpdateNanos;

    public FigureBoxScreen(FigureBoxBlockEntity figureBoxBlockEntity) {
        super(TEMPLATE);
        this.figureBoxBlockEntity = figureBoxBlockEntity;
        setPauseGame(false);
        setShowDefaultBackground(false);
    }

    @Override
    protected void init() {
        previewBounds = null;
        super.init();
        document = getLinkedDocument();
        if (document == null) {
            return;
        }

        openInput = element("figure-box-open");
        scaleInput = element("figure-box-scale");
        xShiftInput = element("figure-box-x-shift");
        yShiftInput = element("figure-box-y-shift");
        zShiftInput = element("figure-box-z-shift");
        xRotationInput = element("figure-box-x-rotation");
        yRotationInput = element("figure-box-y-rotation");
        zRotationInput = element("figure-box-z-rotation");
        previewAnchor = element("figure-box-preview");
        doneButton = element("close-button");

        if (openInput != null) {
            openInput.setChecked(figureBoxBlockEntity.open);
            openInput.addEventListener("change", event -> {
                figureBoxBlockEntity.open = openInput.isChecked();
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level != null) {
                    minecraft.level.setBlock(figureBoxBlockEntity.getBlockPos(),
                            figureBoxBlockEntity.getBlockState().setValue(FigureBoxBlock.OPEN, figureBoxBlockEntity.open), 3);
                }
                updateServer();
            });
        }
        bindNumber(scaleInput, figureBoxBlockEntity.scale, value -> scaleInputValue = value);
        bindNumber(xShiftInput, figureBoxBlockEntity.xShift, value -> xShiftInputValue = value);
        bindNumber(yShiftInput, figureBoxBlockEntity.yShift, value -> yShiftInputValue = value);
        bindNumber(zShiftInput, figureBoxBlockEntity.zShift, value -> zShiftInputValue = value);
        bindNumber(xRotationInput, figureBoxBlockEntity.xRot, value -> xRotationInputValue = value);
        bindNumber(yRotationInput, figureBoxBlockEntity.yRot, value -> yRotationInputValue = value);
        bindNumber(zRotationInput, figureBoxBlockEntity.zRot, value -> zRotationInputValue = value);
        if (doneButton != null) {
            doneButton.addEventListener("click", event -> onClose());
        }
    }

    private Element element(String id) {
        return document.getElementById(id);
    }

    private void bindNumber(Element input, float initial, FloatConsumer consumer) {
        if (input == null) {
            return;
        }
        input.setValue(format(initial));
        consumer.accept(initial);
        updateRangeValue(input, true);
        input.addEventListener("input", event -> applyNumber(input, consumer, false));
        input.addEventListener("change", event -> applyNumber(input, consumer, true));
        if ("range".equalsIgnoreCase(input.getAttribute("type"))) {
            Element valueInput = rangeValueInput(input);
            if (valueInput != null) {
                valueInput.addEventListener("input", event -> applyRangeValueInput(input, valueInput, consumer, false));
                valueInput.addEventListener("change", event -> applyRangeValueInput(input, valueInput, consumer, true));
            }
            input.addEventListener("mousedown", event -> sliderDragging = true);
            input.addEventListener("mouseup", event -> sliderDragging = false);
            input.addEventListener("blur", event -> sliderDragging = false);
        }
    }

    private void applyNumber(Element input, FloatConsumer consumer, boolean commit) {
        float value = roundToHundredth(parseSafe(input.getValue(), 0));
        consumer.accept(value);
        if (commit && !"range".equalsIgnoreCase(input.getAttribute("type"))) {
            input.setValue(format(value));
        }
        updateRangeValue(input, commit);
        if (commit) {
            requestServerUpdate();
        }
    }

    private void updateRangeValue(Element input, boolean force) {
        if (!"range".equalsIgnoreCase(input.getAttribute("type"))) {
            return;
        }
        Element valueInput = rangeValueInput(input);
        if (valueInput != null) {
            String text = String.format(Locale.ROOT, "%.2f", parseSafe(input.getValue(), 0));
            long now = System.nanoTime();
            if (!force && now - lastRangeValueUpdateNanos < 33_000_000L) {
                return;
            }
            if (!text.equals(valueInput.getValue())) {
                valueInput.setValue(text);
            }
            lastRangeValueUpdateNanos = now;
        }
    }

    private Element rangeValueInput(Element rangeInput) {
        return document.getElementById(rangeInput.getAttribute("id") + "-value");
    }

    private void applyRangeValueInput(Element rangeInput, Element valueInput, FloatConsumer consumer, boolean commit) {
        float value;
        try {
            value = roundToHundredth(Mth.clamp(Float.parseFloat(valueInput.getValue()), -180, 180));
        } catch (NumberFormatException exception) {
            return;
        }
        rangeInput.setValue(Float.toString(value));
        consumer.accept(value);
        if (commit) {
            valueInput.setValue(String.format(Locale.ROOT, "%.2f", value));
            requestServerUpdate();
        }
    }

    private float roundToHundredth(float value) {
        return Math.round(value * 100.0f) / 100.0f;
    }

    @Override
    public void tick() {
        super.tick();
        previewBounds = readPreviewBounds();
        if (serverUpdatePending) {
            if (serverUpdateCooldown > 0) {
                serverUpdateCooldown--;
            } else {
                serverUpdatePending = false;
                serverUpdateCooldown = 2;
                updateServer();
            }
        }
    }

    private void requestServerUpdate() {
        serverUpdatePending = true;
    }

    private String format(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private float parseSafe(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void updateServer() {
        ClientFigureBoxUpdate update = new ClientFigureBoxUpdate();
        update.blockPos = figureBoxBlockEntity.getBlockPos();
        update.open = figureBoxBlockEntity.open;
        update.scale = scaleInputValue;
        update.xShift = xShiftInputValue;
        update.yShift = yShiftInputValue;
        update.zShift = zShiftInputValue;
        update.xRot = xRotationInputValue;
        update.yRot = yRotationInputValue;
        update.zRot = zRotationInputValue;
        PacketDistributor.sendToServer(update);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFigureBox(graphics, partialTick);
    }

    private void renderFigureBox(GuiGraphics graphics, float partialTick) {
        if (previewBounds == null) {
            previewBounds = readPreviewBounds();
        }
        if (previewBounds == null) {
            return;
        }
        double left = previewBounds.left;
        double top = previewBounds.top;
        double right = previewBounds.right;
        double bottom = previewBounds.bottom;
        double centerX = (left + right) / 2;
        double centerY = (top + bottom) / 2;
        updateRenderState(partialTick);
        double scale = Math.min(right - left, bottom - top) * 0.72 / Math.max(0.01f, figureBoxBlockEntity.scale);

        graphics.enableScissor((int) left, (int) top, (int) Math.ceil(right), (int) Math.ceil(bottom));
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            Lighting.setupForEntityInInventory();
            poseStack.translate((float) centerX, (float) centerY, 512);
            poseStack.scale((float) scale, (float) scale, (float) -scale);
            poseStack.mulPose(Axis.XP.rotationDegrees(210));
            float yRotation = 0;
            BlockState state = figureBoxBlockEntity.getBlockState();
            if (!state.isAir()) {
                Direction facing = state.getValue(FigureBoxBlock.FACING);
                yRotation = switch (facing) {
                    case NORTH -> 180;
                    case WEST -> -90;
                    case EAST -> 90;
                    default -> 0;
                };
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(-15 - yRotation));
            poseStack.translate(-0.5, -0.5, -0.5);
            BlockRenderDispatcher blockDispatcher = Minecraft.getInstance().getBlockRenderer();
            blockDispatcher.renderSingleBlock(state, poseStack, graphics.bufferSource(), 15728880,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            FigureBoxBlockRenderer.renderEntity(poseStack, figureBoxBlockEntity, graphics.bufferSource(), 15728880);
            graphics.flush();
        } finally {
            Lighting.setupFor3DItems();
            poseStack.popPose();
            graphics.disableScissor();
        }
    }

    private Bounds readPreviewBounds() {
        if (previewAnchor == null || document == null) {
            return null;
        }
        Element.DOMRect rect = previewAnchor.getBoundingClientRect();
        if (rect.width <= 0 || rect.height <= 0) {
            return null;
        }
        double scale = document.getViewport().renderScale();
        return new Bounds(rect.x * scale, rect.y * scale,
                (rect.x + rect.width) * scale, (rect.y + rect.height) * scale);
    }

    private void updateRenderState(float partialTick) {
        if (sliderDragging) {
            xRotationInputValue = readInputValue(xRotationInput, xRotationInputValue);
            yRotationInputValue = readInputValue(yRotationInput, yRotationInputValue);
            zRotationInputValue = readInputValue(zRotationInput, zRotationInputValue);
            figureBoxBlockEntity.scale = scaleInputValue;
            figureBoxBlockEntity.xShift = xShiftInputValue;
            figureBoxBlockEntity.yShift = yShiftInputValue;
            figureBoxBlockEntity.zShift = zShiftInputValue;
            figureBoxBlockEntity.xRot = xRotationInputValue;
            figureBoxBlockEntity.yRot = yRotationInputValue;
            figureBoxBlockEntity.zRot = zRotationInputValue;
            return;
        }
        figureBoxBlockEntity.scale = Mth.lerp(partialTick, figureBoxBlockEntity.scale, scaleInputValue);
        figureBoxBlockEntity.xShift = Mth.lerp(partialTick, figureBoxBlockEntity.xShift, xShiftInputValue);
        figureBoxBlockEntity.yShift = Mth.lerp(partialTick, figureBoxBlockEntity.yShift, yShiftInputValue);
        figureBoxBlockEntity.zShift = Mth.lerp(partialTick, figureBoxBlockEntity.zShift, zShiftInputValue);
        figureBoxBlockEntity.xRot = Mth.rotLerp(partialTick, figureBoxBlockEntity.xRot, xRotationInputValue);
        figureBoxBlockEntity.yRot = Mth.rotLerp(partialTick, figureBoxBlockEntity.yRot, yRotationInputValue);
        figureBoxBlockEntity.zRot = Mth.rotLerp(partialTick, figureBoxBlockEntity.zRot, zRotationInputValue);
    }

    private float readInputValue(Element input, float fallback) {
        return input == null ? fallback : roundToHundredth(parseSafe(input.getValue(), fallback));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (serverUpdatePending) {
            serverUpdatePending = false;
            serverUpdateCooldown = 0;
            updateServer();
        }
        super.onClose();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean wasDragging = sliderDragging;
            sliderDragging = false;
            if (wasDragging) {
                requestServerUpdate();
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @FunctionalInterface
    private interface FloatConsumer {
        void accept(float value);
    }

    private record Bounds(double left, double top, double right, double bottom) {}

}
