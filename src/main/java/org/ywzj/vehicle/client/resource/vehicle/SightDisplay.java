package org.ywzj.vehicle.client.resource.vehicle;

public class SightDisplay extends BaseDisplay {

    private final float scale;

    public SightDisplay(SightDisplayPojo pojo) {
        super(pojo);
        this.scale = Float.isFinite(pojo.scale) && pojo.scale > 0 ? pojo.scale : 1f;
    }

    public float getScale() {
        return scale;
    }

}
