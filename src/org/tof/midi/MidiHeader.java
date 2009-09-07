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
package org.tof.midi;

public class MidiHeader {

	public MidiHeader(int type,float divisionType,int resolution,int tracks) {
		this.type=type;
		this.divisionType=divisionType;
		this.resolution=resolution;
		this.tracks=tracks;
	}
	
	public final int tracks;
	public final int type;
	public final float divisionType;
	public final int resolution;
}
