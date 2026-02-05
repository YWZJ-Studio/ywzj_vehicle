package org.ywzj.vehicle.custom.weapon.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.ywzj.vehicle.all.AllItems;

public class BaseVehicleWeaponData {

    private ResourceLocation weaponId;

    @SerializedName("name")
    private String name = "vehicle.weapon.unknown";

    @SerializedName("velocity")
    private float velocity = 10f;

    @SerializedName("inaccuracy")
    private float inaccuracy = 0.5f;

    @SerializedName("damage")
    private float damage = 5.0f;

    @SerializedName("headshot_multiplier")
    private float headshotMultiplier = 1.5f;

    @SerializedName("recoil")
    private float recoil = 0f;

    @SerializedName("shoot_interval")
    private long shootInterval = 100;

    @SerializedName("max_capacity")
    private int maxCapacity = 64;

    @SerializedName("reload")
    private Reload reload = new Reload(60, Ingredient.of(AllItems.AMMO_AUTO_CANNON.get()));

    @SerializedName("independent")
    public boolean independent;

    public static class Reload {

        @SerializedName("time")
        private int time;

        @SerializedName("ammo")
        private Ingredient ammo;

        public Reload(int time, Ingredient ammo) {
            this.time = time;
            this.ammo = ammo;
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public Ingredient getAmmo() {
            return ammo;
        }

        public void setAmmo(Ingredient ammo) {
            this.ammo = ammo;
        }

        public boolean isAmmo(ItemStack stack) {
            return ammo.test(stack);
        }

    }

    public ResourceLocation getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(ResourceLocation weaponId) {
        this.weaponId = weaponId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getVelocity() {
        return velocity;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public float getInaccuracy() {
        return inaccuracy;
    }

    public void setInaccuracy(float inaccuracy) {
        this.inaccuracy = inaccuracy;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getHeadshotMultiplier() {
        return headshotMultiplier;
    }

    public void setHeadshotMultiplier(float headshotMultiplier) {
        this.headshotMultiplier = headshotMultiplier;
    }

    public float getRecoil() {
        return recoil;
    }

    public void setRecoil(float recoil) {
        this.recoil = recoil;
    }

    public long getShootInterval() {
        return shootInterval;
    }

    public void setShootInterval(long shootInterval) {
        this.shootInterval = shootInterval;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Reload getReload() {
        return reload;
    }

    public void setReload(Reload reload) {
        this.reload = reload;
    }

}
