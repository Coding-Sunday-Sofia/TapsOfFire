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

public abstract class EventList<E extends Event>  {

	public abstract int count();
	public abstract E get(int index);
	
	public abstract int lowerBound(float time);
	public abstract int upperBound(float time);
	public abstract long range(float time,float endTime);
	
	public static int rangeBegin(long range) {
		return (int)(range>>>32);
	}
	public static int rangeEnd(long range) {
		return (int)(range);
	}
	public static boolean rangeEmpty(long range) {
		return rangeBegin(range)==rangeEnd(range);
	}
	public static long rangeMake(int begin,int end) {
		return ((long)begin<<32) | end;
	}
}
