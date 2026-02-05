package org.ywzj.vehicle.all;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;
import org.ywzj.vehicle.YwzjVehicle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AllConfigs {

    public static CommonConfig common;
    public static ServerConfig server;
    public static List<String> figureBoxCaptureBlacklist = new ArrayList<>();

    private static final String CONFIG_DIR_NAME = "limitless_vehicle";
    private static final String BLACKLIST_FILE_NAME = "figure_box_capture_blacklist.txt";

    public static void register(ModLoadingContext context) {
        Pair<CommonConfig, ForgeConfigSpec> specPairCommon = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        common = specPairCommon.getLeft();
        context.registerConfig(ModConfig.Type.COMMON, specPairCommon.getRight());
        Pair<ServerConfig, ForgeConfigSpec> specPairServer = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        server = specPairServer.getLeft();
        context.registerConfig(ModConfig.Type.SERVER, specPairServer.getRight());
        loadExternal();
    }

    /**
     * Loads external configuration files with comprehensive error handling.
     * Creates default configuration if files don't exist.
     */
    public static void loadExternal() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR_NAME);
        Path filePath = configDir.resolve(BLACKLIST_FILE_NAME);
        
        try {
            ensureConfigDirectoryExists(configDir);
            ensureBlacklistFileExists(filePath);
            loadBlacklistFromFile(filePath);
            
            YwzjVehicle.LOGGER.info("Successfully loaded figure box blacklist: {} entries", figureBoxCaptureBlacklist.size());
        } catch (IOException e) {
            YwzjVehicle.LOGGER.error("Failed to load external configuration from: {}", filePath, e);
            loadDefaultBlacklist();
        } catch (SecurityException e) {
            YwzjVehicle.LOGGER.error("Permission denied when accessing configuration directory: {}", configDir, e);
            loadDefaultBlacklist();
        } catch (Exception e) {
            YwzjVehicle.LOGGER.error("Unexpected error while loading external configuration", e);
            loadDefaultBlacklist();
        }
    }

    /**
     * Ensures the configuration directory exists, creating it if necessary.
     */
    private static void ensureConfigDirectoryExists(Path configDir) throws IOException {
        if (Files.notExists(configDir)) {
            try {
                Files.createDirectories(configDir);
                YwzjVehicle.LOGGER.info("Created configuration directory: {}", configDir);
            } catch (IOException e) {
                throw new IOException("Failed to create configuration directory: " + configDir, e);
            }
        }
    }

    /**
     * Ensures the blacklist file exists, creating it with defaults if necessary.
     */
    private static void ensureBlacklistFileExists(Path filePath) throws IOException {
        if (Files.notExists(filePath)) {
            try {
                List<String> defaultLines = Arrays.asList(
                        "# Figure Box Capture Blacklist",
                        "# Add entity IDs (one per line) that should not be captured",
                        "# Lines starting with # are comments",
                        "",
                        "minecraft:ender_dragon",
                        "corpse:corpse"
                );
                Files.write(filePath, defaultLines, StandardOpenOption.CREATE_NEW);
                YwzjVehicle.LOGGER.info("Created default blacklist file: {}", filePath);
            } catch (IOException e) {
                throw new IOException("Failed to create blacklist file: " + filePath, e);
            }
        }
    }

    /**
     * Loads and parses the blacklist from file.
     */
    private static void loadBlacklistFromFile(Path filePath) throws IOException {
        try {
            figureBoxCaptureBlacklist.clear();
            List<String> lines = Files.readAllLines(filePath);
            
            for (String line : lines) {
                String trimmed = line.trim();
                // Skip comments and empty lines
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    figureBoxCaptureBlacklist.add(trimmed);
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to read blacklist file: " + filePath, e);
        }
    }

    /**
     * Loads default blacklist entries when file loading fails.
     */
    private static void loadDefaultBlacklist() {
        figureBoxCaptureBlacklist.clear();
        figureBoxCaptureBlacklist.add("minecraft:ender_dragon");
        figureBoxCaptureBlacklist.add("corpse:corpse");
        YwzjVehicle.LOGGER.warn("Using default blacklist entries due to configuration load failure");
    }

    public static class CommonConfig {

        public final ForgeConfigSpec.ConfigValue<Boolean> explosionDestroyBlocks;
        public final ForgeConfigSpec.ConfigValue<Boolean> explosionDropBlocks;
        public final ForgeConfigSpec.ConfigValue<Double> vehicleExplosionHurtPassengerDamage;
        public final ForgeConfigSpec.ConfigValue<Boolean> selfRighting;
        public final ForgeConfigSpec.ConfigValue<Boolean> infiniteFuel;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> fuelNameWhiteList;
        public final ForgeConfigSpec.ConfigValue<Boolean> hitIndicator;
        public final ForgeConfigSpec.ConfigValue<Boolean> checkTeamOnEnterVehicle;
        public final ForgeConfigSpec.ConfigValue<Boolean> figureBoxOnlyCaptureVehicle;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            explosionDestroyBlocks = builder.comment("Whether explosions destroy blocks")
                    .define("explosionDestroyBlocks", true);
            explosionDropBlocks = builder.comment("Whether explosions drop blocks")
                    .define("explosionDropBlocks", true);
            vehicleExplosionHurtPassengerDamage = builder.comment("Damage dealt to passengers when vehicle explodes")
                    .defineInRange("vehicleExplosionHurtPassengerDamage", 512.0, 0.0, Double.MAX_VALUE);
            selfRighting = builder.comment("Whether vehicles auto-correct when tilted excessively")
                    .define("selfRighting", true);
            infiniteFuel = builder.comment("Whether vehicles can operate without fuel")
                    .define("infiniteFuel", false);
            fuelNameWhiteList = builder.comment("Fluids that can be used as fuel")
                    .defineList("fuelNameWhiteList", Arrays.asList("fuel", "gas", "lava"), obj -> obj instanceof String);
            hitIndicator = builder.comment("Enable hit indicator")
                    .define("hitIndicator", true);
            checkTeamOnEnterVehicle = builder.comment("Whether vehicle passengers must be on the same team")
                    .define("checkTeamOnEnterVehicle", true);
            figureBoxOnlyCaptureVehicle = builder.comment("Whether figure box can only capture vehicles")
                    .define("figureBoxOnlyCaptureVehicle", false);
        }

    }

    public static class ServerConfig {

        public final ForgeConfigSpec.ConfigValue<Double> showVehicleInfoDistance;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            showVehicleInfoDistance = builder.comment("Maximum distance to show vehicle info when looking at it")
                    .defineInRange("showVehicleInfoDistance", 512.0, 0.0, 1024.0);
        }

    }

}
