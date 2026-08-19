package org.ywzj.vehicle.client.gui;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.command.sub.DebugCommand;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VehicleDebugOverlay implements LayeredDraw.Layer {

    private static final int MAX_DEPTH = 16;
    private static final int LINE_H = 10;
    private static final int COL1_X = 2;
    private static final int COL2_X = 2;
    private static final int COL3_X = 240;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!LocalVehiclePlayer.instance.onVehicle() || !DebugCommand.DEBUG) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!mc.hasSingleplayerServer()) {
            return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }
        UUID uuid = mc.player.getUUID();
        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(uuid);
        Entity entity = serverPlayer.level().getEntity(LocalVehiclePlayer.instance.vehicle.getId());
        if (!(entity instanceof AbstractVehicle vehicle)) {
            return;
        }
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        try {
            poseStack.scale(0.5f, 0.5f, 0.5f);
            WeaponUnit weaponUnit = LocalVehiclePlayer.instance.getWeaponUnit();
            int y = 2;
            guiGraphics.drawString(mc.font, "-- vehicle --", COL1_X, y, Color.GRAY); y += LINE_H;
            guiGraphics.drawString(mc.font, "vehicleId: " + vehicle.getVehicleId(), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "displayId: " + vehicle.getDisplayId(), COL1_X, y, Color.WHITE); y += LINE_H;
            Vec3 vel = new Vec3(vehicle.physicsEngine.velocity);
            guiGraphics.drawString(mc.font, "velocity: " + String.format("%.2f, %.2f, %.2f", vel.x, vel.y, vel.z), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "speed: " + String.format("%.2f", vehicle.physicsEngine.velocity.length()), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "xRot: " + String.format("%.2f", vehicle.getXRot()) + "  yRot: " + String.format("%.2f", vehicle.getYRot()) + "  zRot: " + String.format("%.2f", vehicle.getZRot()), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "engineSpeed: " + String.format("%.2f", vehicle.getEngineSpeed()), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "power: " + String.format("%.2f", vehicle.getPower()), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "energy: " + String.format("%.2f", vehicle.getEnergy()), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "mass: " + String.format("%.2f", vehicle.physicsEngine.mass), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "curbWeight: " + String.format("%.2f", vehicle.curbWeight), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "engineOn: " + vehicle.isEngineOn(), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "destroyed: " + vehicle.isDestroyed(), COL1_X, y, Color.WHITE); y += LINE_H;
            guiGraphics.drawString(mc.font, "onGround: " + vehicle.onGround(), COL1_X, y, Color.WHITE); y += LINE_H;
            y += LINE_H;
            guiGraphics.drawString(mc.font, "-- weapon data --", COL2_X, y, Color.GRAY); y += LINE_H;
            if (weaponUnit != null) {
                var currentWeapon = weaponUnit.getCurrentWeapon();
                if (currentWeapon.isPresent()) {
                    BaseVehicleWeaponData data = currentWeapon.get().getData();
                    for (String line : getGetterLines(data, 0)) {
                        guiGraphics.drawString(mc.font, line, COL2_X, y, Color.WHITE);
                        y += LINE_H;
                    }
                } else {
                    guiGraphics.drawString(mc.font, "no weapon selected", COL2_X, y, Color.WHITE);
                }
            } else {
                guiGraphics.drawString(mc.font, "no weaponUnit", COL2_X, y, Color.WHITE);
            }
            y = 2;
            guiGraphics.drawString(mc.font, "-- weapon unit --", COL3_X, y, Color.GRAY); y += LINE_H;
            if (weaponUnit == null) {
                guiGraphics.drawString(mc.font, "null", COL3_X, y, Color.WHITE);
            } else {
                guiGraphics.drawString(mc.font, "name: " + weaponUnit.getName().getString(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "currentWeaponIndex: " + (weaponUnit.getCurrentWeaponIndex() + 1) + " / " + weaponUnit.weapons.size(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "currentSecondaryWeaponIndex: " + (weaponUnit.getCurrentSecondaryWeaponIndex() + 1)  + " / " + weaponUnit.secondaryWeapons.size(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "ammoCapacity: " + weaponUnit.getAmmoCapacity(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "firingMode: " + weaponUnit.getData().getFiringMode(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "withStabilizer: " + weaponUnit.getData().withStabilizer(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "withFocusLocker: " + weaponUnit.getData().withFocusLocker(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "withThermalImager: " + weaponUnit.getData().withThermalImager(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "zoom: " + String.format("%.1f", weaponUnit.getZoom()), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "zoomRange: " + String.format("%.1f", weaponUnit.getData().getZoomMin()) + " ~ " + String.format("%.1f", weaponUnit.getData().getZoomMax()), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "opticalSightType: " + weaponUnit.opticalSightType, COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "fireControlSensorType: " + weaponUnit.getData().getFireControlSensorType(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "crosshairStyle: " + weaponUnit.crosshairStyle, COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "seeker: " + weaponUnit.isSeekerOn(), COL3_X, y, Color.WHITE); y += LINE_H;
                y += 2;
                guiGraphics.drawString(mc.font, "weapons: " + weaponUnit.weapons.size(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "secondaryWeapons: " + weaponUnit.secondaryWeapons.size(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "independentWeapons: " + weaponUnit.independentWeapons.size(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "indexedWeapons: " + weaponUnit.indexedWeapons.size(), COL3_X, y, Color.WHITE); y += LINE_H;
                guiGraphics.drawString(mc.font, "weaponBayUnits: " + weaponUnit.weaponBayUnits.size(), COL3_X, y, Color.WHITE); y += LINE_H;
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static List<String> getGetterLines(Object obj, int depth) {
        List<String> lines = new ArrayList<>();
        String prefix = depth > 0 ? "  ".repeat(depth) : "";
        for (Class<?> clazz = obj.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                SerializedName annotation = field.getAnnotation(SerializedName.class);
                if (annotation == null) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    String name = annotation.value();
                    if (value instanceof Ingredient) {
                        continue;
                    }
                    if (value == null) {
                        lines.add(prefix + name + ": null");
                    } else if (isSimple(value) || depth >= MAX_DEPTH) {
                        lines.add(prefix + name + ": " + value);
                    } else {
                        lines.add(prefix + name + ":");
                        lines.addAll(getGetterLines(value, depth + 1));
                    }
                } catch (Exception e) {
                    lines.add(prefix + annotation.value() + ": <err>");
                }
            }
        }
        return lines;
    }

    private static boolean isSimple(Object value) {
        return value instanceof String
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Enum<?>
            || value instanceof net.minecraft.resources.ResourceLocation
            || value.getClass().isEnum();
    }

}
