package org.ywzj.vehicle.util;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.*;

import javax.annotation.Nonnull;
import java.lang.Math;

/**
 * from TACZ Guns
 */
public class MathUtil {
    public static final float[] QUATERNION_ONE = {0, 0, 0, 1};
    public static final Quaternionf IDENTITY_QUATERNION = new Quaternionf();

    public static double magnificationToFovMultiplier(double magnification, double currentFov) {
        return magnificationToFov(magnification, currentFov) / currentFov;
    }

    public static double magnificationToFov(double magnification, double currentFov) {
        double currentTan = Math.tan(Math.toRadians(currentFov / 2));
        double newTan = currentTan / magnification;
        return Math.toDegrees(Math.atan(newTan)) * 2;
    }

    public static double fovToMagnification(double currentFov, double originFov) {
        return Math.tan(Math.toRadians(originFov / 2)) / Math.tan(Math.toRadians(currentFov / 2));
    }

    public static double zoomSensitivityRatio(double currentFov, double originFov, double coefficient) {
        return Math.atan(Math.tan(Math.toRadians(currentFov / 2)) * coefficient) /
                Math.atan(Math.tan(Math.toRadians(originFov / 2)) * coefficient);
    }

    public static void mulMatrix(PoseStack poseStack, Matrix4fc matrix) {
        Matrix3f linearPart = new Matrix3f(matrix);
        linearPart.invert();
        linearPart.transpose();
        poseStack.last().normal().mul(linearPart);
        poseStack.last().pose().mul(matrix);
    }

    /**
     * 按照 z(roll) -> y(yaw) -> x(pitch) 的旋转顺序，求四元数。
     * @param pitch 绕 x 轴旋转的弧度
     * @param yaw 绕 y 轴旋转的弧度
     * @param roll 绕 z 轴旋转的弧度
     * @return 四元数，前三个数是虚部，最后一个数是实部。
     */
    public static float[] toQuaternion(float pitch, float yaw, float roll) {
        double cy = Math.cos(roll * 0.5);
        double sy = Math.sin(roll * 0.5);
        double cp = Math.cos(yaw * 0.5);
        double sp = Math.sin(yaw * 0.5);
        double cr = Math.cos(pitch * 0.5);
        double sr = Math.sin(pitch * 0.5);
        return new float[]{
                (float) (cy * cp * sr - sy * sp * cr),
                (float) (sy * cp * sr + cy * sp * cr),
                (float) (sy * cp * cr - cy * sp * sr),
                (float) (cy * cp * cr + sy * sp * sr)
        };
    }

    /**
     * 按照 z(roll) -> y(yaw) -> x(pitch) 的旋转顺序，求四元数。
     * @param pitch 绕 x 轴旋转的弧度
     * @param yaw 绕 y 轴旋转的弧度
     * @param roll 绕 z 轴旋转的弧度
     * @param quaternion 求解的结果将写入这个四元数中。
     */
    public static void toQuaternion(float pitch, float yaw, float roll, @Nonnull Quaternionf quaternion) {
        double cy = Math.cos(roll * 0.5);
        double sy = Math.sin(roll * 0.5);
        double cp = Math.cos(yaw * 0.5);
        double sp = Math.sin(yaw * 0.5);
        double cr = Math.cos(pitch * 0.5);
        double sr = Math.sin(pitch * 0.5);

        quaternion.set(
                (float) (cy * cp * sr - sy * sp * cr),
                (float) (sy * cp * sr + cy * sp * cr),
                (float) (sy * cp * cr - cy * sp * sr),
                (float) (cy * cp * cr + sy * sp * sr)
        );
    }

    /**
     * 将四元数转换为欧拉角，
     * @param q 四元数，前三个数是虚部，最后一个数是实部。
     * @return 按照 x(pitch) -> y(yaw) -> z(roll) 的顺序的三轴角数组。
     */
    public static float[] toEulerAngles(float[] q) {
        float[] angles = new float[3];
        // pitch (x-axis rotation)
        double sinrCosp = 2 * (q[3] * q[0] + q[1] * q[2]);
        double cosrCosp = 1 - 2 * (q[0] * q[0] + q[1] * q[1]);
        angles[0] = (float) Math.atan2(sinrCosp, cosrCosp);
        // yaw (y-axis rotation)
        double sinp = 2 * (q[3] * q[1] - q[2] * q[0]);
        if (Math.abs(sinp) >= 1) {
            angles[1] = (float) Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        } else {
            angles[1] = (float) Math.asin(sinp);
        }
        // roll (z-axis rotation)
        double sinyCosp = 2 * (q[3] * q[2] + q[1] * q[0]);
        double cosyCosp = 1 - 2 * (q[1] * q[1] + q[2] * q[2]);
        angles[2] = (float) Math.atan2(sinyCosp, cosyCosp);
        return angles;
    }

