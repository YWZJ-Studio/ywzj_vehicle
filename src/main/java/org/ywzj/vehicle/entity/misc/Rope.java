package org.ywzj.vehicle.entity.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Rope extends Entity {

    public boolean falling = true;
    public double partLength = 1;
    public RopeNode fixedRopeNode;
    public List<RopeNode> ropeNodes = new ArrayList<>();

    public Rope(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public void tick() {
        super.tick();
        if (fixedRopeNode == null) {
            fixedRopeNode = new RopeNode(this.position());
            fixedRopeNode.fixed = true;
            ropeNodes.add(fixedRopeNode);
        }
        fixedRopeNode.lastPos = fixedRopeNode.pos;
        fixedRopeNode.pos = this.position();

        if (falling) {
            RopeNode lastNode = ropeNodes.get(ropeNodes.size() - 1);
            if (level().getBlockState(BlockPos.containing(lastNode.pos.relative(Direction.DOWN, 0.5))).isAir()) {
                if (ropeNodes.size() < 20) { // 最大长度
                    Vec3 nextPos = lastNode.pos.add(0, -partLength, 0);
                    ropeNodes.add(new RopeNode(nextPos));
                }
            } else {
                falling = false;
            }
        }
        tickPhysics();
    }

    private void tickPhysics() {
        double gravity = -0.05f;
        double friction = 0.98;

        // 绳节点自身运动
        for (RopeNode node : ropeNodes) {
            if (node.fixed) {
                continue;
            }
            Vec3 velocity = node.pos.subtract(node.lastPos).scale(friction);
            node.lastPos = node.pos;
            node.pos = node.pos.add(velocity).add(0, gravity, 0);
        }
        // 绳节点受前一个绳节点的影响运动
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < ropeNodes.size() - 1; j++) {
                RopeNode n1 = ropeNodes.get(j);
                RopeNode n2 = ropeNodes.get(j + 1);
                double currentDist = n1.pos.distanceTo(n2.pos);
                double error = Math.abs(currentDist - partLength);
                if (currentDist > 0) {
                    Vec3 changeDir = n1.pos.subtract(n2.pos).normalize();
                    Vec3 offset = changeDir.scale(error * 0.5);
                    if (!n1.fixed) n1.pos = n1.pos.subtract(offset);
                    if (!n2.fixed) n2.pos = n2.pos.add(offset);
                }
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {}

    public static class RopeNode {

        public Vec3 pos;
        public Vec3 lastPos;
        public boolean fixed;

        public RopeNode(Vec3 pos) {
            this.pos = pos;
            this.lastPos = pos;
        }

    }

}
