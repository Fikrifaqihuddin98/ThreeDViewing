package com.grafkom.viewing3d;

/**
 * Indikator 2 & 4 - Pabrik model 3D. Setiap model memiliki:
 *   (a) koordinat posisi, (b) urutan vertex CCW (untuk back-face culling),
 *   (c) koordinat tekstur UV. Vektor normal dihitung oleh MeshBuilder (flat & smooth).
 *
 * Semua model diletakkan agar sisi bawahnya menyentuh bidang lantai y = -1
 * (sama seperti grid pada praktikum Minggu 10).
 */
public final class MeshFactory {

    private MeshFactory() { }

    /** Menambah 1 segiempat. Titik p0..p3 berurutan CCW dari luar: kiri-bawah, kanan-bawah, kanan-atas, kiri-atas. */
    private static void quad(MeshBuilder b, float[] p0, float[] p1, float[] p2, float[] p3, float us, float vs) {
        int a = b.addVertex(p0[0], p0[1], p0[2], 0, 0);
        int c = b.addVertex(p1[0], p1[1], p1[2], us, 0);
        int d = b.addVertex(p2[0], p2[1], p2[2], us, vs);
        int e = b.addVertex(p3[0], p3[1], p3[2], 0, vs);
        b.addQuad(a, c, d, e);
    }

    private static float[] p(float x, float y, float z) { return new float[]{x, y, z}; }

    // ---------------------------------------------------------------- KUBUS
    public static MeshBuilder cube(float h) {
        MeshBuilder b = new MeshBuilder();
        quad(b, p(-h,-h, h), p( h,-h, h), p( h, h, h), p(-h, h, h), 1, 1);  // depan  (+z)
        quad(b, p( h,-h,-h), p(-h,-h,-h), p(-h, h,-h), p( h, h,-h), 1, 1);  // belakang (-z)
        quad(b, p( h,-h, h), p( h,-h,-h), p( h, h,-h), p( h, h, h), 1, 1);  // kanan  (+x)
        quad(b, p(-h,-h,-h), p(-h,-h, h), p(-h, h, h), p(-h, h,-h), 1, 1);  // kiri   (-x)
        quad(b, p(-h, h, h), p( h, h, h), p( h, h,-h), p(-h, h,-h), 1, 1);  // atas   (+y)
        quad(b, p(-h,-h,-h), p( h,-h,-h), p( h,-h, h), p(-h,-h, h), 1, 1);  // bawah  (-y)
        return b;
    }

    // ---------------------------------------------------------------- BOLA (UV sphere)
    public static MeshBuilder sphere(float r, int stacks, int slices) {
        MeshBuilder b = new MeshBuilder();
        int[][] idx = new int[stacks + 1][slices + 1];
        for (int i = 0; i <= stacks; i++) {
            double theta = Math.PI * i / stacks;              // 0 (kutub atas) .. PI (kutub bawah)
            for (int j = 0; j <= slices; j++) {
                double phi = 2.0 * Math.PI * j / slices;      // 0 .. 2PI
                float x = (float) (r * Math.sin(theta) * Math.cos(phi));
                float y = (float) (r * Math.cos(theta));
                float z = (float) (r * Math.sin(theta) * Math.sin(phi));
                float u = (float) j / slices * 2.0f;          // 2x pengulangan horizontal
                float v = 1.0f - (float) i / stacks;
                idx[i][j] = b.addVertex(x, y, z, u, v);
            }
        }
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = idx[i][j], bb = idx[i + 1][j], c = idx[i + 1][j + 1], d = idx[i][j + 1];
                b.addTriangle(a, c, bb);
                b.addTriangle(a, d, c);
            }
        }
        return b;
    }

    // ---------------------------------------------------------------- TORUS (donat)
    public static MeshBuilder torus(float bigR, float smallR, float centerY, int rings, int sides) {
        MeshBuilder b = new MeshBuilder();
        int[][] idx = new int[rings + 1][sides + 1];
        for (int i = 0; i <= rings; i++) {
            double u = 2.0 * Math.PI * i / rings;
            for (int j = 0; j <= sides; j++) {
                double v = 2.0 * Math.PI * j / sides;
                float x = (float) ((bigR + smallR * Math.cos(v)) * Math.cos(u));
                float y = (float) (centerY + smallR * Math.sin(v));
                float z = (float) ((bigR + smallR * Math.cos(v)) * Math.sin(u));
                idx[i][j] = b.addVertex(x, y, z, (float) i / rings * 4.0f, (float) j / sides);
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                int a = idx[i][j], bb = idx[i + 1][j], c = idx[i + 1][j + 1], d = idx[i][j + 1];
                b.addTriangle(a, c, bb);
                b.addTriangle(a, d, c);
            }
        }
        return b;
    }

    // ---------------------------------------------------------------- RUMAH 3D (dari Minggu 10)
    public static MeshBuilder house() {
        MeshBuilder b = new MeshBuilder();
        float[] v0 = p(-1.2f,-1.0f,-1.0f), v1 = p( 1.2f,-1.0f,-1.0f), v2 = p( 1.2f,-1.0f, 1.0f), v3 = p(-1.2f,-1.0f, 1.0f);
        float[] v4 = p(-1.2f, 0.5f,-1.0f), v5 = p( 1.2f, 0.5f,-1.0f), v6 = p( 1.2f, 0.5f, 1.0f), v7 = p(-1.2f, 0.5f, 1.0f);
        float[] v8 = p( 0.0f, 1.5f,-1.0f), v9 = p( 0.0f, 1.5f, 1.0f);

        quad(b, v3, v2, v6, v7, 1.2f, 0.75f);   // dinding depan
        quad(b, v1, v0, v4, v5, 1.2f, 0.75f);   // dinding belakang
        quad(b, v0, v3, v7, v4, 1.0f, 0.75f);   // dinding kiri
        quad(b, v2, v1, v5, v6, 1.0f, 0.75f);   // dinding kanan
        quad(b, v0, v1, v2, v3, 1.2f, 1.0f);    // alas (tertutup)
        quad(b, v4, v7, v9, v8, 1.0f, 0.7f);    // atap kiri
        quad(b, v6, v5, v8, v9, 1.0f, 0.7f);    // atap kanan

        // gable (segitiga) depan: v7, v6, v9   |  belakang: v5, v4, v8
        int g1 = b.addVertex(v7[0], v7[1], v7[2], 0.0f, 0.0f);
        int g2 = b.addVertex(v6[0], v6[1], v6[2], 1.2f, 0.0f);
        int g3 = b.addVertex(v9[0], v9[1], v9[2], 0.6f, 0.5f);
        b.addTriangle(g1, g2, g3);
        int h1 = b.addVertex(v5[0], v5[1], v5[2], 0.0f, 0.0f);
        int h2 = b.addVertex(v4[0], v4[1], v4[2], 1.2f, 0.0f);
        int h3 = b.addVertex(v8[0], v8[1], v8[2], 0.6f, 0.5f);
        b.addTriangle(h1, h2, h3);
        return b;
    }

    // ---------------------------------------------------------------- LANTAI
    /** Bidang datar (menghadap +y) pada ketinggian y, ukuran 2*half x 2*half, UV diulang 'repeat' kali. */
    public static MeshBuilder plane(float half, float y, float repeat) {
        MeshBuilder b = new MeshBuilder();
        quad(b, p(-half, y, half), p(half, y, half), p(half, y, -half), p(-half, y, -half), repeat, repeat);
        return b;
    }
}
