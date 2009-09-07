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

public class GLIndexBufferUS {
	
	public GLIndexBufferUS(int mode,ByteBuffer data) {
		m_mode=mode;
		m_data=data;
		m_data.position(0);
	}
	
	public void bind(GL10 gl) {
		if (GLHelpers.hasVBO(gl)) {
			GL11 gl11=(GL11)gl;
			m_buffer=GLHelpers.generateBuffer(gl11);
			gl11.glBindBuffer(
				GL11.GL_ELEMENT_ARRAY_BUFFER,m_buffer);
			gl11.glBufferData(
				GL11.GL_ELEMENT_ARRAY_BUFFER,
				m_data.capacity(),m_data,
				GL11.GL_STATIC_DRAW);
			gl11.glBindBuffer(
				GL11.GL_ELEMENT_ARRAY_BUFFER,0);
		}
		m_bound=true;
	}
	
	public void unbind(GL10 gl) {
		if (!m_bound) {
			return;
		}
		if (GLHelpers.hasVBO(gl)) {
			GL11 gl11=(GL11)gl;
			GLHelpers.deleteBuffer(gl11,m_buffer);
		}
		m_bound=false;
	}
	
	public void beginRender(GL10 gl) {
		bind(gl);
		if (GLHelpers.hasVBO(gl)) {
			GL11 gl11=(GL11)gl;
			gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER,m_buffer);
		}
	}
	
	public void doRender(GL10 gl) {
		if (GLHelpers.hasVBO(gl)) {
			GL11 gl11=(GL11)gl;
			gl11.glDrawElements(
				m_mode,
				m_data.capacity()/2,GL10.GL_UNSIGNED_SHORT,0);
		} else {
			gl.glDrawElements(
				m_mode,
				m_data.capacity()/2,GL10.GL_UNSIGNED_SHORT,m_data);
		}
	}
	
	public void endRender(GL10 gl) {
		if (GLHelpers.hasVBO(gl)) {
			GL11 gl11=(GL11)gl;
			gl11.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER,0);
		}
	}
	
	public void render(GL10 gl) {
		beginRender(gl);
		doRender(gl);
		endRender(gl);
	}
	
	///////////////////////////////////////////// implementation
	
	private int m_mode;
	private ByteBuffer m_data;
	private int m_buffer;
	private boolean m_bound;
}
