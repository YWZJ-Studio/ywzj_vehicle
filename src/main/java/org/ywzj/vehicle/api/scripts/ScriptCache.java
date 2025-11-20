package org.ywzj.vehicle.api.scripts;

public class ScriptCache {
    private Object data;

    public ScriptCache(Object data) {
        this.data = data;
    }

    public Object get() {
        return data;
    }

    public void set(Object data) {
        this.data = data;
    }
}