    public static float[] slerp(float[] from, float[] to, float alpha) {
        float ax = from[0];
        float ay = from[1];
        float az = from[2];
        float aw = from[3];
        float bx = to[0];
        float by = to[1];
        float bz = to[2];
        float bw = to[3];

        float dot = ax * bx + ay * by + az * bz + aw * bw;
        if (dot < 0) {
            bx = -bx;
            by = -by;
            bz = -bz;
            bw = -bw;
            dot = -dot;
        }
        float epsilon = 1e-6f;
        float s0, s1;
        if ((1.0 - dot) > epsilon) {
            float omega = (float) Math.acos(dot);
            float invSinOmega = 1.0f / (float) Math.sin(omega);
            s0 = (float) Math.sin((1.0 - alpha) * omega) * invSinOmega;
            s1 = (float) Math.sin(alpha * omega) * invSinOmega;
        } else {
            s0 = 1.0f - alpha;
            s1 = alpha;
        }
        float[] result = new float[4];
        result[0] = s0 * ax + s1 * bx;
        result[1] = s0 * ay + s1 * by;
        result[2] = s0 * az + s1 * bz;
        result[3] = s0 * aw + s1 * bw;
        return result;
    }

    public static Quaternionf toQuaternion(float[] q) {
        return new Quaternionf(q[0], q[1], q[2], q[3]);
    }

    public static Quaternionf slerp(Quaternionf from, Quaternionf to, float alpha) {
        float ax = from.x();
        float ay = from.y();
        float az = from.z();
        float aw = from.w();
        float bx = to.x();
        float by = to.y();
        float bz = to.z();
        float bw = to.w();

        float dot = ax * bx + ay * by + az * bz + aw * bw;
        if (dot < 0) {
            bx = -bx;
            by = -by;
            bz = -bz;
            bw = -bw;
            dot = -dot;
        }
        float epsilon = 1e-6f;
        float s0, s1;
        if ((1.0 - dot) > epsilon) {
            float omega = (float) Math.acos(dot);
            float invSinOmega = 1.0f / (float) Math.sin(omega);
            s0 = (float) Math.sin((1.0 - alpha) * omega) * invSinOmega;
            s1 = (float) Math.sin(alpha * omega) * invSinOmega;
        } else {
            s0 = 1.0f - alpha;
            s1 = alpha;
        }
        float rx = s0 * ax + s1 * bx;
        float ry = s0 * ay + s1 * by;
        float rz = s0 * az + s1 * bz;
        float rw = s0 * aw + s1 * bw;
        return new Quaternionf(rx, ry, rz, rw);
    }

    public static Vector3f getEulerAngles(Matrix4f matrix) {
        Vector3f dest = new Vector3f();
        matrix.getEulerAnglesZYX(dest);
        return dest;
    }

    public static float[] solveEquations(float[][] coefficients, float[] constants) {
        int n = constants.length;
        // 高斯消元
        for (int pivot = 0; pivot < n - 1; pivot++) {
            for (int row = pivot + 1; row < n; row++) {
                float factor = coefficients[row][pivot] / coefficients[pivot][pivot];
                for (int col = pivot; col < n; col++) {
                    coefficients[row][col] -= coefficients[pivot][col] * factor;
                }
                constants[row] -= constants[pivot] * factor;
            }
        }
        // 回代求解
        float[] solution = new float[n];
        for (int i = n - 1; i >= 0; i--) {
            float sum = 0.0f;
            for (int j = i + 1; j < n; j++) {
                sum += coefficients[i][j] * solution[j];
            }
            solution[i] = (constants[i] - sum) / coefficients[i][i];
        }
        return solution;
    }

