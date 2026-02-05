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

    @SerializedName("damage_falloff")
    private DamageFalloff damageFalloff = new DamageFalloff();

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

    /**
     * Damage falloff configuration for distance-based damage reduction.
     * Supports multiple falloff models: linear, exponential, and step-based.
     */
    public static class DamageFalloff {

        /**
         * Falloff model type.
         * - NONE: No damage falloff (constant damage at all ranges)
         * - LINEAR: Linear interpolation between start and end distances
         * - EXPONENTIAL: Exponential decay based on half-distance
         * - STEP: Step-based falloff with multiple range brackets
         */
        @SerializedName("type")
        private FalloffType type = FalloffType.NONE;

        /**
         * Distance where damage falloff begins (in blocks).
         * Damage is 100% before this distance.
         */
        @SerializedName("start_distance")
        private double startDistance = 0.0;

        /**
         * Distance where damage reaches minimum multiplier (in blocks).
         * Only used for LINEAR and EXPONENTIAL types.
         */
        @SerializedName("end_distance")
        private double endDistance = 100.0;

        /**
         * Minimum damage multiplier (0.0 to 1.0).
         * Damage will never fall below this percentage.
         */
        @SerializedName("min_multiplier")
        private float minMultiplier = 0.1f;

        /**
         * Half-distance for exponential falloff (in blocks).
         * Distance at which damage is reduced to 50%.
         * Only used for EXPONENTIAL type.
         */
        @SerializedName("half_distance")
        private double halfDistance = 50.0;

        /**
         * Step-based falloff ranges.
         * Each step defines a distance range and damage multiplier.
         * Only used for STEP type.
         * Example: [{"distance": 50, "multiplier": 0.8}, {"distance": 100, "multiplier": 0.5}]
         */
        @SerializedName("steps")
        private java.util.List<FalloffStep> steps = new java.util.ArrayList<>();

        public enum FalloffType {
            NONE,        // No falloff
            LINEAR,      // Linear interpolation
            EXPONENTIAL, // Exponential decay
            STEP         // Step-based ranges
        }

        public static class FalloffStep {
            @SerializedName("distance")
            private double distance;

            @SerializedName("multiplier")
            private float multiplier;

            public FalloffStep() {}

            public FalloffStep(double distance, float multiplier) {
                this.distance = distance;
                this.multiplier = multiplier;
            }

            public double getDistance() {
                return distance;
            }

            public float getMultiplier() {
                return multiplier;
            }
        }

        /**
         * Calculates damage multiplier based on distance traveled.
         * 
         * @param distance Distance from bullet spawn point to hit location
         * @return Damage multiplier (0.0 to 1.0)
         */
        public float calculateMultiplier(double distance) {
            return switch (type) {
                case NONE -> 1.0f;
                case LINEAR -> calculateLinearFalloff(distance);
                case EXPONENTIAL -> calculateExponentialFalloff(distance);
                case STEP -> calculateStepFalloff(distance);
            };
        }

        /**
         * Linear falloff: damage decreases linearly between start and end distances.
         */
        private float calculateLinearFalloff(double distance) {
            if (distance <= startDistance) {
                return 1.0f;
            }
            if (distance >= endDistance) {
                return minMultiplier;
            }
            
            // Linear interpolation
            double range = endDistance - startDistance;
            double progress = (distance - startDistance) / range;
            return (float) (1.0 - progress * (1.0 - minMultiplier));
        }

        /**
         * Exponential falloff: damage decreases exponentially based on half-distance.
         * Formula: multiplier = max(minMultiplier, 0.5^((distance - startDistance) / halfDistance))
         */
        private float calculateExponentialFalloff(double distance) {
            if (distance <= startDistance) {
                return 1.0f;
            }
            
            double effectiveDistance = distance - startDistance;
            double exponent = effectiveDistance / halfDistance;
            float multiplier = (float) Math.pow(0.5, exponent);
            
            return Math.max(minMultiplier, multiplier);
        }

        /**
         * Step-based falloff: damage changes at specific distance thresholds.
         */
        private float calculateStepFalloff(double distance) {
            if (steps.isEmpty()) {
                return 1.0f;
            }
            
            // Find the appropriate step
            float currentMultiplier = 1.0f;
            for (FalloffStep step : steps) {
                if (distance >= step.getDistance()) {
                    currentMultiplier = step.getMultiplier();
                } else {
                    break;
                }
            }
            
            return Math.max(minMultiplier, currentMultiplier);
        }

        // Getters and setters
        public FalloffType getType() {
            return type;
        }

        public void setType(FalloffType type) {
            this.type = type;
        }

        public double getStartDistance() {
            return startDistance;
        }

        public void setStartDistance(double startDistance) {
            this.startDistance = startDistance;
        }

        public double getEndDistance() {
            return endDistance;
        }

        public void setEndDistance(double endDistance) {
            this.endDistance = endDistance;
        }

        public float getMinMultiplier() {
            return minMultiplier;
        }

        public void setMinMultiplier(float minMultiplier) {
            this.minMultiplier = minMultiplier;
        }

        public double getHalfDistance() {
            return halfDistance;
        }

        public void setHalfDistance(double halfDistance) {
            this.halfDistance = halfDistance;
        }

        public java.util.List<FalloffStep> getSteps() {
            return steps;
        }

        public void setSteps(java.util.List<FalloffStep> steps) {
            this.steps = steps;
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

    public DamageFalloff getDamageFalloff() {
        return damageFalloff;
    }

    public void setDamageFalloff(DamageFalloff damageFalloff) {
        this.damageFalloff = damageFalloff;
    }

}
