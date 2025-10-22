package org.ywzj.vehicle.all;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AllConfigs {

    public static CommonConfig common;
//    public static ServerConfig server;

    public static void register(ModLoadingContext context) {
        Pair<CommonConfig, ForgeConfigSpec> specPairCommon = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        common = specPairCommon.getLeft();
        context.registerConfig(ModConfig.Type.COMMON, specPairCommon.getRight());
//        Pair<ServerConfig, ForgeConfigSpec> specPairServer = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
//        server = specPairServer.getLeft();
//        context.registerConfig(ModConfig.Type.SERVER, specPairServer.getRight());
    }

    public static class CommonConfig {

        public final ForgeConfigSpec.ConfigValue<Boolean> explosionBreakBlocks;
        public final ForgeConfigSpec.ConfigValue<Boolean> selfRighting;
        public final ForgeConfigSpec.ConfigValue<Boolean> infiniteFuel;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> fuelNameWhiteList;
        public final ForgeConfigSpec.ConfigValue<Boolean> hitIndicator;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            explosionBreakBlocks = builder.comment("爆炸是否破坏方块")
                    .define("explosionBreakBlocks", true);
            selfRighting = builder.comment("倾角过大时是否自动回正")
                    .define("selfRighting", true);
            infiniteFuel = builder.comment("无需燃油仍可运作")
                    .define("infiniteFuel", false);
            fuelNameWhiteList = builder.comment("允许视作燃油的液体")
                    .defineList("fuelNameWhiteList", Arrays.asList("fuel", "gas", "lava"), obj -> obj instanceof String);
            hitIndicator = builder.comment("开启命中提示")
                    .define("hitIndicator", true);
        }

    }

//    public static class ServerConfig {
//
//        public final ForgeConfigSpec.ConfigValue<Boolean> staminaSystem;
//        public final ForgeConfigSpec.ConfigValue<Boolean> bodyPartHurtSystem;
//        public final ForgeConfigSpec.ConfigValue<Boolean> bodyPartHurtEffect;
//        public final ForgeConfigSpec.ConfigValue<Boolean> queryPrice;
//        public final ForgeConfigSpec.ConfigValue<List<String>> itemTagFilter;
//
//        public ServerConfig(ForgeConfigSpec.Builder builder) {
//            staminaSystem = builder.comment("是否启用耐力系统")
//                    .define("StaminaSystem", true);
//            bodyPartHurtSystem = builder.comment("是否启用身体部位伤害系统")
//                    .define("BodyPartHurtSystem", true);
//            bodyPartHurtEffect = builder.comment("身体部位伤害是否有debuff")
//                    .define("BodyPartHurtEffect", true);
//            queryPrice = builder.comment("是否向服务器查询物品的交易行价格")
//                    .define("QueryPrice", true);
//            itemTagFilter = builder.comment("生成物品交易行唯一键时要过滤的NBT")
//                    .define("ItemTagFilter", Lists.newArrayList());
//        }
//
//    }

}
