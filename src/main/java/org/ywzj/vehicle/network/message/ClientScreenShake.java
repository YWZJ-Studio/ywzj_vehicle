package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.client.handler.FirstPersonHandler;

import java.util.function.Supplier;

/**
 * Network packet to trigger screen shake effect on client.
 */
public class ClientScreenShake {
    
    private final float intensity;
    private final long duration;

    public ClientScreenShake(float intensity, long duration) {
        this.intensity = intensity;
        this.duration = duration;
    }

    public ClientScreenShake(FriendlyByteBuf buf) {
        this.intensity = buf.readFloat();
        this.duration = buf.readLong();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(intensity);
        buf.writeLong(duration);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                FirstPersonHandler.triggerScreenShake(intensity, duration);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
