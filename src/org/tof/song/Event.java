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
package org.tof.song;

public class Event {
	public Event(float time,float endTime) {
		m_time=time;
		m_endTime=endTime;
	}
	
	public final float getTime() {
		return m_time;
	}
	
	public final float getLength() {
		return m_endTime-m_time;
	}
	
	public final float getEndTime() { 
		return m_endTime;
	}
	
	private float m_time;
	private float m_endTime;
}
