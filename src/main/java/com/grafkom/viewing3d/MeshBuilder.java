package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Indikator 2 - Pembangun mesh murni CPU (tanpa OpenGL).
 *
 * Menyimpan vertex (posisi + UV) dan segitiga (urutan CCW = tampak depan), lalu menghasilkan
 * DUA versi array vertex "interleaved" dengan layout per-vertex:
 *      [ x y z | nx ny nz | u v ]   (8 float)
 *
 *  - buildFlat()   : FLAT SHADING   -> setiap segitiga memakai 1 normal face (N_face).
 *  - buildSmooth() : SMOOTH SHADING -> normal vertex = rata-rata normal semua face yang
 *                    berbagi posisi vertex yang sama:
 *                        N_vertex = (sum N_face_i) / || sum N_face_i ||
 */
public class MeshBuilder {

    public static final int FLOATS_PER_VERTEX = 8;

    private final List<float[]> verts = new ArrayList<>();  // {x, y, z, u, v}
    private final List<int[]> tris = new ArrayList<>();     // indeks 3 vertex, urutan CCW

    /** Kunci posisi (dibulatkan) agar vertex di seam UV / sudut tajam tetap dianggap satu titik. */
    private record PosKey(long x, long y, long z) { }

    public int addVertex(float x, float y, float z, float u, float v) {
        verts.add(new float[]{x, y, z, u, v});
        return verts.size() - 1;
    }

    public void addTriangle(int a, int b, int c) {
        tris.add(new int[]{a, b, c});
    }

    /** Segiempat (a,b,c,d) dipecah menjadi 2 segitiga: (a,b,c) dan (a,c,d). */
    public void addQuad(int a, int b, int c, int d) {
        addTriangle(a, b, c);
        addTriangle(a, c, d);
    }

    public int getTriangleCount() { return tris.size(); }

    // ---------------------------------------------------------------- normal face
    /** Normal face (satuan) = normalisasi( (B-A) x (C-A) ). Mengembalikan null bila segitiga degenerate. */
    private float[] faceNormal(int[] t) {
        float[] a = verts.get(t[0]), b = verts.get(t[1]), c = verts.get(t[2]);
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-8f) return null;
        return new float[]{nx / len, ny / len, nz / len};
    }

    private static PosKey keyOf(float[] v) {
        return new PosKey(Math.round(v[0] * 10000.0), Math.round(v[1] * 10000.0), Math.round(v[2] * 10000.0));
    }

    private static int put(float[] buf, int p, float[] v, float[] n) {
        buf[p++] = v[0]; buf[p++] = v[1]; buf[p++] = v[2];   // posisi
        buf[p++] = n[0]; buf[p++] = n[1]; buf[p++] = n[2];   // normal
        buf[p++] = v[3]; buf[p++] = v[4];                    // UV
        return p;
    }

    // ---------------------------------------------------------------- FLAT
    public float[] buildFlat() {
        float[] buf = new float[tris.size() * 3 * FLOATS_PER_VERTEX];
        int p = 0;
        for (int[] t : tris) {
            float[] n = faceNormal(t);
            if (n == null) continue;                 // buang segitiga degenerate (kutub bola)
            for (int k = 0; k < 3; k++) p = put(buf, p, verts.get(t[k]), n);
        }
        return Arrays.copyOf(buf, p);
    }

    // ---------------------------------------------------------------- SMOOTH
    public float[] buildSmooth() {
        // 1) jumlahkan normal face pada setiap posisi vertex
        Map<PosKey, float[]> sum = new HashMap<>();
        for (int[] t : tris) {
            float[] n = faceNormal(t);
            if (n == null) continue;
            for (int k = 0; k < 3; k++) {
                float[] s = sum.computeIfAbsent(keyOf(verts.get(t[k])), key -> new float[3]);
                s[0] += n[0]; s[1] += n[1]; s[2] += n[2];
            }
        }
        // 2) normalisasi jumlah tersebut -> normal vertex, lalu tulis ke array
        float[] buf = new float[tris.size() * 3 * FLOATS_PER_VERTEX];
        int p = 0;
        for (int[] t : tris) {
            float[] fn = faceNormal(t);
            if (fn == null) continue;
            for (int k = 0; k < 3; k++) {
                float[] v = verts.get(t[k]);
                float[] s = sum.get(keyOf(v));
                float len = (float) Math.sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2]);
                float[] n = (len < 1e-8f) ? fn : new float[]{s[0] / len, s[1] / len, s[2] / len};
                p = put(buf, p, v, n);
            }
        }
        return Arrays.copyOf(buf, p);
    }
}
