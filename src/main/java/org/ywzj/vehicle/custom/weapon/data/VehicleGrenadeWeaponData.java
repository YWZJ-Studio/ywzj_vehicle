package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.jetbrains.annotations.Nullable;

public class VehicleGrenadeWeaponData extends BaseVehicleWeaponData {

    @SerializedName("grenade")
    private String grenade = "smoke";

    @SerializedName("life_time")
    private int lifeTime = 600;

    @SerializedName("gravity")
    private float gravity = 0.07f;

    @SerializedName("should_bounce")
    private boolean shouldBounce = true;

    @SerializedName("broke_on_ground")
    private boolean brokeOnGround = false;

    @SerializedName("bounce_factor")
    private double bounceFactor = 0.75;

    @SerializedName("hit_damage")
    private float hitDamage = 1.0f;

    @SerializedName("tail_particles")
    private ParticleOptions tailParticles = ParticleTypes.SMOKE;

    @SerializedName("explosion_damage")
    private float explosionDamage = 0;

    @SerializedName("explosion_radius")
    private float explosionRadius = 0;

    @SerializedName("destroy_block")
    private boolean destroyBlock = false;

    public String getGrenade() {
        return grenade;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public float getGravity() {
        return gravity;
    }

    public boolean isShouldBounce() {
        return shouldBounce;
    }

    public boolean isBrokeOnGround() {
        return brokeOnGround;
    }

    public double getBounceFactor() {
        return bounceFactor;
    }

    public float getHitDamage() {
        return hitDamage;
    }

    @Nullable
    public ParticleOptions getTailParticles() {
        return tailParticles;
    }

    public float getExplosionDamage() {
        return explosionDamage;
    }

    public float getExplosionRadius() {
        return explosionRadius;
    }

    public boolean isDestroyBlock() {
        return destroyBlock;
    }

}
