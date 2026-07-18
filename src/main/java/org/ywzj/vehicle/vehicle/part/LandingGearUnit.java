package org.ywzj.vehicle.vehicle.part;

import net.minecraft.core.HolderLookup;
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

    private boolean changing;
    private final List<GearCube> gearCubes = new ArrayList<>();

    private record GearCube(VehicleCubeOBB gearCubeOBB, VehicleCubeOBB.CubePoint gearCubePoint) {}

    public LandingGearUnit(int index, AbstractVehicle vehicle, LandingGearUnitData data) {
        super(index, vehicle, data);
    }

    @Override
    public void combineAndInit(@UnmodifiableView Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        super.combineAndInit(partUnitsView, vehicle);
        double maxHeight = this.getOBBs().stream()
                .mapToDouble(obb -> obb.extents().y * 2)
                .max()
                .orElse(1);
        VehicleCubeOBB mainCubeOBB = vehicle.getMainCubeOBB();
        mainCubeOBB.height -= maxHeight;
        mainCubeOBB.y += maxHeight;
        mainCubeOBB.rebuild();
        partCubeOBBs.forEach(partCubeOBB -> {
            Vec3 offset = partCubeOBB.offset().subtract(mainCubeOBB.offset());
            Vector3f obbLocalPos = new Vector3f((float) offset.x, gearDownPointY(partCubeOBB), (float) offset.z);
            VehicleCubeOBB.CubePoint cubePoint = new VehicleCubeOBB.CubePoint(mainCubeOBB, obbLocalPos, VehicleCubeOBB.CubeFace.BOTTOM);
            gearCubes.add(new GearCube(partCubeOBB, cubePoint));
        });
        List<VehicleCubeOBB.CubePoint> cubePoints = gearCubes.stream().map(gearCube -> gearCube.gearCubePoint).toList();
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
        boolean reached = true;
        for (GearCube gearCube : gearCubes) {
            float targetY = gearPointY(gearCube.gearCubeOBB);
            float step = gearCube.gearCubeOBB.obb().extents().y / 20;
            Vector3f obbLocalPos = gearCube.gearCubePoint.obbLocalPos();
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
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(provider, nbt);
        for (GearCube gearCube : gearCubes) {
            gearCube.gearCubePoint.obbLocalPos().y = gearPointY(gearCube.gearCubeOBB);
        }
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
        if (gearCubes.isEmpty()) {
            return on ? 0 : 1;
        }
        GearCube gearCube = gearCubes.get(0);
        double upY = gearUpPointY(gearCube.gearCubeOBB);
        return Math.min(1, Math.max(0, (upY - gearCube.gearCubePoint.obbLocalPos().y) / gearCube.gearCubeOBB.obb().extents().y * 2));
    }

    private float gearPointY(VehicleCubeOBB cubeOBB) {
        return on ? gearUpPointY(cubeOBB) : gearDownPointY(cubeOBB);
    }

    private float gearUpPointY(VehicleCubeOBB cubeOBB) {
        return (float) (cubeOBB.offset().y - vehicle.getMainCubeOBB().offset().y + cubeOBB.obb().extents().y);
    }

    private float gearDownPointY(VehicleCubeOBB cubeOBB) {
        return gearUpPointY(cubeOBB) - cubeOBB.obb().extents().y * 2 - 0.1f;
    }

    public float getDragK() {
        return data.getDragK();
    }

}
