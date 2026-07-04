package org.ywzj.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import org.joml.Matrix4f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleChangeDisplay;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleModdingToolScreen extends Screen {

    private static final int ITEM_HEIGHT = 20;
    private static final int LIST_WIDTH = 150;
    private static final int VISIBLE_ITEMS = 10;
    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private int leftPos;
    private int topPos;
    private EditBox searchBox;
    private ForgeSlider smokeRSlider;
    private ForgeSlider smokeGSlider;
    private ForgeSlider smokeBSlider;
    private final AbstractVehicle vehicle;
    private List<ResourceLocation> variableDisplayIds = new ArrayList<>();
    private List<ResourceLocation> filteredVariableDisplayIds = new ArrayList<>();
    private float viewShiftX;
    private float viewShiftY;
    private float viewScale = 1;
    private float viewRotX;
    private float viewRotY;

    public VehicleModdingToolScreen(AbstractVehicle vehicle) {
        super(Component.literal("Vehicle Display Tool"));
        this.vehicle = vehicle;
        this.viewRotX = 180 - vehicle.getXRot();
        this.viewRotY = 180 + LocalVehiclePlayer.instance.getPlayer().getYRot();
        ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).ifPresent(display -> {
            List<BaseDisplay> vehicleDisplays = ClientAssetsManager.INSTANCE.getVariableDisplay(display.getModelPath());
            this.variableDisplayIds = vehicleDisplays.stream()
                    .map(BaseDisplay::getDisplayId)
                    .distinct()
                    .toList();
        });
    }

    @Override
    protected void init() {
        this.leftPos = 20;
        this.topPos = (this.height - (VISIBLE_ITEMS * ITEM_HEIGHT)) / 2;
        updateFilteredList("");
        this.searchBox = new EditBox(this.font, leftPos, topPos - ITEM_HEIGHT, LIST_WIDTH - 2, ITEM_HEIGHT - 3, Component.literal("Search"));
        this.searchBox.setResponder(this::updateFilteredList);
        this.searchBox.setBordered(true);
        this.searchBox.active = true;
        this.searchBox.setTextColor(0xFFFFFF);
        this.addRenderableWidget(searchBox);
        if (vehicle.getPartUnits().stream()
                .anyMatch(partUnit -> partUnit instanceof WeaponUnit weaponUnit && weaponUnit.isInteractive() && !weaponUnit.weapons.isEmpty())) {
            this.addRenderableWidget(Button.builder(Component.translatable("button.vehicle.weapon_selections"), button ->
                            Minecraft.getInstance().setScreen(new VehicleWeaponSelectScreen(vehicle, this)))
                    .bounds(leftPos + LIST_WIDTH + (vehicle instanceof FixedWingVehicle ? 155 : 10), topPos + 5 - 10, 100, ITEM_HEIGHT)
                    .build());
        }
        if (vehicle instanceof FixedWingVehicle) {
            AllConfigs.CommonConfig common = AllConfigs.common;
            int sliderX = leftPos + LIST_WIDTH + 10;
            int sliderY = topPos - 10;
            int sliderWidth = 105;
            int sliderHeight = 10;
            this.smokeRSlider = new ForgeSlider(sliderX, sliderY, sliderWidth, sliderHeight,
                    Component.literal("R "), Component.empty(),
                    0, 255, common.aerobaticSmokeR.get(), 1, 0, true);
            this.smokeRSlider.setFGColor(0xFF6666);
            this.addRenderableWidget(smokeRSlider);
            this.smokeGSlider = new ForgeSlider(sliderX, sliderY + 12, sliderWidth, sliderHeight,
                    Component.literal("G "), Component.empty(),
                    0, 255, common.aerobaticSmokeG.get(), 1, 0, true);
            this.smokeGSlider.setFGColor(0x66FF66);
            this.addRenderableWidget(smokeGSlider);
            this.smokeBSlider = new ForgeSlider(sliderX, sliderY + 24, sliderWidth, sliderHeight,
                    Component.literal("B "), Component.empty(),
                    0, 255, common.aerobaticSmokeB.get(), 1, 0, true);
            this.smokeBSlider.setFGColor(0x6666FF);
            this.addRenderableWidget(smokeBSlider);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        drawDisplayList(guiGraphics, mouseX, mouseY);
        drawVehiclePreview(guiGraphics);
        if (vehicle instanceof FixedWingVehicle) {
            guiGraphics.drawString(font, Component.translatable("ui.aerobatic_smoke"), leftPos + LIST_WIDTH + 10, topPos - 12 - 10, 0xFFAAAAAA);
            AllConfigs.CommonConfig common = AllConfigs.common;
            common.aerobaticSmokeR.set((int) smokeRSlider.getValue());
            common.aerobaticSmokeG.set((int) smokeGSlider.getValue());
            common.aerobaticSmokeB.set((int) smokeBSlider.getValue());
            int dotX = leftPos + LIST_WIDTH + 120;
            int dotY = topPos + 4 - 10;
            int dotSize = 24;
            int color = 0xFF000000
                    | (common.aerobaticSmokeR.get() << 16)
                    | (common.aerobaticSmokeG.get() << 8)
                    | common.aerobaticSmokeB.get();
            guiGraphics.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, color);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateFilteredList(String filter) {
        this.filteredVariableDisplayIds = variableDisplayIds.stream()
                .filter(resourceLocation -> resourceLocation.getPath().toLowerCase().contains(filter.toLowerCase()))
                .toList();
        this.scrollOffset = 0;
    }

    private void drawDisplayList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int listHeight = VISIBLE_ITEMS * ITEM_HEIGHT;
        guiGraphics.fill(x, y, x + LIST_WIDTH, y + listHeight, 0xFF252525);
        int end = Math.min(scrollOffset + VISIBLE_ITEMS, filteredVariableDisplayIds.size());
        for (int index = scrollOffset; index < end; index++) {
            int itemY = y + (index - scrollOffset) * ITEM_HEIGHT;
            boolean isHovered = mouseX >= x + 2 && mouseX <= x + LIST_WIDTH - 2 &&
                    mouseY >= itemY + 1 && mouseY <= itemY + ITEM_HEIGHT - 1;
            boolean isSelected = (index == selectedIndex);
            int bgColor = isSelected ? 0xFF3F6DB5 : (isHovered ? 0xFF4A4A4A : 0xFF303030);
            guiGraphics.fill(x + 2, itemY + 1, x + LIST_WIDTH - 6, itemY + ITEM_HEIGHT - 1, bgColor);
            String text = filteredVariableDisplayIds.get(index).getPath();
            guiGraphics.drawString(font, text, x + 8, itemY + 6, 0xFFFFFFFF);
        }
        drawScrollBar(guiGraphics, x + LIST_WIDTH - 4, y);
    }

    private void drawScrollBar(GuiGraphics guiGraphics, int x, int y) {
        int barWidth = 6;
        int barHeight = VISIBLE_ITEMS * ITEM_HEIGHT;
        int size = filteredVariableDisplayIds.size();

        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF3A3A3A);

        if (size <= VISIBLE_ITEMS) {
            return;
        }

        int knobHeight = Math.max(12,
                barHeight * VISIBLE_ITEMS / size);

        int maxScroll = size - VISIBLE_ITEMS;
        int movable = barHeight - knobHeight;
        int knobY = y + (int) (movable * (scrollOffset / (float) maxScroll));

        guiGraphics.fill(
                x,
                knobY,
                x + barWidth,
                knobY + knobHeight,
                0xFFAAAAAA
        );
    }

    private void drawVehiclePreview(GuiGraphics guiGraphics) {
        double length = vehicle.getStructureLength();
        float scale = (float) (1 / Math.max(length, 3) * 196) * this.viewScale;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate((double) width / 2 + 60 + viewShiftX, (double) height / 2 + viewShiftY, 512);
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
        Optional<BaseDisplay> vehicleDisplayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId());
        if (vehicleDisplayOptional.isPresent()) {
            BaseDisplay vehicleDisplay = vehicleDisplayOptional.get();
            // 介绍
            if (vehicleDisplay.getDescription() != null) {
                poseStack.pushPose();
                {
                    int x = leftPos + LIST_WIDTH + 10;
                    int maxWidth = width - x;
                    poseStack.translate(x, (double) height / 2 + 32, 0);
                    poseStack.scale(0.95f, 0.95f, 0.95f);
                    var lines = font.split(Component.literal(vehicleDisplay.getDescription()), maxWidth);
                    for (int i = 0; i < lines.size(); i++) {
                        guiGraphics.drawString(font, lines.get(i), 0, i * 9, 0xFFFFFFFF);
                    }
                }
                poseStack.popPose();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= leftPos && mouseX <= leftPos + LIST_WIDTH - 5) {
                if (mouseY >= topPos && mouseY <= topPos + (VISIBLE_ITEMS * ITEM_HEIGHT)) {
                    int clickedIndex = scrollOffset + ((int) (mouseY - topPos) / ITEM_HEIGHT);
                    if (clickedIndex >= 0 && clickedIndex < filteredVariableDisplayIds.size()) {
                        this.selectedIndex = clickedIndex;
                        onDisplaySelected(filteredVariableDisplayIds.get(clickedIndex));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX > leftPos + LIST_WIDTH) {
            if (delta > 0) {
                this.viewScale *= 1.1f;
            } else if (delta < 0) {
                this.viewScale *= 0.9f;
            }
            this.viewScale = Math.max(0.1f, Math.min(this.viewScale, 10.0f));
            return true;
        }
        if (filteredVariableDisplayIds.size() > VISIBLE_ITEMS) {
            scrollOffset = (int) Mth.clamp(scrollOffset - Math.signum(delta), 0, filteredVariableDisplayIds.size() - VISIBLE_ITEMS);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean isHandled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (!isHandled) {
            if (button == 0) {
                this.viewRotY += (float) dragX;
                this.viewRotX += (float) dragY;
                return true;
            } else if (button == 1) {
                this.viewShiftX += (float) dragX;
                this.viewShiftY += (float) dragY;
                return true;
            }
        }
        return isHandled;
    }

    private void onDisplaySelected(ResourceLocation displayId) {
        Channel.CHANNEL.sendToServer(new ClientVehicleChangeDisplay(vehicle.getId(), displayId));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean isPauseScreen() { return false; }

}
