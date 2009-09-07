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

import javax.microedition.khronos.opengles.GL10;

public class SpriteRegion extends SpriteBase {
	
	public SpriteRegion() {
		detach();
	}
	
	public SpriteRegion(Sprite sprite,float rx,float ry,float rw,float rh) {
		attach(sprite,rx,ry,rw,rh);
	}
	
	public void attach(Sprite sprite,float rx,float ry,float rw,float rh) {
		detach();
		m_sprite=sprite;
		m_rx=rx;
		m_ry=ry;
		m_width=rw;
		m_height=rh;
	}
	
	public void detach() {
		reset();
		m_rx=0;
		m_ry=0;
		m_sprite=null;
	}
	
	public void destroy(GL10 gl) {
		if (m_sprite!=null) {
			m_sprite.destroy(gl);
			m_sprite=null;
		}
		detach();
	}

	public Sprite getSprite() {
		return m_sprite;
	}
	
	public void render(GL10 gl) {
		if (m_sprite==null) {
			return;
		}
		m_sprite.setCenter(m_centerX,m_centerY);
		m_sprite.setScale(m_scaleX,m_scaleY);
		m_sprite.setAngle(m_angle);
		m_sprite.renderRegion(gl,m_rx,m_ry,m_width,m_height);
	}
	
	///////////////////////////////////////////// implementation
	
	private Sprite m_sprite;
	private float m_rx;
	private float m_ry;
}
