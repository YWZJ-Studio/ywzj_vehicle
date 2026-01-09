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
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraftforge.resource.PathPackResources;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public enum VehiclePackLoader implements RepositorySource {

    INSTANCE;
    private static final Marker MARKER = MarkerManager.getMarker("VehiclePackFinder");
    public PackType packType;

    @Override
    public void loadPacks(Consumer<Pack> pOnLoad) {
        for (Pack extensionsPack : discoverExtensions()) {
            pOnLoad.accept(extensionsPack);
        }
    }

    public List<Pack> discoverExtensions() {
        List<Pack> packs = new ArrayList<>();
        Path resourcePacksPath = FMLPaths.GAMEDIR.get().resolve("vehicle");
        File folder = resourcePacksPath.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                YwzjVehicle.LOGGER.warn(MARKER, "Failed to init vehicle resource directory...", e);
                return null;
            }
        }

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

        YwzjVehicle.LOGGER.info(MARKER, "Start scanning for vehicle packs in {}", resourcePacksPath);
        List<VehiclePack> vehiclePacks = scanExtensions(resourcePacksPath);
        YwzjVehicle.LOGGER.info(MARKER, "Found {} possible vehicle pack(s) and added them to resource set.", vehiclePacks.size());
        List<PathPackResources> extensionPacks = new ArrayList<>();

        for (VehiclePack vehiclePack : vehiclePacks) {
            PathPackResources packResources = new PathPackResources(vehiclePack.namespace, false, vehiclePack.path) {
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
            Pack pack = Pack.readMetaAndCreate("ywzj_vehicle_resources_" + vehiclePack.namespace, Component.translatable(vehiclePack.title), true, (id) ->
                    new DelegatingPackResources(id, false, new PackMetadataSection(Component.translatable(vehiclePack.description),
                            SharedConstants.getCurrentVersion().getPackVersion(packType)), extensionPacks) {
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

    public static @Nullable Path getModIcon(String modId) {
        Optional<? extends ModContainer> m = ModList.get().getModContainerById(modId);
        if (m.isPresent()) {
            IModInfo mod = m.get().getModInfo();
            IModFile file = mod.getOwningFile().getFile();
            if (file != null) {
                Path logoPath = file.findResource("logo.png");
                if (Files.exists(logoPath)) {
                    return logoPath;
                }
            }
        }

        return null;
    }

    private static VehiclePack fromDirPath(Path path) throws IOException {
        Path packInfoFilePath = path.resolve("vehicle_pack.meta.json");
        try (InputStream stream = Files.newInputStream(packInfoFilePath)) {
            PackMeta info = GsonUtil.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

            if (info == null) {
                YwzjVehicle.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
                return null;
            }

            if (info.getTitle() == null) {
                YwzjVehicle.LOGGER.warn(MARKER, "Failed to read title: {}", path.getFileName());
                return null;
            }

            if (info.getDependencies() != null && !modVersionAllMatch(info)) {
                YwzjVehicle.LOGGER.warn(MARKER, "Mod version mismatch: {}", packInfoFilePath.getFileName());
                return null;
            }

            String description = "";
            if (info.getDescription() != null) {
                description = info.getDescription();
            }

            return new VehiclePack(path, info.getNamespace(), info.getTitle(), description);
        } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException exception) {
            YwzjVehicle.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
            YwzjVehicle.LOGGER.warn(exception.getMessage());
        }
        return null;
    }

    private static VehiclePack fromZipPath(Path path)  {
        try(ZipFile zipFile = new ZipFile(path.toFile())){
            ZipEntry extDescriptorEntry = zipFile.getEntry("vehicle_pack.meta.json");
            if (extDescriptorEntry == null) {
                YwzjVehicle.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), "No vehicle_pack.meta.json found");
                return null;
            }

            try (InputStream stream = zipFile.getInputStream(extDescriptorEntry)) {
                PackMeta info = GsonUtil.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

                if (info == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to read info json: {}", path.getFileName());
                    return null;
                }

                if (info.getTitle() == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to read title: {}", path.getFileName());
                    return null;
                }

                if (info.getDependencies() != null && !modVersionAllMatch(info)) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Mod version mismatch: {}", path.getFileName());
                    return null;
                }

                String description = "";
                if (info.getDescription() != null) {
                    description = info.getDescription();
                }

                return new VehiclePack(path, info.getNamespace(), info.getTitle(), description);
            } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException e) {
                YwzjVehicle.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
                return null;
            }
        } catch (IOException e) {
            YwzjVehicle.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
            return null;
        }
    }

    private static List<VehiclePack> scanExtensions(Path extensionsPath) {
        List<VehiclePack> vehiclePacks = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)){
            for (Path entry : stream) {
                VehiclePack vehiclePack = null;
                if (Files.isDirectory(entry)) {
                    vehiclePack = fromDirPath(entry);
                } else if (entry.toString().endsWith(".zip")) {
                    vehiclePack = fromZipPath(entry);
                }
                if (vehiclePack != null) {
                    YwzjVehicle.LOGGER.info(MARKER, "- {}, Main namespace: {}", vehiclePack.path.getFileName(), vehiclePack.namespace);
                    vehiclePacks.add(vehiclePack);
                }
            }
        } catch (IOException e) {
            YwzjVehicle.LOGGER.error(MARKER, "Failed to scan extensions from {}. Error: {}", extensionsPath, e);
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

    public record VehiclePack(Path path, String namespace, String title, String description) {}

}
