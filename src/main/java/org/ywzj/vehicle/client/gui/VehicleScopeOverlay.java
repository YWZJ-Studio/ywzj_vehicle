package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.render.util.GuiHelper;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.util.VehicleHudText;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.weapon.VehicleCannon;
import org.ywzj.vehicle.vehicle.weapon.VehicleMissile;
import org.ywzj.vehicle.vehicle.weapon.VehicleMultiWeapons;

import java.util.Locale;

public class VehicleScopeOverlay implements IGuiOverlay {

    private static final int ARTILLERY_VALUE_COLOR = 0xFFF2F2F2;
    private static final int ARTILLERY_LABEL_COLOR = 0xFFB8B8B8;
    private static final int ARTILLERY_ROW_HEIGHT = 14;
    private static final int ARTILLERY_COLUMN_GAP = 10;
    private static final float[] CRT_RETICLE_SEGMENTS = {
            -90, 0, -50, 0, 50, 0, 90, 0,
            0, -90, 0, -50, 0, 50, 0, 90,
            -24, 0, -6, 0, 6, 0, 24, 0,
            0, -24, 0, -6, 0, 6, 0, 24,
            -6, -1.5f, -6, 1.5f, 6, -1.5f, 6, 1.5f,
            -1.5f, -6, 1.5f, -6, -1.5f, 6, 1.5f, 6
    };
    private static final float[] CRT_FOCUS_LOCK_SEGMENTS = {
            -27, -18, -16, -10, 16, -10, 27, -18,
            -27, 18, -16, 10, 16, 10, 27, 18
    };

