package com.grafkom.viewing3d;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Indikator 3 - Stereo View (Anaglyph Merah-Cyan).
 * Memakai metode OFF-AXIS (asymmetric frustum), bukan toe-in:
 *  - Mata kiri  : hanya kanal MERAH
 *  - Mata kanan : hanya kanal HIJAU + BIRU (CYAN)
 * Kacamata merah-cyan memisahkan keduanya sehingga muncul kesan kedalaman.
 */
public class StereoAnaglyphRenderer {

    private float ipd = 0.20f;   // Interpupillary Distance (jarak antar mata) dalam unit dunia

    public void renderAnaglyph(Camera3D camera, PerspectiveProjection persp,
                               float aspect, Runnable renderSceneCallback) {
        Matrix4f centerView = camera.calculateLookAtMatrix();

        float near = persp.getZNear();
        float far = persp.getZFar();
        float focalLength = camera.getRadius();   // jarak bidang zero-parallax = jarak ke target

        // Ukuran frustum simetris pada bidang near
        float top = near * (float) Math.tan(Math.toRadians(persp.getFovDegree()) / 2.0);
        float right = top * aspect;

        float halfIpd = ipd / 2.0f;
        float shift = halfIpd * near / focalLength; // pergeseran frustum (asimetris)

        // --- Mata KIRI: kamera digeser ke kiri, frustum bergeser ke kanan ---
        Matrix4f projLeft = new Matrix4f().frustum(-right + shift, right + shift, -top, top, near, far);
        Matrix4f viewLeft = new Matrix4f().translation(halfIpd, 0f, 0f).mul(centerView);

        // --- Mata KANAN: kamera digeser ke kanan, frustum bergeser ke kiri ---
        Matrix4f projRight = new Matrix4f().frustum(-right - shift, right - shift, -top, top, near, far);
        Matrix4f viewRight = new Matrix4f().translation(-halfIpd, 0f, 0f).mul(centerView);

        // Bersihkan SEMUA kanal warna dulu (agar tidak ada sisa frame sebelumnya)
        glColorMask(true, true, true, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // PASS 1: mata kiri -> kanal merah saja
        glColorMask(true, false, false, true);
        ProjectionTaxonomyManager.loadMatrices(projLeft, viewLeft);
        renderSceneCallback.run();

        // PASS 2: mata kanan -> kanal hijau + biru (cyan). Hanya depth yang di-clear.
        glColorMask(false, true, true, true);
        glClear(GL_DEPTH_BUFFER_BIT);
        ProjectionTaxonomyManager.loadMatrices(projRight, viewRight);
        renderSceneCallback.run();

        // Kembalikan ke normal
        glColorMask(true, true, true, true);
    }

    public void changeIpd(float delta) {
        ipd = Math.max(0.0f, Math.min(1.0f, ipd + delta));
    }

    public float getIpd() { return ipd; }
    public void setIpd(float ipd) { this.ipd = ipd; }
}
