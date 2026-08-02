package org.ywzj.vehicle.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.Consumer;

public class CoordinateInputScreen extends Screen {

    private static final int PANEL_WIDTH = 250;
    private static final int PANEL_HEIGHT = 160;
    private static final int INPUT_WIDTH = 170;
    private static final int INPUT_HEIGHT = 20;
    private final Vec3 initialPosition;
    private final Consumer<Vec3> onConfirm;
    private EditBox xInput;
    private EditBox yInput;
    private EditBox zInput;
    private Button confirmButton;

    public CoordinateInputScreen(Vec3 initialPosition, Consumer<Vec3> onConfirm) {
        super(Component.translatable("screen.coordinate_input.title"));
        this.initialPosition = initialPosition;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int inputX = left + 55;

        xInput = addCoordinateInput(inputX, top + 35, "screen.coordinate_input.x", initialPosition.x);
        yInput = addCoordinateInput(inputX, top + 63, "screen.coordinate_input.y", initialPosition.y);
        zInput = addCoordinateInput(inputX, top + 91, "screen.coordinate_input.z", initialPosition.z);

        confirmButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.coordinate_input.confirm"), button -> confirm())
                .bounds(left + 23, top + 127, 98, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + 129, top + 127, 98, 20)
                .build());

        updateInputState();
        setInitialFocus(xInput);
    }

    private EditBox addCoordinateInput(int x, int y, String narrationKey, double initialValue) {
        EditBox input = new EditBox(font, x, y, INPUT_WIDTH, INPUT_HEIGHT, Component.translatable(narrationKey));
        input.setMaxLength(32);
        input.setValue(String.format(Locale.ROOT, "%.2f", initialValue));
        input.setResponder(value -> updateInputState());
        return addRenderableWidget(input);
    }

    private void updateInputState() {
        boolean xValid = isValidCoordinate(xInput.getValue());
        boolean yValid = isValidCoordinate(yInput.getValue());
        boolean zValid = isValidCoordinate(zInput.getValue());
        xInput.setTextColor(xValid ? 0xE0E0E0 : 0xFF5555);
        yInput.setTextColor(yValid ? 0xE0E0E0 : 0xFF5555);
        zInput.setTextColor(zValid ? 0xE0E0E0 : 0xFF5555);
        if (confirmButton != null) {
            confirmButton.active = xValid && yValid && zValid;
        }
    }

    private boolean isValidCoordinate(String value) {
        try {
            return Double.isFinite(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void confirm() {
        if (!confirmButton.active) {
            return;
        }
        Vec3 position = new Vec3(
                Double.parseDouble(xInput.getValue()),
                Double.parseDouble(yInput.getValue()),
                Double.parseDouble(zInput.getValue()));
        onClose();
        onConfirm.accept(position);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && confirmButton.active) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xD0101010);
        guiGraphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFFFFF);
        guiGraphics.drawString(font, Component.literal("X"), left + 31, top + 41, 0xE0E0E0);
        guiGraphics.drawString(font, Component.literal("Y"), left + 31, top + 69, 0xE0E0E0);
        guiGraphics.drawString(font, Component.literal("Z"), left + 31, top + 97, 0xE0E0E0);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
