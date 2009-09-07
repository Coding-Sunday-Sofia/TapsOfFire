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
package org.tof.gl.mesh;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;

class Util {
	public static String getColonValue(String string) {
		int colon=string.indexOf(':');
		if (colon==-1) {
			return "";
		} else {
			return string.substring(colon+1).trim();
		}
	}
	
	public static int parseInt(String value) throws IOException {
		try {
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException e) {
			throw new IOException(
				String.format("'%s' is not an integer.",value)
			);
		}
	}

	public static float parseFloat(String value) throws IOException {
		try {
			return Float.parseFloat(value.trim());
		}
		catch (NumberFormatException e) {
			throw new IOException(
				String.format("'%s' is not a float.",value)
			);
		}
	}
	
	public static float[] parseFloatArray(int count,String array) throws IOException {
		String[] values=array.split("\\,");
		if (values.length!=count) {
			throw new IOException(
				String.format("'%s' must contain exactly %d values.",array,count)
			);
		}
		float[] result=new float[count];
		for (int i=0;i!=count;++i) {
			result[i]=parseFloat(values[i]);
		}
		return result;
	}
	
	public static String nextLine(BufferedReader reader) throws IOException {
		String line=reader.readLine();
		if (line==null) {
			throw new EOFException();
		}
		return line;
	}
}
