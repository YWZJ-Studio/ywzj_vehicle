package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class ServerSoundEvent {

    public int entityId;
    public String soundName;
    public Vec3 offset;
    public float volume;
    public float distance;
    public float pitch;
    public int fadeTicks;
    public boolean fadeIn;
    public boolean fadeOut;
    public boolean on;

    public ServerSoundEvent() {}

    public ServerSoundEvent(int entityId, String soundName, Vec3 offset, float volume, float distance, float pitch, int fadeTicks, boolean fadeIn, boolean fadeOut, boolean on) {
        this.entityId = entityId;
        this.soundName = soundName;
        this.offset = offset;
        this.volume = volume;
        this.distance = distance;
        this.pitch = pitch;
        this.fadeTicks = fadeTicks;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.on = on;
    }

    public static ServerSoundEvent decode(FriendlyByteBuf buf) {
        ServerSoundEvent data = new ServerSoundEvent();
        data.entityId = buf.readInt();
        data.soundName = buf.readUtf();
        data.offset = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        data.volume = buf.readFloat();
        data.distance = buf.readFloat();
        data.pitch = buf.readFloat();
        data.fadeTicks = buf.readVarInt();
        data.fadeIn = buf.readBoolean();
        data.fadeOut = buf.readBoolean();
        data.on = buf.readBoolean();
        return data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(soundName);
        buf.writeDouble(offset.x);
        buf.writeDouble(offset.y);
        buf.writeDouble(offset.z);
        buf.writeFloat(volume);
        buf.writeFloat(distance);
        buf.writeFloat(pitch);
        buf.writeVarInt(fadeTicks);
        buf.writeBoolean(fadeIn);
        buf.writeBoolean(fadeOut);
        buf.writeBoolean(on);
    }

}
