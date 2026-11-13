package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * Model 3D sederhana: kumpulan vertex + face (segitiga/segiempat) + warna per face.
 * Tersedia 2 model siap pakai: Color Cube dan Rumah 3D.
 */
public class Mesh3D {

    private final float[] vertices;      // x,y,z berurutan
    private final int[][] faces;         // indeks vertex per face (3 atau 4)
    private final float[][] faceColors;  // r,g,b per face

    public Mesh3D(float[] vertices, int[][] faces, float[][] faceColors) {
        this.vertices = vertices;
        this.faces = faces;
        this.faceColors = faceColors;
    }

    public void draw() {
        // 1) Isi permukaan (didorong sedikit ke belakang agar garis tepi tidak "berebut" depth)
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, 1.0f);
        for (int i = 0; i < faces.length; i++) {
            int[] f = faces[i];
            float[] c = faceColors[i];
            glBegin(f.length == 4 ? GL_QUADS : GL_TRIANGLES);
            glColor3f(c[0], c[1], c[2]);
            for (int idx : f) {
                glVertex3f(vertices[idx * 3], vertices[idx * 3 + 1], vertices[idx * 3 + 2]);
            }
            glEnd();
        }
        glDisable(GL_POLYGON_OFFSET_FILL);

        // 2) Garis tepi hitam
        glLineWidth(2.0f);
        glColor3f(0f, 0f, 0f);
        for (int[] f : faces) {
            glBegin(GL_LINE_LOOP);
            for (int idx : f) {
                glVertex3f(vertices[idx * 3], vertices[idx * 3 + 1], vertices[idx * 3 + 2]);
            }
            glEnd();
        }
    }

    // ---------------------------------------------------------------- model
    public static Mesh3D createColorCube() {
        float[] v = {
            -1,-1,-1,   1,-1,-1,   1, 1,-1,  -1, 1,-1,   // 0..3 (z = -1, belakang)
            -1,-1, 1,   1,-1, 1,   1, 1, 1,  -1, 1, 1    // 4..7 (z = +1, depan)
        };
        int[][] f = {
            {4,5,6,7},  // depan
            {1,0,3,2},  // belakang
            {0,4,7,3},  // kiri
            {5,1,2,6},  // kanan
            {3,7,6,2},  // atas
            {0,1,5,4}   // bawah
        };
        float[][] c = {
            {1.0f,0.35f,0.35f}, {0.35f,1.0f,0.35f}, {0.35f,0.45f,1.0f},
            {1.0f,1.0f,0.35f},  {1.0f,0.45f,1.0f},  {0.35f,1.0f,1.0f}
        };
        return new Mesh3D(v, f, c);
    }

    public static Mesh3D createHouse() {
        float[] v = {
            -1.2f,-1.0f,-1.0f,   1.2f,-1.0f,-1.0f,   1.2f,-1.0f, 1.0f,  -1.2f,-1.0f, 1.0f, // 0..3 dasar
            -1.2f, 0.5f,-1.0f,   1.2f, 0.5f,-1.0f,   1.2f, 0.5f, 1.0f,  -1.2f, 0.5f, 1.0f, // 4..7 atas dinding
             0.0f, 1.5f,-1.0f,   0.0f, 1.5f, 1.0f                                            // 8..9 puncak atap
        };
        int[][] f = {
            {3,2,6,7},   // dinding depan
            {1,0,4,5},   // dinding belakang
            {0,3,7,4},   // dinding kiri
            {2,1,5,6},   // dinding kanan
            {7,6,9},     // gable depan
            {5,4,8},     // gable belakang
            {4,7,9,8},   // atap kiri
            {6,5,8,9}    // atap kanan
        };
        float[][] c = {
            {0.95f,0.85f,0.60f}, {0.95f,0.85f,0.60f}, {0.85f,0.75f,0.50f}, {0.85f,0.75f,0.50f},
            {0.95f,0.90f,0.75f}, {0.95f,0.90f,0.75f}, {0.80f,0.25f,0.20f}, {0.70f,0.20f,0.15f}
        };
        return new Mesh3D(v, f, c);
    }
}
