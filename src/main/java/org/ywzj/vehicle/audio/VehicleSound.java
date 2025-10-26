package org.ywzj.vehicle.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VehicleSound extends SimpleSoundInstance implements TickableSoundInstance {

    private final Integer fadeTicks;
    private final Integer entityId;
    private Entity entity;
    private final double scale;
    private boolean isPlaying;
    private boolean isFadeOut;
    private boolean fadeIn;
    private boolean fadeOut;
    private Integer fadeInTick = 0;

    public VehicleSound(SoundEvent event, float volume, float pitch, boolean loop, int fadeTicks, boolean fadeIn, boolean fadeOut, int entityId) {
        super(event, SoundSource.PLAYERS, volume, pitch, SoundInstance.createUnseededRandom(), 0, 0, 0);
        this.entityId = entityId;
        updateRelativePos();
        this.volume = fadeIn ? 0.0001f : volume;
        this.fadeTicks = fadeTicks;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.scale = 1 / volume;
        this.looping = loop;
        this.isPlaying = true;
        this.isFadeOut = false;
    }

    public void play() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().getSoundManager().play(this));
    }

    public void stop() {
        if (!looping || !fadeOut) {
            isPlaying = false;
        }
        isFadeOut = true;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    @Override
    public boolean isStopped() {
        return !isPlaying;
    }

    @Override
    public void tick() {
        updateRelativePos();
        if (isFadeOut) {
            volume = volume * (float) Math.pow(0.001f, 1 / (double) fadeTicks);
            if (volume <= 0.001f) {
                isPlaying = false;
            }
        } else if (fadeIn) {
            volume = (float) Math.max(0.0001f, Math.log(fadeInTick + 1) / Math.log(fadeTicks + 1) * 1f);
            fadeInTick += 1;
            if (volume == 1f) {
                fadeIn = false;
            }
        }
    }

    private void updateRelativePos() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (entity == null) {
            entity = player.level().getEntity(entityId);
            return;
        }
        if (entity.isRemoved()) {
            stop();
            return;
        }
        Vec3 simulatedPos;
        if (entity.equals(player.getVehicle())) {
            simulatedPos = calRelativePos(entity.position(), Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), 0.7);
        } else {
            simulatedPos = calRelativePos(entity.position(), player.position(), scale);
        }
        this.x = simulatedPos.x;
        this.y = simulatedPos.y;
        this.z = simulatedPos.z;
    }

    public Vec3 calRelativePos(Vec3 soundPos, Vec3 targetPos, double scale) {
        double dX = soundPos.x - targetPos.x;
        double dY = soundPos.y - targetPos.y;
        double dZ = soundPos.z - targetPos.z;
        double xC = targetPos.x + dX * scale;
        double yC = targetPos.y + dY * scale;
        double zC = targetPos.z + dZ * scale;
        return new Vec3(xC, yC, zC);
    }

}
