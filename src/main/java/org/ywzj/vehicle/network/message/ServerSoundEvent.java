package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;

public class ServerSoundEvent {

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

}
