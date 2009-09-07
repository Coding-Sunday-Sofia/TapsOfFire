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
package org.tof.gl.sprite;

import java.io.IOException;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;
import org.tof.gl.GLHelpers;
import org.tof.util.MathHelpers;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.FloatMath;

public class Sprite extends CenterScale {
	
	public Sprite() {
		resetState();
	}
	public Sprite(Context context,GL10 gl,int resource)
		throws IOException
	{
		resetState();
		create(context,gl,resource);
	}
	public Sprite(Context context,GL10 gl,String assetPath)
		throws IOException
	{
		resetState();
		create(context,gl,assetPath);
	}
	public Sprite(GL10 gl,Bitmap bitmap,boolean recycleBitmap) {
		resetState();
		create(gl,bitmap,recycleBitmap);
	}
	
	public void create(Context context,GL10 gl,int resource)
		throws IOException
	{
		destroy(gl);
		Bitmap bitmap=GLHelpers.loadBitmap(context,resource);
		m_width=bitmap.getWidth();
		m_height=bitmap.getHeight();
		m_texture=GLHelpers.loadTexture(gl,bitmap);
		bitmap.recycle();
		m_created=true;
	}
	public void create(Context context,GL10 gl,String assetPath)
		throws IOException
	{
		destroy(gl);
		Bitmap bitmap=GLHelpers.loadBitmap(context,assetPath);
		create(gl,bitmap,true);
	}
	public void create(GL10 gl,Bitmap bitmap,boolean recycleBitmap) {
		destroy(gl);
		m_width=bitmap.getWidth();
		m_height=bitmap.getHeight();
		m_texture=GLHelpers.loadTexture(gl,bitmap);
		m_created=true;
		if (recycleBitmap) {
			bitmap.recycle();
		}
	}
	
	public void destroy(GL10 gl) {
		if (!m_created) {
			return;
		}
		if (gl!=null) {
			GLHelpers.deleteTexture(gl,m_texture);
		}
		resetState();
	}
	
	public float getAngle() {
		return m_angle;
	}
	public void setAngle(float angle) {
		m_angle=angle;
	}
	public void rotate(float dAngle) {
		m_angle+=dAngle;
	}
	public void applyCenterRotation() {
		if ((m_angle % 360)==0) {
			return;
		}
		float sinA=FloatMath.sin(m_angle*MathHelpers.DEGREES_TO_RADIANS);
		float cosA=FloatMath.cos(m_angle*MathHelpers.DEGREES_TO_RADIANS);
		float w=getWidth();
		float h=getHeight();
		float dx=w/2-(w/2*cosA-h/2*sinA);
		float dy=h/2-(h/2*cosA+w/2*sinA);
		translateCenter(dx,dy);
	}
	
	public float getUnscaledWidth() {
		return m_width;
	}
	public float getUnscaledHeight() {
		return m_height;
	}
	
	public float getWidth() {
		return m_width*m_scaleX;
	}
	public float getHeight() {
		return m_height*m_scaleY;
	}
	
	public void render(GL10 gl) {
		checkCreated();
		gl.glBindTexture(GL10.GL_TEXTURE_2D,m_texture);
		if (m_angle==0 && GLHelpers.hasDrawTexture(gl)) {
			m_cropRect[0]=0;
			m_cropRect[1]=m_height;
			m_cropRect[2]=m_width;
			m_cropRect[3]=-m_height;
			float scaledWidth=m_scaleX*m_width;
			float scaledHeight=m_scaleY*m_height;
            ((GL11)gl).glTexParameterfv(
            	GL10.GL_TEXTURE_2D,
            	GL11Ext.GL_TEXTURE_CROP_RECT_OES,m_cropRect,0);
           	((GL11Ext)gl).glDrawTexfOES(
           		m_centerX-scaledWidth/2,m_centerY-scaledHeight/2,0,
           		scaledWidth,scaledHeight);
		} else {
			gl.glPushMatrix();
			gl.glTranslatef(m_centerX,m_centerY,0);
			gl.glRotatef(m_angle,0,0,1);
			gl.glScalef(m_scaleX*m_width,m_scaleY*m_height,1);
			GLHelpers.drawTextureXY(gl);
			gl.glPopMatrix();
		}
	}
	
	public void renderRegion(GL10 gl,float rx,float ry,float rw,float rh) {
		checkCreated();
		gl.glBindTexture(GL10.GL_TEXTURE_2D,m_texture);
		if (m_angle==0 && GLHelpers.hasDrawTexture(gl)) {
			m_cropRect[0]=rx;
			m_cropRect[1]=rh-ry;
			m_cropRect[2]=rw;
			m_cropRect[3]=-rh;
			float scaledWidth=m_scaleX*rw;
			float scaledHeight=m_scaleY*rh;
            ((GL11)gl).glTexParameterfv(
            	GL10.GL_TEXTURE_2D,
            	GL11Ext.GL_TEXTURE_CROP_RECT_OES,m_cropRect,0);
           	((GL11Ext)gl).glDrawTexfOES(
           		m_centerX-scaledWidth/2,m_centerY-scaledHeight/2,0,
           		scaledWidth,scaledHeight);
		} else {
			gl.glPushMatrix();
			gl.glTranslatef(m_centerX,m_centerY,0);
			gl.glRotatef(m_angle,0,0,1);
			gl.glScalef(m_scaleX*rw,m_scaleY*rh,1);
			{
				gl.glMatrixMode(GL10.GL_TEXTURE);
				gl.glTranslatef(rx/m_width,ry/m_height,0);
				gl.glScalef(rw/m_width,rh/m_height,1);
				
				GLHelpers.drawTextureXY(gl);
				
				gl.glLoadMatrixf(GLHelpers.IDENTITY_MATRIX,0);
				gl.glMatrixMode(GL10.GL_MODELVIEW);
			}
			gl.glPopMatrix();
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private void resetState() {
		m_created=false;
		setCenter(0,0);
		setScale(1,1);
		m_angle=0;
	}
	
	private void checkCreated() {
		if (!m_created) {
			throw new IllegalStateException("Sprite is not created.");
		}
	}
		
	
	/////////////////////////////////// data
	
	private boolean m_created;
	
	private int m_texture;
	private float m_width;
	private float m_height;
	private float m_angle;
	private float[] m_cropRect=new float[4];
}
