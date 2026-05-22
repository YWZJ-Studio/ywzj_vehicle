package org.ywzj.vehicle.custom.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.PartUnitEntry;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.NoneVehicle;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.pojo.DefenseStats;
import org.ywzj.vehicle.vehicle.pojo.EnergyInfo;
import org.ywzj.vehicle.vehicle.pojo.PhysicsInfo;
import org.ywzj.vehicle.vehicle.pojo.ViewInfo;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;
import org.ywzj.vehicle.vehicle.structure.VehicleStructOBBs;

import java.util.*;

public class BaseVehicleData<T extends AbstractVehicle> {

    protected ResourceLocation vehicleId;
    protected Component name;
    protected float maxHealth;
    protected ViewInfo viewInfo;
    protected EnergyInfo energyInfo;
    protected PhysicsInfo physicsInfo;
    protected DefenseStats defenseStats;
    protected boolean uav;
    protected boolean withWarningReceiver;
    protected boolean protectPassenger;
    protected boolean experimental;
    protected Vec3 centerOffset;
    protected ResourceLocation structureModel;
    protected double structureLength;
    protected List<PartUnitEntry<?, ?>> parts;
    protected VehicleCubeOBB mainCubeOBB;
    protected final List<VehicleCubeOBB> vehicleBodyOBBs = new ArrayList<>();
    protected final Map<BedrockBone, VehicleCubeGroup> vehiclePartGroups = new HashMap<>();

    public BaseVehicleData() {}

    public AbstractVehicle construct(Level level, Vec3 position, float xRot, float yRot) {
        AbstractVehicle vehicle = fromRegistries(level, position, xRot, yRot);
        if (vehicle != null) {
            return vehicle;
        }
        vehicle = fromCustom(level);
        vehicle.setVehicleId(vehicleId);
        vehicle.setPos(position);
        vehicle.setXRot(xRot);
        vehicle.setYRot(yRot);
        return vehicle;
    }

    protected AbstractVehicle fromRegistries(Level level, Vec3 position, float xRot, float yRot) {
        if (ForgeRegistries.ENTITY_TYPES.containsKey(vehicleId)) {
            EntityType<?> vehicleType = ForgeRegistries.ENTITY_TYPES.getValue(vehicleId);
            Entity entity = vehicleType.create(level);
            if (entity instanceof AbstractVehicle vehicle) {
                vehicle.setPos(position);
                vehicle.setXRot(xRot);
                vehicle.setYRot(yRot);
                return vehicle;
            }
        }
        return null;
    }

    protected AbstractVehicle fromCustom(Level level) {
        return new NoneVehicle(AllEntities.NONE_VEHICLE.get(), level);
    }

    protected static String check(BaseVehicleDataPojo pojo) {
        return "";
    }

    public void build(BaseVehicleDataPojo pojo) {
        String checkResult = check(pojo);
        if (!StringUtils.isBlank(checkResult)) {
            YwzjVehicle.LOGGER.warn(checkResult);
            return;
        }

        this.maxHealth = pojo.maxHealth;
        this.viewInfo = pojo.viewInfo;
        this.energyInfo = pojo.energyInfo;
        this.physicsInfo = pojo.physicsInfo;
        this.defenseStats = pojo.defenseStats;
        this.uav = pojo.uav;
        this.withWarningReceiver = pojo.withWarningReceiver;
        this.protectPassenger = pojo.protectPassenger;
        this.experimental = pojo.experimental;

        this.centerOffset = pojo.centerOffset;
        this.structureModel = pojo.structureModel;
        var model = CommonAssetsManager.structureModelManager()
                .getStructureModel(pojo.structureModel).orElseThrow();
        // 构建OBB
        buildOBBs(model);
        // 构建部件结构
        this.parts = pojo.parts;
        for (var entry : this.parts) {
            var partData = entry.data();
            partData.initStructureModel(model, vehiclePartGroups);
        }
        // 构建物理结构
        buildMainCube(model);
        // 计算载具参考长度
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        List<VehicleCubeOBB> vehicleOBBs = new ArrayList<>(vehicleBodyOBBs);
        for (PartUnitEntry<?, ?> partUnitEntry : parts) {
            vehicleOBBs.addAll(partUnitEntry.data().getRawPartCubeOBBs());
        }
        for (VehicleCubeOBB vehicleOBB : vehicleOBBs) {
            double z1 = vehicleOBB.offset().z + vehicleOBB.getDepth() / 2;
            double z2 = vehicleOBB.offset().z - vehicleOBB.getDepth() / 2;
            if (maxZ < z1) {
                maxZ = z1;
            }
            if (minZ > z2) {
                minZ = z2;
            }
        }
        this.structureLength = maxZ - minZ;
    }

