package org.ywzj.vehicle.vehicle.part;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.UnmodifiableView;
import org.joml.Vector3f;
import org.ywzj.vehicle.custom.part.data.LandingGearUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LandingGearUnit extends SwitchableUnit<LandingGearUnitData> {

    private double maxHeight;
    private boolean changing;
    private final List<VehicleCubeOBB.CubePoint> cubePoints = new ArrayList<>();

    public LandingGearUnit(int index, AbstractVehicle vehicle, LandingGearUnitData data) {
        super(index, vehicle, data);
    }

    @Override
    public void combineAndInit(@UnmodifiableView Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        super.combineAndInit(partUnitsView, vehicle);
        maxHeight = this.getOBBs().stream()
                .mapToDouble(obb -> obb.extents().y * 2)
                .max()
                .orElse(1);
        VehicleCubeOBB mainCubeOBB = vehicle.getMainCubeOBB();
        mainCubeOBB.height -= maxHeight;
        mainCubeOBB.y += maxHeight;
        mainCubeOBB.rebuild();
        partCubeOBBs.forEach(partCubeOBB -> {
            Vec3 offset = partCubeOBB.offset().subtract(mainCubeOBB.offset());
            Vector3f obbLocalPos = new Vector3f((float) offset.x, gearDownPointY(), (float) offset.z);
            VehicleCubeOBB.CubePoint cubePoint = new VehicleCubeOBB.CubePoint(mainCubeOBB, obbLocalPos, VehicleCubeOBB.CubeFace.BOTTOM);
            cubePoints.add(cubePoint);
        });
        mainCubeOBB.cubePoints().addAll(cubePoints);
        mainCubeOBB.cubePointsByFace.get(VehicleCubeOBB.CubeFace.BOTTOM).addAll(cubePoints);
        mainCubeOBB.initBottomPoint();
    }

    @Override
    public boolean onInteract(Player player, InteractionHand hand) {
        return true;
    }

    @Override
    public void setOn(boolean on) {
        if (update(on)) {
            if (vehicle.getDriver() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(on ? "tips.gear_up" : "tips.gear_down"), true);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!changing) {
            return;
        }
        float targetY = gearPointY();
        float step = (float) (maxHeight / 40);
        boolean reached = true;
        for (VehicleCubeOBB.CubePoint cubePoint : cubePoints) {
            Vector3f obbLocalPos = cubePoint.obbLocalPos();
            if (obbLocalPos.y < targetY) {
                obbLocalPos.y = Math.min(targetY, obbLocalPos.y + step);
            } else if (obbLocalPos.y > targetY) {
                obbLocalPos.y = Math.max(targetY, obbLocalPos.y - step);
            }
            if (Math.abs(obbLocalPos.y - targetY) > 1.0E-4f) {
                reached = false;
            }
        }
        changing = !reached;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        setGearPointY(gearPointY());
        changing = false;
    }

    public boolean update(boolean newState) {
        if (changing) {
            return false;
        }
        if (newState != this.on) {
            changing = true;
        }
        this.on = newState;
        return true;
    }

    public double level() {
        if (cubePoints.isEmpty() || maxHeight == 0) {
            return on ? 0 : 1;
        }
        double upY = gearUpPointY();
        return Math.min(1, Math.max(0, (upY - cubePoints.get(0).obbLocalPos().y) / maxHeight));
    }

    private float gearPointY() {
        return on ? gearUpPointY() : gearDownPointY();
    }

    private float gearUpPointY() {
        return -vehicle.getMainCubeOBB().obb().extents().y;
    }

    private float gearDownPointY() {
        return gearUpPointY() - (float) maxHeight - 0.1f;
    }

    private void setGearPointY(float y) {
        cubePoints.forEach(cubePoint -> cubePoint.obbLocalPos().y = y);
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public float getDragK() {
        return data.getDragK();
    }

}
