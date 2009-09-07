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

public class GLRect {
	
	public GLRect() {
		this(0,0,0,0);
	}
	public GLRect(float x,float y,float width,float height) {
		this.x=x;
		this.y=y;
		this.width=width;
		this.height=height;
	}
	public GLRect(GLRect other) {
		this.x=other.x;
		this.y=other.y;
		this.width=other.width;
		this.height=other.height;
	}
	
	public float centerX() {
		return this.x+this.width/2;
	}
	public float centerY() {
		return this.y+this.height/2;
	}
	
	public float x;
	public float y;
	public float width;
	public float height;
}
