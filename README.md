# ThreeDViewing — Grafika Komputer (Minggu 10 + Minggu 11)

Project ini berisi dua praktikum dalam satu codebase (package `com.grafkom.viewing3d`):

| Minggu | Main class | Isi |
|---|---|---|
| 10 | `ThreeDViewing` | 3D Viewing: kamera, proyeksi, stereo anaglyph (OpenGL fixed-function) |
| 11 | `VisualRealismSuite` | **3D Visual Realism Suite**: Phong/Blinn-Phong, Flat/Smooth, Z-Buffer & Culling, Texture Mapping, Shadow Mapping (Modern OpenGL 3.3 + GLSL) |

## Menjalankan
```
mvn compile exec:java                                                         # Minggu 11 (default)
mvn compile exec:java -Dexec.mainClass=com.grafkom.viewing3d.ThreeDViewing    # Minggu 10
```
Atau buka folder ini di IntelliJ / NetBeans / VS Code lalu run `main()` dari class di atas.
(macOS: tambahkan VM option `-XstartOnFirstThread`.)

## Kontrol Minggu 11
| Tombol | Fungsi | Indikator |
|---|---|---|
| **B** | Phong ⇄ Blinn-Phong | 1 |
| **F** | Flat ⇄ Smooth Shading | 2 |
| **Z** | Z-Buffer (Depth Test) ON/OFF | 3 |
| **C** | Back-Face Culling ON/OFF | 3 |
| **T** | Tekstur ON/OFF | 4 |
| **N** | Filter Nearest ⇄ Bilinear+Mipmap | 4 |
| **S** | Shadow Mapping ON/OFF | 5 |
| **X** | Shadow Bias ON/OFF (OFF → Shadow Acne) | 5 |
| **, / .** | Kecilkan / besarkan bias (besar → Peter Panning) | 5 |
| **M** | Ganti model: Kubus / Bola / Torus / Rumah | — |
| **Panah** | Geser posisi cahaya (kiri-kanan = azimuth, atas-bawah = tinggi) | — |
| **L** | Animasi cahaya + kubus kecil berputar | — |
| **W** | Wireframe | — |
| **V** | VSync ON/OFF (untuk uji FPS) | — |
| **1–6** | Jenis proyeksi (dari Minggu 10) | — |
| **[ / ]** | FOV − / + | — |
| Mouse drag / scroll | Orbit / zoom kamera | — |
| **R** / **ESC** | Reset / keluar | — |

Status semua fitur tampil di **judul window** dan di console.

## Struktur kode Minggu 11
```
src/main/java/com/grafkom/viewing3d/
  VisualRealismSuite.java   main class: window, input, render 2-pass
  MeshBuilder.java          vertex + UV, hitung normal FLAT & SMOOTH (averaging)
  MeshFactory.java          Kubus, Bola, Torus, Rumah 3D, Lantai
  RenderMesh.java           VAO/VBO (versi flat & smooth)
  ShaderProgram.java        load/compile/link GLSL + setter uniform
  Texture2D.java            STBImage + UV filtering (Nearest / Mipmap)
  HSRManager.java           Z-Buffer & Back-Face Culling
  ShadowMapFBO.java         FBO + Depth Texture 2048x2048
  LightSource.java          cahaya orbital + Light Space Matrix
  SceneObject.java          mesh + tekstur + transformasi
  (Camera3D, PerspectiveProjection, ProjectionTaxonomyManager = dari Minggu 10)
src/main/resources/shaders/ phong.vert phong.frag depth.vert depth.frag
src/main/resources/textures/ brick.png wood.png floor.png
```
