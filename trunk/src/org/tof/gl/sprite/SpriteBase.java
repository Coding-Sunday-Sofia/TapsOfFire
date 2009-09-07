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

public class SpriteBase extends CenterScale {

	public float getAngle() {
		return m_angle;
	}
	public void setAngle(float angle) {
		m_angle=angle;
	}
	public void rotate(float dAngle) {
		m_angle+=dAngle;
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
	
	///////////////////////////////////////////// implementation

	/*package*/ void reset() {
		setCenter(0,0);
		setScale(1,1);
		setAngle(0);
	}
	
	/*package*/ float m_width;
	/*package*/ float m_height;
	/*package*/ float m_angle;
}
