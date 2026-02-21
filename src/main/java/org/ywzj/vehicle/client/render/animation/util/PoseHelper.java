package org.ywzj.vehicle.client.render.animation.util;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.BoneIndexProvider;
import com.maydaymemory.mae.basic.*;
import org.joml.Vector3f;

import java.util.TreeMap;

public class PoseHelper {

    public static final PoseHelper DUMMY = new PoseHelper(DummyPose.INSTANCE);
    private static final ZYXBoneTransformFactory TRANSFORM_FACTORY = new ZYXBoneTransformFactory();
    private final BoneIndexProvider boneIndexProvider;
    private final TreeMap<Integer, BoneTransformData> transforms = new TreeMap<>();
    private Pose pose;

    /**
     * Create a PoseHelper with a bone index provider.
     * 
     * @param boneIndexProvider Provider to resolve bone names to indices (typically BedrockModel)
     */
    public PoseHelper(BoneIndexProvider boneIndexProvider) {
        this.boneIndexProvider = boneIndexProvider;
    }

    public PoseHelper(Pose pose) {
        this.pose = pose;
        this.boneIndexProvider = null;
    }

    public void hideBone(String boneName) {
        int boneIndex = boneIndexProvider.getIndex(boneName);
        if (boneIndex < 0) {
            return;
        }
        transforms.put(boneIndex, new BoneTransformData(
                0, 0, 0,
                0, 0, 0,
                0, 0, 0
        ));
    }

    /**
     * Set bone transform with rotation (in degrees) and translation.
     * 
     * @param boneName Bone name
     * @param rotX Rotation around X axis in degrees
     * @param rotY Rotation around Y axis in degrees
     * @param rotZ Rotation around Z axis in degrees
     * @param transX Translation on X axis
     * @param transY Translation on Y axis
     * @param transZ Translation on Z axis
     */
    public void setBone(String boneName, 
                       double rotX, double rotY, double rotZ,
                       double transX, double transY, double transZ) {
        int boneIndex = boneIndexProvider.getIndex(boneName);
        if (boneIndex < 0) {
            return;
        }
        transforms.put(boneIndex, new BoneTransformData(
            (float) rotX, (float) rotY, (float) rotZ,
            (float) transX, (float) transY, (float) transZ,
            1.0f, 1.0f, 1.0f
        ));
    }

    /**
     * Set bone transform with rotation (in degrees), translation, and scale.
     */
    public void setBoneWithScale(String boneName,
                                double rotX, double rotY, double rotZ,
                                double transX, double transY, double transZ,
                                double scaleX, double scaleY, double scaleZ) {
        int boneIndex = boneIndexProvider.getIndex(boneName);
        if (boneIndex < 0) {
            System.err.println("Bone not found: " + boneName);
            return;
        }
        transforms.put(boneIndex, new BoneTransformData(
            (float) rotX, (float) rotY, (float) rotZ,
            (float) transX, (float) transY, (float) transZ,
            (float) scaleX, (float) scaleY, (float) scaleZ
        ));
    }

    /**
     * Set bone rotation only (in degrees).
     */
    public void setRotation(String boneName, double rotX, double rotY, double rotZ) {
        setBone(boneName, rotX, rotY, rotZ, 0, 0, 0);
    }

    /**
     * Set bone translation only.
     */
    public void setTranslation(String boneName, double transX, double transY, double transZ) {
        setBone(boneName, 0, 0, 0, transX, transY, transZ);
    }

    /**
     * Clear all bone transforms.
     */
    public void clear() {
        transforms.clear();
    }

    /**
     * Build and return the final Pose object.
     * Bone transforms are added in ascending order of bone index.
     */
    public Pose build() {
        if (pose != null) {
            return pose;
        }

        ArrayPoseBuilder builder = new ArrayPoseBuilder();
        
        // TreeMap ensures iteration in ascending order of bone index
        for (var entry : transforms.entrySet()) {
            int boneIndex = entry.getKey();
            BoneTransformData data = entry.getValue();
            
            // Convert degrees to radians
            float rotXRad = (float) Math.toRadians(data.rotX);
            float rotYRad = (float) Math.toRadians(data.rotY);
            float rotZRad = (float) Math.toRadians(data.rotZ);
            
            // Create transform using factory
            BoneTransform transform = TRANSFORM_FACTORY.createBoneTransform(
                boneIndex,
                new Vector3f(data.transX, data.transY, data.transZ),
                new Vector3f(rotXRad, rotYRad, rotZRad),
                new Vector3f(data.scaleX, data.scaleY, data.scaleZ)
            );
            
            builder.addBoneTransform(transform);
        }
        
        return builder.toPose();
    }

    /**
     * Internal data class for storing bone transform data.
     */
    private record BoneTransformData(float rotX, float rotY, float rotZ,
                                     float transX, float transY, float transZ,
                                     float scaleX, float scaleY, float scaleZ) {}

}
