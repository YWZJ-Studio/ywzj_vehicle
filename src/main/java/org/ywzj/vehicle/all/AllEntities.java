package org.ywzj.vehicle.all;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.misc.FakePlayer;
import org.ywzj.vehicle.entity.vehicle.*;
import org.ywzj.vehicle.entity.weapon.*;

public class AllEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YwzjVehicle.MOD_ID);

    public static final RegistryObject<EntityType<BulletEntity>> BULLET = ENTITIES.register("bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .sized(0.0625F, 0.0625F)
                    .clientTrackingRange(16)
                    .updateInterval(5)
                    .setShouldReceiveVelocityUpdates(false)
                    .setCustomClientFactory(BulletEntity::new)
                    .build("bullet"));

    public static final RegistryObject<EntityType<RocketEntity>> ROCKET = ENTITIES.register("rocket",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .sized(0.0625F, 0.0625F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .setCustomClientFactory(RocketEntity::new)
                    .build("rocket"));

    public static final RegistryObject<EntityType<AerialBombEntity>> AERIAL_BOMB = ENTITIES.register("aerial_bomb",
            () -> EntityType.Builder.<AerialBombEntity>of(AerialBombEntity::new, MobCategory.MISC)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .sized(1F, 1F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .setCustomClientFactory(AerialBombEntity::new)
                    .build("aerial_bomb"));

    public static final RegistryObject<EntityType<MissileEntity>> MISSILE = ENTITIES.register("missile",
            () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .sized(0.0625F, 0.0625F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .setCustomClientFactory(MissileEntity::new)
                    .build("missile"));

    public static final RegistryObject<EntityType<SmokeGrenadeEntity>> SMOKE_GRENADE = ENTITIES.register("smoke_grenade",
            () -> EntityType.Builder.<SmokeGrenadeEntity>of(SmokeGrenadeEntity::new, MobCategory.MISC)
            .setShouldReceiveVelocityUpdates(true)
            .setTrackingRange(16)
            .setUpdateInterval(1)
            .setCustomClientFactory(SmokeGrenadeEntity::new)
            .sized(0.3f, 0.3f)
            .noSave()
            .fireImmune()
            .build("smoke_grenade"));

    public static final RegistryObject<EntityType<DecoyFlareEntity>> DECOY_FLARE = ENTITIES.register("decoy_flare",
            () -> EntityType.Builder.<DecoyFlareEntity>of(DecoyFlareEntity::new, MobCategory.MISC)
                    .noSave()
                    .fireImmune()
                    .sized(16, 16)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .setCustomClientFactory(DecoyFlareEntity::new)
                    .build("decoy_flare"));

    public static final RegistryObject<EntityType<FakePlayer>> FAKE_PLAYER = ENTITIES.register("fake_player",
            () -> EntityType.Builder.of(FakePlayer::new, MobCategory.CREATURE).sized(0.6f, 1.8f)
                    .clientTrackingRange(16)
                    .build("fake_player"));

    public static final RegistryObject<EntityType<CommonWheeledVehicle>> WHEELED_VEHICLE = ENTITIES.register("common_wheeled_vehicle",
            () -> CommonWheeledVehicle.TYPE);

    public static final RegistryObject<EntityType<DumpTruck>> DUMP_TRUCK = registerVehicle("dump_truck", DumpTruck::new);

    public static final RegistryObject<EntityType<Lav150>> LAV150 = registerVehicle("lav150", Lav150::new);

    public static final RegistryObject<EntityType<Ztl11>> ZTL11 = registerVehicle("ztl11", Ztl11::new);

    public static final RegistryObject<EntityType<Ztz99a>> ZTZ99A = registerVehicle("ztz99a", Ztz99a::new);

    public static final RegistryObject<EntityType<Z10>> Z10 = registerVehicle("z10", Z10::new);

    public static final RegistryObject<EntityType<Mi24>> MI24 = registerVehicle("mi24", Mi24::new);

    public static final RegistryObject<EntityType<Ka50>> KA50 = registerVehicle("ka50", Ka50::new);

    public static final RegistryObject<EntityType<Motorcycle>> MOTORCYCLE = registerVehicle("motorcycle", Motorcycle::new);

    public static final RegistryObject<EntityType<Hiace>> HIACE = registerVehicle("hiace", Hiace::new);

    public static final RegistryObject<EntityType<Quadcopter>> QUADCOPTER = registerVehicle("quadcopter", Quadcopter::new);

    public static final RegistryObject<EntityType<M1a2>> M1A2 = registerVehicle("m1a2", M1a2::new);

    public static final RegistryObject<EntityType<Cssa5>> CSSA5 = registerVehicle("cssa5", Cssa5::new);

    public static <T extends AbstractVehicle> RegistryObject<EntityType<T>> registerVehicle(
            String name, EntityType.EntityFactory<T> factory
    ) {
        RegistryObject<EntityType<T>> entityTypeRegistryObject = ENTITIES.register(name, () -> EntityType.Builder
                .of(factory, MobCategory.MISC)
                .sized(1f, 1f)
                .updateInterval(1)
                .clientTrackingRange(16)
                .build(name));
        AllTabs.VEHICLE_ENTITY_IDS.add(YwzjVehicle.modLoc(name).toString());
        return entityTypeRegistryObject;
    }

    @SubscribeEvent
    public static void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
        event.put(AllEntities.FAKE_PLAYER.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0D).build());
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        eventBus.register(AllEntities.class);
    }

}
