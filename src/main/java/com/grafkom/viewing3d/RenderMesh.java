package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh di GPU (VAO + VBO). Menyimpan DUA buffer sekaligus:
 *   - versi FLAT   (normal per-face)
 *   - versi SMOOTH (normal per-vertex hasil rata-rata)
 * sehingga tombol F cukup memilih VAO mana yang digambar.
 * Layout atribut sesuai phong.vert: 0 = posisi, 1 = normal, 2 = UV.
 */
public class RenderMesh {

    private final int flatVao, flatVbo, flatCount;
    private final int smoothVao, smoothVbo, smoothCount;

    public RenderMesh(MeshBuilder builder) {
        float[] flat = builder.buildFlat();
        float[] smooth = builder.buildSmooth();
        int[] f = upload(flat);
        int[] s = upload(smooth);
        flatVao = f[0];   flatVbo = f[1];   flatCount = flat.length / MeshBuilder.FLOATS_PER_VERTEX;
        smoothVao = s[0]; smoothVbo = s[1]; smoothCount = smooth.length / MeshBuilder.FLOATS_PER_VERTEX;
    }

    private static int[] upload(float[] data) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);

        int stride = MeshBuilder.FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return new int[]{vao, vbo};
    }

    public void draw(boolean smoothShading) {
        glBindVertexArray(smoothShading ? smoothVao : flatVao);
        glDrawArrays(GL_TRIANGLES, 0, smoothShading ? smoothCount : flatCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteVertexArrays(flatVao);   glDeleteBuffers(flatVbo);
        glDeleteVertexArrays(smoothVao); glDeleteBuffers(smoothVbo);
    }
}
