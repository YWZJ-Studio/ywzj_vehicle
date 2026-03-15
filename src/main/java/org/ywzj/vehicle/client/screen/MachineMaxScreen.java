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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientMachineMaxAction;
import org.ywzj.vehicle.recipe.VehiclePrintingIngredient;
import org.ywzj.vehicle.recipe.VehiclePrintingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MachineMaxScreen extends Screen {

    private static final int ITEM_HEIGHT = 18;
    private static final int ITEM_WIDTH = 130;
    private static final int VISIBLE_ITEMS = 10;
    private int imageWidth;
    private int imageHeight;
    private int leftPos;
    private int topPos;
    private int scrollOffset = 0;
    private static int selectedIndex = -1;
    public int tickCount;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private Button printingButton;
    private EditBox searchBox;
    private final List<Map.Entry<ResourceLocation, BaseVehicleData>> vehicleList =
            new ArrayList<>(CommonAssetsManager.vehicleDataManager().getVehicleData().entrySet());
    private List<Map.Entry<ResourceLocation, BaseVehicleData>> filteredVehicleList = new ArrayList<>();
    private final MachineMaxBlockEntity machineMaxBlockEntity;
    private AbstractVehicle vehicle;

    public MachineMaxScreen(MachineMaxBlockEntity machineMaxBlockEntity) {
        super(Component.literal("Machine Max"));
        this.machineMaxBlockEntity = machineMaxBlockEntity;
    }

    @Override
    protected void init() {
        this.imageWidth = width - 10;
        this.imageHeight = height - 10;
        this.leftPos = (this.width - imageWidth) / 2;
        this.topPos = (this.height - imageHeight) / 2;
        updateFilteredList("");
        int x = leftPos + 12;
        int y = topPos + 11;
        this.searchBox = new EditBox(this.font, x, y, ITEM_WIDTH - 2, ITEM_HEIGHT - 3, Component.literal("Search"));
        this.searchBox.setResponder(this::updateFilteredList);
        this.searchBox.setBordered(true);
        this.searchBox.active = true;
        this.searchBox.setTextColor(0xFFFFFF);
        this.addRenderableWidget(searchBox);
        this.printingButton = Button.builder(Component.translatable("button.machine_max.printing"), button -> onCraft())
                .pos(width - 60, topPos + 145)
                .size(50, 20)
                .build();
        this.addRenderableWidget(printingButton);
        if (selectedIndex >= 0 && selectedIndex < filteredVehicleList.size()) {
            onVehicleSelected(filteredVehicleList.get(selectedIndex).getKey());
        }
    }

    @Override
    public void tick() {
        if (machineMaxBlockEntity.isCrafting() || machineMaxBlockEntity.hasProduct()) {
            printingButton.active = false;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        drawMainBackground(guiGraphics);
        drawVehicleList(guiGraphics, mouseX, mouseY);
        drawPreviewPanel(guiGraphics);
        drawProgressBar(guiGraphics);
        drawReceipt(guiGraphics, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!this.hoveredStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.hoveredStack, mouseX, mouseY);
        }
    }

    private void drawMainBackground(GuiGraphics guiGraphics) {
        // 整体背景
        guiGraphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                0xFF1C1C1C
        );
        // 预览框背景
        guiGraphics.fill(
                leftPos + ITEM_WIDTH + 25,
                topPos + 10,
                leftPos + imageWidth - 10,
                topPos + 130,
                0xFF2A2A2A
        );
    }

    private void updateFilteredList(String filter) {
        this.filteredVehicleList = vehicleList.stream()
                .filter(entry -> entry.getValue().getName().getString().toLowerCase().contains(filter.toLowerCase()))
                .toList();
        this.scrollOffset = 0;
    }

    private void drawVehicleList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = leftPos + 10;
        int y = topPos + 10 + ITEM_HEIGHT;

        guiGraphics.fill(x, y, x + ITEM_WIDTH, y + 180, 0xFF252525);

        int start = scrollOffset;
        int end = Math.min(start + VISIBLE_ITEMS, filteredVehicleList.size());

        for (int i = start; i < end; i++) {
            int itemY = y + (i - start) * ITEM_HEIGHT;

            boolean hovered =
                    mouseX >= x + 2 && mouseX <= x + 140 &&
                            mouseY >= itemY + 2 && mouseY <= itemY + ITEM_HEIGHT - 2;

            boolean selected = i == selectedIndex;

            int bgColor;
            if (selected) {
                bgColor = 0xFF3F6DB5;
            } else if (hovered) {
                bgColor = 0xFF3A3A3A;
            } else {
                bgColor = 0xFF303030;
            }

            guiGraphics.fill(
                    x + 2,
                    itemY + 2,
                    x + ITEM_WIDTH,
                    itemY + ITEM_HEIGHT - 2,
                    bgColor
            );

            guiGraphics.drawString(
                    font,
                    filteredVehicleList.get(i).getValue().getName(),
                    x + 6,
                    itemY + 5,
                    0xFFFFFF
            );
        }

        drawScrollBar(guiGraphics, x + 90, y);
    }

    private void drawScrollBar(GuiGraphics guiGraphics, int x, int y) {
        int barX = x + 44;
        int barY = y;
        int barWidth = 6;
        int barHeight = VISIBLE_ITEMS * ITEM_HEIGHT;

        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF3A3A3A);

        if (filteredVehicleList.size() <= VISIBLE_ITEMS) {
            return;
        }

        int knobHeight = Math.max(12,
                barHeight * VISIBLE_ITEMS / filteredVehicleList.size());

        int maxScroll = filteredVehicleList.size() - VISIBLE_ITEMS;
        int movable = barHeight - knobHeight;
        int knobY = barY + (int) (movable * (scrollOffset / (float) maxScroll));

        guiGraphics.fill(
                barX,
                knobY,
                barX + barWidth,
                knobY + knobHeight,
                0xFFAAAAAA
        );
    }

    private void drawPreviewPanel(GuiGraphics guiGraphics) {
        int x = leftPos + ITEM_WIDTH + 30;
        int y = topPos + 15;

        guiGraphics.fill(x, y, x + 110, y + 90, 0xFF111111);

        if (selectedIndex < 0 || selectedIndex >= filteredVehicleList.size()) {
            return;
        }

        if (vehicle == null) {
            return;
        }

        double length = vehicle.getStructureLength();
        float scale = (float) (1 / Math.max(length, 3) * 100);

        // 模型预览
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(x + 55, y + 60, 512);
            float rotation = (System.currentTimeMillis() % 10000) / 10000f * 360f;
            poseStack.mulPose(Axis.XP.rotationDegrees(165));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            poseStack.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            Lighting.setupForEntityInInventory();
            RenderSystem.runAsFancy(() ->
                    dispatcher.render(
                            vehicle,
                            0, 0, 0,
                            vehicle.getYRot(),
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
            poseStack.pushPose();
            {
                int maxWidth = (int) ((imageWidth - ITEM_WIDTH - 35) / 1.06f);
                poseStack.translate(x + 116, topPos + 20, 0);
                poseStack.scale(0.95f, 0.95f, 0.95f);
                if (vehicleDisplay.getDescription() != null) {
                    var lines = font.split(Component.literal(vehicleDisplay.getDescription()), maxWidth);
                    for (int i = 0; i < lines.size(); i++) {
                        guiGraphics.drawString(font, lines.get(i), 0, i * 9, 0xFFFFFFFF);
                    }
                } else {
                    guiGraphics.drawString(font, Component.translatable("screen.no_description"), 0, 0, 0xFFFFFFFF);
                }
            }
            poseStack.popPose();
        }
        if (machineMaxBlockEntity.hasProduct()) {
            guiGraphics.drawCenteredString(font, Component.translatable("tips.machine_max_product"), x + 55, topPos + 111, 0xFFFFFFFF);
        }
    }

    private void drawProgressBar(GuiGraphics guiGraphics) {
        int x = leftPos + ITEM_WIDTH + 30;
        int y = topPos + 110;

        guiGraphics.fill(x, y, x + 110, y + 10, 0xFF3A3A3A);
        guiGraphics.fill(x, y, x + (int) (110 * machineMaxBlockEntity.progress), y + 10, 0xFF4CAF50);
    }

    private void drawReceipt(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = leftPos + ITEM_WIDTH + 30;
        int y = topPos + 140;
        this.hoveredStack = ItemStack.EMPTY;
        if (selectedIndex < 0 || selectedIndex >= filteredVehicleList.size()) {
            return;
        }
        ResourceLocation vehicleId = filteredVehicleList.get(selectedIndex).getKey();
        Optional<? extends Recipe<?>> recipeOptional = Minecraft.getInstance().level.getRecipeManager().byKey(vehicleId);
        if (!recipeOptional.isPresent()) {
            guiGraphics.drawString(font, Component.translatable("tips.no_recipe"), x, y, 0xFFFFFF);
            return;
        }
        recipeOptional.ifPresent(recipe -> {
            if (recipe instanceof VehiclePrintingRecipe vehicleRecipe) {
                List<VehiclePrintingIngredient> inputs = vehicleRecipe.getInputs();
                for (int index = 0; index < inputs.size(); index++) {
                    int column = index / 5;
                    int row = index % 5;
                    int currentX = x + (column * 72);
                    int currentY = y + (row * 16);
                    VehiclePrintingIngredient input = inputs.get(index);
                    if (input.ingredient().getItems().length > 0) {
                        ItemStack stack = input.ingredient().getItems()[0];
                        int count = input.count();
                        guiGraphics.renderFakeItem(stack, currentX, currentY);
                        String text = stack.getHoverName().getString() + " * " + count;
                        guiGraphics.drawString(font, text, currentX + 20, currentY + 4, 0xFFFFFF);
                        if (mouseX >= currentX && mouseX <= currentX + 16 &&
                                mouseY >= currentY && mouseY <= currentY + 16) {
                            this.hoveredStack = stack;
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = leftPos + 10;
            int y = topPos + 10 + ITEM_HEIGHT;
            if (mouseX >= x && mouseX <= x + 140 && mouseY >= y && mouseY <= y + VISIBLE_ITEMS * ITEM_HEIGHT) {
                int index = scrollOffset + ((int) mouseY - y) / ITEM_HEIGHT;
                if (index >= 0 && index < filteredVehicleList.size()) {
                    selectedIndex = index;
                    onVehicleSelected(filteredVehicleList.get(index).getKey());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (filteredVehicleList.size() > VISIBLE_ITEMS) {
            scrollOffset = (int) Mth.clamp(scrollOffset - Math.signum(delta),
                    0,
                    filteredVehicleList.size() - VISIBLE_ITEMS);
        }
        return true;
    }

    private void onVehicleSelected(ResourceLocation vehicleId) {
        if (machineMaxBlockEntity.craftingVehicleId != null) {
            vehicleId = machineMaxBlockEntity.craftingVehicleId;
        }
        Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(vehicleId);
        if (!vehicleDataOptional.isPresent()) {
            return;
        }
        vehicle = vehicleDataOptional.get().construct(Minecraft.getInstance().level, Vec3.ZERO, 0, 0);
        vehicle.initData();
        vehicle.initDisplayData();
        if (machineMaxBlockEntity.isCrafting() || machineMaxBlockEntity.hasProduct()) {
            printingButton.active = false;
        }
        Optional<? extends Recipe<?>> recipeOptional = Minecraft.getInstance().level.getRecipeManager().byKey(vehicleId);
        if (!recipeOptional.isPresent()) {
            printingButton.visible = false;
            return;
        }
        printingButton.visible = true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void onCraft() {
        ClientMachineMaxAction clientMachineMaxAction = new ClientMachineMaxAction();
        if (selectedIndex >= 0 && selectedIndex < filteredVehicleList.size()) {
            clientMachineMaxAction.craftingVehicleId = filteredVehicleList.get(selectedIndex).getKey();
            if (clientMachineMaxAction.craftingVehicleId == null) {
                return;
            }
            clientMachineMaxAction.blockPos = machineMaxBlockEntity.getBlockPos();
            clientMachineMaxAction.action = ClientMachineMaxAction.Action.CRAFT;
            Channel.CHANNEL.sendToServer(clientMachineMaxAction);
            machineMaxBlockEntity.bedrockBoneWrappers.clear();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
