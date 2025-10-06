package org.ywzj.vehicle.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientWeaponUnitControl;
import org.ywzj.vehicle.network.message.ServerPartUnitRot;
import org.ywzj.vehicle.util.EntityUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PartUnit {

    protected final Component name;
    protected final int index;
    protected final AbstractVehicle vehicle;
    protected LivingEntity operator;
    public PassengerPose operatorPose;
    private BedrockBone unitBone;
    protected final List<VehicleBedrockCubeOBB> unitBedrockCubeOBBs;
    public float xRot;
    public float yRot;
    public float xRotO;
    public float yRotO;
    public float xRotSpeed;
    public float yRotSpeed;
    public float xRotMax = 90;
    public float xRotMin = -90;
    public float yRotMax = Float.MAX_VALUE;
    public float yRotMin = -Float.MAX_VALUE;
    public float xAimRot;
    public float yAimRot;

    public PartUnit(String name, int index, AbstractVehicle vehicle) {
        this.name = Component.translatable(name);
        this.index = index;
        this.vehicle = vehicle;
        this.unitBedrockCubeOBBs = new ArrayList<>();
        this.initStructureModel(name);
        this.initOBBs();
    }

    public void tick() {
        if (vehicle.hasPower()) {
            tickRot();
            updateOBBs();
        } else {
            this.xRotO = this.xRot;
            this.yRotO = this.yRot;
        }
    }

    protected void initStructureModel(String name) {
        BedrockModel model = BedrockModelLoader.getModel(vehicle.getVehicleType().getStructureBedrockModel());
        if (model != null) {
            this.unitBone = model.getBoneMap().get(name);
        }
    }

    protected void initOBBs() {
        if (unitBone != null) {
            List<BedrockCubePerFace> cubes = new ArrayList<>(unitBone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
            for (BedrockCubePerFace cube : cubes) {
                unitBedrockCubeOBBs.add(VehicleBedrockCubeOBB.init(vehicle, unitBone, cube));
            }
            for (BedrockBone child : unitBone.getChildren()) {
                List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
                for (BedrockCubePerFace cube : childCubes) {
                    unitBedrockCubeOBBs.add(VehicleBedrockCubeOBB.init(vehicle, child, cube));
                }
            }
        }
    }

    public List<VehicleBedrockCubeOBB> getUnitBedrockCubeOBBs() {
        return unitBedrockCubeOBBs;
    }

    public List<OBB> getOBBs() {
        return unitBedrockCubeOBBs.stream().map(VehicleBedrockCubeOBB::obb).toList();
    }

    public void updateOBBs() {
        for (VehicleBedrockCubeOBB unitBedrockCubeOBB : unitBedrockCubeOBBs) {
            OBB obb = unitBedrockCubeOBB.obb();
            Vec3 center = unitBedrockCubeOBB.center();
            Quaternionf selfRot = new Quaternionf(unitBedrockCubeOBB.selfRot());
            obb.setCenter(vehicle.relativeRotPos(center).toVector3f());
            obb.setRotation(vehicle.rotYXZ().mul(selfRot));
        }
    }

    protected void tickRot() {
        this.xRotO = this.xRot;
        this.yRotO = this.yRot;
        float xDiff = Mth.wrapDegrees(this.xAimRot - this.xRot);
        float yDiff = Mth.wrapDegrees(this.yAimRot - this.yRot);
        if (Math.abs(xDiff) > xRotSpeed) {
            this.xRot += Math.signum(xDiff) * xRotSpeed;
        } else {
            this.xRot = this.xAimRot;
        }
        this.xRot = Math.max(Math.min(this.xRot, xRotMax), xRotMin);
        if (Math.abs(yDiff) > yRotSpeed) {
            this.yRot += Math.signum(yDiff) * yRotSpeed;
        } else {
            this.yRot = this.yAimRot;
        }
        this.yRot = Math.max(Math.min(this.yRot, yRotMax), yRotMin);
        if (!vehicle.level().isClientSide()) {
            if (xDiff != 0 || yDiff != 0) {
                vehicle.level().players().stream()
                        .filter(player -> EntityUtil.withinBroadcastRange(vehicle, player) && vehicle.getOwnOperatorUnit(player) != this)
                        .forEach(player ->
                                Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ServerPartUnitRot(this)));
            }
        }
    }

    public static void onClientMessageReceived(ClientWeaponUnitControl message, Supplier<NetworkEvent.Context> ctxSupplier) {
        if (ctxSupplier.get().getSender() != null) {
            Level level = ctxSupplier.get().getSender().level();
            Entity entity = level.getEntity(message.vehicleEntityId);
            if (entity instanceof AbstractVehicle vehicle) {
                if (message.weaponIndex < vehicle.operatorUnits.size()) {
                    if (message.shoot) {
                        vehicle.shoot(message.weaponIndex, new Vec3(message.ammoX, message.ammoY, message.ammoZ), message.ammoXRot, message.ammoYRot);
                    } else {
                        if (vehicle.operatorUnits.get(message.weaponIndex) instanceof WeaponUnit serverWeaponUnit) {
                            serverWeaponUnit.xAimRot = message.xAimRot;
                            serverWeaponUnit.yAimRot = message.yAimRot % 360;
                        }
                    }
                }
            }
        }
    }

    public Component getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public AbstractVehicle getVehicle() {
        return vehicle;
    }

    public void setOperator(LivingEntity operator) {
        this.operator = operator;
    }

}
