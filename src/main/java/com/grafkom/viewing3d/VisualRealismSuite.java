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
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * ============================================================
 *  3D VISUAL REALISM SUITE  (Grafika Komputer - Minggu 11)
 *  Memperluas aplikasi ThreeDViewing (Minggu 10): Camera3D,
 *  PerspectiveProjection & ProjectionTaxonomyManager dipakai ulang.
 * ============================================================
 *
 *  KONTROL
 *   Mouse drag kiri : orbit kamera            Scroll : zoom
 *   1 - 6           : jenis proyeksi (dari Minggu 10)
 *   [ / ]           : FOV - / +
 *   M               : ganti model (Kubus / Bola / Torus / Rumah)
 *
 *   B : Phong  <->  Blinn-Phong                (Indikator 1)
 *   F : Flat   <->  Smooth Shading             (Indikator 2)
 *   Z : Z-Buffer (Depth Test) ON/OFF           (Indikator 3)
 *   C : Back-Face Culling ON/OFF               (Indikator 3)
 *   T : Tekstur ON/OFF                         (Indikator 4)
 *   N : Filter Nearest <-> Bilinear+Mipmap     (Indikator 4)
 *   S : Shadow Mapping ON/OFF                  (Indikator 5)
 *   X : Shadow Bias ON/OFF (OFF = Shadow Acne) (Indikator 5)
 *   , / . : kecilkan / besarkan bias (besar = Peter Panning)
 *
 *   Panah kiri/kanan : putar cahaya (azimuth)   Panah atas/bawah : tinggi cahaya
 *   L : animasi (cahaya berputar + kubus kecil berputar)
 *   W : wireframe     V : VSync ON/OFF (untuk mengukur FPS)
 *   R : reset         ESC : keluar
 */
public class VisualRealismSuite {

    private static final int SHADOW_UNIT = 1;   // texture unit untuk Shadow Map

    private long window;

    // ---- Komponen dari Minggu 10 (dipakai ulang)
    private Camera3D camera;
    private PerspectiveProjection perspective;
    private ProjectionType currentType = ProjectionType.PERSPECTIVE_3POINT;

    // ---- Komponen baru Minggu 11
    private ShaderProgram mainShader, depthShader;
    private ShadowMapFBO shadowFBO;
    private LightSource light;
    private SceneObject ground, satellite, lamp;
    private SceneObject[] mainObjects;
    private Texture2D texBrick, texWood, texFloor;
    private int mainIndex = 0;

    // ---- Status fitur (toggle)
    private boolean blinnPhong = true;
    private boolean smoothShading = true;
    private boolean depthTest = true;
    private boolean backFaceCulling = true;
    private boolean useTexture = true;
    private boolean mipmapped = true;
    private boolean shadows = true;
    private boolean useBias = true;
    private float biasScale = 1.0f;
    private boolean animate = false;
    private boolean wireframe = false;
    private boolean vsync = true;

    // ---- Mouse, FPS, judul
    private boolean dragging = false;
    private double lastX, lastY;
    private double fpsTimer = 0.0;
    private int fpsFrames = 0;
    private double fps = 0.0;
    private boolean titleDirty = true;

