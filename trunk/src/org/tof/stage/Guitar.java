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
package org.tof.stage;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.microedition.khronos.opengles.GL10;
import org.tof.Config;
import org.tof.R;
import org.tof.gl.GLBufferObject;
import org.tof.gl.GLHelpers;
import org.tof.gl.GLRect;
import org.tof.gl.ReGLU;
import org.tof.gl.mesh.Mesh;
import org.tof.song.EventList;
import org.tof.song.NoteEvent;
import org.tof.song.Song;
import android.content.Context;
import android.util.FloatMath;

class Guitar {
	
	public Guitar(Song song) {
		m_song=song;
	}
	
	/////////////////////////////////// setup

	public void setActiveStrings(int strings) {
		m_activeStrings=strings;
	}
	
	public void setReadiness(float readiness) {
		m_readiness=readiness;
	}
	
	public void setPosition(int position,float bpm) {
		m_position=position;
		m_bpm=bpm;
	}
	
	/////////////////////////////////// gl
	
	public void loadResources(Context context,GL10 gl) throws IOException {
		m_noteMesh=new Mesh(gl,context.getAssets().open("note.mesh"));
		m_stringTexture=GLHelpers.loadTexture(gl,context,R.drawable.string);
		m_barTexture=GLHelpers.loadTexture(gl,context,R.drawable.bar);
		loadWaveformResources(context,gl);
	}
	
	public void unloadResources(GL10 gl) {
		if (m_stringTexture!=0) {
			if (gl!=null) {
				GLHelpers.deleteTexture(gl,m_stringTexture);
			}
			m_stringTexture=0;
		}
		if (m_barTexture!=0) {
			if (gl!=null) {
				GLHelpers.deleteTexture(gl,m_barTexture);
			}
			m_barTexture=0;
		}
		unloadWaveformResources(gl);
	}
	
	public void setViewport(GLRect viewport) {
		m_viewport=new GLRect(viewport);
		lookAtBoard(
			FOV_Y,
			m_viewport.width/m_viewport.height,
			Song.STRING_COUNT*STRING_SPACE,
			BOARD_LENGTH*3);
		ReGLU.gluPerspective(
			m_projectionMatrix,
			FOV_Y,
			m_viewport.width/m_viewport.height,
			1,100);
		
	}
	
	public void render(GL10 gl) {
		GLHelpers.setViewport(
			gl,
			m_viewport.x,m_viewport.y,
			m_viewport.width,m_viewport.height);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadMatrixf(m_projectionMatrix,0);
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadMatrixf(m_modelviewMatrix,0);
		
		//gl.glEnable(GL10.GL_DEPTH_TEST);
		//gl.glEnable(GL10.GL_FOG);
		gl.glFogfv(GL10.GL_FOG_COLOR,FOG_COLOR,0);
		gl.glFogf(GL10.GL_FOG_START,0);//BOARD_LENGTH);
		gl.glFogf(GL10.GL_FOG_END,BOARD_LENGTH);
		gl.glFogx(GL10.GL_FOG_MODE,GL10.GL_LINEAR);
		
		//m_meter.begin();
		
		renderBoard(gl);
		renderNotes(gl);
		
		//m_meter.end();
		
		gl.glDisable(GL10.GL_FOG);
		//gl.glDisable(GL10.GL_DEPTH_TEST);
	}
	
	/////////////////////////////////// strings*
	
	public static final int stringsAdd(int strings,int string) {
		return strings | (1<<string);
	}
	public static final int stringsRemove(int strings,int string) {
		return string & ~(1<<string);
	}
	public static final boolean stringsCheck(int strings,int string) {
		return (strings & (1<<string))!=0;
	}
	
	public static final int STRING_0=stringsAdd(0,0);
	public static final int STRING_1=stringsAdd(0,1);
	public static final int STRING_2=stringsAdd(0,2);
	
	///////////////////////////////////////////// implementation
	
	/////////////////////////////////// board
	
