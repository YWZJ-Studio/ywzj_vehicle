package org.ywzj.vehicle.vehicle.part;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HookUnit extends PartUnit<PartUnitData> {

    public static final float MIN_LENGTH = 0.5f;
    public static final float MAX_LENGTH = 16f;
    public static final float LENGTH_STEP = 0.5f;
    public static final double INTERACTION_DISTANCE = 4;
    private static final String PENDING_KEY = "ywzj_vehicle:pending_hook";
    private static final double ENERGY_TRANSFER_RATE = 0.2;
    private static final double DIRECTION_RESPONSE = 0.2;
    private static final double MAX_YAW_STEP = Math.toRadians(12);
    private static final double MAX_TILT_STEP = Math.toRadians(6);
    private static final double SPEED_EPSILON = 1.0E-6;
    private UUID connectedVehicle;
    private int connectedEntityId = -1;
    private String connectedPart = "";
    private String connectionId = "";
    private float length = MIN_LENGTH;

    public HookUnit(int index, AbstractVehicle vehicle, PartUnitData data) {
        super(index, vehicle, data);
        this.syncData.define(SyncDataSerializers.INT, value -> connectedEntityId = value, () -> connectedEntityId, -1);
        this.syncData.define(SyncDataSerializers.STRING, value -> connectedPart = value, () -> connectedPart, "");
        this.syncData.define(SyncDataSerializers.STRING, value -> connectionId = value, () -> connectionId, "");
        this.syncData.define(SyncDataSerializers.FLOAT, this::setLength, this::getLength, MIN_LENGTH);
        this.renderInVehicleStatus = false;
    }

    public boolean isConnected() {
        return !connectionId.isEmpty();
    }

    public boolean isUsable() {
        return !isDetached() && !isDestroyed() && !vehicle.isDestroyed() && !vehicle.isRemoved()
                && structureGroup != null && !partCubeOBBs.isEmpty();
    }

    public float getLength() {
        return length;
    }

    private void setLength(float value) {
        length = Mth.clamp(value, MIN_LENGTH, MAX_LENGTH);
    }

    public Vec3 getAnchorPosition(float partialTick) {
        return worldPosition(getPivotOffset(), partialTick);
    }

    private Vec3 getTowRotationPivot() {
        // 挂点关于载具中心的对称点：前挂点拖拽时绕后端归正，反之亦然。
        Vec3 center = vehicle.position().add(vehicle.centerOffset);
        return center.add(center.subtract(getAnchorPosition(1)));
    }

    public HookUnit getConnectedHook() {
        Entity entity = vehicle.level() instanceof ServerLevel level
                ? (connectedVehicle == null ? null : level.getEntity(connectedVehicle))
                : vehicle.level().getEntity(connectedEntityId);
        if (entity instanceof AbstractVehicle other && other != vehicle) {
            PartUnit<?> part = other.getPartUnit(connectedPart).orElse(null);
            if (part instanceof HookUnit hook && isConnected() && connectionId.equals(hook.connectionId)
                    && id.equals(hook.connectedPart)) {
                boolean reciprocal = vehicle.level().isClientSide()
                        ? hook.connectedEntityId == vehicle.getId() : vehicle.getUUID().equals(hook.connectedVehicle);
                if (reciprocal) {
                    return hook;
                }
            }
        }
        return null;
    }

    public boolean ownsConnection() {
        HookUnit other = getConnectedHook();
        return other != null && vehicle.getUUID().compareTo(other.vehicle.getUUID()) < 0;
    }

    public boolean canInteract(Player player) {
        if (!isUsable() || !player.isAlive() || player.isSpectator() || player.level() != vehicle.level()) {
            return false;
        }
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(INTERACTION_DISTANCE));
        var hit = VectorUtil.hitObbPosition(player, start, end);
        return hit != null && hit.getLeft() == vehicle && VectorUtil.hitPartUnit(vehicle, start, end) == this;
    }

    @Override
    public boolean onInteract(Player player, InteractionHand hand) {
        if (!player.isHolding(Items.CHAIN) && !(isConnected() && player.isShiftKeyDown())) {
            return true;
        }
        if (hand != InteractionHand.MAIN_HAND || vehicle.level().isClientSide() || !canInteract(player)) {
            return false;
        }
        CompoundTag playerData = player.getPersistentData();
        if (isConnected()) {
            if (player.isShiftKeyDown()) {
                disconnect();
                if (!player.getAbilities().instabuild) {
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.CHAIN));
                }
                playerData.remove(PENDING_KEY);
                tell(player, "disconnected");
            } else {
                tell(player, "occupied");
            }
            return false;
        }
        CompoundTag pending = playerData.getCompound(PENDING_KEY);
        HookUnit first = pendingHook(player, pending);
        if (first == this) {
            playerData.remove(PENDING_KEY);
            tell(player, "cancelled");
        } else if (first == null) {
            CompoundTag selection = new CompoundTag();
            selection.putUUID("Vehicle", vehicle.getUUID());
            selection.putString("Part", id);
            selection.putString("Dimension", vehicle.level().dimension().location().toString());
            playerData.put(PENDING_KEY, selection);
            tell(player, "selected");
        } else if (first.vehicle == vehicle) {
            tell(player, "same_vehicle");
        } else if (first.getAnchorPosition(1).distanceTo(getAnchorPosition(1)) > MAX_LENGTH) {
            tell(player, "too_far");
        } else {
            String token = UUID.randomUUID().toString();
            float initialLength = (float) first.getAnchorPosition(1).distanceTo(getAnchorPosition(1));
            connectTo(first, token, initialLength);
            first.connectTo(this, token, initialLength);
            if (!player.getAbilities().instabuild) {
                var chainStack = player.getMainHandItem().is(Items.CHAIN)
                        ? player.getMainHandItem() : player.getOffhandItem();
                chainStack.shrink(1);
            }
            playerData.remove(PENDING_KEY);
            tell(player, "connected");
        }
        return false;
    }

    private static HookUnit pendingHook(Player player, CompoundTag pending) {
        if (!(player.level() instanceof ServerLevel level) || !pending.hasUUID("Vehicle")
                || !level.dimension().location().toString().equals(pending.getString("Dimension"))) {
            return null;
        }
        if (level.getEntity(pending.getUUID("Vehicle")) instanceof AbstractVehicle vehicle
                && vehicle.getPartUnit(pending.getString("Part")).orElse(null) instanceof HookUnit hook
                && hook.isUsable() && !hook.isConnected()) {
            return hook;
        }
        return null;
    }

    private void connectTo(HookUnit other, String token, float length) {
        connectedVehicle = other.vehicle.getUUID();
        connectedEntityId = other.vehicle.getId();
        connectedPart = other.id;
        connectionId = token;
        setLength(length);
        syncData.tick();
    }

    public void adjustLength(Player player, boolean shorten) {
        if (vehicle.level().isClientSide() || !player.isShiftKeyDown() || !canInteract(player)) {
            return;
        }
        HookUnit other = getConnectedHook();
        if (other == null) {
            return;
        }
        setLength(length + (shorten ? -LENGTH_STEP : LENGTH_STEP));
        other.setLength(length);
        if (shorten) {
            enforceLength(other);
        }
        syncData.tick();
        other.syncData.tick();
    }

    public void disconnect() {
        HookUnit other = getConnectedHook();
        clearConnection();
        if (other != null) {
            other.clearConnection();
        }
    }

    private void clearConnection() {
        connectedVehicle = null;
        connectedEntityId = -1;
        connectedPart = "";
        connectionId = "";
        setLength(MIN_LENGTH);
        syncData.tick();
    }

    @Override
    public void tick() {
        if (!vehicle.level().isClientSide() && isConnected()) {
            if (!isUsable()) {
                disconnect();
            } else {
                HookUnit other = getConnectedHook();
                if (other != null) {
                    connectedEntityId = other.vehicle.getId();
                    if (!other.isUsable()) {
                        disconnect();
                    } else {
                        if (ownsConnection()) {
                            other.setLength(length);
                        }
                        tickTow(other);
                    }
                } else if (connectedVehicle != null && ((ServerLevel) vehicle.level()).getEntity(connectedVehicle) != null) {
                    clearConnection();
                } else {
                    // 区块卸载时保留 UUID，等待另一端重新加载。
                    connectedEntityId = -1;
                }
            }
        }
        super.tick();
    }

    @Override
    public void onRemoved() {
        if (!vehicle.level().isClientSide()) {
            if (vehicle.getRemovalReason() != null && vehicle.getRemovalReason().shouldDestroy()) {
                disconnect();
            }
        }
    }

    private void tickTow(HookUnit other) {
        Vec3 velocity = vehicle.getDeltaMovement();
        Vec3 otherVelocity = other.vehicle.getDeltaMovement();
        Vec3 separation = other.getAnchorPosition(1).subtract(getAnchorPosition(1));
        Vec3 nextSeparation = separation.add(otherVelocity.subtract(velocity));
        double tautLength = length - 0.01;
        if (separation.lengthSqr() < tautLength * tautLength
                && nextSeparation.lengthSqr() < tautLength * tautLength) {
            return;
        }

        double distance = separation.length();
        if (distance <= SPEED_EPSILON) {
            return;
        }
        // 各端只处理自身的悬吊姿态；静止或低速端也需要响应挂点拉力。
        alignSuspendedToTow(separation.scale(1 / distance));
        // 低速端不处理动能转移，也不代高速端执行。
        if (velocity.lengthSqr() < otherVelocity.lengthSqr()) {
            return;
        }
        // 拉力沿两端挂点连线
        Vec3 pullDirection = separation.scale(-1 / distance);
        vehicle.physicsEngine.transferEnergyTo(other.vehicle.physicsEngine, pullDirection,
                ENERGY_TRANSFER_RATE, DIRECTION_RESPONSE);
        Vec3 pullVelocity = vehicle.getDeltaMovement();
        if (pullVelocity.lengthSqr() > SPEED_EPSILON * SPEED_EPSILON) {
            other.alignToTow(pullDirection.scale(pullVelocity.length()));
        }
        enforceLength(other);
    }

    private void alignToTow(Vec3 pullVelocity) {
        if (isSuspendedTow(pullVelocity)) {
            return;
        }
        Vec3 arm = getAnchorPosition(1).subtract(getTowRotationPivot()).multiply(1, 0, 1);
        Vec3 horizontalPull = pullVelocity.multiply(1, 0, 1);
        if (arm.lengthSqr() < 1.0E-4 || horizontalPull.lengthSqr() <= SPEED_EPSILON * SPEED_EPSILON) {
            return;
        }
        double angle = Math.atan2(arm.cross(horizontalPull).y, arm.dot(horizontalPull));
        // 低速或长力臂时缓慢归正，避免刚拉紧就以固定角速度甩动车身。
        double response = Math.min(DIRECTION_RESPONSE, horizontalPull.length() / arm.length());
        float yawStep = (float) Mth.clamp(angle * response, -MAX_YAW_STEP, MAX_YAW_STEP);
        // 绕挂点对侧端归正，使受拉挂点朝向实际绳向。
        vehicle.physicsEngine.rotateAround(getTowRotationPivot(), new Quaternionf().rotateY(yawStep));
    }

    private boolean isSuspendedTow(Vec3 pullDirection) {
        return !vehicle.onGround() && !vehicle.isInWater() && !vehicle.physicsEngine.lockCenterRot
                && pullDirection.y > SPEED_EPSILON;
    }

    private void alignSuspendedToTow(Vec3 pullDirection) {
        if (!isSuspendedTow(pullDirection)) {
            return;
        }
        // 让重心逐渐落到受拉挂点下方，以挂点为支点补上俯仰和侧倾。
        Vec3 anchor = getAnchorPosition(1);
        vehicle.physicsEngine.alignDirection(anchor, anchor.subtract(vehicle.physicsEngine.getCenterOfMass()), pullDirection, DIRECTION_RESPONSE, MAX_TILT_STEP);
    }

    private void enforceLength(HookUnit other) {
        // 按逆质量分摊超长位置修正；不直接覆盖速度，避免绕过逐 tick 动能转移。
        vehicle.physicsEngine.limitDistance(other.vehicle.physicsEngine, getAnchorPosition(1), other.getAnchorPosition(1), length);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        if (isConnected() && connectedVehicle != null) {
            tag.putUUID("HookVehicle", connectedVehicle);
            tag.putString("HookPart", connectedPart);
            tag.putString("HookConnection", connectionId);
            tag.putFloat("HookLength", length);
            // 实体 ID 供客户端生成时定位，服务端始终通过 UUID 恢复连接。
            tag.putInt("HookEntityId", connectedEntityId);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        connectedVehicle = tag.hasUUID("HookVehicle") ? tag.getUUID("HookVehicle") : null;
        connectedPart = tag.getString("HookPart");
        connectionId = connectedVehicle == null ? "" : tag.getString("HookConnection");
        connectedEntityId = tag.contains("HookEntityId") ? tag.getInt("HookEntityId") : -1;
        setLength(tag.getFloat("HookLength"));
    }

    private static void tell(Player player, String key) {
        player.displayClientMessage(Component.translatable("tips.hook." + key), true);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, float partialTick) {
        HookUnit other = getConnectedHook();
        if (other == null || !isUsable() || !other.isUsable() || !ownsConnection()) {
            return;
        }
        Vec3 start = getAnchorPosition(partialTick);
        Vec3 end = other.getAnchorPosition(partialTick);
        Quaternionf inverse = vehicle.rotYXZ(partialTick).conjugate();
        Vec3 origin = vehicle.position(partialTick).add(vehicle.centerOffset);
        Vector3f right = new Vector3f(1, 0, 0).rotate(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        inverse.transform(right);
        var builder = buffer.getBuffer(RenderType.entityCutoutNoCull(YwzjVehicle.resourceLocation("minecraft:textures/block/chain.png")));
        int segments = Math.max(1, Mth.ceil(length));
        double sag = Math.min(3, Math.sqrt(Math.max(0, length * length - start.distanceToSqr(end))) * 0.5);
        Vec3 previous = start;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Vec3 next = start.lerp(end, t).add(0, -4 * sag * t * (1 - t), 0);
            Vec3 a = new Vec3(inverse.transform(previous.subtract(origin).toVector3f())).add(vehicle.centerOffset);
            Vec3 b = new Vec3(inverse.transform(next.subtract(origin).toVector3f())).add(vehicle.centerOffset);
            RopeUnit.renderSegment(builder, poseStack.last().pose(), a, b, right, light);
            previous = next;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderTips(GuiGraphics graphics) {
        if (!isUsable()) {
            return;
        }
        List<RenderHelper.TipRow> rows = new ArrayList<>();
        List<Component> hints = new ArrayList<>();
        if (isConnected()) {
            rows.add(new RenderHelper.TipRow(Component.translatable("tips.hook.length",
                    String.format(Locale.ROOT, "%.1f", getLength())), false));
            hints.add(Component.translatable("tips.hook.scroll"));
            hints.add(Component.translatable("tips.hook.disconnect"));
        } else {
            hints.add(Component.translatable("tips.hook.connect"));
        }
        RenderHelper.renderTipsPanel(graphics, getName(), rows, hints);
    }

}
