package org.ywzj.vehicle.all;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.ywzj.vehicle.api.entity.RemoteTickEntity;
import org.ywzj.vehicle.api.entity.TargetObstruction;
import org.ywzj.vehicle.api.event.HitVehicleEvent;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.command.RootCommand;
import org.ywzj.vehicle.entity.misc.RadarMarkerEntity;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.item.FuelTankItem;
import org.ywzj.vehicle.item.VehicleItem;
import org.ywzj.vehicle.mixin.common.ExplosionAccessor;
import org.ywzj.vehicle.network.message.ServerBroadcastEntities;
import org.ywzj.vehicle.network.message.ServerHitVehicleEvent;
import org.ywzj.vehicle.resource.VehiclePackLoader;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.DoorUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AllEvents {

    @EventBusSubscriber
    public static class AllModEvents {

        @SubscribeEvent
        public static void onRegisterCaps(RegisterCapabilitiesEvent event) {
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> {
                        if (stack.getItem() instanceof FuelTankItem) {
                            return new FuelTankItem.FuelTankFluidHandler(
                                    stack,
                                    stack.getMaxDamage(),
                                    AllFluids.FUEL_SOURCE.get(),
                                    stack.getMaxDamage()
                            );
                        }
                        return null;
                    },
                    AllItems.FUEL_TANK.get()
            );
        }

        @SubscribeEvent
        public static void onAddPackFinders(AddPackFindersEvent event) {
            event.addRepositorySource(VehiclePackLoader.INSTANCE);
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            RootCommand.register(event.getDispatcher());
        }

        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (event.getEntity() instanceof LocalPlayer) {
                LocalVehiclePlayer.instance.clear();
            }
        }

        @SubscribeEvent
        public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity().isPassenger() && event.getEntity().getVehicle() instanceof AbstractVehicle) {
                event.getEntity().stopRiding();
            }
        }

        @SubscribeEvent
        public static void onPlayerTeleport(EntityTeleportEvent event) {
            if (event.getEntity() instanceof Player player) {
                if (player.isPassenger() && player.getVehicle() instanceof AbstractVehicle) {
                    player.stopRiding();
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
            AbstractVehicle.Seat seat = LocalVehiclePlayer.instance.seat;
            if (seat != null && !seat.partUnit.getData().passengerCanUseItem()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            ItemStack itemStack = event.getItemStack();
            Player player = event.getEntity();
            if (itemStack.getItem() instanceof VehicleItem vehicleItem) {
                vehicleItem.interactEntity(itemStack, player, event.getTarget(), event.getHand());
            }
            if (event.getTarget() instanceof AbstractVehicle vehicle) {
                Vec3 eyePosition = player.getEyePosition();
                PartUnit<?> partUnit = VectorUtil.hitPartUnit(vehicle, eyePosition, eyePosition.add(player.getLookAngle().scale(4)));
                if (partUnit != null) {
                    if (!partUnit.onInteract(player, event.getHand())) {
                        event.setCanceled(true);
                    }
                    return;
                }
                if (!vehicle.level().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND && !player.isShiftKeyDown()) {
                    // 交互登上载具时，先打开最近的门
                    DoorUnit doorUnit = vehicle.getNearestDoorUnit(player);
                    if (doorUnit != null && !doorUnit.isOn()) {
                        doorUnit.setOn(true);
                        event.setCanceled(true);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onEntityMount(EntityMountEvent event) {
            if (event.isCanceled()) {
                return;
            }
            if (event.getEntityBeingMounted() instanceof AbstractVehicle vehicle && event.getEntityMounting() instanceof LivingEntity livingEntity) {
                if (event.isMounting()) {
                    if (livingEntity instanceof ServerPlayer serverPlayer) {
                        if (AllConfigs.common.checkTeamOnEnterVehicle.get() && serverPlayer.getTeam() != null) {
                            if (vehicle.getPassengers().stream()
                                    .anyMatch(entity -> entity instanceof ServerPlayer passenger
                                            && passenger.getTeam() != null
                                            && !passenger.getTeam().isAlliedTo(serverPlayer.getTeam()))) {
                                event.setCanceled(true);
                            }
                        }
                    }
                } else {
                    if (!event.getLevel().isClientSide()) {
                        // 离开载具时，打开最近的门
                        DoorUnit doorUnit = vehicle.getNearestDoorUnit(livingEntity);
                        if (doorUnit != null && !doorUnit.isOn()) {
                            doorUnit.setOn(true);
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingDamageEvent.Pre event) {
            if (event.getEntity().getVehicle() instanceof AbstractVehicle vehicle) {
                if (vehicle.getDetachedBodyAnchor(event.getEntity()) != null) {
                    event.getEntity().stopRiding();
                    return;
                }
                if (vehicle.protectPassenger) {
                    event.setNewDamage(0);
                }
            }
        }

        @SubscribeEvent
        public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
            Iterator<Entity> iterator = event.getAffectedEntities().iterator();
            while (iterator.hasNext()) {
                Entity entity = iterator.next();
                if (entity instanceof AbstractVehicle) {
                    iterator.remove();
                    Explosion explosion = event.getExplosion();
                    Vec3 explosionPos = explosion.center();
                    float explosionRadius = ((ExplosionAccessor) explosion).getRadius() * 2.0F;
                    if (!entity.ignoreExplosion(explosion)) {
                        double distanceRatio = Math.sqrt(entity.distanceToSqr(explosionPos)) / explosionRadius;
                        if (distanceRatio <= 1.0D) {
                            double dx = entity.getX() - explosionPos.x;
                            double dy = entity.getEyeY() - explosionPos.y;
                            double dz = entity.getZ() - explosionPos.z;
                            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (distance != 0.0D) {
                                double visibilityFactor = Explosion.getSeenPercent(explosionPos, entity);
                                double impactStrength = (1.0D - distanceRatio) * visibilityFactor;
                                float damage = (float) ((int) ((impactStrength * impactStrength + impactStrength) / 2.0D * 7.0D * explosionRadius + 1.0D));
                                entity.hurt(entity.level().damageSources().explosion(explosion), damage);
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onVehicleFire(VehicleFireEvent.Post event) {
            if (event.isClientSide()) {
                event.getWeapon().onClientFire();
                event.getWeapon().getWeaponUnit().onClientFire();
            }
        }

        @SubscribeEvent
        public static void onHitVehicle(HitVehicleEvent event) {
            ServerPlayer serverPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(event.shooterUuid);
            if (serverPlayer == null) {
                return;
            }
            Level level = serverPlayer.level();
            if (level.getEntity(event.entityId) instanceof AbstractVehicle vehicle) {
                List<Entity> players = level.getEntities(vehicle,
                        AABB.ofSize(vehicle.position(), 256, 256, 256),
                        entity -> entity instanceof ServerPlayer && entity != serverPlayer);
                players.add(serverPlayer);
                players.forEach(player -> PacketDistributor.sendToPlayer((ServerPlayer) player, new ServerHitVehicleEvent(event)));
            }
        }

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post event) {
            MinecraftServer server = event.getServer();
            for (ServerLevel level : server.getAllLevels()) {
                VehicleExplosion.tick(level);
            }
            if (server.getTickCount() % AllConfigs.server.serverBroadcastEntitiesInterval.get() == 0) {
                for (ServerLevel level : server.getAllLevels()) {
                    // 对符合条件的超视距实体进行广播
                    // 目前仅供雷达使用
                    List<Entity> entities = new ArrayList<>();
                    for (Entity target : level.getEntities().getAll()) {
                        if (!target.isAlive()) {
                            continue;
                        }
                        if ((target instanceof AbstractVehicle vehicle && !vehicle.isDestroyed())
                                || target instanceof MissileEntity
                                || target instanceof TargetObstruction
                                || target instanceof RadarMarkerEntity
                                || AllConfigs.serverBroadcastEntityWhitelist.stream()
                                .anyMatch(entityType -> BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().matches(entityType))) {
                            entities.add(target);
                        }
                    }
                    if (entities.isEmpty()) {
                        continue;
                    }
                    ServerBroadcastEntities serverBroadcastEntities = new ServerBroadcastEntities();
                    serverBroadcastEntities.entities = new ArrayList<>();
                    for (Entity target : entities) {
                        CompoundTag data = new CompoundTag();
                        if (target instanceof RemoteTickEntity targetRemoteTickEntity) {
                            targetRemoteTickEntity.writeData(data);
                        }
                        serverBroadcastEntities.entities.add(new ServerBroadcastEntities.BroadcastEntity(target.getId(),
                                EntityType.getKey(target.getType()),
                                target.position(),
                                target.getDeltaMovement(),
                                data));
                    }
                    for (ServerPlayer player : level.players()) {
                        Entity vehicle = player.getVehicle();
                        if (vehicle instanceof AbstractVehicle) {
                            PacketDistributor.sendToPlayer(player, serverBroadcastEntities);
                        }
                    }
                }
            }
        }

    }

}
