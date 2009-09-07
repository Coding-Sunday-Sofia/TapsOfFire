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
import android.graphics.RectF;

public class SpriteFont extends CenterScale {
	
	public void destroy(GL10 gl) {
		m_sprite.destroy(gl);
	}
	
	public float getHeight() {
		return m_height*m_scaleY;
	}
	
	public float getLeading() {
		return m_leading*m_scaleY;
	}
	
	public float getUnscaledHeight() {
		return m_height;
	}
	
	public float getUnscaledLeading() {
		return m_leading;
	}
	
	public float measureUnscaledWidth(CharSequence text) {
		float width=0;
		for (int i=0,e=text.length();i!=e;++i) {
			int charIndex=SpriteFontBuilder.getIndex(text.charAt(i));
			if (charIndex==-1) {
				continue;
			}
			width+=m_charRects[charIndex].width();
		}
		return width;
	}
	
	public float measureWidth(CharSequence text) {
		return measureUnscaledWidth(text)*m_scaleX;
	}
	
	public void render(GL10 gl,CharSequence text) {
		m_sprite.setCenter(m_centerX,m_centerY);
		m_sprite.translateCenter(-measureWidth(text)/2,0);
		m_sprite.setScale(m_scaleX,m_scaleY);
		for (int i=0,e=text.length();i!=e;++i) {
			int charIndex=SpriteFontBuilder.getIndex(text.charAt(i));
			if (charIndex==-1) {
				continue;
			}
			RectF charRect=m_charRects[charIndex];
			if (charRect==null) {
				continue;
			}
			float dx=charRect.width()*m_scaleX/2;
			m_sprite.translateCenter(dx,0);
			m_sprite.renderRegion(
				gl,
				charRect.left,charRect.top,
				charRect.width(),charRect.height());
			m_sprite.translateCenter(dx,0);
		}
	}
	
	///////////////////////////////////////////// implementation
	
	/*package*/ SpriteFont() {
	}
    
    /*package*/ Sprite m_sprite;
    /*package*/ float m_height;
    /*package*/ float m_leading;
    /*package*/ RectF[] m_charRects;
}
