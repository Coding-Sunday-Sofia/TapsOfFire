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
package org.tof.player;

import java.io.File;
import java.io.IOException;

public class VorbisDecoder implements PCMDecoder {
	
	public VorbisDecoder() {
	}
	
	public VorbisDecoder(File file) throws IOException {
		open(file);
	}
	
	public boolean isOpened() {
		return m_nativeInstance!=0;
	}
	
	public void open(File file) throws IOException {
		close();
		handleOpenResult(file,nativeOpen(file.getPath()));
		m_file=file;
	}
	
	public void close() {
		nativeClose();
		m_file=null;
	}
	
	public boolean isSeekable() {
		return nativeIsSeekable();
	}
	
	public int getRate() {
		return nativeGetRate();
	}
	
	public int getChannels() {
		return nativeGetChannels();
	}
	
	public int getTimeLength() {
		return nativeGetTimeLength();
	}
	
	public int getTimePosition() {
		return nativeGetTimePosition();
	}
	
	public void seekToTime(int time) throws IOException {
		handleError("seek",nativeSeekToTime(time));
	}
	
	public int read(byte[] buffer,int offset,int length) throws IOException {
		if (buffer==null || offset<0 || length<0 ||
			(offset+length)>buffer.length)
		{
			throw new IllegalArgumentException();
		}
		int result=nativeRead(buffer,offset,length);
		if (result==0) {
			return -1;
		}
		if (result<0) {
			handleError("decode",result);
		}
		return result;
	}
	
	///////////////////////////////////////////// implementation
	
	protected void finalize() {
		nativeClose();
	}
	
	private static void handleOpenResult(File file,int result) throws IOException {
		if (result!=0) {
			throw new IOException(
				String.format("Failed to open '%s' (%d).",file,result)
			);
		}
	}
	
	private void handleError(String what,int result) throws IOException {
		if (result==0) {
			return;
		}
		throw new IOException(
			String.format("Failed to %s '%s' (%d).",what,m_file,result)
		);
	}
	
	private native int nativeOpen(String path);
	private native void nativeClose();
	private native boolean nativeIsSeekable();
	private native int nativeGetRate();
	private native int nativeGetChannels();
	private native int nativeGetTimeLength();
	private native int nativeGetTimePosition();
	private native int nativeSeekToTime(int time);
	private native int nativeRead(byte[] buffer,int offset,int length);
	private static native void nativeStaticSetup();
	
	/////////////////////////////////// data
	
	int m_nativeInstance;
	private File m_file;
	
	static { 
		System.loadLibrary("tof");
		nativeStaticSetup();
	}
}
