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

public class CharString implements CharSequence {
	
	public CharString(int bufferCapacity) {
		m_buffer=new char[bufferCapacity];
	}

	public char charAt(int index) {
		if (index<0 || index>=m_length) {
			throw new IndexOutOfBoundsException(""+index);
		}
		return m_buffer[index];
	}

	public int length() {
		return m_length;
	}

	public CharSequence subSequence(int start,int end) {
		if (start<0 || end<0 ||
			start>=m_length || end>=m_length ||
			start>end)
		{
			throw new IndexOutOfBoundsException(start+","+end);
		}
		char[] copy=new char[end-start];
		System.arraycopy(m_buffer,start,copy,0,end-start);
		return new CharString(copy,end-start);
	}

	public String toString() {
		return new String(m_buffer,0,m_length);
	}
	
	public void clear() {
		m_length=0;
	}
	
	public CharString append(char value) {
		growBuffer(1);
		m_buffer[m_length++]=value;
		return this;
	}
	
	public CharString append(int value) {
		if (value==0) {
			return append('0');
		}
		if (value==Integer.MIN_VALUE) {
			return append("-2147483648");
		}
		growBuffer(11);
		char[] buffer=m_buffer;
		int out=m_length;
		if (value<0) {
			buffer[out++]='-';
			value=-value;
		}
		boolean skipZeroes=true;
		for (int divider=1000000000;divider!=0;divider/=10) {
			int digit=(value/divider);
			if (digit==0 && skipZeroes) {
				continue;
			}
			skipZeroes=false;
			buffer[out++]=(char)(digit+'0');
			value=(value%divider);
		}
		m_length=out;
		return this;		
	}
	
	public CharString append(CharSequence value) {
		growBuffer(value.length());
		char[] buffer=m_buffer;
		int out=m_length;
		for (int i=0,e=value.length();i!=e;++i) {
			buffer[out++]=value.charAt(i);
		}
		m_length=out;
		return this;
	}
	
	///////////////////////////////////////////// implementation
	
	private CharString(char[] buffer,int length) {
		m_buffer=buffer;
		m_length=length;
	}
	
	private void growBuffer(int delta) {
		if ((m_buffer.length-m_length)>=delta) {
			return;
		}
		char[] copy=new char[(m_length+delta)*2];
		System.arraycopy(m_buffer,0,copy,0,m_length);
		m_buffer=copy;
	}
	
	/////////////////////////////////// data
	
	private char[] m_buffer;
	private int m_length;
}
