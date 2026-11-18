package org.ywzj.vehicle.resource;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import cpw.mods.jarhandling.SecureJar;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraftforge.resource.PathPackResources;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplayPojo;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.util.GetJarResources;
import org.ywzj.vehicle.util.ResourceScanner;

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
    private List<PackIssue> issues = List.of();
    public record VehiclePack(Path path, PackMeta meta) {}
    public record PackIssue(Path path, Component reason) {}
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
        issues = buildVehiclePacks(VEHICLE_PACKS_PATH);
        for (PackIssue issue : issues) {
            YwzjVehicle.LOGGER.error(MARKER, "Vehicle pack {}: {}", issue.path(), issue.reason().getString());
        }
        if (!issues.isEmpty() && packType == PackType.SERVER_DATA) {
            throw new IllegalStateException("Vehicle pack validation failed; see the errors above.");
        }
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

    private static PackMeta readMeta(Path path) throws IOException {
        String filename = "vehicle_pack.meta.json";
        if (Files.isDirectory(path)) {
            try (var reader = Files.newBufferedReader(path.resolve(filename), StandardCharsets.UTF_8)) {
                return GsonUtil.GSON.fromJson(reader, PackMeta.class);
            }
        }
        try (ZipFile zip = new ZipFile(path.toFile())) {
            ZipEntry entry = zip.getEntry(filename);
            if (entry == null) throw new IOException("Missing " + filename);
            try (var reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                return GsonUtil.GSON.fromJson(reader, PackMeta.class);
            }
        }
    }

    private List<PackIssue> buildVehiclePacks(Path path) {
        List<VehiclePack> validPacks = new ArrayList<>();
        List<PackIssue> issues = new ArrayList<>();
        Set<String> namespaces = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    if (Files.notExists(entry.resolve("vehicle_pack.meta.json"))) {
                        continue;
                    }
                } else if (!entry.toString().endsWith(".zip")) {
                    continue;
                }
                try {
                    PackMeta meta = readMeta(entry);
                    if (meta == null || meta.getNamespace() == null || meta.getNamespace().isEmpty()
                            || !ResourceLocation.isValidNamespace(meta.getNamespace())) {
                        issues.add(new PackIssue(entry, Component.translatableWithFallback("screen.vehicle_packs.namespace", "Invalid or missing namespace in vehicle_pack.meta.json.")));
                        continue;
                    }
                    List<Component> problems = checkDependencies(meta);
                    if (!problems.isEmpty()) {
                        problems.forEach(reason -> issues.add(new PackIssue(entry, reason)));
                        continue;
                    }
                    if (!namespaces.add(meta.getNamespace())) {
                        YwzjVehicle.LOGGER.error(MARKER, "- {}, Duplicated namespace: {}", entry.getFileName(), meta.getNamespace());
                        continue;
                    }
                    validPacks.add(new VehiclePack(entry, meta));
                    YwzjVehicle.LOGGER.info(MARKER, "- {}, Main namespace: {}", entry.getFileName(), meta.getNamespace());
                } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException e) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Skipping unreadable vehicle pack {}", entry, e);
                }
            }
        } catch (IOException ioException) {
            YwzjVehicle.LOGGER.error(MARKER, "Failed to scan vehicle packs from {}", path, ioException);
        }
        this.vehiclePacks = validPacks;
        return List.copyOf(issues);
    }

    private static List<Component> checkDependencies(PackMeta meta) throws InvalidVersionSpecificationException {
        List<Component> problems = new ArrayList<>();
        if (meta.getDependencies() == null) {
            return problems;
        }
        for (var dependency : meta.getDependencies().entrySet()) {
            String id = dependency.getKey();
            String spec = dependency.getValue();
            if (spec == null || spec.isBlank()) {
                throw new InvalidVersionSpecificationException("Missing version for " + id);
            }
            VersionRange range = VersionRange.createFromVersionSpec(spec);
            var installed = ModList.get().getModContainerById(id).map(mod -> mod.getModInfo().getVersion());
            if (installed.isPresent() && range.containsVersion(installed.get())) continue;
            Component required = Component.literal(spec);
            if (range.getRestrictions().size() == 1) {
                var bound = range.getRestrictions().get(0);
                if (bound.getLowerBound() != null && bound.isLowerBoundInclusive() && bound.getUpperBound() == null) {
                    required = Component.translatableWithFallback("screen.vehicle_packs.minimum", "%s or newer", bound.getLowerBound().toString());
                }
            }
            problems.add(installed.isEmpty()
                    ? Component.translatableWithFallback("screen.vehicle_packs.missing", "%s is not installed. Required: %s.", id, required)
                    : Component.translatableWithFallback("screen.vehicle_packs.version", "%s: installed %s; required %s.", id, installed.get().toString(), required));
        }
        return problems;
    }

    public List<PackIssue> getIssues() {
        return issues;
    }

    public Path getPacksDirectory() {
        return VEHICLE_PACKS_PATH;
    }

    public List<VehiclePack> getVehiclePacks() {
        return vehiclePacks;
    }

    public Map<ResourceLocation, BaseDisplayPojo> scanVehicleDisplayDefinitions() {
        List<PackResources> resources = new ArrayList<>();
        for (VehiclePack vehiclePack : vehiclePacks) {
            String namespace = vehiclePack.meta().getNamespace();
            resources.add(Files.isDirectory(vehiclePack.path())
                    ? new PathPackResources(namespace, false, vehiclePack.path())
                    : new FilePackResources(namespace, vehiclePack.path().toFile(), false));
        }
        try (var manager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, resources)) {
            return ResourceScanner.scanDirectory(manager, FileToIdConverter.json("display/vehicle"),
                    GsonUtil.GSON, BaseDisplayPojo.class);
        }
    }

}
