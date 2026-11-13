package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Satu objek di dalam scene: mesh + tekstur + transformasi + materi. */
public class SceneObject {

    public final String name;
    public final RenderMesh mesh;
    public final Texture2D texture;
    public final Vector3f position;
    public final Vector3f color;           // dipakai bila tekstur dimatikan (tombol T)
    public float scale = 1.0f;
    public float rotationYDeg = 0.0f;
    public float specularStrength = 0.8f;
    public boolean castsShadow = true;

    public SceneObject(String name, RenderMesh mesh, Texture2D texture, Vector3f position, Vector3f color) {
        this.name = name;
        this.mesh = mesh;
        this.texture = texture;
        this.position = position;
        this.color = color;
    }

    /** Model Matrix = Translate * RotateY * Scale */
    public Matrix4f getModelMatrix() {
        return new Matrix4f()
                .translate(position)
                .rotateY((float) Math.toRadians(rotationYDeg))
                .scale(scale);
    }
}
