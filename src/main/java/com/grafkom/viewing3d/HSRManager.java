package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * Indikator 3 - Hidden Surface Removal (HSR).
 *  - Z-Buffer (Depth Test)
 *  - Back-Face Culling (winding order CCW = tampak depan)
 */
public class HSRManager {

    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // Pixel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);     // Buang polygon belakang
            glFrontFace(GL_CCW);     // Counter-Clockwise adalah tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }
}