	private void renderBoard(GL10 gl) {
		float beatPeriod=60000.0f/m_bpm;
		float timeToUnits=BOARD_LENGTH_AHEAD/(beatPeriod*BEATS_AHEAD);
		
		GLHelpers.setColor(gl,Config.getBaseColor(),m_readiness);
		
		{
			gl.glBindTexture(GL10.GL_TEXTURE_2D,m_stringTexture);
			gl.glPushMatrix();
			gl.glScalef(STRING_WIDTH,1,BOARD_LENGTH);
			gl.glTranslatef(STRING_SPACE/STRING_WIDTH,0,+0.5f);
			
			for (int string=0;string!=Song.STRING_COUNT;++string) {
				if (Guitar.stringsCheck(m_activeStrings,string)) {
					gl.glScalef(2f,1,1);
				}
				GLHelpers.drawTextureXZ(gl);
				if (Guitar.stringsCheck(m_activeStrings,string)) {
					gl.glScalef(1/2f,1,1);
				}
				gl.glTranslatef(-STRING_SPACE/STRING_WIDTH,0,0);
			}
			
			
			gl.glPopMatrix();
		}
		
		{
			gl.glBindTexture(GL10.GL_TEXTURE_2D,m_barTexture);
			gl.glPushMatrix();

			int beatsAhead=BEATS_AHEAD+1;
			float z=(m_position%beatPeriod)*timeToUnits;
			if (z<0) {
				z=-z;
			} else {
				z=beatPeriod*timeToUnits-z;
			}
			if (z>BAR_WIDTH) {
				beatsAhead-=1;
			}
			gl.glTranslatef(0,0,z+SADDLE_OFFSET);
			gl.glScalef(
				(Song.STRING_COUNT-1)*STRING_SPACE+NOTE_HEAD_SIZE*2,
				1,
				BAR_WIDTH);
			float dz=beatPeriod*timeToUnits/BAR_WIDTH;
			for (int i=0;i!=beatsAhead;++i) {
				GLHelpers.drawTextureLineX(gl);
				gl.glTranslatef(0,0,dz);
			}
			
			gl.glPopMatrix();
		}
	}
	
	private void lookAtBoard(float fovy,float aspect,float boardWidth,float boardLength) {
		float tgA2=(float)Math.tan(fovy/2*(Math.PI/180));
		float sinA=(float)Math.sin(fovy  *(Math.PI/180));
		
		float K=boardWidth/(2*tgA2*aspect);
		float R=boardLength/(K*sinA);
		
		float eyeZ;
		{
			float a=R*R;
			float b=2*boardLength;
			float c=K*K+boardLength*boardLength-R*R*K*K;
			float d=b*b-4*a*c;
			d=FloatMath.sqrt(d);
			eyeZ=(-b+d)/(2*a);
		}
		
		float eyeY=FloatMath.sqrt(K*K-eyeZ*eyeZ);
		
		float tgO=eyeZ/eyeY;
		float centerZ=eyeY*(tgO+tgA2)/(1-tgO*tgA2)-eyeZ;

		ReGLU.gluLookAt(m_modelviewMatrix,0,eyeY,-eyeZ,0,0,centerZ,0,1,0);
		m_lookEye[1]=eyeY;
		m_lookEye[2]=-eyeZ;
		m_lookCenter[2]=centerZ;
	}
	
	///////////////////////////////////////////// notes
	
//	private AvgElapsedMeter m_meter=new AvgElapsedMeter("______________________-> ",100);
	
//	private NoteEvent m_fakeNote=new NoteEvent(0,456,1343);
	
