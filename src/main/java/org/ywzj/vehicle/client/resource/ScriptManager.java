package org.ywzj.vehicle.client.resource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonParseException;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.mozillaa.javascript.Context;
import org.mozillaa.javascript.ContextFactory;
import org.mozillaa.javascript.Script;
import org.ywzj.vehicle.YwzjVehicle;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class ScriptManager extends SimplePreparableReloadListener<Map<ResourceLocation, Script>> {

    private final FileToIdConverter filetoidconverter = new FileToIdConverter("scripts", ".js");
    private Map<ResourceLocation, Script> scripts = Map.of();

    @NotNull
    @Override
    public Map<ResourceLocation, Script> prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller pProfiler) {
        Map<ResourceLocation, Script> output = Maps.newHashMap();
        try (Context ctx = ContextFactory.getGlobal().enterContext()) {
            ctx.setInterpretedMode(false);
            for(Map.Entry<ResourceLocation, Resource> entry : filetoidconverter.listMatchingResources(manager).entrySet()) {
                ResourceLocation resourcelocation = entry.getKey();
                ResourceLocation resourcelocation1 = filetoidconverter.fileToId(resourcelocation);

                try (Reader reader = entry.getValue().openAsReader()) {
                    Script compiled = ctx.compileReader(reader, entry.getKey().toString(), 1, null);
                    output.put(resourcelocation1, compiled);
                } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                    YwzjVehicle.LOGGER.error("Couldn't parse data file {} from {}", resourcelocation1, resourcelocation, jsonparseexception);
                }
            }
        }
        return output;
    }

    @Override
    public void apply(@NotNull Map<ResourceLocation, Script> map, @NotNull ResourceManager manager, @NotNull ProfilerFiller pProfiler) {
        scripts = ImmutableMap.copyOf(map);
    }

    public Map<ResourceLocation, Script> getScripts() {
        return Collections.unmodifiableMap(scripts);
    }

    public Optional<Script> getScript(ResourceLocation resourcelocation) {
        return Optional.ofNullable(scripts.get(resourcelocation));
    }

}
