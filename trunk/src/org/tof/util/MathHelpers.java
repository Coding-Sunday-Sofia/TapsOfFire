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
package org.tof.util;

public class MathHelpers {

	public static final float PI=3.141592653589793f;
	public static final float DEGREES_TO_RADIANS=PI/180;

    public static int roundUpPower2(int x) {
        x=x-1;
		x|=(x>>1);
		x|=(x>>2);
		x|=(x>>4);
		x|=(x>>8);
		x|=(x>>16);
		return x+1;
    }
    
    public static int round(float value) {
    	if (value>0) {
    		return (int)(value+0.5f);
    	} else {
    		return (int)(value-0.5f);
    	}
    }
    
	public static int float2fixed(float value) {
		return (int)(value*65536);
	}
    
}