	private void renderNotes(GL10 gl) {
		
//		{
//			gl.glPushMatrix();
//			gl.glBindTexture(GL10.GL_TEXTURE_2D,m_stringTexture);
//			
//			gl.glTranslatef(STRING_SPACE,0,SADDLE_OFFSET+0.3f);
//			m_fakeNote.setString(0);
//			m_fakeNote.pick();
//			renderNoteTail(gl,4,m_fakeNote,false);
//			
//			gl.glTranslatef(-STRING_SPACE,0,0);
//			m_fakeNote.setString(1);
//			m_fakeNote.pick();
//			renderNoteTail(gl,4,m_fakeNote,false);
//	
//			gl.glTranslatef(-STRING_SPACE,0,0);
//			m_fakeNote.setString(2);
//			m_fakeNote.makeIntact();			
//			renderNoteTail(gl,4,m_fakeNote,false);
//			
//			gl.glPopMatrix();
//		}
//		
//		{
//			gl.glDisable(GL10.GL_TEXTURE_2D);
//			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
//			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
//			
//			gl.glEnable(GL10.GL_CULL_FACE);
//			gl.glFrontFace(GL10.GL_CCW);
//			m_noteMesh.beginRender(gl);
//			gl.glEnable(GL10.GL_COLOR_MATERIAL);
//	
//			gl.glPushMatrix();
//			
//			m_fakeNote.pick();
//			
//			gl.glTranslatef(STRING_SPACE,0,SADDLE_OFFSET+0.3f);
//			m_fakeNote.setString(0);
//			renderNoteHead(gl,m_fakeNote);
//			
//			gl.glTranslatef(-STRING_SPACE,0,0);
//			m_fakeNote.setString(1);
//			renderNoteHead(gl,m_fakeNote);
//	
//			gl.glTranslatef(-STRING_SPACE,0,0);
//			m_fakeNote.setString(2);
//			renderNoteHead(gl,m_fakeNote);
//
//			gl.glPopMatrix();
//			
//			gl.glDisable(GL10.GL_COLOR_MATERIAL);
//			m_noteMesh.endRender(gl);
//			gl.glDisable(GL10.GL_CULL_FACE);
//			
//			gl.glEnable(GL10.GL_TEXTURE_2D);
//			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
//		}
//		
//		//gl.glDisable(GL10.GL_BLEND);
//		
//		if (true) return;		
		
		float beatPeriod=60000/m_bpm;
		float timeToUnits=BOARD_LENGTH_AHEAD/(beatPeriod*BEATS_AHEAD);
		
		gl.glBindTexture(GL10.GL_TEXTURE_2D,m_stringTexture);
		GLHelpers.beginDrawTextureXZ(gl);
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			int color=Config.getStringColor(string);
			float x=STRING_SPACE*(Song.STRING_COUNT-1-2*string)/2;

			EventList<NoteEvent> notes=m_song.getNoteEvents(string);
			long range=notes.range(
				m_position-beatPeriod*SADDLE_OFFSET/BOARD_LENGTH_AHEAD*BEATS_AHEAD,
				m_position+beatPeriod*BEATS_AHEAD);
			int rangeBegin=EventList.rangeBegin(range);
			int rangeEnd=EventList.rangeEnd(range);
			for (int i=rangeBegin;i!=rangeEnd;++i) {
				NoteEvent note=notes.get(i);
				float z=SADDLE_OFFSET+(note.getTime()-m_position)*timeToUnits;
				boolean behindSaddle=(z<=SADDLE_OFFSET);
				float length=note.getLength()*timeToUnits;
				
				gl.glPushMatrix();
				gl.glTranslatef(x,0,z);
				gl.glScalef(NOTE_TAIL_WIDTH,1,length);
				gl.glTranslatef(0,0,0.5f);
				{
					float factor=(!behindSaddle || note.isUnpicked())?
						1.0f:
						NOTE_SLIP_COLOR_FACTOR;
					GLHelpers.setColor(gl,color,m_readiness*factor);
				}
				GLHelpers.doDrawTextureXZ(gl);
				gl.glPopMatrix();

				if (behindSaddle && note.isPicked()) {
					gl.glPushMatrix();
					gl.glTranslatef(x,0,z);
					float position=m_position+note.getString()*note.getTime();
					drawWaveform(gl,length,position*timeToUnits,color);
					gl.glPopMatrix();
					gl.glBindTexture(GL10.GL_TEXTURE_2D,m_stringTexture);
					GLHelpers.beginDrawTextureXZ(gl);
				}
			}
		}
		
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glFrontFace(GL10.GL_CCW);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);
		m_noteMesh.beginRender(gl);
		
		gl.glPushMatrix();
		gl.glScalef(NOTE_HEAD_SIZE,NOTE_HEAD_SIZE,NOTE_HEAD_SIZE);

		float lastX=0;
		float lastZ=0;
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			GLHelpers.setColor(gl,Config.getStringColor(string),m_readiness);
			float x=STRING_SPACE*(Song.STRING_COUNT-1-2*string)/2;

			EventList<NoteEvent> notes=m_song.getNoteEvents(string);
			long range=notes.range(
				m_position-beatPeriod*SADDLE_OFFSET/BOARD_LENGTH_AHEAD*BEATS_AHEAD,
				m_position+beatPeriod*BEATS_AHEAD);
			int rangeBegin=EventList.rangeBegin(range);
			int rangeEnd=EventList.rangeEnd(range);
			for (int i=rangeBegin;i!=rangeEnd;++i) {
				NoteEvent note=notes.get(i);
				float z=SADDLE_OFFSET+(note.getTime()-m_position)*timeToUnits;
				if (z<=SADDLE_OFFSET) {
					continue;
				}
				//gl.glPushMatrix();
				gl.glTranslatef((x-lastX)/NOTE_HEAD_SIZE,0,(z-lastZ)/NOTE_HEAD_SIZE);
				//gl.glScalef(NOTE_HEAD_SIZE,NOTE_HEAD_SIZE,NOTE_HEAD_SIZE);
				m_noteMesh.renderGeometry(gl);
				lastX=x;
				lastZ=z;
			}
		}

		gl.glPopMatrix();

		m_noteMesh.endRender(gl);
		gl.glDisable(GL10.GL_COLOR_MATERIAL);
		gl.glDisable(GL10.GL_CULL_FACE);
		
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
	}
	
