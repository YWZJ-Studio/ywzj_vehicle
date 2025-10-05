package org.ywzj.vehicle.all;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.*;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AllVehicles {

    private final static ConcurrentHashMap<Class<? extends AbstractVehicle>, VehicleType> ALL_VEHICLES = new ConcurrentHashMap<>();

    public static final VehicleType LAV150 = registerVehicle("lav150", Lav150.class);
    public static final VehicleType ZTL11 = registerVehicle("ztl11", Ztl11.class);
    public static final VehicleType ZTZ99A = registerVehicle("ztz99a", Ztz99a.class);
    public static final VehicleType Z10 = registerVehicle("z10", Z10.class);
    public static final VehicleType MI24 = registerVehicle("mi24", Mi24.class);
    public static final VehicleType MOTORCYCLE = registerVehicle("motorcycle", Motorcycle.class);

    public static void register() {}

    public static VehicleType registerVehicle(String name, Class<? extends AbstractVehicle> entityClass) {
        VehicleType vehicleType = new VehicleType(name, entityClass);
        ALL_VEHICLES.put(entityClass, vehicleType);
        YwzjVehicle.LOGGER.info("Vehicle {} registered", name);
        return vehicleType;
    }

    public static VehicleType getVehicleType(Class<? extends AbstractVehicle> entityClass) {
        return ALL_VEHICLES.get(entityClass);
    }

    public static List<VehicleType> getVehicleTypes() {
        return new ArrayList<>(ALL_VEHICLES.values());
    }

    public static class VehicleType {

        private final String name;
        private final Class<? extends AbstractVehicle> entityClass;
        private RegistryObject<EntityType<? extends AbstractVehicle>> entityTypeRegistryObject;
        private final ResourceLocation visualBedrockModel;
        private final ResourceLocation visualBedrockTexture;
        private final ResourceLocation structureBedrockModel;

        public VehicleType(String name, Class<? extends AbstractVehicle> entityClass) {
            this.name = name;

            //todo: 读取Data
//            this.health = 100f;

            this.entityClass = entityClass;
            this.visualBedrockModel = YwzjVehicle.modLoc("bedrock/entity/" + name);
            this.visualBedrockTexture = YwzjVehicle.modLoc("textures/entity/" + name +".png");
            this.structureBedrockModel = YwzjVehicle.modLoc("bedrock/entity/" + name + "_structure");
        }

        public String getName() {
            return name;
        }

        //todo: 读取Data
        public Float getHealth() {
            return 100f;
        }

        public ResourceLocation getVisualBedrockModel() {
            return visualBedrockModel;
        }

        public ResourceLocation getVisualBedrockTexture() {
            return visualBedrockTexture;
        }

        public ResourceLocation getStructureBedrockModel() {
            return structureBedrockModel;
        }

        public RegistryObject<EntityType<? extends AbstractVehicle>> registerEntity() {
            Constructor<? extends AbstractVehicle> constructor;
            try {
                constructor = entityClass.getConstructor(EntityType.class, Level.class);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
            entityTypeRegistryObject = AllEntities.ENTITIES.register(name, () -> EntityType.Builder
                    .<AbstractVehicle>of((type, level) -> {
                        try {
                            return constructor.newInstance(type, level);
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    }, MobCategory.MISC)
                    .sized(1f, 1f)
                    .updateInterval(1)
                    .clientTrackingRange(16)
                    .build(name));
            return entityTypeRegistryObject;
        }

        public EntityType getEntityType() {
            return entityTypeRegistryObject.get();
        }

    }

}
