package org.ywzj.vehicle.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.ywzj.vehicle.resource.VehiclePackLoader;

import java.nio.file.Path;
import java.util.List;

public class VehiclePackIssuesScreen extends Screen {

    private final List<VehiclePackLoader.PackIssue> issues;

    public VehiclePackIssuesScreen(List<VehiclePackLoader.PackIssue> issues) {
        super(Component.translatable("screen.vehicle_packs.title"));
        this.issues = issues;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(420, width - 32);
        int left = (width - panelWidth) / 2;
        var text = Component.translatable("screen.vehicle_packs.hint");
        Path previous = null;
        for (var issue : issues) {
            if (!issue.path().equals(previous)) {
                text.append("\n\n").append(Component.literal(issue.path().getFileName().toString()).withStyle(ChatFormatting.GOLD));
                previous = issue.path();
            }
            text.append("\n").append(issue.reason());
        }
        addRenderableWidget(new FittingMultiLineTextWidget(left, 40, panelWidth, height - 82, text, font));
        int buttonWidth = (panelWidth - 8) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.vehicle_packs.open_folder"),
                        button -> Util.getPlatform().openFile(VehiclePackLoader.INSTANCE.getPacksDirectory().toFile()))
                .bounds(left, height - 32, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.quit"), button -> minecraft.stop())
                .bounds(left + panelWidth - buttonWidth, height - 32, buttonWidth, 20).build());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFF55);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

}
