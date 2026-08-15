package org.ywzj.vehicle.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.event.HitVehicleEvent;
import org.ywzj.vehicle.client.gui.VehicleHitIndicatorOverlay;
import org.ywzj.vehicle.client.particle.BulletHoleParticle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.particle.BulletHoleOption;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class ServerHitVehicleEvent {

    public UUID shooterUuid;
    public int entityId;
    public Vec3 hitPosition;
    public Vec3 hitVector;
    public float caliber;
    public float damage;
    public Component message;

    public ServerHitVehicleEvent() {}

    public ServerHitVehicleEvent(HitVehicleEvent hitVehicleEvent) {
        this.shooterUuid = hitVehicleEvent.shooterUuid;
        this.entityId = hitVehicleEvent.entityId;
        this.hitPosition = hitVehicleEvent.hitPosition;
        this.hitVector = hitVehicleEvent.hitVector;
        this.caliber = hitVehicleEvent.caliber;
        this.damage = hitVehicleEvent.damage;
        this.message = hitVehicleEvent.message;
    }

    public static ServerHitVehicleEvent decode(FriendlyByteBuf buf) {
        ServerHitVehicleEvent serverHitVehicleEvent = new ServerHitVehicleEvent();
        serverHitVehicleEvent.shooterUuid = buf.readUUID();
        serverHitVehicleEvent.entityId = buf.readInt();
        serverHitVehicleEvent.hitPosition = new Vec3(buf.readVector3f());
        serverHitVehicleEvent.hitVector = new Vec3(buf.readVector3f());
        serverHitVehicleEvent.caliber = buf.readFloat();
        serverHitVehicleEvent.damage = buf.readFloat();
        serverHitVehicleEvent.message = buf.readComponent();
        return serverHitVehicleEvent;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(shooterUuid);
        buf.writeInt(entityId);
        buf.writeVector3f(hitPosition.toVector3f());
        buf.writeVector3f(hitVector.toVector3f());
        buf.writeFloat(caliber);
        buf.writeFloat(damage);
        buf.writeComponent(message);
    }

    public static void onServerMessageReceived(ServerHitVehicleEvent message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().setPacketHandled(true);
        if (!AllConfigs.common.hitIndicator.get()) {
            return;
        }
        ctxSupplier.get().enqueueWork(() -> {
            if (message.shooterUuid.equals(LocalVehiclePlayer.instance.getPlayer().getUUID())) {
                VehicleHitIndicatorOverlay.lastHitTime = System.currentTimeMillis();
                if (!VehicleHitIndicatorOverlay.events.isEmpty() && VehicleHitIndicatorOverlay.events.get(0).entityId != message.entityId) {
                    VehicleHitIndicatorOverlay.events.clear();
                }
                VehicleHitIndicatorOverlay.events.add(message);
                if (VehicleHitIndicatorOverlay.events.size() > 128) {
                    VehicleHitIndicatorOverlay.events.remove(0);
                }
            }
            if (LocalVehiclePlayer.instance.getPlayer().level().getEntity(message.entityId) instanceof AbstractVehicle vehicle) {
                Vec3 start = message.hitPosition;
                Vec3 end = start.add(message.hitVector.normalize().scale(3));
                Optional<VehicleDisplay<?, ?>> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId());
                if (displayOptional.isEmpty()) {
                    return;
                }
                VehicleDisplay<?, ?> display = displayOptional.get();
                if (display.getModel() == null) {
                    return;
                }
                if (!display.getModel().hasBakedModel()) {
                    return;
                }
                VectorUtil.HitBone hitBone = VectorUtil.hitBone(vehicle, start, end, new Vec3(0, 1, 0));
                if (hitBone != null) {
                    Vec3 hitPos = hitBone.position();
                    vehicle.level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F),
                            true, hitPos.x, hitPos.y, hitPos.z,
                            0, 0, 0);
                    BulletHoleOption option = new BulletHoleOption(
                            Direction.UP, BlockPos.containing(message.hitPosition),
                            1.0F, 0.0F, 0.0F, message.caliber);
                    option.withBakedBone(message.entityId, hitBone.attachmentBoneIndex(), hitBone.offset(), hitBone.rotation());
                    Particle particle = Minecraft.getInstance().particleEngine.createParticle(option,
                            message.hitPosition.x, message.hitPosition.y, message.hitPosition.z,
                            0, 0, 0);
                    if (particle instanceof BulletHoleParticle bulletHoleParticle) {
                        vehicle.getBulletHoleParticles().add(bulletHoleParticle);
                    }
                }
            }
        });
    }

}
