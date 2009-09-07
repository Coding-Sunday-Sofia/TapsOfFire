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
import java.nio.ByteBuffer;
import javax.microedition.khronos.opengles.GL10;
import org.tof.gl.GLBufferObject;
import org.tof.gl.GLHelpers;
import org.tof.gl.GLIndexBufferUS;
import org.tof.util.MathHelpers;
import android.opengl.Matrix;

public class Geometry {
	
	public Geometry(GL10 gl,BufferedReader reader) throws IOException {
		float[] allNormals=null;
		float[] allVertices=null;
		int count=-1;
		while (true) {
			String line=Util.nextLine(reader);
			if (line.equals("EndGeometry")) {
				break;
			}
			if (line.startsWith("count")) {
				count=Util.parseInt(Util.getColonValue(line));
				continue;
			}
			if (line.equals("normals") || line.equals("vertices")) {
				if (count==-1) {
					throw invalidGeometryException("count is not specified");
				}
			}
			if (line.equals("normals")) {
				allNormals=new float[count];
				for (int i=0;i!=count;++i) {
					allNormals[i]=Util.parseFloat(reader.readLine());
				}
				continue;
			}
			if (line.equals("vertices")) {
				allVertices=new float[count];
				for (int i=0;i!=count;++i) {
					allVertices[i]=Util.parseFloat(reader.readLine());
				}
				continue;
			}
		}
		if (allNormals==null || allVertices==null) {
			throw invalidGeometryException("normals/vertices are not specified");
		}
		float[] minVertices=new float[count];
		float[] minNormals=new float[count];
		short[] minIndices=new short[count/3];
		int minCount=0;
		for (int i=0;i!=count;i+=3) {
			int index=-1;
			for (int j=0;j!=minCount;j+=3) {
				int n=0;
				for (;n!=3;++n) {
					if (minVertices[j+n]!=allVertices[i+n]) {
						break;
					}
				}
				if (n==3) {
					index=j;
					break;
				}
			}
			if (index!=-1) {
				int n=0;
				for (;n!=3;++n) {
					if (minNormals[index+n]!=allNormals[i+n]) {
						break;
					}
				}
				if (n!=3) {
					index=-1;
				}
			}
			if (index==-1) {
				for (int n=0;n!=3;++n) {
					minVertices[minCount+n]=allVertices[i+n];
				}
				for (int n=0;n!=3;++n) {
					minNormals[minCount+n]=allNormals[i+n];
				}
				index=minCount;
				minCount+=3;
			}
			minIndices[i/3]=(short)(index/3);
		}
		m_count=minCount;
		m_vertices=minVertices;
		m_normals=minNormals;
		m_indices=minIndices;
	}
	
	public void bind(GL10 gl) {
		if (m_bound) {
			return;
		}
		if (m_normalsBuffer==null) {
			ByteBuffer normals=GLHelpers.allocateFloatBuffer(m_count);
			for (int i=0;i!=m_count;++i) {
				normals.putInt(MathHelpers.float2fixed(m_normals[i]));
			}
			m_normalsBuffer=GLBufferObject.createNormals(GL10.GL_FIXED,normals);
		}
		m_normalsBuffer.bind(gl);

		if (m_verticesBuffer==null) {
			ByteBuffer vertices=GLHelpers.allocateFloatBuffer(m_count);
			for (int i=0;i!=m_count;++i) {
				vertices.putInt(MathHelpers.float2fixed(m_vertices[i]));
			}
			m_verticesBuffer=GLBufferObject.createVertices(3,GL10.GL_FIXED,vertices);
		}
		m_verticesBuffer.bind(gl);

		if (m_indexBuffer==null) {
			ByteBuffer indices=GLHelpers.allocateShortBuffer(m_count/3);
			for (int i=0;i!=(m_count/3);++i) {
				indices.putShort(m_indices[i]);
			}
			m_indexBuffer=new GLIndexBufferUS(GL10.GL_TRIANGLES,indices);
		}
		m_indexBuffer.bind(gl);
		
		m_bound=true;
	}
	
	public void unbind(GL10 gl) {
		if (!m_bound) {
			return;
		}
		m_normalsBuffer.unbind(gl);
		m_verticesBuffer.unbind(gl);
		m_indexBuffer.unbind(gl);
		m_bound=false;
	}
	
	public void transform(float[] matrix) {
		float[] buffer=new float[8];
		transform(matrix,m_vertices,m_count,buffer);
		transform(matrix,m_normals,m_count,buffer);
	}
	
	/////////////////////////////////// rendering
	
	public void beginRender(GL10 gl) {
		bind(gl);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		m_normalsBuffer.set(gl);
		m_verticesBuffer.set(gl);
		m_indexBuffer.beginRender(gl);
	}
	
	public void doRender(GL10 gl) {
		m_indexBuffer.doRender(gl);
	}
	
	public void endRender(GL10 gl) {
		m_indexBuffer.endRender(gl);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
	
	public void render(GL10 gl) {
		beginRender(gl);
		doRender(gl);
		endRender(gl);
	}
	
	///////////////////////////////////////////// implementation
	
	private static void transform(float[] matrix,float[] points,int count,float[] buffer) {
		buffer[3]=1;
		for (int i=0;i!=count;i+=3) {
			buffer[0]=points[i];
			buffer[1]=points[i+1];
			buffer[2]=points[i+2];
			Matrix.multiplyMV(buffer,4,matrix,0,buffer,0);
			points[i]=buffer[4];
			points[i+1]=buffer[5];
			points[i+2]=buffer[6];
		}
	}
	
	private static IOException invalidGeometryException(String why)  {
		return new IOException("Invalid geometry: "+why+".");
	}
	
	/////////////////////////////////// data
	
	private int m_count;
	private float[] m_vertices;
	private float[] m_normals;
	private short[] m_indices;
	
	private GLIndexBufferUS m_indexBuffer;
	private GLBufferObject m_normalsBuffer;
	private GLBufferObject m_verticesBuffer;
	private boolean m_bound;
}
