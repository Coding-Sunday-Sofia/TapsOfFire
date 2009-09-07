/*
 * Taps of Fire
 * Copyright (C) 2009 Dmitry Skiba
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package org.tof.gl;

import javax.microedition.khronos.opengles.GL10;
import android.opengl.Matrix;

public class ReGLU {

    /**
     * Define a viewing transformation in terms of an eye point, a center of
     * view, and an up vector.
     * 
     * This version doesn't allocate any memory, it uses static buffer.
     * 
     * @param gl a GL10 interface
     * @param eyeX eye point X
     * @param eyeY eye point Y
     * @param eyeZ eye point Z
     * @param centerX center of view X
     * @param centerY center of view Y
     * @param centerZ center of view Z
     * @param upX up vector X
     * @param upY up vector Y
     * @param upZ up vector Z
     */
    public static void gluLookAt(GL10 gl, float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ, float upX, float upY,
            float upZ) {

        // See the OpenGL GLUT documentation for gluLookAt for a description
        // of the algorithm. We implement it in a straightforward way:

        float fx = centerX - eyeX;
        float fy = centerY - eyeY;
        float fz = centerZ - eyeZ;

        // Normalize f
        float rlf = 1.0f / Matrix.length(fx, fy, fz);
        fx *= rlf;
        fy *= rlf;
        fz *= rlf;

        // compute s = f x up (x means "cross product")
        float sx = fy * upZ - fz * upY;
        float sy = fz * upX - fx * upZ;
        float sz = fx * upY - fy * upX;
        
        // and normalize s
        float rls = 1.0f / Matrix.length(sx, sy, sz);
        sx *= rls;
        sy *= rls;
        sz *= rls;
        
        // compute u = s x f
        float ux = sy * fz - sz * fy;
        float uy = sz * fx - sx * fz;
        float uz = sx * fy - sy * fx;

        float[] m = MATRIX_BUFFER;
        m[0] = sx;
        m[1] = ux;
        m[2] = -fx;
        m[3] = 0.0f;

        m[4] = sy;
        m[5] = uy;
        m[6] = -fy;
        m[7] = 0.0f;

        m[8] = sz;
        m[9] = uz;
        m[10] = -fz;
        m[11] = 0.0f;

        m[12] = 0.0f;
        m[13] = 0.0f;
        m[14] = 0.0f;
        m[15] = 1.0f;

        gl.glMultMatrixf(m, 0);
        gl.glTranslatef(-eyeX, -eyeY, -eyeZ);
    }

    /**
     * Define a viewing transformation in terms of an eye point, a center of
     * view, and an up vector.
     * 
     * This version sets matrix.
     * 
     * @param gl a GL10 interface
     * @param eyeX eye point X
     * @param eyeY eye point Y
     * @param eyeZ eye point Z
     * @param centerX center of view X
     * @param centerY center of view Y
     * @param centerZ center of view Z
     * @param upX up vector X
     * @param upY up vector Y
     * @param upZ up vector Z
     */
    public static void gluLookAt(float[] matrix, float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ, float upX, float upY,
            float upZ) {

        // See the OpenGL GLUT documentation for gluLookAt for a description
        // of the algorithm. We implement it in a straightforward way:

        float fx = centerX - eyeX;
        float fy = centerY - eyeY;
        float fz = centerZ - eyeZ;

        // Normalize f
        float rlf = 1.0f / Matrix.length(fx, fy, fz);
        fx *= rlf;
        fy *= rlf;
        fz *= rlf;

        // compute s = f x up (x means "cross product")
        float sx = fy * upZ - fz * upY;
        float sy = fz * upX - fx * upZ;
        float sz = fx * upY - fy * upX;
        
        // and normalize s
        float rls = 1.0f / Matrix.length(sx, sy, sz);
        sx *= rls;
        sy *= rls;
        sz *= rls;
        
        // compute u = s x f
        float ux = sy * fz - sz * fy;
        float uy = sz * fx - sx * fz;
        float uz = sx * fy - sy * fx;

        float[] m = matrix;
        m[0] = sx;
        m[1] = ux;
        m[2] = -fx;
        m[3] = 0.0f;

        m[4] = sy;
        m[5] = uy;
        m[6] = -fy;
        m[7] = 0.0f;

        m[8] = sz;
        m[9] = uz;
        m[10] = -fz;
        m[11] = 0.0f;

        m[12] = 0.0f;
        m[13] = 0.0f;
        m[14] = 0.0f;
        m[15] = 1.0f;

        Matrix.translateM(m,0,-eyeX, -eyeY, -eyeZ);
    }

    /**
     * Set up a perspective projection matrix
     * 
     * @param gl a GL10 interface
     * @param fovy specifies the field of view angle, in degrees, in the Y
     *        direction.
     * @param aspect specifies the aspect ration that determins the field of
     *        view in the x direction. The aspect ratio is the ratio of x
     *        (width) to y (height).
     * @param zNear specifies the distance from the viewer to the near clipping
     *        plane (always positive).
     * @param zFar specifies the distance from the viewer to the far clipping
     *        plane (always positive).
     */
    public static void gluPerspective(float[] matrix, float fovy, float aspect,
            float zNear, float zFar) {
        float top = zNear * (float) Math.tan(fovy * (Math.PI / 360.0));
        float bottom = -top;
        float left = bottom * aspect;
        float right = top * aspect;
        Matrix.frustumM(matrix, 0, left, right, bottom, top, zNear, zFar);
    }
    
	private static final float[] MATRIX_BUFFER=new float[16];
}
