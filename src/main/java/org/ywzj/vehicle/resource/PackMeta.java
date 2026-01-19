package org.ywzj.vehicle.resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;

public class PackMeta {

    @SerializedName("namespace")
    private String namespace;

    @SerializedName("title")
    private String title = "unknown";

    @SerializedName("description")
    private String description = "";

    @SerializedName("version")
    private String version = "1.0.0";

    @SerializedName("date")
    private String date = "2026-01-01";

    @SerializedName("license")
    private String license = "All Rights Reserved";

    @SerializedName("authors")
    private List<String> authors = Lists.newArrayList();

    @SerializedName("url")
    private String url;

    @SerializedName("dependencies")
    private HashMap<String, String> dependencies = Maps.newHashMap();

    public String getNamespace() {
        return namespace;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getDate() {
        return date;
    }

    public String getLicense() {
        return license;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getUrl() {
        return url;
    }

    public HashMap<String, String> getDependencies() {
        return dependencies;
    }

}
