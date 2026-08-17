package org.ywzj.vehicle.blockentity;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllBlockEntities;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleDisplay;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.recipe.VehiclePrintingRecipe;

import java.util.*;

public class MachineMaxBlockEntity extends BlockEntity {

    private static final ICube[] NO_CUBES = new ICube[0];
    private boolean crafting;
    private boolean hasProduct;
    public float progress;
    public float step;
    public ResourceLocation craftingVehicleId;
    public VehicleDisplay<?, ?> vehicleDisplay;
    public BaseVehicleData vehicleData;
    public TreeBedrockModel printingModel;
    public TreeModelInstance printingModelInstance;
    public List<PrintingCube> printingCubes = List.of();
    public ICube[] staticPrintingCubes = NO_CUBES;
    public ICube[] visiblePrintingCubes = NO_CUBES;
    private int cachedVisibleCubeCount = -1;

    public MachineMaxBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.MACHINE_MAX_BLOCK_ENTITY.get(), pos, state);
    }

    public void craft(ResourceLocation craftingVehicleId, VehiclePrintingRecipe vehiclePrintingRecipe) {
        this.craftingVehicleId = craftingVehicleId;
        this.progress = 0;
        this.crafting = true;
        this.hasProduct = false;
        this.step = (float) 1 / vehiclePrintingRecipe.getPrintingTime();
        sync();
    }

    public AbstractVehicle takeProduct() {
        if (!this.hasProduct) {
            return null;
        }
        AbstractVehicle vehicle = CommonAssetsManager.vehicleDataManager().getVehicleData()
                .get(craftingVehicleId).construct(level, Vec3.ZERO, 0, 0);
        vehicle.initData();
        this.craftingVehicleId = null;
        this.progress = 0;
        this.hasProduct = false;
        sync();
        return vehicle;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMaxBlockEntity blockEntity) {
        if (!level.isClientSide) {
            if (blockEntity.crafting) {
                blockEntity.progress = Math.min(1, blockEntity.progress + blockEntity.step);
                if (blockEntity.progress >= 1) {
                    blockEntity.crafting = false;
                    blockEntity.hasProduct = true;
                }
                blockEntity.sync();
            }
        } else {
            blockEntity.tickAnimation();
            if (blockEntity.crafting && level.random.nextFloat() < 0.2F) {
                Vec3 worldPos = pos.getCenter();
                double xOffset = (level.random.nextDouble() - 0.5D) * 0.5D;
                double zOffset = (level.random.nextDouble() - 0.5D) * 0.5D;
                level.addParticle(
                        ParticleTypes.POOF,
                        worldPos.x + xOffset, worldPos.y + 1D, worldPos.z + zOffset,
                        0.0D, 0.1D, 0.0D
                );
            }
        }
    }

    public void sync() {
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    public boolean isCrafting() {
        return crafting;
    }

    public boolean hasProduct() {
        return hasProduct;
    }

    public boolean hasPrintingPreview() {
        return crafting || hasProduct;
    }

    /**
     * 打印时按 cube 进度显示；成品保留全部静态 cube，直到被取走。
     */
    public int getVisiblePrintingCubeCount() {
        if (printingCubes.isEmpty()) {
            return 0;
        }
        if (hasProduct) {
            return printingCubes.size();
        }
        if (!crafting || progress <= 0) {
            return 0;
        }
        return Math.min((int) Math.ceil(progress * printingCubes.size()), printingCubes.size());
    }

    public PrintingCube getCurrentPrintingCube() {
        if (printingCubes.isEmpty()) {
            return null;
        }
        if (hasProduct) {
            return printingCubes.get(printingCubes.size() - 1);
        }
        if (!crafting) {
            return null;
        }
        int visibleCount = getVisiblePrintingCubeCount();
        return printingCubes.get(Math.max(0, visibleCount - 1));
    }

    public boolean isPrintingPolyMeshStage() {
        if (!hasPrintingPreview() || printingModel == null) {
            return false;
        }
        return hasProduct || printingCubes.isEmpty() || getVisiblePrintingCubeCount() >= printingCubes.size();
    }

    public void clearPrintingPreview() {
        clearAnimation();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (craftingVehicleId != null) {
            tag.putString("craftingVehicleId", craftingVehicleId.toString());
        }
        tag.putBoolean("crafting", crafting);
        tag.putBoolean("hasProduct", hasProduct);
        tag.putFloat("progress", progress);
        tag.putFloat("step", step);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        craftingVehicleId = tag.contains("craftingVehicleId")
                ? YwzjVehicle.resourceLocation(tag.getString("craftingVehicleId"))
                : null;
        crafting = tag.getBoolean("crafting");
        hasProduct = tag.getBoolean("hasProduct");
        progress = tag.getFloat("progress");
        step = tag.getFloat("step");

        if (level != null && level.isClientSide()) {
            if (hasPrintingPreview()) {
                prepareAnimation();
                updateVisiblePrintingCubes();
            } else {
                clearAnimation();
            }
        }
    }

    private void tickAnimation() {
        if (!hasPrintingPreview()) {
            clearAnimation();
            return;
        }
        prepareAnimation();
        updateVisiblePrintingCubes();
    }

    /**
     * Tree 模型与其绑定姿势实例仅由当前方块实体持有。cube 会在此处烘焙为模型空间静态几何。
     */
    private void prepareAnimation() {
        if (!hasPrintingPreview() || printingModel != null || craftingVehicleId == null) {
            return;
        }

        vehicleDisplay = ClientAssetsManager.INSTANCE.getVehicleDisplay(craftingVehicleId).orElse(null);
        if (vehicleDisplay == null || vehicleDisplay.getModelPojo() == null) {
            return;
        }

        Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(craftingVehicleId);
        if (vehicleDataOptional.isEmpty()) {
            return;
        }

        TreeBedrockModel treeModel = TreeBedrockModel.bake(vehicleDisplay.getModelPojo());
        TreeModelInstance treeInstance = treeModel.createInstance();
        List<PrintingCube> cubes = new ArrayList<>();
        for (TreeBoneDefinition bone : treeModel.bones()) {
            Matrix4f boneTransform = treeInstance.getGlobalTransform(bone.index());
            for (ICube cube : bone.cubes()) {
                Matrix4f cubeTransform = cubeTransform(boneTransform, cube);
                cubes.add(new PrintingCube(
                        bone.index(),
                        bakeStaticCube(cube, cubeTransform),
                        cubeModelCenter(cube, cubeTransform)
                ));
            }
        }
        // 按最终模型空间中心从低到高打印；相同高度仍保持展平时的稳定顺序。
        cubes.sort(Comparator.comparingDouble(cube -> cube.modelCenter().y));

        vehicleData = vehicleDataOptional.get();
        printingModel = treeModel;
        printingModelInstance = treeInstance;
        printingCubes = List.copyOf(cubes);
        staticPrintingCubes = printingCubes.stream().map(PrintingCube::cube).toArray(ICube[]::new);
        cachedVisibleCubeCount = -1;
        updateVisiblePrintingCubes();
    }

    private void updateVisiblePrintingCubes() {
        int visibleCount = getVisiblePrintingCubeCount();
        if (visibleCount == cachedVisibleCubeCount) {
            return;
        }
        visiblePrintingCubes = visibleCount == 0
                ? NO_CUBES
                : visibleCount == staticPrintingCubes.length
                ? staticPrintingCubes
                : Arrays.copyOf(staticPrintingCubes, visibleCount);
        cachedVisibleCubeCount = visibleCount;
    }

    private void clearAnimation() {
        vehicleDisplay = null;
        vehicleData = null;
        printingModel = null;
        printingModelInstance = null;
        printingCubes = List.of();
        staticPrintingCubes = NO_CUBES;
        visiblePrintingCubes = NO_CUBES;
        cachedVisibleCubeCount = -1;
    }

    private static Matrix4f cubeTransform(Matrix4f boneTransform, ICube cube) {
        Matrix4f transform = new Matrix4f(boneTransform);
        if (cube.hasRotation()) {
            float[] pivot = cube.pivot();
            transform.translate(pivot[0], pivot[1], pivot[2])
                    .rotate(cube.rotation())
                    .translate(-pivot[0], -pivot[1], -pivot[2]);
        }
        return transform;
    }

    /**
     * Tree 的绑定姿势只有平移与旋转；将该刚体变换编码回 SBM 原生 cube，避免运行时骨骼变换。
     */
    private static ICube bakeStaticCube(ICube cube, Matrix4f transform) {
        Quaternionf rotation = transform.getUnnormalizedRotation(new Quaternionf()).normalize();
        Vector3f offset = new Vector3f(transform.m30(), transform.m31(), transform.m32());
        new Quaternionf(rotation).invert().transform(offset);
        float x = cube.x() + offset.x;
        float y = cube.y() + offset.y;
        float z = cube.z() + offset.z;
        float[] pivot = new float[3];

        if (cube instanceof CubeBox box) {
            return new CubeBox(x, y, z, cube.width(), cube.height(), cube.depth(), cube.inflate(),
                    box.uvs(), box.uvOrder(), pivot, rotation);
        }
        if (cube instanceof CubePerFace perFace) {
            return new CubePerFace(x, y, z, cube.width(), cube.height(), cube.depth(), cube.inflate(),
                    perFace.uvs(), perFace.emptyFacesMask(), pivot, rotation);
        }
        throw new IllegalArgumentException("Unsupported Tree cube type: " + cube.getClass().getName());
    }

    private static Vec3 cubeModelCenter(ICube cube, Matrix4f transform) {
        Vector3f center = new Vector3f(
                cube.x() + cube.width() * 0.5F,
                cube.y() + cube.height() * 0.5F,
                cube.z() + cube.depth() * 0.5F
        ).mulPosition(transform);
        return new Vec3(center.x, center.y, center.z);
    }

    public record PrintingCube(int boneIndex, ICube cube, Vec3 modelCenter) {}

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

}