//	private void renderNoteHead(GL10 gl,NoteEvent note) {
//		int color=Config.getStringColor(note.getString());
//		gl.glPushMatrix();
//		gl.glScalef(NOTE_HEAD_SIZE,NOTE_HEAD_SIZE,NOTE_HEAD_SIZE);
//		GLHelpers.setColor(gl,color,m_readiness);
//		m_noteMesh.render(gl);
//		gl.glPopMatrix();
//	}
//
//	private void renderNoteTail(GL10 gl,float length,NoteEvent note,boolean behindSaddle) {
//		float beatPeriod=60000/m_bpm;
//		float timeToUnits=BOARD_LENGTH_AHEAD/(beatPeriod*BEATS_AHEAD);
//		int color=Config.getStringColor(note.getString());
//		
//		gl.glPushMatrix();
//		gl.glScalef(NOTE_TAIL_WIDTH,1,length);
//		gl.glTranslatef(0,0,0.5f);
//		{
//			float factor=(!behindSaddle || note.isUnpicked())?
//				1.0f:
//				NOTE_SLIP_COLOR_FACTOR;
//			GLHelpers.setColor(gl,color,m_readiness*factor);
//		}
//		gl.glBindTexture(GL10.GL_TEXTURE_2D,m_stringTexture);
//		GLHelpers.drawTextureXZ(gl);
//		gl.glPopMatrix();
//
//		if (behindSaddle && note.isPicked()) {
//			float uniquePosition=m_position+note.getString()*note.getTime();
//			drawWaveform(gl,length,uniquePosition*2*timeToUnits,color);
//		}
//	}
	
	/////////////////////////////////// waveform
	
	private void loadWaveformResources(Context context,GL10 gl) throws IOException {
		m_waveformTexture=GLHelpers.loadTexture(gl,context,R.drawable.waveform);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_WRAP_S,GL10.GL_REPEAT);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_WRAP_T,GL10.GL_REPEAT);
		
		if (m_waveformVertices==null) {
			ByteBuffer vertices=GLHelpers.allocateFloatBuffer(WAVEFORM_POINTS*3);
			vertices.
				putFloat(+WAVEFORM_HEAD_WIDTH/2).putFloat(0).putFloat(0).
				putFloat(-WAVEFORM_HEAD_WIDTH/2).putFloat(0).putFloat(0).
				putFloat(+0.5f).putFloat(0).putFloat(WAVEFORM_HEAD_HEIGHT).
				putFloat(-0.5f).putFloat(0).putFloat(WAVEFORM_HEAD_HEIGHT).
				putFloat(+0.5f).putFloat(0).putFloat(1-WAVEFORM_TAIL_HEIGHT).
				putFloat(-0.5f).putFloat(0).putFloat(1-WAVEFORM_TAIL_HEIGHT).
				putFloat(+WAVEFORM_TAIL_WIDTH/2).putFloat(0).putFloat(1).
				putFloat(-WAVEFORM_TAIL_WIDTH/2).putFloat(0).putFloat(1);
			
			ByteBuffer texcoords=GLHelpers.allocateFloatBuffer(WAVEFORM_POINTS*2);
			{
				vertices.position(0);
				for (int i=0;i!=WAVEFORM_POINTS;++i) {
					float x=vertices.getFloat();
					/*float y=*/vertices.getFloat();
					float z=vertices.getFloat();
					texcoords.putFloat(-x+0.5f);
					texcoords.putFloat(z);
				}
			}
			m_waveformVertices=GLBufferObject.createVertices(3,GL10.GL_FLOAT,vertices);
			m_waveformTexcoords=GLBufferObject.createTexcoords(2,GL10.GL_FLOAT,texcoords);
		}
		m_waveformVertices.bind(gl);
		m_waveformTexcoords.bind(gl);
	}
	
	private void unloadWaveformResources(GL10 gl) {
		if (m_waveformTexture!=0) {
			if (gl!=null) {
				GLHelpers.deleteTexture(gl,m_waveformTexture);
			}
			m_waveformTexture=0;
		}
		m_waveformVertices.unbind(gl);
		m_waveformTexcoords.unbind(gl);
	}
	
	private void drawWaveform(GL10 gl,float length,float position,int color) {
		position*=WAVEFORM_POSITION_SPEEDUP;
		gl.glBlendFunc(GL10.GL_SRC_ALPHA,GL10.GL_ONE);
		gl.glBindTexture(GL10.GL_TEXTURE_2D,m_waveformTexture);
		gl.glPushMatrix();
		
		gl.glMatrixMode(GL10.GL_TEXTURE);
		gl.glTranslatef(0,-position/WAVEFORM_LENGTH,0);
		gl.glScalef(1,length/WAVEFORM_LENGTH_FACTOR,1);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		
		gl.glScalef(WAVEFORM_WIDTH_SCALE_1,1,length);
		GLHelpers.setColor(gl,color,m_readiness);
		m_waveformTexcoords.set(gl);
		m_waveformVertices.set(gl);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP,0,WAVEFORM_POINTS);
		
		gl.glScalef(WAVEFORM_WIDTH_SCALE_2,1,1);
		GLHelpers.setColor(gl,0xFFFFFFFF,1);
		m_waveformTexcoords.set(gl);
		m_waveformVertices.set(gl);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP,0,WAVEFORM_POINTS);
		
		gl.glMatrixMode(GL10.GL_TEXTURE);
		gl.glLoadMatrixf(GLHelpers.IDENTITY_MATRIX,0);
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glBlendFunc(GL10.GL_SRC_ALPHA,GL10.GL_ONE_MINUS_SRC_ALPHA);
	}
	
	/////////////////////////////////// data
	
	private Song m_song;
	private int m_position;
	private float m_bpm;
	
	private int m_activeStrings;
	private float m_readiness;
	
	private GLRect m_viewport;
	private float[] m_projectionMatrix=new float[16];
	private float[] m_modelviewMatrix=new float[16];
	
	private float[] m_lookEye=new float[]{0,3,-1};
	private float[] m_lookCenter=new float[]{0,0,4};
	
	private int m_stringTexture;
	private int m_barTexture;
	
	private Mesh m_noteMesh;

	private int m_waveformTexture;
	private GLBufferObject m_waveformVertices;
	private GLBufferObject m_waveformTexcoords;
	
	/////////////////////////////////// constants
	
	private static final int FOV_Y=60;
	
	private static final int BEATS_AHEAD=5;
	
	private static final float SADDLE_OFFSET=0f;
	private static final float BOARD_LENGTH=14.0f;
	private static final float BOARD_LENGTH_AHEAD=BOARD_LENGTH-SADDLE_OFFSET;
	
	private static final float STRING_WIDTH=0.05f;
	private static final float STRING_SPACE=1.0f;
	
	private static final float BAR_WIDTH=0.05f;

	private static final float[] FOG_COLOR=new float[]{0.1f,0.1f,0.1f,1};
	
	private static final float 
		NOTE_SLIP_COLOR_FACTOR	=0.3f,
		NOTE_HEAD_SIZE			=0.25f,
		NOTE_TAIL_WIDTH			=0.15f;

	private static final float
		WAVEFORM_LENGTH			=20f,
		WAVEFORM_LENGTH_FACTOR	=WAVEFORM_LENGTH*0.9f,
		WAVEFORM_WIDTH_SCALE_1	=2.0f,
		WAVEFORM_WIDTH_SCALE_2	=0.3f,
		WAVEFORM_POSITION_SPEEDUP=2;
	
	private static final int 
		WAVEFORM_POINTS			=8;
	private static final float 
		WAVEFORM_HEAD_WIDTH		=1,
		WAVEFORM_HEAD_HEIGHT	=0.1f,
		WAVEFORM_TAIL_WIDTH		=NOTE_TAIL_WIDTH,
		WAVEFORM_TAIL_HEIGHT	=0.7f;
	
}
