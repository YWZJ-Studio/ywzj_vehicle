package org.ywzj.vehicle.vehicle.parts;

public interface IRotatableUnit {

    float getXRot();
    float getYRot();

    float getXAimRot();
    float getYAimRot();

    void setXRot(float xRot);
    void setYRot(float yRot);

    void setXAimRot(float xAimRot);
    void setYAimRot(float yAimRot);

}
