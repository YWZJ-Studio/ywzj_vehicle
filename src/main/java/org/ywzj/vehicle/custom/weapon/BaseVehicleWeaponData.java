package org.ywzj.vehicle.custom.weapon;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.ywzj.vehicle.all.AllItems;

public class BaseVehicleWeaponData {

    @SerializedName("name")
    private String name = "vehicle.weapon.unknown";

    @SerializedName("damage")
    private float damage = 5.0f;

    @SerializedName("headshot_multiplier")
    private float headshotMultiplier = 1.5f;

    @SerializedName("shoot_interval")
    private long shootInterval = 100;

    @SerializedName("max_capacity")
    private int maxCapacity = 64;

    @SerializedName("reload")
    private Reload reload = new Reload(60, Ingredient.of(AllItems.AMMO_AUTO_CANNON.get()));

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
