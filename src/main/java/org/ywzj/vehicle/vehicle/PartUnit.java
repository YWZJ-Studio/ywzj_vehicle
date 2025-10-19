package org.ywzj.vehicle.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Quaternionf;
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ClientVehicleAction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 具有结构模型的载具部件
 * 任何乘位都应关联于一个载具部件，以计算座位与镜头位置
 */
public class PartUnit {

    protected final Component name;
    protected final int index;
    protected final AbstractVehicle vehicle;
    protected LivingEntity owner;
    public Vec3 ownerViewOffset;
    public Vec3 seatOffset;
    public PassengerPose operatorPose;
    protected BedrockBone unitBone;
    protected final List<VehicleBedrockCubeOBB> unitBedrockCubeOBBs;

    public PartUnit(String name, int index, AbstractVehicle vehicle) {
        this.name = Component.translatable(name);
        this.index = index;
        this.vehicle = vehicle;
        this.unitBedrockCubeOBBs = new ArrayList<>();
        this.initStructureModel(name);
        this.initOBBs();
    }

    public PartUnit(Component name, int index, AbstractVehicle vehicle) {
        this.index = index;
        this.vehicle = vehicle;
        this.name = name;
        this.unitBedrockCubeOBBs = new ArrayList<>();
    }

    public void tick() {
        updateOBBs();
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
                unitBedrockCubeOBBs.add(VehicleBedrockCubeOBB.init(unitBone, cube));
            }
            for (BedrockBone child : unitBone.getChildren()) {
                List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
                for (BedrockCubePerFace cube : childCubes) {
                    unitBedrockCubeOBBs.add(VehicleBedrockCubeOBB.init(child, cube));
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
            Vec3 center = unitBedrockCubeOBB.center(this.vehicle);
            Quaternionf selfRot = new Quaternionf(unitBedrockCubeOBB.selfRot());
            obb.setCenter(vehicle.relativeRotPos(center).toVector3f());
            obb.setRotation(vehicle.rotYXZ().mul(selfRot));
        }
    }

    public static void onClientMessageReceived(ClientVehicleAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        if (ctxSupplier.get().getSender() != null) {
            Level level = ctxSupplier.get().getSender().level();
            Entity entity = level.getEntity(message.vehicleEntityId);
            if (entity instanceof AbstractVehicle vehicle) {
                if (message.weaponIndex < vehicle.operatorUnits.size()) {
                    if (message.shoot) {
                        vehicle.shoot(message.weaponIndex, new Vec3(message.ammoX, message.ammoY, message.ammoZ), message.ammoXRot, message.ammoYRot);
                    } else {
                        if (vehicle.operatorUnits.get(message.weaponIndex) instanceof RotatableUnit rotatableUnit) {
                            rotatableUnit.xAimRot = message.xAimRot;
                            rotatableUnit.yAimRot = message.yAimRot % 360;
                        }
                    }
                }
            }
        }
    }

    /**
     * 计算车身未旋转时某相对于载具枢轴的偏移xyz在经由车身旋转后的实际世界坐标
     */
    public Vec3 worldPosition(Vec3 offsetFromVehicle) {
        if (offsetFromVehicle == null) {
            return vehicle.position();
        }
        return vehicle.relativeRotPos(vehicle.position().add(offsetFromVehicle));
    }

    public Vec3 worldOwnerViewPosition() {
        float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
        if (ownerViewOffset == null) {
            return worldPosition(new Vec3(0, eyeHeight, 0));
        }
        return worldPosition(ownerViewOffset).add(new Vec3(0, eyeHeight, 0));
    }

    public Vec3 worldSeatPosition() {
        float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
        Vec3 seatOffset = this.seatOffset;
        if (seatOffset == null) {
            seatOffset = new Vec3(0, eyeHeight, 0);
        }
        return vehicle.relativeRotPos(vehicle.position().add(seatOffset));
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

    public LivingEntity getOwner() {
        return owner;
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

}
