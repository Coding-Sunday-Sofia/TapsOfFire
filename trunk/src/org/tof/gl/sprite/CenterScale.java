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

class CenterScale {
	
	public float getCenterX() {
		return m_centerX;
	}
	public float getCenterY() {
		return m_centerY;
	}
	public void setCenter(float x,float y) {
		m_centerX=x;
		m_centerY=y;
	}
	public void translateCenter(float dx,float dy) {
		m_centerX+=dx;
		m_centerY+=dy;
	}
	
	public float getScaleX() {
		return m_scaleX;
	}
	public float getScaleY() {
		return m_scaleY;
	}
	public void setScale(float scaleX,float scaleY) {
		m_scaleX=scaleX;
		m_scaleY=scaleY;
	}
	public void scale(float scaleX,float scaleY) {
		m_scaleX*=scaleX;
		m_scaleY*=scaleY;
	}
	public void setScale(float scale) {
		setScale(scale,scale);
	}
	public void scale(float scale) {
		scale(scale,scale);
	}
	
	///////////////////////////////////////////// implementation
	
	/*package*/ float m_centerX=0;
	/*package*/ float m_centerY=0;
	/*package*/ float m_scaleX=1;
	/*package*/ float m_scaleY=1;
}
