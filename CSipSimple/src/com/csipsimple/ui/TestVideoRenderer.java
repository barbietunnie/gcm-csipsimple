/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.ui;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.pjsip.pjsua.pjsua;


import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;


public class TestVideoRenderer implements Renderer {

    private Context context;
    
    public TestVideoRenderer(Context context)
    {
            this.context = context;
    }
    
    
    
    private IntBuffer texturesBuffer;
    

    private void LoadTextures(GL10 gl) {
            // create textures
            gl.glEnable(GL10.GL_TEXTURE_2D);
            texturesBuffer = IntBuffer.allocate(1);
            gl.glGenTextures(1, texturesBuffer);
            
            // setup video texture
            gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(0));
            gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
            gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
            
    }
    

    private static float[] quadCoords = new float[] {
            0, 1, 0,
            1, 1, 0,
            0, 0, 0,
            1, 0, 0
    };
    private static float[] quadTexCoords = new float[] {
        0, 0,
        1, 0,
        0, 1,
        1, 1
};    
    

    private static FloatBuffer quadBuffer;
    private static FloatBuffer quadTexBuffer;
    
    static
    {
            quadBuffer = (FloatBuffer) ByteBuffer.allocateDirect( 3 * 4 * 4 )
            				.order(ByteOrder.nativeOrder()).asFloatBuffer()
            				.put(quadCoords).position(0);
            quadTexBuffer = (FloatBuffer) ByteBuffer.allocateDirect( 2 * 4 * 4 )
							.order(ByteOrder.nativeOrder()).asFloatBuffer()
							.put(quadTexCoords).position(0);
    }

    

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glShadeModel(GL10.GL_SMOOTH);
            gl.glClearColor(0, 0, 0, 0);

            gl.glClearDepthf(1.0f);
            gl.glEnable(GL10.GL_DEPTH_TEST);
            gl.glDepthFunc(GL10.GL_LEQUAL);
            
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    }


    
	public void onDrawFrame(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(0));

		float[] w = new float[] { 1.0f };
		float[] h = new float[] { 1.0f };
		pjsua.pjmedia_ogl_surface_draw(w, h);

		quadTexBuffer.put(2, w[0]).put(6, w[0]);
		quadTexBuffer.put(5, h[0]).put(7, h[0]);

	//	gl.glTranslatef(0.0f, 0.0f, -0.5f);

		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, quadBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, quadTexBuffer);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

	}

    public void onSurfaceChanged(GL10 gl, int width, int height) {
    	LoadTextures(gl);
            // avoid division by zero
            if (height == 0) height = 1;
            // draw on the entire screen
            gl.glViewport(0, 0, width, height);
            // setup projection matrix
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(0f, 1.0f, 0f, 1.0f, -1.0f, 1.0f);
            //GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 1.0f, 100.0f);
    }

}