    public void inject(T vehicle) {}

    public record PartUnitsAndSeats(
            Map<String, PartUnit<?>> partUnitMap,
            List<AbstractVehicle.Seat> seats
    ) {}

    /**
     * 为载具实例构造部件
     */
    public PartUnitsAndSeats createPartUnits(AbstractVehicle vehicle) {
        Map<String, PartUnit<?>> partUnitMap = new LinkedHashMap<>();
        List<AbstractVehicle.Seat> seats = new ArrayList<>();
        HashMap<VehicleCubeGroup, VehicleCubeGroup> vehicleCubeGroupCopy = vehicleCubeGroupsCopy();
        int index = 0;
        // 部件数据初始化
        for (var partData : parts) {
            var partUnit = partData.create(index, vehicle);
            partUnit.buildStructure(vehicleCubeGroupCopy);
            if (partData.data().isSeat()) {
                int seatIndex = seats.size();
                seats.add(new AbstractVehicle.Seat(seatIndex, partUnit));
            }
            partUnitMap.put(partData.data().getId(), partUnit);
            index++;
        }
        // 部件之间的组织
        var view = Collections.unmodifiableMap(partUnitMap);
        for (var partUnit : partUnitMap.values()) {
            partUnit.combineAndInit(view, vehicle);
        }
        return new PartUnitsAndSeats(partUnitMap, seats);
    }

    private void buildOBBs(BedrockModel model) {
        for (Map.Entry<String, BedrockBone> boneEntry : model.getBoneMap().entrySet()) {
            BedrockBone bone = boneEntry.getValue();
            // 车体单独构建
            if (boneEntry.getKey().equals("vehicle_body")) {
                VehicleCubeGroup group = new VehicleCubeGroup(null, bone.rotation, new Vec3(bone.x / 16, bone.y / 16, bone.z / 16));
                buildVehicleBodyOBBs(bone, group);
            } else if (!boneEntry.getKey().equals("main_structure")) {
                // 从基岩模型最外层的各组构建
                if (bone.parent != null && bone.parent.parent == null) {
                    VehicleCubeGroup group = new VehicleCubeGroup(null, bone.rotation, new Vec3(bone.x / 16, bone.y / 16, bone.z / 16));
                    buildPartUnitsOBBs(bone, group, new HashSet<>(model.getBoneMap().values()));
                }
            }
        }
    }

    private void buildVehicleBodyOBBs(BedrockBone bone, VehicleCubeGroup group) {
        bone.cubes.forEach(cube -> vehicleBodyOBBs.add(VehicleCubeOBB.init(group, cube)));
        bone.getChildren().forEach(child -> {
            VehicleCubeGroup childGroup = new VehicleCubeGroup(group, child.rotation, new Vec3(child.x / 16, child.y / 16, child.z / 16));
            buildVehicleBodyOBBs(child, childGroup);
        });
    }

    private void buildMainCube(BedrockModel model) {
        BedrockBone main = model.getBoneMap().get("main_structure");
        if (main != null) {
            mainCubeOBB = largestVehicleCubeOBB(main);
            if (mainCubeOBB == null) {
                YwzjVehicle.LOGGER.warn("main_structure has no cubes: {}", getName());
            }
        } else {
            if (vehicleBodyOBBs.isEmpty()) {
                YwzjVehicle.LOGGER.warn("vehicleBodyOBBs is empty: {}", getName());
                return;
            }
            vehicleBodyOBBs.sort(Comparator.comparingDouble(vehicleBodyOBB -> -vehicleBodyOBB.depth * vehicleBodyOBB.width * vehicleBodyOBB.height));
            mainCubeOBB = vehicleBodyOBBs.get(0);
        }
    }

    public VehicleCubeOBB largestVehicleCubeOBB(BedrockBone bone) {
        VehicleCubeOBB[] maxHolder = new VehicleCubeOBB[1];
        VehicleCubeGroup group = new VehicleCubeGroup(
                null,
                bone.rotation,
                new Vec3(bone.x / 16.0, bone.y / 16.0, bone.z / 16.0)
        );
        findMaxRecursive(bone, group, maxHolder);
        return maxHolder[0];
    }

