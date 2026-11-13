package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

/**
 * Membungkus Vertex Shader + Fragment Shader (GLSL 330 core):
 * membaca file dari folder resources, compile, link, dan menyediakan setter uniform.
 */
public class ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniforms = new HashMap<>();
    private final float[] matBuf = new float[16];

    public ShaderProgram(String vertexResource, String fragmentResource) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertexResource), vertexResource);
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragmentResource), fragmentResource);

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Gagal link shader program:\n" + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("File shader tidak ditemukan di resources: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int compile(int type, String source, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Gagal compile shader " + name + ":\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    public void use() { glUseProgram(programId); }

    private int loc(String name) {
        return uniforms.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setInt(String name, int v)         { glUniform1i(loc(name), v); }
    public void setBool(String name, boolean v)    { glUniform1i(loc(name), v ? 1 : 0); }
    public void setFloat(String name, float v)     { glUniform1f(loc(name), v); }
    public void setVec3(String name, Vector3f v)   { glUniform3f(loc(name), v.x, v.y, v.z); }
    public void setVec3(String name, float x, float y, float z) { glUniform3f(loc(name), x, y, z); }
    public void setMat4(String name, Matrix4f m)   { glUniformMatrix4fv(loc(name), false, m.get(matBuf)); }

    public void cleanup() { glDeleteProgram(programId); }
}
