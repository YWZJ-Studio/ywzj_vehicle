package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import net.neoforged.neoforge.network.PacketDistributor;
import org.ywzj.vehicle.network.message.ClientDecorationAction;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;

import java.util.Locale;

public class DecorationSettingsScreen extends ApricityScreen {

    private static final String TEMPLATE = "screens/decoration_settings.html";
    private final DecorationUnit decorationUnit;
    private Document document;
    private Element scaleInput;
    private Element xOffsetInput;
    private Element yOffsetInput;
    private Element zOffsetInput;
    private Element xRotationInput;
    private Element yRotationInput;
    private Element zRotationInput;
    private Element previewAnchor;
    private Element doneButton;
    private Element deleteButton;
    private float viewShiftX;
    private float viewShiftY;
    private float viewScale = 1;
    private float viewRotX;
    private float viewRotY;
    private boolean previewDragging;
    private boolean serverUpdatePending;
    private int serverUpdateCooldown;
    private boolean sliderDragging;
    private boolean renderStateInitialized;
    private long lastInterpolationNanos;
    private float renderScale;
    private float renderXRotation;
    private float renderYRotation;
    private float renderZRotation;
    private Vec3 renderOffset;
    private long lastRangeValueUpdateNanos;

    public DecorationSettingsScreen(DecorationUnit decorationUnit) {
        super(TEMPLATE);
        this.decorationUnit = decorationUnit;
        Vector3f rotation = new Vector3f();
        decorationUnit.rotation.getEulerAnglesYXZ(rotation);
        this.viewRotX = (float) (180 - Math.toDegrees(rotation.x));
        this.viewRotY = (float) (decorationUnit.getVehicle().getYRot() - Math.toDegrees(rotation.y));
        setPauseGame(false);
        setShowDefaultBackground(false);
    }

    @Override
    protected void init() {
        super.init();
        document = getLinkedDocument();
        if (document == null) {
            return;
        }
        scaleInput = element("decoration-scale");
        xOffsetInput = element("decoration-x-offset");
        yOffsetInput = element("decoration-y-offset");
        zOffsetInput = element("decoration-z-offset");
        xRotationInput = element("decoration-x-rotation");
        yRotationInput = element("decoration-y-rotation");
        zRotationInput = element("decoration-z-rotation");
        previewAnchor = element("decoration-settings-preview");
        doneButton = element("close-button");
        deleteButton = element("delete-button");

        bindNumber(scaleInput, decorationUnit.scale, value -> decorationUnit.scale = value);
        bindNumber(xOffsetInput, (float) decorationUnit.offsetFromBone.x,
                value -> decorationUnit.offsetFromBone = new Vec3(value, decorationUnit.offsetFromBone.y, decorationUnit.offsetFromBone.z));
        bindNumber(yOffsetInput, (float) decorationUnit.offsetFromBone.y,
                value -> decorationUnit.offsetFromBone = new Vec3(decorationUnit.offsetFromBone.x, value, decorationUnit.offsetFromBone.z));
        bindNumber(zOffsetInput, (float) decorationUnit.offsetFromBone.z,
                value -> decorationUnit.offsetFromBone = new Vec3(decorationUnit.offsetFromBone.x, decorationUnit.offsetFromBone.y, value));
        bindNumber(xRotationInput, decorationUnit.selfXRot, value -> decorationUnit.selfXRot = value);
        bindNumber(yRotationInput, decorationUnit.selfYRot, value -> decorationUnit.selfYRot = value);
        bindNumber(zRotationInput, decorationUnit.selfZRot, value -> decorationUnit.selfZRot = value);
        if (doneButton != null) {
            doneButton.addEventListener("click", event -> onClose());
        }
        if (deleteButton != null) {
            deleteButton.addEventListener("click", event -> {
                serverUpdatePending = false;
                removeDecoration();
                onClose();
            });
        }
    }

    private Element element(String id) {
        return document.getElementById(id);
    }

