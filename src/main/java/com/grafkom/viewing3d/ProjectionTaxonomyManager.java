package com.grafkom.viewing3d;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Indikator 4 - Taksonomi Proyeksi.
 * Enum ProjectionType menyimpan 6 jenis proyeksi beserta sudut kamera "preset"-nya.
 *
 *  PARALEL    : Orthographic, Isometric (aksonometrik), Oblique Cavalier
 *  PERSPEKTIF : 1 titik hilang, 2 titik hilang, 3 titik hilang
 */
public class ProjectionTaxonomyManager {

    public enum ProjectionType {
        //                 label                          perspektif? azimuth elevasi
        ORTHOGRAPHIC      ("Orthographic (tampak depan)",  false,   0.0f,   0.0f),
        ISOMETRIC         ("Isometric (aksonometrik)",     false,  45.0f,  35.264f),
        OBLIQUE_CAVALIER  ("Oblique Cavalier",             false,   0.0f,   0.0f),
        PERSPECTIVE_1POINT("Perspektif 1 titik hilang",    true,    0.0f,   0.0f),
        PERSPECTIVE_2POINT("Perspektif 2 titik hilang",    true,   45.0f,   0.0f),
        PERSPECTIVE_3POINT("Perspektif 3 titik hilang",    true,   45.0f,  30.0f);

        public final String label;
        public final boolean perspective;
        public final float presetAzimuth;
        public final float presetElevation;

        ProjectionType(String label, boolean perspective, float az, float el) {
            this.label = label;
            this.perspective = perspective;
            this.presetAzimuth = az;
            this.presetElevation = el;
        }
    }

    private static final float[] BUF = new float[16];

    /** Membuat matriks proyeksi sesuai jenis proyeksi yang dipilih. */
    public static Matrix4f getProjectionMatrix(ProjectionType type, float aspect,
                                               PerspectiveProjection persp, float cameraRadius) {
        // Setengah tinggi volume pandang paralel: dibuat mengikuti jarak kamera
        // supaya scroll mouse (zoom) tetap terasa pada proyeksi paralel.
        float s = cameraRadius * (float) Math.tan(Math.toRadians(persp.getFovDegree()) / 2.0);
        float near = persp.getZNear();
        float far = persp.getZFar();

        switch (type) {
            case ORTHOGRAPHIC:
            case ISOMETRIC:
                return new Matrix4f().ortho(-s * aspect, s * aspect, -s, s, near, far);

            case OBLIQUE_CAVALIER: {
                // Shearing sumbu z: x' = x - k*cos(a)*z ; y' = y - k*sin(a)*z
                // Cavalier -> k = 1 (skala kedalaman penuh), sudut a = 45 derajat
                float k = 1.0f;
                float a = (float) Math.toRadians(45.0);
                Matrix4f shear = new Matrix4f()
                        .m20(-k * (float) Math.cos(a))
                        .m21(-k * (float) Math.sin(a));
                return new Matrix4f().ortho(-s * aspect, s * aspect, -s, s, near, far).mul(shear);
            }

            default:   // PERSPECTIVE_1POINT / 2POINT / 3POINT
                return persp.calculateProjectionMatrix();
        }
    }

    /** Mengirim matriks proyeksi & view ke pipeline OpenGL (fixed-function). */
    public static void loadMatrices(Matrix4f projection, Matrix4f view) {
        glMatrixMode(GL_PROJECTION);
        glLoadMatrixf(projection.get(BUF));
        glMatrixMode(GL_MODELVIEW);
        glLoadMatrixf(view.get(BUF));
    }
}
