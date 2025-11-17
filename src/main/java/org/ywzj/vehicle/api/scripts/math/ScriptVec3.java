package org.ywzj.vehicle.api.scripts.math;

import net.minecraft.world.phys.Vec3;

/**
 * 原版Vec3的脚本接口封装
 */
public final class ScriptVec3 {
    public double x;
    public double y;
    public double z;

    public ScriptVec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static ScriptVec3 of(Vec3 v) {
        if (v == null) return new ScriptVec3(0, 0, 0);
        return new ScriptVec3(v.x, v.y, v.z);
    }

    public ScriptVec3 set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public ScriptVec3 set(ScriptVec3 o) {
        return set(o.x, o.y, o.z);
    }

    public ScriptVec3 copy() {
        return new ScriptVec3(x, y, z);
    }

    public ScriptVec3 add(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
        return this;
    }

    public ScriptVec3 add(ScriptVec3 o) {
        return add(o.x, o.y, o.z);
    }

    public ScriptVec3 sub(double dx, double dy, double dz) {
        this.x -= dx;
        this.y -= dy;
        this.z -= dz;
        return this;
    }

    public ScriptVec3 sub(ScriptVec3 o) {
        return sub(o.x, o.y, o.z);
    }

    public ScriptVec3 scale(double s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
        return this;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double dot(ScriptVec3 o) {
        return this.x * o.x + this.y * o.y + this.z * o.z;
    }

    public ScriptVec3 cross(ScriptVec3 o) {
        double nx = this.y * o.z - this.z * o.y;
        double ny = this.z * o.x - this.x * o.z;
        double nz = this.x * o.y - this.y * o.x;
        return set(nx, ny, nz);
    }

    public ScriptVec3 normalize() {
        double len = length();
        if (len < 1.0E-4) {
            return set(0.0, 0.0, 0.0);
        }
        return scale(1.0 / len);
    }

    @Override
    public String toString() {
        return "Vec3{" + x + "," + y + "," + z + "}";
    }
}
