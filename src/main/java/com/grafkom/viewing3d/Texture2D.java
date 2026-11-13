package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Indikator 4 - Texture Mapping.
 * Membaca PNG/JPG dari folder resources (classpath) menggunakan STBImage, lalu memuatnya ke GPU.
 * Dua mode filtering yang bisa ditukar saat runtime:
 *   - Nearest            : GL_NEAREST (blocky / pikselasi)
 *   - Bilinear + Mipmap  : GL_LINEAR_MIPMAP_LINEAR (min) + GL_LINEAR (mag)
 */
public class Texture2D {

    private final int textureID;
    private boolean mipmapped = true;

    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Wrap parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        stbi_set_flip_vertically_on_load(true);   // v = 0 di bagian bawah gambar (konvensi OpenGL)
        ByteBuffer fileData = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            byte[] bytes = readBytes(resourcePath);
            ByteBuffer data = null;
            if (bytes != null) {
                fileData = MemoryUtil.memAlloc(bytes.length);
                fileData.put(bytes).flip();
                data = stbi_load_from_memory(fileData, width, height, channels, 4);
            }

            if (data != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                        GL_RGBA, GL_UNSIGNED_BYTE, data);
                glGenerateMipmap(GL_TEXTURE_2D);
                stbi_image_free(data);
            } else {
                System.err.println("Failed to load texture: " + resourcePath + " -> memakai tekstur pengganti (papan catur magenta)");
                uploadFallbackChecker();
            }
        } finally {
            if (fileData != null) MemoryUtil.memFree(fileData);
        }
        applyFilter();
    }

    private static byte[] readBytes(String resourcePath) {
        try (InputStream in = Texture2D.class.getResourceAsStream(resourcePath)) {
            return (in == null) ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /** Tekstur cadangan 64x64 supaya program tidak crash jika file gambar hilang. */
    private void uploadFallbackChecker() {
        int n = 64;
        ByteBuffer px = MemoryUtil.memAlloc(n * n * 4);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                boolean on = (((x / 8) + (y / 8)) % 2) == 0;
                px.put((byte) (on ? 255 : 0)).put((byte) 0).put((byte) (on ? 255 : 0)).put((byte) 255);
            }
        }
        px.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, n, n, 0, GL_RGBA, GL_UNSIGNED_BYTE, px);
        glGenerateMipmap(GL_TEXTURE_2D);
        MemoryUtil.memFree(px);
    }

    /** true = Bilinear + Mipmap (halus), false = Nearest Neighbor (blocky). */
    public void setMipmapped(boolean mipmapped) {
        this.mipmapped = mipmapped;
        glBindTexture(GL_TEXTURE_2D, textureID);
        applyFilter();
    }

    private void applyFilter() {
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() { glDeleteTextures(textureID); }
}