    public static float[] getRelativeQuaternion(float[] qa, float[] qb) {
        /*
        Given two quaternions A and B, find the quaternion C such that the result of A multiplied by C is equal to B.
        Solve the following equations:
             aw*ci -ak*cj +aj*ck +ai*cw = bi
             ak*ci +aw*cj -ai*ck +aj*cw = bj
            -aj*ci +ai*cj +aw*ck +ak*cw = bk
            -ai*ci -aj*cj -ak*ck +aw*cw = bw
        */
        float[][] coefficients = {
                {qa[3], -qa[2], qa[1], qa[0]},
                {qa[2], qa[3], -qa[0], qa[1]},
                {-qa[1], qa[0], qa[3], qa[2]},
                {-qa[0], -qa[1], -qa[2], qa[3]},
        };
        float[] constants = {qb[0], qb[1], qb[2], qb[3]};
        return solveEquations(coefficients, constants);
    }

    public static Quaternionf getRelativeQuaternion(Quaternionf qa, Quaternionf qb) {
        /*
        Given two quaternions A and B, find the quaternion C such that the result of A multiplied by C is equal to B.
        Solve the following equations:
             aw*ci -ak*cj +aj*ck +ai*cw = bi
             ak*ci +aw*cj -ai*ck +aj*cw = bj
            -aj*ci +ai*cj +aw*ck +ak*cw = bk
            -ai*ci -aj*cj -ak*ck +aw*cw = bw
        */
        float[][] coefficients = {
                {qa.w(), -qa.z(), qa.y(), qa.x()},
                {qa.z(), qa.w(), -qa.x(), qa.y()},
                {-qa.y(), qa.x(), qa.w(), qa.z()},
                {-qa.x(), -qa.y(), -qa.z(), qa.w()},
        };
        float[] constants = {qb.x(), qb.y(), qb.z(), qb.w()};
        float[] result = solveEquations(coefficients, constants);
        return new Quaternionf(result[0], result[1], result[2], result[3]);
    }

    /**
     * 在两个变换矩阵之间旋转、位移的插值。
     *
     * @param resultMatrix 输出结果将乘进此矩阵
     */
    public static void applyMatrixLerp(Matrix4f fromMatrix, Matrix4f toMatrix, Matrix4f resultMatrix, float alpha) {
        // 计算位移的插值
        Vector3f translation = new Vector3f(toMatrix.m30() - fromMatrix.m30(), toMatrix.m31() - fromMatrix.m31(), toMatrix.m32() - fromMatrix.m32());
        translation.mul(alpha);
        // 计算旋转的插值
        Vector3f fromRotation = MathUtil.getEulerAngles(fromMatrix);
        float[] qFrom = MathUtil.toQuaternion(fromRotation.x(), fromRotation.y(), fromRotation.z());
        Vector3f toRotation = MathUtil.getEulerAngles(toMatrix);
        float[] qTo = MathUtil.toQuaternion(toRotation.x(), toRotation.y(), toRotation.z());
        float[] qRelative = getRelativeQuaternion(qFrom, qTo);
        Quaternionf qLerped = MathUtil.toQuaternion(MathUtil.slerp(QUATERNION_ONE, qRelative, alpha));
        // 应用位移和旋转
        resultMatrix.m30(resultMatrix.m30() + translation.x);
        resultMatrix.m31(resultMatrix.m31() + translation.y);
        resultMatrix.m32(resultMatrix.m32() + translation.z);
        resultMatrix.rotate(qLerped);
    }

    public static float splineCurve(float[] y, float tension, float alpha) {
        if (y.length != 4) {
            throw new IllegalArgumentException("y value length must be 4 when doing catmull-rom spline");
        }
        if (tension < 0 || tension > 1) {
            throw new IllegalArgumentException("tension must be 0~1 when doing catmull-rom spline");
        }
        float v0 = (y[2] - y[0]) * 0.5f;
        float v1 = (y[3] - y[1]) * 0.5f;
        float t2 = alpha * alpha;
        float t3 = alpha * t2;
        float h1 = 2f * t3 - 3f * t2 + 1f;
        float h2 = -2f * t3 + 3f * t2;
        float h3 = t3 - 2f * t2 + alpha;
        float h4 = t3 - t2;
        return h1 * y[1] + h2 * y[2] + h3 * v0 + h4 * v1;
    }
}