    public static void main(String[] args) {
        new VisualRealismSuite().run();
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

    // ------------------------------------------------------------ inisialisasi window
    private void initWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal menginisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);               // Modern OpenGL 3.3 Core
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);       // wajib di macOS
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);                             // MSAA 4x

        window = glfwCreateWindow(1100, 750, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Gagal membuat window. Pastikan GPU/driver mendukung OpenGL 3.3 (update driver grafis).");
        }

        // ---- Keyboard
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action == GLFW_RELEASE) return;
            boolean press = (action == GLFW_PRESS);      // toggle hanya saat ditekan (bukan repeat)
            switch (key) {
                case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(w, true);

                case GLFW_KEY_1 -> selectProjection(ProjectionType.ORTHOGRAPHIC);
                case GLFW_KEY_2 -> selectProjection(ProjectionType.ISOMETRIC);
                case GLFW_KEY_3 -> selectProjection(ProjectionType.OBLIQUE_CAVALIER);
                case GLFW_KEY_4 -> selectProjection(ProjectionType.PERSPECTIVE_1POINT);
                case GLFW_KEY_5 -> selectProjection(ProjectionType.PERSPECTIVE_2POINT);
                case GLFW_KEY_6 -> selectProjection(ProjectionType.PERSPECTIVE_3POINT);
                case GLFW_KEY_LEFT_BRACKET  -> { perspective.changeFov(-2f); titleDirty = true; }
                case GLFW_KEY_RIGHT_BRACKET -> { perspective.changeFov(+2f); titleDirty = true; }

                case GLFW_KEY_M -> { if (press) { mainIndex = (mainIndex + 1) % mainObjects.length; log("Model: " + mainObjects[mainIndex].name); } }
                case GLFW_KEY_B -> { if (press) { blinnPhong = !blinnPhong; log("Shading model: " + (blinnPhong ? "Blinn-Phong" : "Phong")); } }
                case GLFW_KEY_F -> { if (press) { smoothShading = !smoothShading; log("Normal: " + (smoothShading ? "Smooth (per-vertex)" : "Flat (per-face)")); } }
                case GLFW_KEY_Z -> { if (press) { depthTest = !depthTest; log("Z-Buffer Depth Test: " + onOff(depthTest)); } }
                case GLFW_KEY_C -> { if (press) { backFaceCulling = !backFaceCulling; log("Back-Face Culling: " + onOff(backFaceCulling)); } }
                case GLFW_KEY_T -> { if (press) { useTexture = !useTexture; log("Texture: " + onOff(useTexture)); } }
                case GLFW_KEY_N -> { if (press) { mipmapped = !mipmapped; applyTextureFilter(); log("Filter: " + (mipmapped ? "Bilinear + Mipmap" : "Nearest")); } }
                case GLFW_KEY_S -> { if (press) { shadows = !shadows; log("Shadow Mapping: " + onOff(shadows)); } }
                case GLFW_KEY_X -> { if (press) { useBias = !useBias; log("Shadow Bias: " + onOff(useBias) + (useBias ? "" : "  (amati Shadow Acne)")); } }
                case GLFW_KEY_COMMA  -> { biasScale = Math.max(0.0f, biasScale - 0.25f); log(String.format(Locale.ROOT, "Bias scale: %.2f", biasScale)); }
                case GLFW_KEY_PERIOD -> { biasScale = Math.min(12.0f, biasScale + 0.25f); log(String.format(Locale.ROOT, "Bias scale: %.2f (besar -> Peter Panning)", biasScale)); }
                case GLFW_KEY_L -> { if (press) { animate = !animate; log("Animasi: " + onOff(animate)); } }
                case GLFW_KEY_W -> { if (press) { wireframe = !wireframe; log("Wireframe: " + onOff(wireframe)); } }
                case GLFW_KEY_V -> { if (press) { vsync = !vsync; glfwSwapInterval(vsync ? 1 : 0); log("VSync: " + onOff(vsync)); } }
                case GLFW_KEY_R -> { if (press) resetView(); }

                case GLFW_KEY_LEFT  -> light.rotate(-3f, 0f);
                case GLFW_KEY_RIGHT -> light.rotate(+3f, 0f);
                case GLFW_KEY_UP    -> light.rotate(0f, +3f);
                case GLFW_KEY_DOWN  -> light.rotate(0f, -3f);
                default -> { }
            }
            titleDirty = true;
        });

        // ---- Mouse
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

        System.out.println("OpenGL  : " + glGetString(GL_VERSION));
        System.out.println("Renderer: " + glGetString(GL_RENDERER));
    }

    // ------------------------------------------------------------ inisialisasi scene
    private void initScene() {
        glEnable(GL_MULTISAMPLE);

        // Kamera & proyeksi: sama seperti Minggu 10
        camera = new Camera3D(new Vector3f(0, 0, 10), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        perspective = new PerspectiveProjection(45.0f, 1100f / 750f, 0.1f, 100.0f);

        // Shader (GLSL) & Shadow Map FBO
        mainShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowFBO = new ShadowMapFBO();
        light = new LightSource();

        // Tekstur (Indikator 4)
        texBrick = new Texture2D("/textures/brick.png");
        texWood = new Texture2D("/textures/wood.png");
        texFloor = new Texture2D("/textures/floor.png");

        // Mesh (dibagi pakai: kubus & bola dipakai lebih dari satu objek)
        RenderMesh cubeMesh = new RenderMesh(MeshFactory.cube(1.0f));
        RenderMesh sphereMesh = new RenderMesh(MeshFactory.sphere(1.0f, 32, 48));
        RenderMesh torusMesh = new RenderMesh(MeshFactory.torus(0.9f, 0.4f, -0.6f, 56, 32));
        RenderMesh houseMesh = new RenderMesh(MeshFactory.house());
        RenderMesh planeMesh = new RenderMesh(MeshFactory.plane(6.0f, -1.0f, 4.0f));

        Vector3f origin = new Vector3f(0, 0, 0);
        mainObjects = new SceneObject[]{
                new SceneObject("Kubus",  cubeMesh,   texBrick, new Vector3f(origin), new Vector3f(0.90f, 0.45f, 0.20f)),
                new SceneObject("Bola",   sphereMesh, texWood,  new Vector3f(origin), new Vector3f(0.25f, 0.50f, 0.90f)),
                new SceneObject("Torus",  torusMesh,  texWood,  new Vector3f(origin), new Vector3f(0.85f, 0.30f, 0.70f)),
                new SceneObject("Rumah 3D", houseMesh, texBrick, new Vector3f(origin), new Vector3f(0.95f, 0.85f, 0.60f))
        };

        ground = new SceneObject("Lantai", planeMesh, texFloor, new Vector3f(origin), new Vector3f(0.6f, 0.6f, 0.6f));
        ground.specularStrength = 0.1f;
        ground.castsShadow = false;   // lantai hanya MENERIMA bayangan (menghindari acne pada lantai)

        satellite = new SceneObject("Kubus kecil", cubeMesh, texWood, new Vector3f(2.6f, 0.5f, 1.0f), new Vector3f(0.3f, 0.8f, 0.4f));
        satellite.scale = 0.5f;       // melayang: bayangannya terpisah jelas dari objek

        lamp = new SceneObject("Lampu", sphereMesh, texWood, new Vector3f(origin), new Vector3f(1, 1, 1));
        lamp.scale = 0.2f;
        lamp.castsShadow = false;

        selectProjection(currentType);
        log("Siap. Tekan B/F/Z/C/T/N/S untuk mencoba fitur. Judul window menampilkan status.");
    }

    // ------------------------------------------------------------ logika
    private void selectProjection(ProjectionType type) {
        currentType = type;
        camera.updateOrbitalPosition(camera.getRadius(), type.presetAzimuth, type.presetElevation);
        titleDirty = true;
    }

    private void applyTextureFilter() {
        texBrick.setMipmapped(mipmapped);
        texWood.setMipmapped(mipmapped);
        texFloor.setMipmapped(mipmapped);
    }

    private void resetView() {
        camera.updateOrbitalPosition(10.0f, currentType.presetAzimuth, currentType.presetElevation);
        perspective.setFovDegree(45.0f);
        light.reset();
        biasScale = 1.0f;
        blinnPhong = true; smoothShading = true; depthTest = true; backFaceCulling = true;
        useTexture = true; shadows = true; useBias = true; animate = false; wireframe = false;
        if (!mipmapped) { mipmapped = true; applyTextureFilter(); }
        satellite.rotationYDeg = 0f;
        log("Reset ke pengaturan awal");
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }
    private static void log(String s) { System.out.println("[Suite] " + s); }

    private void updateTitle() {
        glfwSetWindowTitle(window, String.format(Locale.ROOT,
                "%s | %s | %s | Depth %s | Cull %s | Tex %s %s | Shadow %s (bias %s x%.2f) | FPS %.0f | %s",
                mainObjects[mainIndex].name,
                blinnPhong ? "Blinn-Phong" : "Phong",
                smoothShading ? "Smooth" : "Flat",
                onOff(depthTest), onOff(backFaceCulling),
                onOff(useTexture), mipmapped ? "(Bilinear+Mip)" : "(Nearest)",
                onOff(shadows), onOff(useBias), biasScale,
                fps, currentType.label));
        titleDirty = false;
    }

    private void update(float dt) {
        if (animate) {
            light.rotate(dt * 25.0f, 0f);
            satellite.rotationYDeg += dt * 40.0f;
        }
        lamp.position.set(light.getPosition());
    }

    // ------------------------------------------------------------ render loop
    private void loop() {
        glClearColor(0.05f, 0.06f, 0.09f, 1.0f);
        double last = glfwGetTime();
        int[] fbW = new int[1], fbH = new int[1];

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            // hitung FPS tiap 0.5 detik
            fpsFrames++;
            fpsTimer += dt;
            if (fpsTimer >= 0.5) {
                fps = fpsFrames / fpsTimer;
                fpsFrames = 0;
                fpsTimer = 0.0;
                titleDirty = true;
            }

            glfwGetFramebufferSize(window, fbW, fbH);
            int w = Math.max(1, fbW[0]);
            int h = Math.max(1, fbH[0]);

            update(dt);
            renderFrame(w, h);
            if (titleDirty) updateTitle();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderFrame(int w, int h) {
        SceneObject main = mainObjects[mainIndex];
        Matrix4f lightSpace = light.getLightSpaceMatrix();

        // ============ RENDER PASS 1 : DEPTH PASS (sudut pandang CAHAYA) ============
        if (shadows) {
            shadowFBO.bindForWriting();                 // viewport 2048x2048 + clear depth
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            HSRManager.setupDepthTest(true);            // depth test selalu aktif di pass ini
            HSRManager.setupBackFaceCulling(false);
            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpace);
            for (SceneObject o : new SceneObject[]{ground, main, satellite}) {
                if (!o.castsShadow) continue;
                depthShader.setMat4("model", o.getModelMatrix());
                o.mesh.draw(true);
            }
            shadowFBO.unbind(w, h);                     // kembali ke layar
        }

        // ============ RENDER PASS 2 : LIGHTING PASS (sudut pandang KAMERA) ============
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        HSRManager.setupDepthTest(depthTest);           // tombol Z
        HSRManager.setupBackFaceCulling(backFaceCulling); // tombol C
        glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);

        float aspect = (float) w / h;
        perspective.setAspectRatio(aspect);
        Matrix4f proj = ProjectionTaxonomyManager.getProjectionMatrix(currentType, aspect, perspective, camera.getRadius());
        Matrix4f view = camera.calculateLookAtMatrix();
        Vector3f lightPos = light.getPosition();

        mainShader.use();
        mainShader.setMat4("projection", proj);
        mainShader.setMat4("view", view);
        mainShader.setMat4("lightSpaceMatrix", lightSpace);
        mainShader.setVec3("lightPos", lightPos);
        mainShader.setVec3("viewPos", camera.getEye());
        mainShader.setVec3("lightColor", light.getColor());
        mainShader.setBool("useBlinnPhong", blinnPhong);
        mainShader.setBool("useShadow", shadows);
        mainShader.setBool("useBias", useBias);
        mainShader.setFloat("biasScale", biasScale);
        mainShader.setInt("textureSampler", 0);
        mainShader.setInt("shadowMap", SHADOW_UNIT);
        shadowFBO.bindTexture(SHADOW_UNIT);             // Shadow Map -> unit 1
        mainShader.setBool("unlit", false);

        for (SceneObject o : new SceneObject[]{ground, main, satellite}) {
            drawObject(o);
        }

        // penanda posisi cahaya (digambar polos, tanpa bayangan)
        mainShader.setBool("unlit", true);
        mainShader.setMat4("model", lamp.getModelMatrix());
        lamp.mesh.draw(true);
        mainShader.setBool("unlit", false);

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glActiveTexture(GL_TEXTURE0);
    }

    private void drawObject(SceneObject o) {
        o.texture.bind(0);                              // Texture -> unit 0
        mainShader.setBool("useTexture", useTexture);
        mainShader.setVec3("objectColor", o.color);
        mainShader.setFloat("specularStrength", o.specularStrength);
        mainShader.setMat4("model", o.getModelMatrix());
        o.mesh.draw(smoothShading);
    }
}
