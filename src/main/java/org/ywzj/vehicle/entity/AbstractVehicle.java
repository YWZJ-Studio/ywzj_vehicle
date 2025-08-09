package org.ywzj.vehicle.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractVehicle extends Mob {

    public ControlUnit controlUnit = new ControlUnit();
    public List<WeaponUnit> weaponUnits = new ArrayList<>();

    protected AbstractVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public Entity getDriver() {
        return getFirstPassenger();
    }

    public abstract void shoot(int weaponIndex);

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.PLAYER_ATTACK)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    public Vec3 getCameraOffset() {
        return new Vec3(0, 1.5, 0);
    }

    public static class ControlUnit {

        public Player operator;
        public boolean forward;
        public boolean backward;
        public boolean left;
        public boolean right;

        public void setOperator(Player operator) {
            this.operator = operator;
        }

        public void reset() {
            forward = false;
            backward = false;
            left = false;
            right = false;
        }

    }

}