    private void findMaxRecursive(BedrockBone bone, VehicleCubeGroup group, VehicleCubeOBB[] maxHolder) {
        for (BedrockCube cube : bone.cubes) {
            VehicleCubeOBB currentOBB = VehicleCubeOBB.init(group, cube);
            if (maxHolder[0] == null || currentOBB.volume() > maxHolder[0].volume()) {
                maxHolder[0] = currentOBB;
            }
        }
        for (BedrockBone child : bone.getChildren()) {
            VehicleCubeGroup childGroup = new VehicleCubeGroup(
                    group,
                    child.rotation,
                    new Vec3(child.x / 16.0, child.y / 16.0, child.z / 16.0)
            );
            findMaxRecursive(child, childGroup, maxHolder);
        }
    }

    /**
     * 从基岩Bone递归构建载具部件的结构OBB组
     * 每个结构OBB组持有其Bone以及匿名子Bone下所有的块
     */
    public void buildPartUnitsOBBs(BedrockBone bone, VehicleCubeGroup group, Set<BedrockBone> namedBones) {
        if (bone == null) {
            return;
        }
        bone.cubes.forEach(cube -> VehicleCubeOBB.init(group, cube));
        vehiclePartGroups.put(bone, group);
        bone.getChildren().forEach(child -> {
            VehicleCubeGroup childGroup = new VehicleCubeGroup(group, child.rotation, new Vec3(child.x / 16, child.y / 16, child.z / 16));
            buildPartUnitsOBBs(child, childGroup, namedBones);
            // 合并匿名组的块
            if (!namedBones.contains(child)) {
                childGroup.cubeOBBs.forEach(group::addCubeOBB);
            }
            child.getChildren().forEach(subChild -> {
                if (!namedBones.contains(subChild)) {
                    vehiclePartGroups.get(subChild).cubeOBBs.forEach(childGroup::addCubeOBB);
                }
            });
        });
    }

    /**
     * 所有载具实例的结构OBB组森林都是独立的拷贝副本
     */
    public HashMap<VehicleCubeGroup, VehicleCubeGroup> vehicleCubeGroupsCopy() {
        HashMap<VehicleCubeGroup, VehicleCubeGroup> clone = new HashMap<>();
        vehiclePartGroups.values().stream().filter(vehicleCubeGroup -> vehicleCubeGroup.parent == null)
                .forEach(parentGroup -> vehicleCubeGroupsClone(clone, parentGroup));
        return clone;
    }

    private void vehicleCubeGroupsClone(HashMap<VehicleCubeGroup, VehicleCubeGroup> clone, VehicleCubeGroup parentGroup) {
        VehicleCubeGroup cloneVehicleCubeGroup = new VehicleCubeGroup(parentGroup.parent == null ? null : clone.get(parentGroup.parent), parentGroup.rotation, parentGroup.pivot);
        cloneVehicleCubeGroup.baseRotation = parentGroup.baseRotation;
        clone.put(parentGroup, cloneVehicleCubeGroup);
        parentGroup.children.forEach(childGroup -> vehicleCubeGroupsClone(clone, childGroup));
    }

    public VehicleStructOBBs getVehicleStructObbs() {
        var obbs = vehicleBodyOBBs.stream().map(VehicleCubeOBB::new).toList();
        return new VehicleStructOBBs(obbs, new VehicleCubeOBB(mainCubeOBB));
    }

    public ResourceLocation getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(ResourceLocation vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Component getName() {
        return name;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public double getStructureLength() {
        return structureLength;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public ViewInfo getViewInfo() {
        return viewInfo;
    }

    public EnergyInfo getEnergyInfo() {
        return energyInfo;
    }

    public PhysicsInfo getPhysicsInfo() {
        return physicsInfo;
    }

    public DefenseStats getDefenseStats() {
        return defenseStats;
    }

    public boolean isUav() {
        return uav;
    }

    public boolean withWarningReceiver() {
        return withWarningReceiver;
    }

    public boolean isProtectPassenger() {
        return protectPassenger;
    }

    public boolean isExperimental() {
        return experimental;
    }

    public Vec3 getCenterOffset() {
        return centerOffset;
    }

}
