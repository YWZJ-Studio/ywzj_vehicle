package org.ywzj.vehicle.vehicle.part;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.client.screen.DecorationSettingsScreen;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.sync.PartUnitSyncData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;

public class DecorationUnit extends PartUnit<PartUnitData> {

    public String decorationDisplayId = "";
    public String baseBoneName = "";
    public float scale;
    public float selfXRot;
    public float selfYRot;
    public float selfZRot;
    public Vec3 offsetFromBone;
    public Vec3 offsetFromVehicle;
    public Quaternionf rotation;
    public boolean setting;

    public DecorationUnit(int index, AbstractVehicle vehicle, PartUnitData data) {
        super(index, vehicle, data);
        this.syncData = new PartUnitSyncData(this, 20);
        OBB obb = new OBB(vehicle.position().toVector3f(), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf());
        this.partCubeOBBs = new ArrayList<>();
        this.partCubeOBBs.add(new VehicleCubeOBB(obb));
    }

    public void update(DecorationAction message) {
        decorationDisplayId = message.decorationDisplayId;
        baseBoneName = message.baseBoneName;
        scale = message.scale;
        selfXRot = message.selfXRot;
        selfYRot = message.selfYRot;
        selfZRot = message.selfZRot;
        offsetFromBone = message.offsetFromBone;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        if (decorationDisplayId != null) {
            tag.putString("decorationDisplayId", decorationDisplayId);
        }
        if (baseBoneName != null) {
            tag.putString("baseBoneName", baseBoneName);
        }
        tag.putFloat("x", (float) offsetFromBone.x);
        tag.putFloat("y", (float) offsetFromBone.y);
        tag.putFloat("z", (float) offsetFromBone.z);
        tag.putFloat("scale", scale);
        tag.putFloat("selfXRot", selfXRot);
        tag.putFloat("selfYRot", selfYRot);
        tag.putFloat("selfZRot", selfZRot);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        if (nbt.contains("decorationDisplayId")) {
            decorationDisplayId = nbt.getString("decorationDisplayId");
        }
        if (nbt.contains("baseBoneName")) {
            baseBoneName = nbt.getString("baseBoneName");
        }
        offsetFromBone = new Vec3(nbt.getFloat("x"), nbt.getFloat("y"), nbt.getFloat("z"));
        scale = nbt.getFloat("scale");
        selfXRot = nbt.getFloat("selfXRot");
        selfYRot = nbt.getFloat("selfYRot");
        selfZRot = nbt.getFloat("selfZRot");
    }

    @Override
    public void tick() {
        super.tick();
        OBB obb = partCubeOBBs.get(0).obb();
        if (offsetFromVehicle != null && rotation != null) {
            obb.setCenter(vehicle.relativeRotPos(vehicle.position().add(offsetFromVehicle), false).toVector3f());
            obb.setRotation(vehicle.rotYXZ().mul(rotation));
        } else {
            obb.setCenter(vehicle.position().toVector3f());
        }
    }

    public boolean onInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide() && hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown()) {
            openScreen();
        }
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void openScreen() {
        Minecraft.getInstance().setScreen(new DecorationSettingsScreen(this));
    }

}
