package org.ywzj.vehicle.entity.misc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.ParticleUtil;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.*;

public class VehiclePart extends AbstractVehicle {

    private PartUnit<?> partUnit;
    private String partUnitId = "";
    private CompoundTag partUnitData;
    private List<String> excludedBoneNames = List.of();
    private Vec3 bottomOffset = Vec3.ZERO;
    private boolean initialObbsUpdated;

    public VehiclePart(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public void initPart(PartUnit<?> partUnit) {
        AbstractVehicle sourceVehicle = partUnit.getVehicle();
        this.vehicleId = sourceVehicle.getVehicleId();
        this.displayId = sourceVehicle.getDisplayId();
        this.partUnitId = partUnit.getId();
        this.partUnitData = partUnit.serializeNBT(registryAccess());
        if (!createPartUnitCopy(true)) {
            throw new IllegalArgumentException("Unable to copy PartUnit: " + partUnit.getId());
        }
    }

    private void initializePart(PartUnit<?> partUnit, boolean copyHealth) {
        this.partUnit = partUnit;
        if (copyHealth) {
            this.setMaxHealth(partUnit.getMaxHealth());
            this.setHealth(partUnit.getHealth());
        } else if (this.getMaxHealth() < 0) {
            this.setMaxHealth(partUnit.getMaxHealth());
            this.setHealth(partUnit.getHealth());
        }
        this.excludedBoneNames = collectExcludedBoneNames(partUnit);
        this.centerOffset = Vec3.ZERO;
        this.dataInitialized = true;
        this.initialObbsUpdated = false;
        VehicleCubeOBB sourceCube = partUnit.getLargestCube();
        if (sourceCube == null) {
            throw new IllegalArgumentException("PartUnit has no structure cube: " + partUnit.getId());
        }
        this.bottomOffset = sourceCube.offset().subtract(0, sourceCube.height / 2, 0);
        VehicleCubeGroup group = new VehicleCubeGroup(null, new Quaternionf(), Vec3.ZERO);
        float halfW = (float) sourceCube.width / 2;
        float halfH = (float) sourceCube.height / 2;
        float halfD = (float) sourceCube.depth / 2;
        OBB obb = new OBB(new Vector3f(0, halfH, 0), new Vector3f(halfW, halfH, halfD), new Quaternionf());
        VehicleCubeOBB cube = new VehicleCubeOBB(obb);
        cube.group = group;
        cube.x = -halfW;
        cube.y = 0;
        cube.z = -halfD;
        cube.height = sourceCube.height;
        cube.width = sourceCube.width;
        cube.depth = sourceCube.depth;
        this.vehicleCubeOBBs = new ArrayList<>(List.of(cube));
        this.mainCubeOBB = cube;
        this.physicsEngine.mass = 1;
        this.physicsEngine.friction = 0.01f;
        this.physicsEngine.center = new Vec3(0, halfH, 0);
        this.defenseStats = partUnit.getDefenseStats();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("PartUnitId", partUnitId);
        compound.put("PartUnitData", getPartUnitData());
        compound.putFloat("PartZRot", getZRot());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("PartUnitId", Tag.TAG_STRING)) {
            this.partUnitId = compound.getString("PartUnitId");
        }
        if (compound.contains("PartUnitData", Tag.TAG_COMPOUND)) {
            this.partUnitData = compound.getCompound("PartUnitData");
        }
        if (compound.contains("PartZRot", Tag.TAG_ANY_NUMERIC)) {
            this.setZRot(compound.getFloat("PartZRot"));
        }
        createPartUnitCopy(false);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.vehicleId);
        buffer.writeResourceLocation(this.getDisplayId());
        buffer.writeInt(this.destroyedTick);
        buffer.writeUtf(partUnitId);
        buffer.writeNbt(getPartUnitData());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.vehicleId = buffer.readResourceLocation();
        this.displayId = buffer.readResourceLocation();
        this.destroyedTick = buffer.readInt();
        partUnitId = buffer.readUtf();
        partUnitData = buffer.readNbt();
        createPartUnitCopy(false);
        initDisplayData();
    }

    @Override
    public void initData() {
        this.dataInitialized = true;
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {}

    @Override
    public void tick() {
        if (partUnit != null || createPartUnitCopy(false)) {
            if (!initialObbsUpdated) {
                updateOBBs();
                initialObbsUpdated = true;
            }
            super.tick();
        }
    }

    @Override
    protected Vec3 tickMove() {
        return Vec3.ZERO;
    }

    @Override
    protected void tickParts() {}

    @OnlyIn(Dist.CLIENT)
    protected void tickParticle() {
        if (isDestroyed()) {
            Level level = level();
            if (!level.isClientSide()) {
                return;
            }
            if (destroyedTick <= 20 * 5) {
                ParticleUtil.spawnDestroyedPartCloud(level, position().add(0, mainCubeOBB.height / 2, 0), mainCubeOBB.depth);
            }
            if (tickCount % 20 == 0) {
                ParticleUtil.spawnWreckageSmoke(level, getBoundingBox(), 5);
            }
        }
    }

    @Override
    protected void tickEnergy() {}

    @Override
    protected void tickPower() {}

    @Override
    protected void tickEngineSpeed() {}

    @Override
    public Component getDisplayName() {
        if (partUnit != null) {
            return Component.translatable(partUnit.getData().getName());
        }
        return Component.literal("Part");
    }

    public List<String> getExcludedBoneNames() {
        return excludedBoneNames;
    }

    public Vec3 getBottomOffset() {
        return bottomOffset;
    }

    @Nullable
    public PartUnit<?> getPartUnit() {
        if (partUnit == null) {
            createPartUnitCopy(false);
        }
        return partUnit;
    }

    private CompoundTag getPartUnitData() {
        if (partUnit != null) {
            return partUnit.serializeNBT(registryAccess());
        }
        return partUnitData != null ? partUnitData.copy() : new CompoundTag();
    }

    private boolean createPartUnitCopy(boolean copyHealth) {
        if (partUnit != null) {
            return true;
        }
        if (partUnitId.isBlank() || partUnitData == null) {
            return false;
        }
        Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(vehicleId);
        if (vehicleDataOptional.isPresent()) {
            Optional<PartUnit<?>> partUnitOptional = vehicleDataOptional.get().copyPartUnit(this, partUnitId, partUnitData);
            if (partUnitOptional.isPresent()) {
                initializePart(partUnitOptional.get(), copyHealth);
                partUnitData = null;
                return true;
            }
        }
        return false;
    }

    private static List<String> collectExcludedBoneNames(PartUnit<?> partUnit) {
        Set<String> excludedBoneNames = new LinkedHashSet<>();
        collectExcludedBoneNames(partUnit, excludedBoneNames, new HashSet<>());
        return List.copyOf(excludedBoneNames);
    }

    private static void collectExcludedBoneNames(PartUnit<?> partUnit, Set<String> excludedBoneNames,
                                                 Set<PartUnit<?>> visitedPartUnits) {
        if (!visitedPartUnits.add(partUnit)) {
            return;
        }
        for (PartUnit<?> attachedPartUnit : partUnit.getAttPartUnits()) {
            if (attachedPartUnit.isDetachable()) {
                excludedBoneNames.add(attachedPartUnit.getRenderBoneName());
            } else {
                collectExcludedBoneNames(attachedPartUnit, excludedBoneNames, visitedPartUnits);
            }
        }
    }

}
