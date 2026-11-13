package com.grafkom.viewing3d;

import com.grafkom.viewing3d.ProjectionTaxonomyManager.ProjectionType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.Locale;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Main class: 3D Viewing Interactive Visualizer.
 *
 * KONTROL
 *   Mouse drag kiri : orbit kamera        Scroll        : zoom
 *   Tombol 1 - 6    : ganti jenis proyeksi
 *   S               : stereo anaglyph ON/OFF (hanya mode perspektif 4-6)
 *   Panah atas/bawah: FOV +/-             Panah kiri/kanan : IPD -/+
 *   M               : ganti model (cube / rumah)
 *   R               : reset                ESC : keluar
 */
public class ThreeDViewing {

    private long window;

    private Camera3D camera;
    private PerspectiveProjection perspective;
    private StereoAnaglyphRenderer stereo;
    private Mesh3D cube, house, currentMesh;

    private ProjectionType currentType = ProjectionType.PERSPECTIVE_3POINT;
    private boolean stereoEnabled = false;
    private boolean titleDirty = true;

    // status mouse
    private boolean dragging = false;
    private double lastX, lastY;

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }

    private void run() {
        initWindow();
        initScene();
        loop();
        Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }

    // ------------------------------------------------------------ inisialisasi
    private void initWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal menginisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(1000, 700, "3D Viewing", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window GLFW");

        // ---- Keyboard ----
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action == GLFW_RELEASE) return;
            boolean press = (action == GLFW_PRESS);
            switch (key) {
                case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(w, true);
                case GLFW_KEY_1 -> selectProjection(ProjectionType.ORTHOGRAPHIC);
                case GLFW_KEY_2 -> selectProjection(ProjectionType.ISOMETRIC);
                case GLFW_KEY_3 -> selectProjection(ProjectionType.OBLIQUE_CAVALIER);
                case GLFW_KEY_4 -> selectProjection(ProjectionType.PERSPECTIVE_1POINT);
                case GLFW_KEY_5 -> selectProjection(ProjectionType.PERSPECTIVE_2POINT);
                case GLFW_KEY_6 -> selectProjection(ProjectionType.PERSPECTIVE_3POINT);
                case GLFW_KEY_S -> { if (press) { stereoEnabled = !stereoEnabled; titleDirty = true; } }
                case GLFW_KEY_M -> { if (press) { currentMesh = (currentMesh == cube) ? house : cube; } }
                case GLFW_KEY_R -> { if (press) resetView(); }
                case GLFW_KEY_UP    -> { perspective.changeFov(+2f); titleDirty = true; }
                case GLFW_KEY_DOWN  -> { perspective.changeFov(-2f); titleDirty = true; }
                case GLFW_KEY_RIGHT -> { stereo.changeIpd(+0.02f); titleDirty = true; }
                case GLFW_KEY_LEFT  -> { stereo.changeIpd(-0.02f); titleDirty = true; }
                default -> { }
            }
        });

        // ---- Mouse ----
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) dragging = (action == GLFW_PRESS);
        });
        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (dragging) {
                float dx = (float) (x - lastX);
                float dy = (float) (y - lastY);
                camera.orbit(-dx * 0.4f, dy * 0.4f);
                titleDirty = true;
            }
            lastX = x;
            lastY = y;
        });
        glfwSetScrollCallback(window, (w, xoff, yoff) -> {
            camera.zoom(-(float) yoff * 0.8f);
            titleDirty = true;
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();
    }

    private void initScene() {
        camera = new Camera3D(new Vector3f(0, 0, 8), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        perspective = new PerspectiveProjection(45.0f, 1000f / 700f, 0.1f, 100.0f);
        stereo = new StereoAnaglyphRenderer();
        cube = Mesh3D.createColorCube();
        house = Mesh3D.createHouse();
        currentMesh = cube;
        selectProjection(currentType);   // atur kamera ke preset awal
    }

    // ------------------------------------------------------------ logika
    private void selectProjection(ProjectionType type) {
        currentType = type;
        camera.updateOrbitalPosition(camera.getRadius(), type.presetAzimuth, type.presetElevation);
        titleDirty = true;
    }

    private void resetView() {
        camera.updateOrbitalPosition(8.0f, currentType.presetAzimuth, currentType.presetElevation);
        perspective.setFovDegree(45.0f);
        stereo.setIpd(0.20f);
        titleDirty = true;
    }

    private void updateTitle() {
        String stereoInfo = currentType.perspective
                ? (stereoEnabled ? "Stereo ON" : "Stereo OFF")
                : "Stereo n/a (pilih mode 4-6)";
        glfwSetWindowTitle(window, String.format(Locale.ROOT,
                "[%s] | %s | FOV %.0f | IPD %.2f | jarak %.1f",
                currentType.label, stereoInfo, perspective.getFovDegree(),
                stereo.getIpd(), camera.getRadius()));
        titleDirty = false;
    }

    // ------------------------------------------------------------ render
    private void loop() {
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.04f, 0.04f, 0.07f, 1.0f);

        int[] fbW = new int[1], fbH = new int[1];
        while (!glfwWindowShouldClose(window)) {
            glfwGetFramebufferSize(window, fbW, fbH);
            int w = Math.max(1, fbW[0]);
            int h = Math.max(1, fbH[0]);
            glViewport(0, 0, w, h);
            float aspect = (float) w / h;
            perspective.setAspectRatio(aspect);

            if (titleDirty) updateTitle();

            if (stereoEnabled && currentType.perspective) {
                stereo.renderAnaglyph(camera, perspective, aspect, this::drawScene);
            } else {
                glColorMask(true, true, true, true);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                Matrix4f proj = ProjectionTaxonomyManager.getProjectionMatrix(
                        currentType, aspect, perspective, camera.getRadius());
                ProjectionTaxonomyManager.loadMatrices(proj, camera.calculateLookAtMatrix());
                drawScene();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    /** Hanya menggambar geometri. Matriks sudah di-load oleh pemanggil. */
    private void drawScene() {
        drawGrid();
        currentMesh.draw();
    }

    private void drawGrid() {
        glLineWidth(1.0f);
        glColor3f(0.45f, 0.45f, 0.45f);
        glBegin(GL_LINES);
        for (int i = -6; i <= 6; i++) {
            glVertex3f(i, -1.0f, -6.0f);  glVertex3f(i, -1.0f, 6.0f);
            glVertex3f(-6.0f, -1.0f, i);  glVertex3f(6.0f, -1.0f, i);
        }
        glEnd();
    }
}
