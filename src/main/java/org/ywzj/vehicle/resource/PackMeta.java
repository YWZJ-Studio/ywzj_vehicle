package org.ywzj.vehicle.resource;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class PackMeta {

    @SerializedName("namespace")
    private String namespace;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("dependencies")
    private HashMap<String, String> dependencies = Maps.newHashMap();

    public PackMeta(String namespace, String title, String description, HashMap<String, String> dependencies) {
        this.namespace = namespace;
        this.title = title;
        this.description = description;
        this.dependencies = dependencies;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public HashMap<String, String> getDependencies() {
        return dependencies;
    }

}
