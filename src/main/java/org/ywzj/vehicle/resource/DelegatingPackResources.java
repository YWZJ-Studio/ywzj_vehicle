package org.ywzj.vehicle.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DelegatingPackResources extends AbstractPackResources implements Pack.ResourcesSupplier {

    private final PackMetadataSection packMeta;
    private final List<PackResources> delegates;
    private final Map<String, List<PackResources>> namespacesAssets;
    private final Map<String, List<PackResources>> namespacesData;

    public DelegatingPackResources(PackLocationInfo info, PackMetadataSection packMeta, List<? extends PackResources> packs) {
        super(info);
        this.packMeta = packMeta;
        this.delegates = ImmutableList.copyOf(packs);
        this.namespacesAssets = buildNamespaceMap(PackType.CLIENT_RESOURCES, this.delegates);
        this.namespacesData = buildNamespaceMap(PackType.SERVER_DATA, this.delegates);
    }

    private Map<String, List<PackResources>> buildNamespaceMap(PackType type, List<PackResources> packList)
    {
        Map<String, List<PackResources>> map = new HashMap<>();
        for (PackResources pack : packList)
        {
            for (String namespace : pack.getNamespaces(type))
            {
                map.computeIfAbsent(namespace, k -> new ArrayList<>()).add(pack);
            }
        }
        map.replaceAll((k, list) -> ImmutableList.copyOf(list));
        return ImmutableMap.copyOf(map);
    }

    @Nullable
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
        if (deserializer.getMetadataSectionName().equals("pack")) {
            return (T) this.packMeta;
        }
        return null;
    }

    public void listResources(PackType type, String resourceNamespace, String paths, PackResources.ResourceOutput resourceOutput) {
        for (PackResources delegate : this.delegates) {
            delegate.listResources(type, resourceNamespace, paths, resourceOutput);
        }
    }

    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? this.namespacesAssets.keySet() : this.namespacesData.keySet();
    }

    public void close() {
        for (PackResources pack : this.delegates) {
            pack.close();
        }
    }

    @Nullable
    public IoSupplier<InputStream> getRootResource(String... paths) {
        return null;
    }

    @Nullable
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        for (PackResources pack : getCandidatePacks(type, location)) {
            IoSupplier<InputStream> ioSupplier = pack.getResource(type, location);
            if (ioSupplier != null) {
                return ioSupplier;
            }
        }
        return null;
    }

    @Nullable
    public Collection<PackResources> getChildren() {
        return this.delegates;
    }

    private List<PackResources> getCandidatePacks(PackType type, ResourceLocation location) {
        Map<String, List<PackResources>> map = type == PackType.CLIENT_RESOURCES ? this.namespacesAssets : this.namespacesData;
        List<PackResources> packsWithNamespace = map.get(location.getNamespace());
        return packsWithNamespace == null ? Collections.emptyList() : packsWithNamespace;
    }

    public PackResources openPrimary(PackLocationInfo packLocationInfo) {
        return this;
    }

    public PackResources openFull(PackLocationInfo packLocationInfo, Pack.Metadata metadata) {
        return this;
    }

}