    private void bindNumber(Element input, float initial, FloatConsumer consumer) {
        if (input == null) {
            return;
        }
        input.setValue(String.format(Locale.ROOT, "%.2f", initial));
        updateRangeValue(input, true);
        input.addEventListener("input", event -> applyNumber(input, consumer, initial, false));
        input.addEventListener("change", event -> applyNumber(input, consumer, initial, true));
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

    private void applyNumber(Element input, FloatConsumer consumer, float fallback, boolean commit) {
        float value = roundToHundredth(parseSafe(input.getValue(), fallback));
        consumer.accept(value);
        if (commit && !"range".equalsIgnoreCase(input.getAttribute("type"))) {
            input.setValue(String.format(Locale.ROOT, "%.2f", value));
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

    private float parseSafe(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void updateServer() {
        ClientDecorationAction update = new ClientDecorationAction();
        update.action = ClientDecorationAction.Action.SET;
        update.displayId = decorationUnit.getDisplayId().toString();
        if (StringUtils.isEmpty(update.displayId)) {
            return;
        }
        update.vehicleId = decorationUnit.getVehicle().getId();
        update.decorationUnitId = decorationUnit.getId();
        update.baseBoneName = decorationUnit.baseBoneName;
        update.scale = decorationUnit.scale;
        update.selfXRot = decorationUnit.selfXRot;
        update.selfYRot = decorationUnit.selfYRot;
        update.selfZRot = decorationUnit.selfZRot;
        update.offsetFromBone = decorationUnit.offsetFromBone;
        PacketDistributor.sendToServer(update);
    }

    private void removeDecoration() {
        ClientDecorationAction update = new ClientDecorationAction();
        update.action = ClientDecorationAction.Action.REMOVE;
        update.displayId = decorationUnit.getDisplayId().toString();
        if (StringUtils.isEmpty(update.displayId)) {
            return;
        }
        update.vehicleId = decorationUnit.getVehicle().getId();
        update.decorationUnitId = decorationUnit.getId();
        PacketDistributor.sendToServer(update);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPreview(graphics);
    }

    private void renderPreview(GuiGraphics graphics) {
        Bounds bounds = getPreviewBounds();
        if (bounds == null) {
            return;
        }
        AbstractVehicle vehicle = decorationUnit.getVehicle();
        updateRenderState();
        double length = vehicle.getStructureLength();
        float scale = (float) (Math.min(bounds.width(), bounds.height()) * 1.5 / Math.max(length, 3)) * viewScale;
        graphics.enableScissor((int) bounds.left, (int) bounds.top, (int) Math.ceil(bounds.right), (int) Math.ceil(bounds.bottom));
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate((float) bounds.centerX() + viewShiftX, (float) bounds.centerY() + viewShiftY, 512);
            poseStack.scale(scale, scale, -scale);
            poseStack.mulPose(Axis.XP.rotationDegrees(viewRotX));
            poseStack.mulPose(Axis.YP.rotationDegrees(viewRotY));
            Vec3 centerOffset = vehicle.getBoundingBox().getCenter().subtract(vehicle.position());
            poseStack.translate((float) -centerOffset.x, (float) -centerOffset.y, (float) -centerOffset.z);
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            Lighting.setupForEntityInInventory();
            float targetScale = decorationUnit.scale;
            float targetXRotation = decorationUnit.selfXRot;
            float targetYRotation = decorationUnit.selfYRot;
            float targetZRotation = decorationUnit.selfZRot;
            Vec3 targetOffset = decorationUnit.offsetFromBone;
            decorationUnit.scale = renderScale;
            decorationUnit.selfXRot = renderXRotation;
            decorationUnit.selfYRot = renderYRotation;
            decorationUnit.selfZRot = renderZRotation;
            decorationUnit.offsetFromBone = renderOffset;
            try {
                RenderSystem.runAsFancy(() -> {
                    dispatcher.render(vehicle, 0, 0, 0, 0, 1.0F, poseStack, graphics.bufferSource(), 15728880);
                    drawDecorationArrow(poseStack, graphics);
                });
                graphics.flush();
            } finally {
                decorationUnit.scale = targetScale;
                decorationUnit.selfXRot = targetXRotation;
                decorationUnit.selfYRot = targetYRotation;
                decorationUnit.selfZRot = targetZRotation;
                decorationUnit.offsetFromBone = targetOffset;
            }
        } finally {
            dispatcherResetShadow();
            Lighting.setupFor3DItems();
            poseStack.popPose();
            graphics.disableScissor();
        }
    }

    private void updateRenderState() {
        if (sliderDragging) {
            decorationUnit.selfXRot = readInputValue(xRotationInput, decorationUnit.selfXRot);
            decorationUnit.selfYRot = readInputValue(yRotationInput, decorationUnit.selfYRot);
            decorationUnit.selfZRot = readInputValue(zRotationInput, decorationUnit.selfZRot);
        }
        if (!renderStateInitialized) {
            renderScale = decorationUnit.scale;
            renderXRotation = decorationUnit.selfXRot;
            renderYRotation = decorationUnit.selfYRot;
            renderZRotation = decorationUnit.selfZRot;
            renderOffset = decorationUnit.offsetFromBone;
            renderStateInitialized = true;
            lastInterpolationNanos = System.nanoTime();
            return;
        }
        float alpha = sliderDragging ? 1.0f : interpolationAlpha();
        renderScale = Mth.lerp(alpha, renderScale, decorationUnit.scale);
        renderXRotation = Mth.rotLerp(alpha, renderXRotation, decorationUnit.selfXRot);
        renderYRotation = Mth.rotLerp(alpha, renderYRotation, decorationUnit.selfYRot);
        renderZRotation = Mth.rotLerp(alpha, renderZRotation, decorationUnit.selfZRot);
        Vec3 target = decorationUnit.offsetFromBone;
        renderOffset = new Vec3(
                Mth.lerp(alpha, (float) renderOffset.x, (float) target.x),
                Mth.lerp(alpha, (float) renderOffset.y, (float) target.y),
                Mth.lerp(alpha, (float) renderOffset.z, (float) target.z));
    }

    private float readInputValue(Element input, float fallback) {
        if (input == null) {
            return fallback;
        }
        return roundToHundredth(parseSafe(input.getValue(), fallback));
    }

    private float interpolationAlpha() {
        long now = System.nanoTime();
        float delta = Math.min(0.1f, Math.max(0, (now - lastInterpolationNanos) / 1_000_000_000.0f));
        lastInterpolationNanos = now;
        return 1.0f - (float) Math.exp(-48.0f * delta);
    }

    private void dispatcherResetShadow() {
        Minecraft.getInstance().getEntityRenderDispatcher().setRenderShadow(true);
    }

    private void drawDecorationArrow(PoseStack poseStack, GuiGraphics graphics) {
        if (decorationUnit.offsetFromVehicle == null || decorationUnit.rotation == null) {
            return;
        }
        Vector3f rotation = new Vector3f();
        decorationUnit.rotation.getEulerAnglesYXZ(rotation);
        poseStack.pushPose();
        AbstractVehicle vehicle = decorationUnit.getVehicle();
        Vec3 root = vehicle.centerOffset;
        poseStack.rotateAround(Axis.YP.rotationDegrees(-vehicle.getViewYRot(1.0F)), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.XP.rotationDegrees(vehicle.getViewXRot(1.0F)), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getViewZRot(1.0F)), (float) root.x, (float) root.y, (float) root.z);
        poseStack.translate(decorationUnit.offsetFromVehicle.x, decorationUnit.offsetFromVehicle.y, decorationUnit.offsetFromVehicle.z);
        poseStack.scale(decorationUnit.scale, decorationUnit.scale, decorationUnit.scale);
        poseStack.rotateAround(Axis.YP.rotation(rotation.y), 0, 0, 0);
        poseStack.rotateAround(Axis.XP.rotation(rotation.x), 0, 0, 0);
        poseStack.rotateAround(Axis.ZP.rotation(rotation.z), 0, 0, 0);
        RenderHelper.renderArrow3D(poseStack, graphics.bufferSource(), 0.15f, 0.3f, 0, 255, 0, 255);
        poseStack.popPose();
    }

    private Bounds getPreviewBounds() {
        if (previewAnchor == null || document == null) {
            return null;
        }
        Element.DOMRect rect = previewAnchor.getBoundingClientRect();
        if (rect.width <= 0 || rect.height <= 0) {
            return null;
        }
        double scale = document.getViewport().renderScale();
        return new Bounds(rect.x * scale, rect.y * scale, (rect.x + rect.width) * scale, (rect.y + rect.height) * scale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        Bounds bounds = getPreviewBounds();
        if (bounds != null && bounds.contains(mouseX, mouseY)) {
            viewScale = Mth.clamp(viewScale * (float) Math.pow(1.1, deltaY), 0.1f, 10.0f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Bounds bounds = getPreviewBounds();
        if ((button == 0 || button == 1) && bounds != null && bounds.contains(mouseX, mouseY)) {
            previewDragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && previewDragging) {
            viewRotY += (float) dragX;
            viewRotX += (float) dragY;
            return true;
        }
        if (button == 1 && previewDragging) {
            viewShiftX += (float) dragX;
            viewShiftY += (float) dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
        if (previewDragging && (button == 0 || button == 1)) {
            previewDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

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
    public boolean isPauseScreen() { return false; }

    @FunctionalInterface
    private interface FloatConsumer { void accept(float value); }

    private record Bounds(double left, double top, double right, double bottom) {
        private boolean contains(double x, double y) { return x >= left && x <= right && y >= top && y <= bottom; }
        private double width() { return right - left; }
        private double height() { return bottom - top; }
        private double centerX() { return (left + right) / 2; }
        private double centerY() { return (top + bottom) / 2; }
    }

}
