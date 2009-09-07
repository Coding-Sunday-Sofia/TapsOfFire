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
import javax.microedition.khronos.opengles.GL10;

class Light {
	
	public Light() {
	}
	
	public void apply(int index,GL10 gl) {
		if (m_position!=null) {
			gl.glLightfv(GL10.GL_LIGHT0+index,GL10.GL_POSITION,m_position,0);
		}
		if (m_diffuse!=null) {
			gl.glLightfv(GL10.GL_LIGHT0+index,GL10.GL_DIFFUSE,m_diffuse,0);
		}
		if (m_ambient!=null) {
			gl.glLightfv(GL10.GL_LIGHT0+index,GL10.GL_AMBIENT,m_ambient,0);
		}
	}
	
	public void read(BufferedReader reader) throws IOException {
		while (true) {
			String line=Util.nextLine(reader);
			if (line.equals("EndLight")) {
				break;
			}
			if (line.startsWith("Position")) {
				m_position=Util.parseFloatArray(4,Util.getColonValue(line));
				continue;
			}
			if (line.startsWith("Diffuse")) {
				m_diffuse=Util.parseFloatArray(4,Util.getColonValue(line));
				continue;
			}
			if (line.startsWith("Ambient")) {
				m_ambient=Util.parseFloatArray(4,Util.getColonValue(line));
				continue;
			}
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private float[] m_position;
	private float[] m_diffuse;
	private float[] m_ambient;
}