    public static double fov;
    public static float magnification = 1f;
    public static int color = Color.GREEN;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!LocalVehiclePlayer.instance.onVehicle() || LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.vehicle;
        renderCrosshair(guiGraphics, partialTick, vehicle);
        renderArtilleryReadout(guiGraphics, vehicle);
        renderWeaponEngagementEnvelope(guiGraphics, partialTick, screenWidth, screenHeight, vehicle);
    }

    public void renderCrosshair(GuiGraphics guiGraphics, float partialTick, AbstractVehicle vehicle) {
        if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
            var type = weaponUnit.getOpticalSightType();
            if (type == WeaponUnitData.OpticalSightType.OPTICAL_SCOPE || type == WeaponUnitData.OpticalSightType.CUSTOM) {
                var sight = type == WeaponUnitData.OpticalSightType.CUSTOM
                        ? ClientAssetsManager.INSTANCE.getSightDisplay(weaponUnit.getData().getOpticalSightDisplay()).orElse(null)
                        : null;
                if (sight != null) {
                    GuiHelper.drawOpticalScope(guiGraphics, guiGraphics.guiWidth(), guiGraphics.guiHeight(), sight.getTexture(), sight.getScale() * magnification);
                } else {
                    GuiHelper.drawOpticalScope(guiGraphics, guiGraphics.guiWidth(), guiGraphics.guiHeight(), null, 1);
                }
                return;
            }
            Vec3 posO = VectorUtil.worldToScreen(weaponUnit.weaponHitPosO);
            Vec3 pos = VectorUtil.worldToScreen(weaponUnit.weaponHitPos);
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            {
                poseStack.translate(
                        Mth.lerp(partialTick, posO.x, pos.x),
                        Mth.lerp(partialTick, posO.y, pos.y),
                        0);
                if (weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.CRT) {
                    boolean stabilizerOn = weaponUnit.withStabilizer() && !weaponUnit.isDestroyed()
                            && (!weaponUnit.needPower || vehicle.hasPower());
                    boolean focusLocked = weaponUnit.withFocusLocker() && weaponUnit.getFocusLockPos() != null;
                    renderCrtReticle(guiGraphics, focusLocked);
                    float sightScale = guiGraphics.guiHeight() / VehicleHudLayout.HEIGHT;
                    renderSightText(guiGraphics, (LocalVehiclePlayer.instance.outOfRangeFinding ? ">" : "") + (int) LocalVehiclePlayer.instance.aimLocationDistance + " m", 0, 33 * sightScale, true);
                    renderSightText(guiGraphics, "x" + String.format(Locale.ROOT, "%.1f", weaponUnit.getZoom()), 44 * sightScale, 14 * sightScale, false);
                    renderSightText(guiGraphics, stabilizerOn ? Component.translatable("ui.stabilizer_on").getString() : "", 44 * sightScale, 28 * sightScale, false);
                    if (focusLocked) {
                        renderSightText(guiGraphics, Component.translatable("ui.focus_lock").getString(), 44 * sightScale, 42 * sightScale, false);
                    }
                }
            }
            poseStack.popPose();
        }
    }

    private static void renderCrtReticle(GuiGraphics guiGraphics, boolean focusLocked) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        try {
            float scale = guiGraphics.guiHeight() / VehicleHudLayout.HEIGHT;
            float halfWidth = Math.max(0.5f, 0.75f / (scale * (float) Minecraft.getInstance().getWindow().getGuiScale()));
            poseStack.scale(scale, scale, 1);
            renderCrtSegments(guiGraphics, CRT_RETICLE_SEGMENTS, halfWidth);
            if (focusLocked) {
                renderCrtSegments(guiGraphics, CRT_FOCUS_LOCK_SEGMENTS, halfWidth);
            }
            guiGraphics.flush();
        } finally {
            poseStack.popPose();
        }
    }

    private static void renderCrtSegments(GuiGraphics guiGraphics, float[] segments, float halfWidth) {
        PoseStack poseStack = guiGraphics.pose();
        for (int i = 0; i < segments.length; i += 4) {
            float x = segments[i];
            float y = segments[i + 1];
            float dx = segments[i + 2] - x;
            float dy = segments[i + 3] - y;
            float length = (float) Math.hypot(dx, dy);
            poseStack.pushPose();
            try {
                poseStack.translate(x, y, 0);
                poseStack.mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), 0, -halfWidth, length, halfWidth, 0, color);
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static void renderSightText(GuiGraphics guiGraphics, String text, float x, float y, boolean centered) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(x, y, 0);
            float scale = guiGraphics.guiHeight() / VehicleHudLayout.HEIGHT * 0.9f;
            poseStack.scale(scale, scale, 1);
            if (centered) {
                VehicleHudText.drawCenteredString(guiGraphics, text, 0, 0, color);
            } else {
                VehicleHudText.drawString(guiGraphics, text, 0, 0, color);
            }
        } finally {
            poseStack.popPose();
        }
    }

    private void renderArtilleryReadout(GuiGraphics guiGraphics, AbstractVehicle vehicle) {
        if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
            weaponUnit.getCurrentWeapon().ifPresent(weapon -> {
                var selectedWeapon = weapon instanceof VehicleMultiWeapons multiWeapons ? multiWeapons.getSelectedWeapon() : weapon;
                if (selectedWeapon instanceof VehicleCannon cannon && cannon.getData().isArtillery()) {
                    WeaponUnit aimingWeaponUnit = weapon.getWeaponUnit();
                    if (aimingWeaponUnit.isParentWeaponUnitAim()) {
                        aimingWeaponUnit = aimingWeaponUnit.getRootParentWeaponUnit();
                    }
                    AimContext aimContext = aimingWeaponUnit.aimContext();
                    float azimuth = Mth.wrapDegrees(aimContext.direction.y);
                    double range = weaponUnit.weaponHitRange;
                    try (VehicleHudLayout layout = VehicleHudLayout.begin(guiGraphics)) {
                        float left = Math.max(18, layout.width() / 2 - 180);
                        float top = VehicleHudLayout.HEIGHT / 2 - 108;
                        Component azimuthLabel = Component.translatable("ui.artillery.azimuth");
                        Component elevationLabel = Component.translatable("ui.artillery.elevation");
                        Component rangeLabel = Component.translatable("ui.artillery.range");
                        var font = Minecraft.getInstance().font;
                        float valueX = left + Math.max(font.width(azimuthLabel),
                                Math.max(font.width(elevationLabel), font.width(rangeLabel))) + ARTILLERY_COLUMN_GAP;
                        renderArtilleryRow(guiGraphics, azimuthLabel,
                                String.format(Locale.ROOT, "%.1f°", azimuth), left, valueX, top);
                        renderArtilleryRow(guiGraphics, elevationLabel,
                                String.format(Locale.ROOT, "%.1f°", -aimContext.direction.x),
                                left, valueX, top + ARTILLERY_ROW_HEIGHT);
                        renderArtilleryRow(guiGraphics, rangeLabel,
                                Double.isFinite(range) ? String.format(Locale.ROOT, "%.0f m", range) : "-- m",
                                left, valueX, top + ARTILLERY_ROW_HEIGHT * 2);
                    }
                }
            });
        }
    }

    private static void renderArtilleryRow(GuiGraphics graphics, Component label, String value,
                                           float left, float valueX, float top) {
        VehicleHudText.drawString(graphics, label, left, top, ARTILLERY_LABEL_COLOR);
        VehicleHudText.drawString(graphics, value, valueX, top, ARTILLERY_VALUE_COLOR);
    }

    public void renderWeaponEngagementEnvelope(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(centerX, centerY, 0);
            if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit
                    && weaponUnit.getOpticalSightType() == WeaponUnitData.OpticalSightType.CRT) {
                weaponUnit.getCurrentWeapon().ifPresent(vehicleWeapon -> {
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                    int baseX = 0;
                    int baseY = 140;
                    float yRotRange = weaponUnit.getYRotMax() - weaponUnit.getYRotMin();
                    if (yRotRange <= 0 || yRotRange >= 360 || weaponUnit.yRotSpeed == 0) {
                        return;
                    }
                    // 光瞄视野框
                    drawOpticalSightFrame(guiGraphics,
                            (int) (baseX + weaponUnit.getYRotMin() - weaponUnit.ySelfRot),
                            (int) (baseX + weaponUnit.getYRotMax() - weaponUnit.ySelfRot),
                            (int) (baseY + weaponUnit.getXRotMin() - weaponUnit.xSelfRot),
                            (int) (baseY + weaponUnit.getXRotMax() - weaponUnit.xSelfRot),
                            color);
                    if (vehicleWeapon instanceof VehicleMissile vehicleMissile) {
                        VehicleMissileWeaponData vehicleMissileWeaponData = vehicleMissile.getData();
                        // 导弹射界框
                        drawMissileEngagementFrame(guiGraphics,
                                (int) (baseX + Math.max(vehicleMissileWeaponData.getYRotMin(), weaponUnit.getYRotMin()) - weaponUnit.ySelfRot),
                                (int) (baseX + Math.min(vehicleMissileWeaponData.getYRotMax(), weaponUnit.getYRotMax()) - weaponUnit.ySelfRot),
                                (int) (baseY + Math.max(vehicleMissileWeaponData.getXRotMin(), weaponUnit.getXRotMin()) - weaponUnit.xSelfRot),
                                (int) (baseY + Math.min(vehicleMissileWeaponData.getXRotMax(), weaponUnit.getXRotMax()) - weaponUnit.xSelfRot),
                                color);
                    }
                    int x = (int) Mth.lerp(partialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                    int y = (int) Mth.lerp(partialTick, weaponUnit.xRotO, weaponUnit.getXRot());
                    // 光瞄指向十字
                    guiGraphics.fill(baseX + x, baseY + y - 8, baseX + x + 1, baseY + y - 2, color);
                    guiGraphics.fill(baseX + x, baseY + y + 3, baseX + x + 1, baseY + y + 9, color);
                    guiGraphics.fill(baseX + x - 8, baseY + y, baseX + x - 2, baseY + y + 1, color);
                    guiGraphics.fill(baseX + x + 3, baseY + y, baseX + x + 9, baseY + y + 1, color);
                });
            }
        }
        poseStack.popPose();
    }

    private static void drawOpticalSightFrame(GuiGraphics guiGraphics, int left, int right, int top, int bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        int horizontal = Math.max(1, (right - left) / 5);
        int vertical = Math.max(1, (bottom - top) / 5);
        guiGraphics.hLine(left, left + horizontal, top, color);
        guiGraphics.vLine(left, top, top + vertical, color);
        guiGraphics.hLine(right - horizontal, right, top, color);
        guiGraphics.vLine(right, top, top + vertical, color);
        guiGraphics.hLine(left, left + horizontal, bottom, color);
        guiGraphics.vLine(left, bottom - vertical, bottom, color);
        guiGraphics.hLine(right - horizontal, right, bottom, color);
        guiGraphics.vLine(right, bottom - vertical, bottom, color);
    }

    private static void drawMissileEngagementFrame(GuiGraphics guiGraphics, int left, int right, int top, int bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        int thickness = Math.min(2, Math.min(right - left, bottom - top));
        guiGraphics.fill(left, top, right, top + thickness, color);
        guiGraphics.fill(left, bottom - thickness, right, bottom, color);
        guiGraphics.fill(left, top, left + thickness, bottom, color);
        guiGraphics.fill(right - thickness, top, right, bottom, color);
    }

}
