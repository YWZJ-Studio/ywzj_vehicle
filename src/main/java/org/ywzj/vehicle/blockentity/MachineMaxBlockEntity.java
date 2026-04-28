package org.ywzj.vehicle.blockentity;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
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
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllBlockEntities;
import org.ywzj.vehicle.client.render.entity.block.MachineMaxBlockRenderer;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.recipe.VehiclePrintingRecipe;

import java.util.*;

public class MachineMaxBlockEntity extends BlockEntity {

    private boolean crafting;
    private boolean hasProduct;
    public float progress;
    public float step;
    public ResourceLocation craftingVehicleId;
    public BaseDisplay vehicleDisplay;
    public BaseVehicleData vehicleData;
    public List<MachineMaxBlockRenderer.BedrockBoneWrapper> bedrockBoneWrappers = new ArrayList<>();
    public MachineMaxBlockRenderer.BedrockBoneWrapper printingBoneWrapper;

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
            if (blockEntity.crafting) {
                if (level.random.nextFloat() < 0.2F) {
                    Vec3 worldPos = pos.getCenter();
                    double x = worldPos.x;
                    double y = worldPos.y + 1D;
                    double z = worldPos.z;
                    double xOffset = (level.random.nextDouble() - 0.5D) * 0.5D;
                    double zOffset = (level.random.nextDouble() - 0.5D) * 0.5D;
                    level.addParticle(
                            ParticleTypes.POOF,
                            x + xOffset, y, z + zOffset,
                            0.0D, 0.1D, 0.0D
                    );
                }
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
        if (tag.contains("craftingVehicleId")) {
            craftingVehicleId = YwzjVehicle.resourceLocation(tag.getString("craftingVehicleId"));
            if (level != null && level.isClientSide()) {
                prepareAnimation();
            }
        } else {
            craftingVehicleId = null;
        }
        if (tag.contains("crafting")) {
            crafting = tag.getBoolean("crafting");
        }
        if (tag.contains("hasProduct")) {
            hasProduct = tag.getBoolean("hasProduct");
        }
        if (tag.contains("progress")) {
            progress = tag.getFloat("progress");
        }
        if (tag.contains("step")) {
            step = tag.getFloat("step");
        }
    }

    private void tickAnimation() {
        int total = bedrockBoneWrappers.size();
        int visibleCount = (int) (progress * total);
        visibleCount = Math.min(visibleCount, total);
        for (int index = 0; index < visibleCount; index++) {
            bedrockBoneWrappers.get(index).appear();
            printingBoneWrapper = bedrockBoneWrappers.get(index);
        }
    }

    private void prepareAnimation() {
        if (bedrockBoneWrappers.isEmpty()) {
            vehicleDisplay = ClientAssetsManager.INSTANCE.getVehicleDisplay(craftingVehicleId).orElse(null);
            if (vehicleDisplay == null) {
                return;
            }
            BedrockModel model = vehicleDisplay.getModel();
            if (model == null) {
                return;
            }
            Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(craftingVehicleId);
            if (!vehicleDataOptional.isPresent()) {
                return;
            }
            vehicleData = vehicleDataOptional.get();
            bedrockBoneWrappers.clear();
            HashSet<BedrockBone> bones = new HashSet<>();
            for (BedrockBone bone : model.getBoneMap().values()) {
                if (bone.parent != null && bone.parent.parent == null) {
                    MachineMaxBlockRenderer.buildBedrockBoneWrappers(bone, bones, bedrockBoneWrappers, null);
                }
            }
            bedrockBoneWrappers.sort(Comparator.comparingDouble(bone -> bone.y));
        }
    }

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
