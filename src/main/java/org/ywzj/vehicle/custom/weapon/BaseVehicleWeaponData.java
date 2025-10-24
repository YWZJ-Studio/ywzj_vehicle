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
    private float headshot = 1.5f;

    @SerializedName("shoot_interval")
    private long shootInterval = 100;

    @SerializedName("max_capacity")
    private int maxCapacity = 64;

    @SerializedName("reload")
    private Reload reload = new Reload();

    public static class Reload {
        @SerializedName("time")
        private int time = 60;

        @SerializedName("ammo")
        private Ingredient ammo = Ingredient.of(AllItems.AMMO_AUTO_CANNON.get());

        public int getTime() {
            return time;
        }

        public Ingredient getIngredient() {
            return ammo;
        }

        public boolean isAmmo(ItemStack stack) {
            return ammo.test(stack);
        }
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public float getDamage() {
        return damage;
    }

    public float getHeadshotMultiplier() {
        return headshot;
    }

    public long getShootInterval() {
        return shootInterval;
    }

    public Reload getReload() {
        return reload;
    }

    public String getName() {
        return name;
    }
}
