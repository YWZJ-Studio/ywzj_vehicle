package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.YwzjVehicle;

public class ServerSoundEvent implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ServerSoundEvent> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ServerSoundEvent::decode);
    public static final CustomPacketPayload.Type<ServerSoundEvent> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "sound_event"));
    public int entityId;
    public String soundName;
    public float volume;
    public boolean on;

    public ServerSoundEvent() {}

    public ServerSoundEvent(int entityId, String soundName, float volume, boolean on) {
        this.entityId = entityId;
        this.soundName = soundName;
        this.volume = volume;
        this.on = on;
    }

    public ServerSoundEvent(int entityId, String soundName, boolean on) {
        this.entityId = entityId;
        this.soundName = soundName;
        this.volume = 1f;
        this.on = on;
    }

    public static ServerSoundEvent decode(FriendlyByteBuf buf) {
        ServerSoundEvent data = new ServerSoundEvent();
        data.entityId = buf.readInt();
        data.soundName = buf.readUtf();
        data.volume = buf.readFloat();
        data.on = buf.readBoolean();
        return data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(soundName);
        buf.writeFloat(volume);
        buf.writeBoolean(on);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
