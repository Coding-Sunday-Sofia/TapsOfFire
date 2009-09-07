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
package org.tof.gl.mesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.Matrix;

public class Mesh {
	
	public Mesh(GL10 gl,InputStream stream) throws IOException {
		m_lights=new ArrayList<Light>();
		m_transforms=new ArrayList<Transform>();
		BufferedReader reader=new BufferedReader(
			new InputStreamReader(stream),
			1024
		);
		while (true) {
			String line=reader.readLine();
			if (line==null) {
				break;
			}
			if (line.equals("Geometry")) {
				m_geometry=new Geometry(gl,reader);
				continue;
			}
			if (line.equals("Lights")) {
				readLights(reader);
				continue;
			}
			if (line.equals("Transforms")) {
				readTransforms(reader);
				continue;
			}
		}
		if (m_geometry==null) {
			throw new IOException("Invalid mesh: no geometry.");
		}
		applyTransforms();
	}
	
	public void bind(GL10 gl) {
		m_geometry.bind(gl);
	}
	
	public void unbind(GL10 gl) {
		m_geometry.unbind(gl);
	}
	
	/////////////////////////////////// rendering
	
	public void beginRender(GL10 gl) {
		if (m_lights.size()!=0) {
			gl.glEnable(GL10.GL_LIGHTING);
			for (int i=0,e=m_lights.size();i!=e;++i) {
				gl.glEnable(GL10.GL_LIGHT0+i);
				m_lights.get(i).apply(i,gl);
			}
		}
		m_geometry.beginRender(gl);
	}
	
	public void renderGeometry(GL10 gl) {
		if (m_transforms.size()!=0) {
			gl.glPushMatrix();
			for (int i=0,e=m_transforms.size();i!=e;++i) {
				m_transforms.get(i).apply(gl);
			}
		}
		m_geometry.doRender(gl);
		if (m_transforms.size()!=0) {
			gl.glPopMatrix();
		}
	}
	
	public void endRender(GL10 gl) {
		m_geometry.endRender(gl);
		if (m_lights.size()!=0) {
			for (int i=0;i!=m_lights.size();++i) {
				gl.glDisable(GL10.GL_LIGHT0+i);
			}
			gl.glDisable(GL10.GL_LIGHTING);
		}
	}
	
	public void render(GL10 gl) {
		beginRender(gl);
		renderGeometry(gl);
		endRender(gl);
	}
	
	///////////////////////////////////////////// implementation
	
	private void applyTransforms() {
		float[] matrix=new float[16];
		Matrix.setIdentityM(matrix,0);
		for (int i=0,e=m_transforms.size();i!=e;++i) {
			m_transforms.get(i).apply(matrix);
		}
		m_geometry.transform(matrix);
		m_transforms.clear();		
	}
	
	private void readLights(BufferedReader reader) throws IOException {
		while (true) {
			String line=Util.nextLine(reader);
			if (line.equals("EndLights")) {
				break;
			}
			if (line.startsWith("Light")) {
				Light light=new Light();
				light.read(reader);
				m_lights.add(light);
			}
		}
	}
	
	private void readTransforms(BufferedReader reader) throws IOException {
		while (true) {
			String line=Util.nextLine(reader);
			if (line.equals("EndTransforms")) {
				break;
			}
			if (line.startsWith("Translate")) {
				m_transforms.add(new TranslateTransform(Util.getColonValue(line)));
				continue;
			}
			if (line.startsWith("Rotate")) {
				m_transforms.add(new RotateTransform(Util.getColonValue(line)));
				continue;
			}
			if (line.startsWith("Scale")) {
				m_transforms.add(new ScaleTransform(Util.getColonValue(line)));
				continue;
			}
		}
	}
	
	/////////////////////////////////// data

	private Geometry m_geometry;
	private ArrayList<Light> m_lights;
	private ArrayList<Transform> m_transforms;
}	
