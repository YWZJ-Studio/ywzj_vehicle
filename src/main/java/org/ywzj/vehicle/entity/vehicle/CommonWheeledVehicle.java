package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.api.entity.ICustomVehicle;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.vehicle.PhysicsEngine;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;


/**
 * 数据包载具模板，轮式车辆
 */
public class CommonWheeledVehicle extends WheeledVehicle implements ICustomVehicle, IEntityAdditionalSpawnData {

    public static final EntityType<CommonWheeledVehicle> TYPE = EntityType.Builder
            .<CommonWheeledVehicle>of(CommonWheeledVehicle::new, MobCategory.MISC)
            .sized(1f, 1f)
            .updateInterval(1)
            .clientTrackingRange(16)
            .setCustomClientFactory(CommonWheeledVehicle::new)
            .build("common_wheeled_vehicle");

    private ResourceLocation customId = ICustomVehicle.EMPTY_ID;

    public CommonWheeledVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.physicsEngine = new PhysicsEngine(this, mainCubeOBB);
    }

    public CommonWheeledVehicle(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(TYPE, level);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(ICustomVehicle.TAG_VEHICLE_ID, Tag.TAG_STRING)) {
            this.customId = ResourceLocation.tryParse(compound.getString(ICustomVehicle.TAG_VEHICLE_ID));
            if (this.customId != null) {
                this.initData(this.customId);
            } else {
                this.customId = ICustomVehicle.EMPTY_ID;
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString(ICustomVehicle.TAG_VEHICLE_ID, this.customId.toString());
    }

    @Override
    public void initData(ResourceLocation customId) {
        this.customId = customId;
        CommonAssetsManager.vehicleDataManager().getVehicleData(customId).ifPresent(data -> {
            var struct = data.getVehicleStructObbs();
            this.mainCubeOBB = struct.mainCubeOBB();
            this.vehicleOBBs = struct.obbs();
            var weapons = data.createPartUnits(this);
            this.partUnits.addAll(weapons.partUnitMap().values());
            this.seats.addAll(weapons.seats());
        });
    }

    @Override
    public void shoot(int partUnitIndex, List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(ammoSpawnPositions, ammoXRot, ammoYRot, operator);
        }
    }

    @Override
    public ResourceLocation getCustomId() {
        return this.customId;
    }

    @Override
    public void setCustomId(ResourceLocation customId) {
        this.customId = customId;
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.customId);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        ResourceLocation customId = additionalData.readResourceLocation();
        this.initData(customId);
    }

    @NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
