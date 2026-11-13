package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Sumber cahaya titik yang mengorbit target (0,0,0) dalam koordinat sferis.
 * Juga menghitung Light Space Matrix (P_light * V_light) untuk Shadow Mapping:
 * proyeksi ORTOGRAFIK, kamera cahaya menghadap pusat scene.
 */
public class LightSource {

    private float azimuthDeg = 40.0f;
    private float elevationDeg = 50.0f;
    private final float distance = 8.0f;
    private final Vector3f color = new Vector3f(1.0f, 0.98f, 0.92f);

    // Volume ortografik cahaya: mencakup lantai 12 x 12 (diagonal ~8.5) dan seluruh objek
    private static final float HALF_SIZE = 9.0f;
    private static final float NEAR = 1.0f;
    private static final float FAR = 18.0f;

    public Vector3f getPosition() {
        float az = (float) Math.toRadians(azimuthDeg);
        float el = (float) Math.toRadians(elevationDeg);
        return new Vector3f(
                distance * (float) (Math.cos(el) * Math.sin(az)),
                distance * (float) Math.sin(el),
                distance * (float) (Math.cos(el) * Math.cos(az)));
    }

    public Matrix4f getLightSpaceMatrix() {
        Vector3f pos = getPosition();
        Matrix4f view = new Matrix4f().lookAt(pos.x, pos.y, pos.z, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix4f proj = new Matrix4f().ortho(-HALF_SIZE, HALF_SIZE, -HALF_SIZE, HALF_SIZE, NEAR, FAR);
        return proj.mul(view);
    }

    public void rotate(float dAzimuthDeg, float dElevationDeg) {
        azimuthDeg += dAzimuthDeg;
        elevationDeg = Math.max(10.0f, Math.min(85.0f, elevationDeg + dElevationDeg));
    }

    public void reset() { azimuthDeg = 40.0f; elevationDeg = 50.0f; }

    public Vector3f getColor() { return color; }
    public float getAzimuthDeg() { return azimuthDeg; }
    public float getElevationDeg() { return elevationDeg; }
}
