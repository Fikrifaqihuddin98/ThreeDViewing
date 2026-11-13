package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Indikator 1 - Kamera 3D (Synthetic Camera Model).
 * Menyimpan eye, target (At), up, lalu menghitung matriks LookAt SECARA MANUAL.
 * Mendukung navigasi ORBITAL (kamera berputar mengelilingi target).
 */
public class Camera3D {

    private final Vector3f eye;
    private final Vector3f target;
    private final Vector3f up;

    // Koordinat sferis kamera orbital
    private float radius;
    private float azimuthDeg;
    private float elevationDeg;

    public Camera3D(Vector3f eye, Vector3f target, Vector3f up) {
        this.eye = new Vector3f(eye);
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);

        // Hitung radius, azimuth, elevation awal dari posisi eye
        Vector3f d = new Vector3f(eye).sub(target);
        this.radius = d.length();
        this.azimuthDeg = (float) Math.toDegrees(Math.atan2(d.x, d.z));
        this.elevationDeg = (float) Math.toDegrees(Math.asin(d.y / radius));
    }

    /** Perhitungan manual Matriks LookAt (View Matrix) sesuai rumus modul. */
    public Matrix4f calculateLookAtMatrix() {
        Vector3f n = new Vector3f();
        eye.sub(target, n).normalize();          // n = (E - At) / |E - At|

        Vector3f u = new Vector3f();
        up.cross(n, u).normalize();              // u = (Up x n) / |Up x n|

        Vector3f v = new Vector3f();
        n.cross(u, v);                           // v = n x u

        Matrix4f view = new Matrix4f();
        // Baris 1: u, -dot(u,E)
        view.m00(u.x).m10(u.y).m20(u.z).m30(-u.dot(eye));
        // Baris 2: v, -dot(v,E)
        view.m01(v.x).m11(v.y).m21(v.z).m31(-v.dot(eye));
        // Baris 3: n, -dot(n,E)
        view.m02(n.x).m12(n.y).m22(n.z).m32(-n.dot(eye));
        // Baris 4: 0 0 0 1
        view.m03(0f).m13(0f).m23(0f).m33(1f);
        return view;
    }

    /** Update posisi eye dari koordinat sferis (mengelilingi target). */
    public void updateOrbitalPosition(float radius, float azimuthDeg, float elevationDeg) {
        this.radius = Math.max(2.0f, Math.min(40.0f, radius));
        this.azimuthDeg = azimuthDeg;
        this.elevationDeg = Math.max(-89.0f, Math.min(89.0f, elevationDeg));

        float azRad = (float) Math.toRadians(this.azimuthDeg);
        float elRad = (float) Math.toRadians(this.elevationDeg);
        eye.x = target.x + this.radius * (float) (Math.cos(elRad) * Math.sin(azRad));
        eye.y = target.y + this.radius * (float) Math.sin(elRad);
        eye.z = target.z + this.radius * (float) (Math.cos(elRad) * Math.cos(azRad));
    }

    public void orbit(float dAzimuthDeg, float dElevationDeg) {
        updateOrbitalPosition(radius, azimuthDeg + dAzimuthDeg, elevationDeg + dElevationDeg);
    }

    public void zoom(float dRadius) {
        updateOrbitalPosition(radius + dRadius, azimuthDeg, elevationDeg);
    }

    public Vector3f getEye()   { return new Vector3f(eye); }
    public Vector3f getTarget(){ return new Vector3f(target); }
    public float getRadius()   { return radius; }
    public float getAzimuthDeg()   { return azimuthDeg; }
    public float getElevationDeg() { return elevationDeg; }
}
