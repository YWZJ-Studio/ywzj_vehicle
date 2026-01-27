package org.ywzj.vehicle.resource;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import cpw.mods.jarhandling.SecureJar;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraftforge.resource.PathPackResources;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.serialize.GsonUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public enum VehiclePackLoader implements RepositorySource {

    INSTANCE;
    private static final Marker MARKER = MarkerManager.getMarker("VehiclePackFinder");
    public PackType packType;
    private static final Path VEHICLE_PACKS_PATH = FMLPaths.GAMEDIR.get().resolve("limitless_vehicle");
    private List<VehiclePack> vehiclePacks;
    static {
        File folder = VEHICLE_PACKS_PATH.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                YwzjVehicle.LOGGER.warn(MARKER, "Failed to init vehicle resource directory...", e);
            }
        }
    }

    @Override
    public void loadPacks(@NotNull Consumer<Pack> pOnLoad) {
        for (Pack pack : vehiclePacksAsResource()) {
            pOnLoad.accept(pack);
        }
    }

    public void scanVehiclePacks() {
        YwzjVehicle.LOGGER.info(MARKER, "Start scanning for vehicle packs in {}", VEHICLE_PACKS_PATH);
        vehiclePacks = scanVehiclePacks(VEHICLE_PACKS_PATH);
        YwzjVehicle.LOGGER.info(MARKER, "Found {} possible vehicle pack(s)", vehiclePacks.size());
    }

    private List<Pack> vehiclePacksAsResource() {
//        // 确保配置文件加载，这个阶段将比标准的forge配置文件加载早
//        PreLoadConfig.load(resourcePacksPath);
//
//        // 仅在第一次加载时复制默认资源包
//        if (firstLoad) {
//            if (!PreLoadConfig.override.get()) {
//                for (ResourceManager.ExtraEntry entry : ResourceManager.EXTRA_ENTRIES) {
//                    GetJarResources.copyModDirectory(entry.modMainClass(), entry.srcPath(), resourcePacksPath, entry.extraDirName());
//                }
//            }
//            firstLoad = false;
//        }
        List<Pack> packs = new ArrayList<>();
        List<PathPackResources> extensionPacks = new ArrayList<>();
        for (VehiclePack vehiclePack : vehiclePacks) {
            PathPackResources packResources = new PathPackResources(vehiclePack.meta.getNamespace(), false, vehiclePack.path) {

                private final SecureJar secureJar = SecureJar.from(vehiclePack.path);

                @NotNull
                protected Path resolve(String... paths) {
                    if (paths.length < 1) {
                        throw new IllegalArgumentException("Missing path");
                    } else {
                        return this.secureJar.getPath(String.join("/", paths));
                    }
                }

                public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
                    return super.getResource(type, location);
                }

                public void listResources(PackType type, String namespace, String path, ResourceOutput resourceOutput) {
                    super.listResources(type, namespace, path, resourceOutput);
                }

            };
            extensionPacks.add(packResources);
            Pack pack = Pack.readMetaAndCreate("ywzj_vehicle_resources_" + vehiclePack.meta.getNamespace(),
                    Component.translatable(vehiclePack.meta.getTitle()),
                    true,
                    (id) -> new DelegatingPackResources(id,
                            false,
                            new PackMetadataSection(Component.translatable(vehiclePack.meta.getDescription()), SharedConstants.getCurrentVersion().getPackVersion(packType)), extensionPacks) {
                                public IoSupplier<InputStream> getRootResource(String... paths) {
                                    if (paths.length == 1 && paths[0].equals("pack.png")) {
                                        return packResources.getRootResource("pack.png");
                                    }
                                    return null;
                                }
                            }, packType, Pack.Position.BOTTOM, PackSource.BUILT_IN);
            packs.add(pack);
        }
        return packs;
    }

    private static VehiclePack fromDirPath(Path path) throws IOException {
        Path packMetaPath = path.resolve("vehicle_pack.meta.json");
        try (InputStream stream = Files.newInputStream(packMetaPath)) {
            PackMeta packMeta = GsonUtil.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

            if (packMeta == null) {
                YwzjVehicle.LOGGER.warn(MARKER, "Failed to read packMeta json: {}", path.getFileName());
                return null;
            }

            if (packMeta.getNamespace() == null) {
                YwzjVehicle.LOGGER.warn(MARKER, "Failed to read namespace: {}", path.getFileName());
                return null;
            }

            if (packMeta.getDependencies() != null && !modVersionAllMatch(packMeta)) {
                YwzjVehicle.LOGGER.warn(MARKER, "Mod version mismatch: {}", path.getFileName());
                return null;
            }

            return new VehiclePack(path, packMeta);
        } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException exception) {
            YwzjVehicle.LOGGER.warn(MARKER, "Failed to read info json: {}", path.getFileName());
            YwzjVehicle.LOGGER.warn(exception.getMessage());
        }
        return null;
    }

    private static VehiclePack fromZipPath(Path path)  {
        try(ZipFile zipFile = new ZipFile(path.toFile())){
            ZipEntry packMetaEntry = zipFile.getEntry("vehicle_pack.meta.json");
            if (packMetaEntry == null) {
                YwzjVehicle.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), "No vehicle_pack.meta.json found");
                return null;
            }

            try (InputStream stream = zipFile.getInputStream(packMetaEntry)) {
                PackMeta packMeta = GsonUtil.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

                if (packMeta == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to read packMeta json: {}", path.getFileName());
                    return null;
                }

                if (packMeta.getNamespace() == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to read namespace: {}", path.getFileName());
                    return null;
                }

                if (packMeta.getDependencies() != null && !modVersionAllMatch(packMeta)) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Mod version mismatch: {}", path.getFileName());
                    return null;
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

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)){
            for (Path entry : stream) {
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
            }
        } catch (IOException e) {
            YwzjVehicle.LOGGER.error(MARKER, "Failed to scan extensions from {}. Error: {}", path, e);
        }

        return vehiclePacks;
    }

    private static boolean modVersionAllMatch(PackMeta info) throws InvalidVersionSpecificationException {
        HashMap<String, String> dependencies = info.getDependencies();
        for (String modId : dependencies.keySet()) {
            if (!modVersionMatch(modId, dependencies.get(modId))) {
                return false;
            }
        }
        return true;
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
