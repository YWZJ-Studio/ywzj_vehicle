package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.NoneVehicle;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.util.VehicleHudText;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.RotatableUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.*;

public final class VehicleStatusOverlay implements IGuiOverlay {

    private static final int WHITE = 0xFFF2F2F2;
    private static final int GRAY = 0xFFB8B8B8;
    private static final int FUEL_YELLOW = 0xFFFFD54A;
    private static final int MARGIN = 18;
    private static final int BOTTOM_MARGIN = 12;
    private static final int GAP = 8;
    private static final int ROW_HEIGHT = 14;
    private static final int CREW_ROWS_PER_COLUMN = 8;
    private static final int HEALTH_BAR_HEIGHT = 6;
    private static final int FUEL_BAR_HEIGHT = 4;
    private static final int BAR_GAP = 3;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
        if (!localVehiclePlayer.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = localVehiclePlayer.vehicle;
        try (VehicleHudLayout layout = VehicleHudLayout.begin(graphics)) {
            float width = Math.min(252, Math.max(140, layout.width() / 2 - MARGIN * 2));
            float size = Math.min(112, width * 0.52f);
            float left = MARGIN;
            float bottom = VehicleHudLayout.HEIGHT - BOTTOM_MARGIN - 12;
            float top = bottom - size;
            float crewLeft = left + size + GAP;
            float crewWidth = width - size - GAP;
            int nameColor = WHITE;
            if (vehicle.getTeam() != null && vehicle.getTeam().getColor().getColor() != null) {
                nameColor = 0xFF000000 | vehicle.getTeam().getColor().getColor();
            }
            VehicleHudText.drawString(graphics,
                    VehicleHudText.truncate(vehicle.getDisplayName().getString(), (int) width),
                    left, top - ROW_HEIGHT, nameColor);

            float stroke = VehicleHudLayout.HEIGHT / Math.max(1, Minecraft.getInstance().getWindow().getHeight());
            fill(graphics, left, top, left + size, bottom, 0x80404040);
            fill(graphics, left, top, left + size, top + stroke, WHITE);
            fill(graphics, left, bottom - stroke, left + size, bottom, WHITE);
            fill(graphics, left, top, left + stroke, bottom, WHITE);
            fill(graphics, left + size - stroke, top, left + size, bottom, WHITE);
            boolean showStatus = !(vehicle instanceof NoneVehicle);
            renderVehicleHeading(graphics, partialTick, vehicle,
                    left + 4, top + 4, size - 8, size - 8, stroke);
            int groundSpeed = (int) (vehicle.getDeltaMovement().horizontalDistance() * 72); // 格/刻转千米/小时：20 × 3.6
            String speedText = groundSpeed + " km/h";
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(left + size - 2, top + 3, 0);
                graphics.pose().scale(0.65f, 0.65f, 1);
                VehicleHudText.drawString(graphics, speedText, -VehicleHudText.width(speedText), 0, WHITE);
            } finally {
                graphics.pose().popPose();
            }
            renderCrew(graphics, vehicle, crewLeft, top, crewWidth, size);
            if (showStatus) {
                renderHealth(graphics, vehicle, left, bottom + 4, size);
                renderFuel(graphics, vehicle, left, bottom + 4 + HEALTH_BAR_HEIGHT + BAR_GAP, size);
            }
        }
    }

    private static void renderVehicleHeading(GuiGraphics guiGraphics, float partialTick, AbstractVehicle vehicle,
                                            float left, float top, float width, float height, float stroke) {
        List<HeadingCube> cubes = new ArrayList<>();
        Map<VehicleCubeGroup, Quaternionf> rotations = new HashMap<>();
        for (PartUnit<?> part : vehicle.getPartUnits()) {
            if (part instanceof RotatableUnit<?> rotatableUnit && part.getStructureGroup() != null) {
                VehicleCubeGroup group = part.getStructureGroup();
                rotations.put(group, new Quaternionf(group.baseRotation)
                        .mul(Axis.YN.rotationDegrees(rotatableUnit.getViewYRot(partialTick))));
            }
            if (part instanceof WeaponUnit weapon && weapon.xTurnGroup != null
                    && weapon.xTurnGroup != weapon.getStructureGroup()) {
                VehicleCubeGroup group = weapon.xTurnGroup;
                rotations.put(group, new Quaternionf(group.baseRotation));
            }
        }
        Player player = LocalVehiclePlayer.instance.getPlayer();
        PartUnit<?> playerPartUnit = vehicle.getOwnOperatorUnit(player);
        float zRot = playerPartUnit instanceof RotatableUnit<?> && playerPartUnit.getStructureGroup() != null
                ? headingYRot(headingTransform(playerPartUnit.getStructureGroup(), Vec3.ZERO, rotations, partialTick).rotation()) : 0;
        float heading = (float) Math.toRadians(180 - zRot);
        for (VehicleCubeOBB cube : vehicle.getVehicleCubeOBBs()) {
            addHeadingCube(cubes, cube, rotations, heading, partialTick, 0);
        }
        for (PartUnit<?> part : vehicle.getPartUnits()) {
            if (part instanceof WeaponUnit weapon && !weapon.isSeat()) {
                continue;
            }
            if (!part.renderInVehicleStatus) {
                continue;
            }
            int fillColor = damageFillColor(part.getHealth(), part.getMaxHealth());
            for (VehicleCubeOBB cube : part.getPartCubeOBBs()) {
                addHeadingCube(cubes, cube, rotations, heading, partialTick, fillColor);
            }
            if (part instanceof WeaponUnit weapon) {
                for (WeaponUnit subWeapon : weapon.getSubWeaponUnits()) {
                    int subFillColor = damageFillColor(subWeapon.getHealth(), subWeapon.getMaxHealth());
                    for (VehicleCubeOBB cube : subWeapon.getPartCubeOBBs()) {
                        addHeadingCube(cubes, cube, rotations, heading, partialTick, subFillColor);
                    }
                }
            }
        }
        if (cubes.isEmpty()) {
            return;
        }
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        // 按实际绘制的旋转矩形计算缩放边界，包含偏置炮塔和子武器。
        for (HeadingCube cube : cubes) {
            float cos = Math.abs(Mth.cos(cube.rotation));
            float sin = Math.abs(Mth.sin(cube.rotation));
            float extentX = cos * cube.halfWidth + sin * cube.halfDepth;
            float extentY = sin * cube.halfWidth + cos * cube.halfDepth;
            minX = Math.min(minX, cube.x - extentX);
            maxX = Math.max(maxX, cube.x + extentX);
            minY = Math.min(minY, cube.y - extentY);
            maxY = Math.max(maxY, cube.y + extentY);
        }
        float scale = Math.min(width / Math.max(0.001f, maxX - minX),
                height / Math.max(0.001f, maxY - minY));
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(left + width / 2, top + height / 2, 0);
            poseStack.scale(scale, scale, 1);
            poseStack.translate(-(minX + maxX) / 2, -(minY + maxY) / 2, 0);
            // 先填充所有受损结构，再绘制轮廓，避免重叠结构的填色遮住边线。
            for (HeadingCube cube : cubes) {
                renderHeadingCube(guiGraphics, cube, stroke / scale, true);
            }
            for (HeadingCube cube : cubes) {
                renderHeadingCube(guiGraphics, cube, stroke / scale, false);
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static void renderHeadingCube(GuiGraphics graphics, HeadingCube cube, float stroke, boolean fill) {
        if (fill && cube.fillColor == 0) {
            return;
        }
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(cube.x, cube.y, 0);
            poseStack.mulPose(Axis.ZP.rotation(cube.rotation));
            float hw = cube.halfWidth;
            float hd = cube.halfDepth;
            if (fill) {
                RenderHelper.fill(graphics, RenderType.gui(), -hw, -hd, hw, hd, 0, cube.fillColor);
                return;
            }
            float horizontal = Math.min(stroke, hd * 2);
            float vertical = Math.min(stroke, hw * 2);
            RenderHelper.fill(graphics, RenderType.gui(), -hw, -hd, hw, -hd + horizontal, 0, cube.color);
            RenderHelper.fill(graphics, RenderType.gui(), -hw, hd - horizontal, hw, hd, 0, cube.color);
            RenderHelper.fill(graphics, RenderType.gui(), -hw, -hd, -hw + vertical, hd, 0, cube.color);
            RenderHelper.fill(graphics, RenderType.gui(), hw - vertical, -hd, hw, hd, 0, cube.color);
        } finally {
            poseStack.popPose();
        }
    }

    private record HeadingCube(float x, float y, float halfWidth, float halfDepth, float rotation, int color, int fillColor) {}

    private static void addHeadingCube(List<HeadingCube> cubes, VehicleCubeOBB cube,
                                       Map<VehicleCubeGroup, Quaternionf> rotations, float heading,
                                       float partialTick, int fillColor) {
        if (cube.group == null) {
            return;
        }
        VehicleCubeGroup.GlobalTransform transform = headingTransform(cube.group,
                new Vec3(cube.x + cube.width / 2, cube.y + cube.height / 2, cube.z + cube.depth / 2),
                rotations, partialTick);
        float x = (float) transform.offset().x;
        float y = (float) transform.offset().z;
        float cos = Mth.cos(heading);
        float sin = Mth.sin(heading);
        int borderColor = fillColor == 0 ? Color.WHITE : fillColor | 0xFF000000;
        cubes.add(new HeadingCube(x * cos - y * sin, x * sin + y * cos,
                (float) cube.width / 2, (float) cube.depth / 2,
                heading + (float) Math.toRadians(headingYRot(transform.rotation())), borderColor, fillColor));
    }

    private static VehicleCubeGroup.GlobalTransform headingTransform(VehicleCubeGroup group, Vec3 offset,
                                                                     Map<VehicleCubeGroup, Quaternionf> rotations,
                                                                     float partialTick) {
        return group.globalTransform(offset, true,
                current -> rotations.getOrDefault(current, current.rotation),
                current -> current.offsetO.lerp(current.offset, partialTick));
    }

    private static float headingYRot(Quaternionf rotation) {
        Vector3f forward = rotation.transform(new Vector3f(0, 0, 1));
        return VectorUtil.vecToRot(new Vec3(forward)).y;
    }

    private static int damageFillColor(float health, float maxHealth) {
        if (maxHealth <= 0 || health < 0 || health >= maxHealth) {
            return 0;
        }
        int green = Math.round(255 * Mth.clamp(health / maxHealth, 0, 1));
        return 0x80FF0000 | green << 8;
    }

    private static void renderCrew(GuiGraphics graphics, AbstractVehicle vehicle,
                                   float left, float top, float width, float height) {
        int rows = Math.min(CREW_ROWS_PER_COLUMN, Math.max(1, vehicle.seats.size()));
        int columns = Math.max(1, (vehicle.seats.size() + CREW_ROWS_PER_COLUMN - 1) / CREW_ROWS_PER_COLUMN);
        float scale = Math.min(1, height / (rows * ROW_HEIGHT));
        float columnWidth = width / columns;
        for (int index = 0; index < vehicle.seats.size(); index++) {
            Integer passengerId = vehicle.seats.get(index).passengerId;
            Entity passenger = passengerId == null ? null : vehicle.level().getEntity(passengerId);
            String name = passenger == null ? "[]" : "[" + passenger.getDisplayName().getString() + "]";
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(left + index / rows * columnWidth,
                        top + index % rows * ROW_HEIGHT * scale, 0);
                graphics.pose().scale(scale, scale, 1);
                VehicleHudText.drawString(graphics,
                        VehicleHudText.truncate((index + 1) + ". " + name, (int) ((columnWidth - 4) / scale)),
                        0, 2, passenger == null ? GRAY : WHITE);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    private static void renderHealth(GuiGraphics graphics, AbstractVehicle vehicle, float left, float top, float width) {
        float maximum = vehicle.getMaxHealth();
        float percent = maximum > 0 ? Mth.clamp(vehicle.getHealth() / maximum, 0, 1) : 0;
        float previous = maximum > 0 ? Mth.clamp(vehicle.healthO / maximum, 0, 1) : percent;
        float lastPercent = Mth.lerp(Mth.clamp(vehicle.hurtTick / 10f, 0, 1), percent, previous);
        int red = vehicle.isDestroyed() || percent <= 0.5f ? 255 : (int) (255 * (2 - percent * 2));
        int green = vehicle.isDestroyed() ? 64 : percent > 0.5f ? 255 : (int) (255 * percent * 2);
        int color = 0xFF000000 | red << 16 | green << 8;
        String health = String.format(Locale.ROOT, "%.0f/%.0f", vehicle.getHealth(), maximum);
        float difference = vehicle.healthO - vehicle.getHealth();
        String delta = difference > 0 ? String.format(Locale.ROOT, "-%.2f", difference)
                : difference < 0 && !vehicle.isDestroyed() ? String.format(Locale.ROOT, "+%.2f", -difference) : "";
        renderStatusBar(graphics, left, top, width, HEALTH_BAR_HEIGHT, percent, lastPercent, color, health);
        VehicleHudText.drawString(graphics, delta, left + width + GAP, top,
                difference > 0 ? Color.RED : Color.GREEN, false);
    }

    private static void renderFuel(GuiGraphics graphics, AbstractVehicle vehicle, float left, float top, float width) {
        float capacity = Math.max(0, vehicle.energyInfo.energyCapacity);
        float fuel = Math.max(0, vehicle.getEnergy());
        float percent = capacity > 0 ? Mth.clamp(fuel / capacity, 0, 1) : 0;
        String text = String.format(Locale.ROOT, "%.0f/%.0f", fuel, capacity);
        renderStatusBar(graphics, left, top, width, FUEL_BAR_HEIGHT, percent, percent, FUEL_YELLOW, text);
    }

    private static void renderStatusBar(GuiGraphics graphics, float left, float top, float width, float height,
                                        float percent, float lastPercent, int color, String text) {
        float stroke = VehicleHudLayout.HEIGHT / Math.max(1, Minecraft.getInstance().getWindow().getHeight());
        fill(graphics, left, top, left + width, top + height, Color.BG_DARK);
        fill(graphics, left - stroke, top, left, top + height, Color.GRAY);
        fill(graphics, left + width, top, left + width + stroke, top + height, Color.GRAY);
        fill(graphics, left, top - stroke, left + width, top, Color.GRAY);
        fill(graphics, left, top + height, left + width, top + height + stroke, Color.GRAY);
        fill(graphics, left, top, left + width * percent, top + height, color);
        if (lastPercent != percent) {
            fill(graphics, left + width * Math.min(percent, lastPercent), top,
                    left + width * Math.max(percent, lastPercent), top + height, Color.WHITE);
        }
        float scale = Math.min(height / 8, (width - 4) / Math.max(1, VehicleHudText.width(text)));
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left + width / 2, top + (height - 7 * scale) / 2, 0);
            graphics.pose().scale(scale, scale, 1);
            RenderHelper.drawCenteredString(graphics, Minecraft.getInstance().font, text, 0, 0, Color.WHITE);
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void fill(GuiGraphics graphics, float left, float top, float right, float bottom, int color) {
        RenderHelper.fill(graphics, RenderType.gui(), left, top, right, bottom, 0, color);
    }

}
