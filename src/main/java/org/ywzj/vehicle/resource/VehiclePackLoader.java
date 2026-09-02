package org.ywzj.vehicle.resource;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.util.GetJarResources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public enum VehiclePackLoader implements RepositorySource {

    INSTANCE;
    private static final Marker MARKER = MarkerManager.getMarker("VehiclePackFinder");
    public PackType packType;
    private static final Path VEHICLE_PACKS_PATH = FMLPaths.GAMEDIR.get().resolve("limitless_vehicle");
    private static final Path VEHICLE_PACKS_BACKUP_PATH = FMLPaths.GAMEDIR.get().resolve("limitless_vehicle_backup");
    private static final String DEFAULT_VEHICLE_PACK_PATH = "default_vehicle";
    private List<VehiclePack> vehiclePacks;
    static {
        try {
            File folder = VEHICLE_PACKS_PATH.toFile();
            if (!folder.isDirectory()) {
                Files.createDirectories(folder.toPath());
            }
            folder = VEHICLE_PACKS_BACKUP_PATH.toFile();
            if (!folder.isDirectory()) {
                Files.createDirectories(folder.toPath());
            }
        } catch (Exception e) {
            YwzjVehicle.LOGGER.warn(MARKER, "Failed to init vehicle resource directory...", e);
        }
    }

    @Override
    public void loadPacks(@NotNull Consumer<Pack> pOnLoad) {
        for (Pack pack : vehiclePacksAsResource()) {
            pOnLoad.accept(pack);
        }
    }

    public void scanVehiclePacks() {
        checkDefaultVehiclePack();
        YwzjVehicle.LOGGER.info(MARKER, "Start scanning for vehicle packs in {}", VEHICLE_PACKS_PATH);
        vehiclePacks = scanVehiclePacks(VEHICLE_PACKS_PATH);
        YwzjVehicle.LOGGER.info(MARKER, "Found {} possible vehicle pack(s)", vehiclePacks.size());
    }

    private void checkDefaultVehiclePack() {
        Path defaultVehiclePackPath = Path.of(VEHICLE_PACKS_PATH + "/" + DEFAULT_VEHICLE_PACK_PATH);
        if (Files.isDirectory(defaultVehiclePackPath)) {
            try (InputStream streamExist = Files.newInputStream(defaultVehiclePackPath.resolve("vehicle_pack.meta.json"))) {
                PackMeta packMetaExist = GsonUtil.GSON.fromJson(new InputStreamReader(streamExist, StandardCharsets.UTF_8), PackMeta.class);
                if (packMetaExist != null) {
                    String versionExist = packMetaExist.getVersion();
                    try (InputStream streamJar = GetJarResources.readModFile("/" + DEFAULT_VEHICLE_PACK_PATH + "/vehicle_pack.meta.json")) {
                        PackMeta packMetaJar = GsonUtil.GSON.fromJson(new InputStreamReader(streamJar, StandardCharsets.UTF_8), PackMeta.class);
                        if (packMetaJar != null) {
                            String versionJar = packMetaJar.getVersion();
                            if (versionExist.compareTo(versionJar) >= 0) {
                                return;
                            }
                        }
                    }
                }
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                String timestamp = LocalDateTime.now().format(formatter);
                GetJarResources.copyFolder(defaultVehiclePackPath.toUri(), VEHICLE_PACKS_BACKUP_PATH.resolve(DEFAULT_VEHICLE_PACK_PATH + "_" + timestamp));
                GetJarResources.deleteFiles(defaultVehiclePackPath);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        GetJarResources.copyModDirectory("/" + DEFAULT_VEHICLE_PACK_PATH, defaultVehiclePackPath);
    }

    private List<Pack> vehiclePacksAsResource() {
        List<Pack> packs = new ArrayList<>();
        List<PackResources> extensionPacks = new ArrayList<>();
        PackResources openPrimary;
        for (VehiclePack vehiclePack : vehiclePacks) {
            PackLocationInfo locationInfo = new PackLocationInfo(vehiclePack.meta.getNamespace(), Component.empty(), PackSource.BUILT_IN, Optional.empty());
            if (Files.isDirectory(vehiclePack.path)) {
                openPrimary = new PathPackResources.PathResourcesSupplier(vehiclePack.path).openPrimary(locationInfo);
            } else {
                openPrimary = new FilePackResources.FileResourcesSupplier(vehiclePack.path).openPrimary(locationInfo);
            }
            PackResources packResources = openPrimary;
            extensionPacks.add(packResources);
            PackMetadataSection metadataSection = new PackMetadataSection(Component.translatable(vehiclePack.meta.getDescription()), SharedConstants.getCurrentVersion().getPackVersion(packType));
            DelegatingPackResources delegatingPackResources = new DelegatingPackResources(locationInfo, metadataSection, extensionPacks) {
                @Override
                public IoSupplier<InputStream> getRootResource(String... paths) {
                    if (paths.length == 1 && paths[0].equals("pack.png")) {
                        return packResources.getRootResource("pack.png");
                    }
                    return null;
                }
            };
            Pack pack = Pack.readMetaAndCreate(locationInfo,
                    delegatingPackResources,
                    packType,
                    new PackSelectionConfig(true, Pack.Position.BOTTOM, false));
            packs.add(pack);
        }
        return packs;
    }

    private static VehiclePack fromDirPath(Path path) throws IOException {
        Path packMetaPath = path.resolve("vehicle_pack.meta.json");
        try (InputStream stream = Files.newInputStream(packMetaPath)) {
            PackMeta packMeta = GsonUtil.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

            if (packMeta == null) {
                throw new RuntimeException("Failed to read packMeta json");
            }

            if (packMeta.getNamespace() == null) {
                throw new RuntimeException("Failed to read namespace");
            }

            if (packMeta.getDependencies() != null) {
                List<String> incompatible = modVersionAllMatch(packMeta);
                if (!incompatible.isEmpty()) {
                    throw new RuntimeException("Mod version mismatch: " + String.join(", ", incompatible));
                }
            }

            return new VehiclePack(path, packMeta);
        } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException exception) {
            YwzjVehicle.LOGGER.warn(MARKER, "Failed to read info json: {}", path.getFileName());
            YwzjVehicle.LOGGER.warn(exception.getMessage());
        }
        return null;
    }

    private static VehiclePack fromZipPath(Path path)  {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry packMetaEntry = zipFile.getEntry("vehicle_pack.meta.json");
            if (packMetaEntry == null) {
                YwzjVehicle.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), "No vehicle_pack.meta.json found");
                return null;
            }

            try (InputStream stream = zipFile.getInputStream(packMetaEntry)) {
                PackMeta packMeta = GsonUtil.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

                if (packMeta == null) {
                    throw new RuntimeException("Failed to read packMeta json");
                }

                if (packMeta.getNamespace() == null) {
                    throw new RuntimeException("Failed to read namespace");
                }

                if (packMeta.getDependencies() != null) {
                    List<String> incompatible = modVersionAllMatch(packMeta);
                    if (!incompatible.isEmpty()) {
                        throw new RuntimeException("Mod version mismatch: " + String.join(", ", incompatible));
                    }
                }

                return new VehiclePack(path, packMeta);
            } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException e) {
                YwzjVehicle.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
                return null;
            }
        } catch (IOException e) {
            YwzjVehicle.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
            return null;
        }
    }

    private static List<VehiclePack> scanVehiclePacks(Path path) {
        List<VehiclePack> vehiclePacks = new ArrayList<>();
        Set<String> namespaces = new HashSet<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                try {
                    VehiclePack vehiclePack = null;
                    if (Files.isDirectory(entry)) {
                        vehiclePack = fromDirPath(entry);
                    } else if (entry.toString().endsWith(".zip")) {
                        vehiclePack = fromZipPath(entry);
                    }
                    if (vehiclePack != null) {
                        if (namespaces.contains(vehiclePack.meta().getNamespace())) {
                            YwzjVehicle.LOGGER.error(MARKER, "- {}, Duplicated namespace: {}", vehiclePack.path.getFileName(), vehiclePack.meta.getNamespace());
                            continue;
                        }
                        namespaces.add(vehiclePack.meta().getNamespace());
                        YwzjVehicle.LOGGER.info(MARKER, "- {}, Main namespace: {}", vehiclePack.path.getFileName(), vehiclePack.meta.getNamespace());
                        vehiclePacks.add(vehiclePack);
                    }
                } catch (RuntimeException runtimeException) {
                    throw new RuntimeException("Failed to load vehicle pack: " + entry + "\n" + runtimeException.getMessage());
                }
            }
        } catch (IOException ioException) {
            YwzjVehicle.LOGGER.error(MARKER, "Failed to scan vehicle packs from {}. Error: {}", path, ioException);
        }

        return vehiclePacks;
    }

    private static List<String> modVersionAllMatch(PackMeta info) throws InvalidVersionSpecificationException {
        HashMap<String, String> dependencies = info.getDependencies();
        List<String> incompatible = new ArrayList<>();
        for (Map.Entry<String, String> modIdAndVersion : dependencies.entrySet()) {
            if (!modVersionMatch(modIdAndVersion.getKey(), modIdAndVersion.getValue())) {
                incompatible.add(modIdAndVersion.getKey() + ": " + modIdAndVersion.getValue());
            }
        }
        return incompatible;
    }

    private static boolean modVersionMatch(String modId, String version) throws InvalidVersionSpecificationException {
        VersionRange versionRange = VersionRange.createFromVersionSpec(version);
        return ModList.get().getModContainerById(modId).map(mod -> {
            ArtifactVersion modVersion = mod.getModInfo().getVersion();
            return versionRange.containsVersion(modVersion);
        }).orElse(false);
    }

    public List<VehiclePack> getVehiclePacks() {
        return vehiclePacks;
    }

    public record VehiclePack(Path path, PackMeta meta) {}

}
