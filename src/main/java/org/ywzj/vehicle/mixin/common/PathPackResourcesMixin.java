package org.ywzj.vehicle.mixin.common;

import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 从 mod jar读取资源时，将common目录的资源同时额外附加至assets/data目录中
 */
@Mixin(value = net.minecraftforge.resource.PathPackResources.class)
public abstract class PathPackResourcesMixin {

    @Final
    @Shadow(remap = false)
    private static Logger LOGGER;

    @Shadow(remap = false)
    protected abstract Path resolve(String... paths);

    @Shadow
    public abstract IoSupplier<InputStream> getRootResource(String... paths);

    // 若查找未命中，尝试回退到 common/<namespace>/...
    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void injectCommonFallback(PackType type, ResourceLocation location, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
        if (cir.getReturnValue() != null) return;
        IoSupplier<InputStream> fromCommon = this.getRootResource(ywzj_vehicle$commonPathFromLocation(location));
        if (fromCommon != null) {
            cir.setReturnValue(fromCommon);
        }
    }

    // 如果原data/assets目录没有对应资源，将 common 目录的资源合并进来
    @Inject(method = "listResources", at = @At("TAIL"))
    private void listResourcesMerged(PackType type, String namespace, String path, ResourceOutput resourceOutput, CallbackInfo ci) {
        FileUtil.decomposePath(path).get()
                .ifLeft(parts -> {
                    Path typeRoot = this.resolve(type.getDirectory(), namespace).toAbsolutePath();
                    Path commonRoot = this.resolve("common", namespace).toAbsolutePath();

                    // 尝试从 common 目录列出资源
                    net.minecraft.server.packs.PathPackResources.listPath(
                            namespace, commonRoot, parts,
                            (rl, supplier) -> {
                                Path candidate = typeRoot.resolve(rl.getPath());
                                if (!Files.exists(candidate)) {
                                    resourceOutput.accept(rl, supplier);
                                }
                            }
                    );
                })
                .ifRight(dataResult -> LOGGER.error("Invalid path {}: {}", path, dataResult.message()));
    }

    // 合并 common 的命名空间
    @Inject(method = "getNamespaces", at = @At("RETURN"), cancellable = true)
    private void mergeCommonNamespaces(PackType type, CallbackInfoReturnable<Set<String>> cir) {
        Set<String> ret = new HashSet<>(cir.getReturnValue());
        ret.addAll(ywzj_vehicle$getNamespacesFromCommon());
        cir.setReturnValue(ret);
    }

    @Unique
    private static String[] ywzj_vehicle$commonPathFromLocation(ResourceLocation location) {
        String[] parts = location.getPath().split("/");
        String[] result = new String[parts.length + 2];
        result[0] = "common";
        result[1] = location.getNamespace();
        System.arraycopy(parts, 0, result, 2, parts.length);
        return result;
    }

    @Unique
    private Set<String> ywzj_vehicle$getNamespacesFromCommon() {
        try {
            Path root = this.resolve("common");
            try (Stream<Path> walker = Files.walk(root, 1)) {
                return walker
                    .filter(Files::isDirectory)
                    .map(root::relativize)
                    .filter(p -> p.getNameCount() > 0)
                    .map(p -> p.toString().replaceAll("/$", ""))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            }
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }
}