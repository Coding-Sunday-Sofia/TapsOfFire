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

import java.nio.ByteBuffer;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class GLBufferObject {

	public static GLBufferObject createVertices(int size,int type,ByteBuffer data) {
		return new GLBufferObject(GL10.GL_VERTEX_ARRAY,type,size,data);
	}

	public static GLBufferObject createTexcoords(int size,int type,ByteBuffer data) {
		return new GLBufferObject(GL10.GL_TEXTURE_COORD_ARRAY,type,size,data);
	}

	public static GLBufferObject createNormals(int type,ByteBuffer data) {
		return new GLBufferObject(GL10.GL_NORMAL_ARRAY,type,-1,data);
	}
	
	public void bind(GL10 gl) {
		if (m_bound) {
			return;
		}
		m_bound=true;
		if (GLHelpers.hasVBO(gl)) {
			GL11 gl11=(GL11)gl;
			m_buffer=GLHelpers.generateBuffer(gl11);
			gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER,m_buffer);
			gl11.glBufferData(GL11.GL_ARRAY_BUFFER,m_data.capacity(),m_data,GL11.GL_STATIC_DRAW);
			gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER,0);
		}
	}
	
	public void unbind(GL10 gl) {
		if (!m_bound) {
			return;
		}
		m_bound=false;
		if (gl!=null) {
			if (GLHelpers.hasVBO(gl)) {
				GL11 gl11=(GL11)gl;
				GLHelpers.deleteBuffer(gl11,m_buffer);
			}
		}
		m_buffer=0;
	}
	
	public void set(GL10 gl) {
		bind(gl);
		if (GLHelpers.hasVBO(gl)) {
			GL11 gl11=(GL11)gl;
			gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER,m_buffer);
			switch (m_type) {
				case GL10.GL_VERTEX_ARRAY:
					gl11.glVertexPointer(m_pointSize,m_dataType,0,0);
					break;
				case GL10.GL_TEXTURE_COORD_ARRAY:
					gl11.glTexCoordPointer(m_pointSize,m_dataType,0,0);
					break;
				case GL10.GL_NORMAL_ARRAY:
					gl11.glNormalPointer(m_dataType,0,0);
					break;
			}
			gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER,0);
		} else {
			switch (m_type) {
				case GL10.GL_VERTEX_ARRAY:
					gl.glVertexPointer(m_pointSize,m_dataType,0,m_data);
					break;
				case GL10.GL_TEXTURE_COORD_ARRAY:
					gl.glTexCoordPointer(m_pointSize,m_dataType,0,m_data);
					break;
				case GL10.GL_NORMAL_ARRAY:
					gl.glNormalPointer(m_dataType,0,m_data);
					break;
			}
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private GLBufferObject(int type,int dataType,int pointSize,ByteBuffer data) {
		m_type=type;
		m_dataType=dataType;
		m_pointSize=pointSize;
		m_data=data;
		m_data.position(0);
	}
	
	private int m_type;
	private int m_dataType;
	private int m_pointSize;
	private ByteBuffer m_data;
	private int m_buffer;
	private boolean m_bound;
}
